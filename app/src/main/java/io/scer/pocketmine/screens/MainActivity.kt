package io.scer.pocketmine.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import io.scer.pocketmine.R
import io.scer.pocketmine.screens.fragments.ConsoleFragment
import io.scer.pocketmine.screens.fragments.ServerFragment
import io.scer.pocketmine.screens.fragments.SettingsFragment
import io.scer.pocketmine.server.Server
import io.scer.pocketmine.utils.AsyncRequest
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class MainActivity : AppCompatActivity(), Handler.Callback, BottomNavigationView.OnNavigationItemSelectedListener {
    private var assemblies: HashMap<String, JSONObject>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navigation.setOnNavigationItemSelectedListener(this)

        val arch = System.getProperty("os.arch") ?: "7"
        if (!arch.contains("aarch64") && !arch.contains("8")) {
            AlertDialog.Builder(this).setMessage(getString(R.string.error_32bit)).setCancelable(false).setNegativeButton(getString(R.string.exit)) { _, _ -> finish() }.create().show()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            init()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET), 1)
        }

        val appDirectoryPath = applicationInfo.dataDir
        val externalDirectory = Environment.getExternalStorageDirectory().path + "/PocketMine-MP"
        Server.makeInstance(Server.Files(
                dataDirectory = File(Environment.getExternalStorageDirectory().path + "/PocketMine-MP"),
                phar = File(externalDirectory, "PocketMine-MP.phar"),
                appDirectory = File(externalDirectory),
                php = File(appDirectoryPath, "php"),
                killer = File(appDirectoryPath, "killall"),
                settingsFile = File(externalDirectory, "php.ini"),
                serverSetting = File(externalDirectory, "server.properties")
        ))

        try {
            for (path in arrayOf("php", "killall")) {
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

        if (savedInstanceState == null)
            replaceFragment(ServerFragment.newInstance())
    }

    private fun init() {
        assemblies = AsyncRequest().execute("stable", "beta", "development").get()
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
        if (assemblies != null && !Server.getInstance().isInstalled) {
            downloadPMBuild("stable")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_server -> replaceFragment(ServerFragment.newInstance())
            R.id.action_console -> replaceFragment(ConsoleFragment.newInstance())
            R.id.action_settings -> replaceFragment(SettingsFragment.newInstance())
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init()
            } else {
                Toast.makeText(this, R.string.not_enough_rights, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    /**
     * Replace fragment view
     * @param fragment - Fragment view
     */
    private fun replaceFragment(fragment: Fragment): Boolean {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.content, fragment)
                .commit()
        return true
    }

    @SuppressLint("TrustAllX509TrustManager")
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
            R.id.download -> downloadPM()
            R.id.kill -> Server.getInstance().kill()
        }
        return super.onOptionsItemSelected(item)
    }

    @Throws(IOException::class)
    private fun copyAsset(path: String): File {
        val file = File(Server.getInstance().files.appDirectory.toString() + "/" + path)
        applicationContext.assets.open(path).copyTo(FileOutputStream(file))
        return file
    }

    private fun downloadPM() {
        if (assemblies == null) {
            Snackbar.make(content, R.string.assemblies_error, Snackbar.LENGTH_LONG).show()
            return
        } else if (Server.getInstance().isRunning) {
            Server.getInstance().kill()
        }

        val builds = assemblies!!.keys.toTypedArray()

        AlertDialog.Builder(this)
                .setTitle(R.string.select_channel)
                .setItems(builds) { _, index ->
                    val channel = builds[index]
                    val json = assemblies!![channel]
                    try {
                        val view = layoutInflater.inflate(R.layout.build_info, null)

                        if (json!!.getBoolean("is_dev")) {
                            view.findViewById<TextView>(R.id.development_build).visibility = View.VISIBLE
                        }

                        view.findViewById<TextView>(R.id.api).text = json.getString("base_version")
                        view.findViewById<TextView>(R.id.build_number).text = json.getString("build_number")
                        view.findViewById<TextView>(R.id.branch).text = json.getString("branch")
                        view.findViewById<TextView>(R.id.game_version).text = json.getString("mcpe_version")

                        AlertDialog.Builder(this)
                                .setTitle(R.string.build_info)
                                .setView(view)
                                .setPositiveButton(R.string.download) { _, _ ->
                                    downloadPMBuild(channel)
                                }
                                .create().show()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }.create().show()
    }

    private fun downloadPMBuild(channel: String) {
        try {
            downloadFile(assemblies!![channel]!!.getString("download_url"), Server.getInstance().files.phar)
        } catch (e: Exception) {
            Snackbar.make(content, R.string.assemblies_error, Snackbar.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}