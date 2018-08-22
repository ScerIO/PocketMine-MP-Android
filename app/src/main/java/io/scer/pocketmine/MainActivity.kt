package io.scer.pocketmine

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import io.scer.pocketmine.server.Server
import io.scer.pocketmine.server.ServerError
import io.scer.pocketmine.server.ServerEventsHandler
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.*
import android.widget.ProgressBar
import java.net.URL
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@SuppressLint("StaticFieldLeak")
class MainActivity : AppCompatActivity(), Handler.Callback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Server.makeInstance(applicationInfo.dataDir)

        instance = this

        val arch = System.getProperty("os.arch")
        if (!arch.contains("aarch64") && !arch.contains("8")) {
            AlertDialog.Builder(this).setMessage(getString(R.string.error_32bit)).setCancelable(false).setNegativeButton(getString(R.string.exit)) { _, _ -> finish() }.create().show()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            init()
        } else {
            //ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET), 1)
        }

        try {
            val files = arrayOf("php", "killall")
            for (path in files) {
                val file = File(Server.getInstance().files.appDirectory.toString() + "/" + path)
                val lastModified = Date(
                        file.lastModified()
                )
                // 5 august 2018
                if (lastModified.time < 1533452138000L) {
                    file.delete()
                }
                if (!file.exists()) {
                    val targetFile = copyAsset(path)
                    targetFile.setExecutable(true, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val isStarted = Server.getInstance().isRunning
        toggleButtons(isStarted)

        start.setOnClickListener {
            service = Intent(this, ServerService::class.java)
            ContextCompat.startForegroundService(this, service!!)
        }

        stop.setOnClickListener {
            Server.getInstance().sendCommand("stop")
        }

        Server.getInstance().addEventListener(object: ServerEventsHandler {
            override fun error(type: ServerError, message: String?) {
                when(type) {
                    ServerError.PHAR_NOT_EXIST -> Snackbar.make(content, R.string.phar_does_not_exist, Snackbar.LENGTH_LONG).show()
                    ServerError.UNKNOWN -> Snackbar.make(content, "Error: $message", Snackbar.LENGTH_LONG).show()
                }
                stopService(service)
            }

            override fun stop() {
                runOnUiThread {
                    toggleButtons(false)
                }
                stopService(service)
            }

            override fun start() {
                runOnUiThread {
                    toggleButtons(true)
                }
            }
        })
    }

    fun toggleButtons(started: Boolean) {
        start.isEnabled = !started
        stop.isEnabled = started
    }

    private fun init() {
        File(Server.getInstance().files.dataDirectory, "tmp").mkdirs()
        val ini = Server.getInstance().files.settingsFile
        if (!ini.exists()) {
            try {
                ini.createNewFile()
                val stream = FileOutputStream(ini)
                stream.write("date.timezone=UTC\nshort_open_tag=0\nasp_tags=0\nphar.readonly=0\nphar.require_hash=1\nigbinary.compact_strings=0\nzend.assertions=-1\nerror_reporting=-1\ndisplay_errors=1\ndisplay_startup_errors=1\n".toByteArray(charset("UTF8")))
                stream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        if (!Server.getInstance().isInstalled) {
            downloadPM()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init()
            } else {
                Toast.makeText(this, "To make the application work correctly, you need grant access to the memory of your device!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    @SuppressLint("TrustAllX509TrustManager", "InflateParams")
    private fun downloadFile(url: String, file: File) {
        if (file.exists()) file.delete()
        val view = layoutInflater.inflate(R.layout.download, null)
        val download = view.findViewById<ProgressBar>(R.id.downloadingProgress)

        val builder = AlertDialog.Builder(this)
        builder
                .setTitle(getString(R.string.downloading).replace("%name%", file.name))
                .setCancelable(false)
                .setView(view)
        val dialog = builder.create()
        dialog.show()

        Thread(Runnable {
            val output: OutputStream
            val input: InputStream
            try {
                if (file.exists()) file.delete()
                val ssl = SSLContext.getInstance("SSL")
                ssl.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(p1: Array<X509Certificate>, p2: String) {}

                    override fun checkServerTrusted(p1: Array<X509Certificate>, p2: String) {}

                    override fun getAcceptedIssuers(): Array<X509Certificate>? {
                        return null
                    }
                }), java.security.SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(ssl.socketFactory)
                HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
                val req = URL(url)
                val connection = req.openConnection()
                connection.connect()
                input = BufferedInputStream(connection.getInputStream())
                output = FileOutputStream(file)
                var read: Long = 0
                val max = connection.contentLength.toLong()
                runOnUiThread { download.max = max.toInt() / 1024 }
                val buffer = ByteArray(4096)
                var count: Int = input.read(buffer)
                while (count >= 0) {
                    output.write(buffer, 0, count)
                    read += count.toLong()
                    val temp = (read / 1000).toInt()
                    runOnUiThread { download.progress = temp }
                    count = input.read(buffer)
                }
                output.close()
                input.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            runOnUiThread {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        }).start()
    }

    override fun handleMessage(message: Message): Boolean {
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.console -> startActivity(Intent(this, ConsoleActivity::class.java))
            R.id.download -> downloadPM()
            R.id.kill -> Server.getInstance().kill()
        }
        return super.onOptionsItemSelected(item)
    }

    @Throws(IOException::class)
    private fun copyAsset(path: String): File {
        val file = File(Server.getInstance().files.appDirectory.toString() + "/" + path)
        copyStream(MainActivity.instance!!.applicationContext.assets.open(path), FileOutputStream(file))
        return file
    }

    private fun downloadPM() {
        downloadFile("https://jenkins.pmmp.io/job/PocketMine-MP/lastSuccessfulBuild/artifact/PocketMine-MP.phar", Server.getInstance().files.phar)
    }

    companion object {

        var instance: MainActivity? = null
        private var service: Intent? = null
        var fontSize = 14

        @Throws(IOException::class)
        private fun copyStream(inputStream: InputStream, outputStream: OutputStream) {
            val buffer = ByteArray(8192)
            var cou: Int = inputStream.read(buffer)
            while (cou != -1) {
                outputStream.write(buffer, 0, cou)
                cou = inputStream.read(buffer)
            }
            inputStream.close()
            outputStream.close()
        }
    }
}