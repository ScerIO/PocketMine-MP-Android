package io.scer.pocketmine.server

import android.os.Environment
import io.scer.pocketmine.ConsoleActivity
import java.io.*
import java.nio.charset.Charset

class Server(dataDir: String) {
    var files: ServerFiles = ServerFiles(dataDir)
    private val handlers = ArrayList<ServerEventsHandler>()

    fun addEventListener(listener: ServerEventsHandler) {
        handlers.add(listener)
    }

    fun removeEventListener(listener: ServerEventsHandler) {
        handlers.remove(listener)
    }

    companion object {
        private var instance : Server? = null
        fun makeInstance(dataDir: String?): Server {
            if (instance == null)
                instance = Server(dataDir!!)

            return instance!!
        }
        fun getInstance(): Server {
            return instance!!
        }
    }

    private var process: Process? = null
    private var stdin: OutputStream? = null
    private var stdout: InputStream? = null

    private fun execCommand(command: String) {
        try {
            Runtime.getRuntime().exec(command)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendCommand(command: String) {
        try {
            if (!isRunning) return
            ConsoleActivity.log("> $command\n")
            stdin!!.write((command + "\n").toByteArray())
            stdin!!.flush()
        } catch (ignored: Exception) {}
    }

    fun kill() {
        execCommand( "${files.killer} php")
    }

    val isRunning: Boolean
        get() {
            if (process === null) return false

            try {
                process!!.exitValue()
            } catch (e: Exception) {
                return true
            }

            return false
        }

    val isInstalled: Boolean
        get() = files.pharIsExist

    fun run() {
        if (!isInstalled) {
            handlers.forEach { it.error(ServerError.PHAR_NOT_EXIST, null) }
        }
        val builder = ProcessBuilder(files.php.toString(), "-c", files.settingsFile.toString(), files.phar.toString(), "--no-wizard", "--settings.enable-dev-builds=1", "--enable-ansi", "--console.title-tick=0")
        builder.redirectErrorStream(true)
        builder.directory(files.dataDirectory)
        builder.environment()["TMPDIR"] = files.dataDirectory.toString() + "/tmp"
        try {
            process = builder.start()
            stdout = process!!.inputStream
            stdin = process!!.outputStream
            object : Thread() {
                override fun run() {
                    val reader = BufferedReader(InputStreamReader(stdout!!, Charset.forName("UTF-8")))
                    handlers.forEach { it.start() }
                    while (isRunning) {
                        try {
                            var line = reader.readLine()
                            while (line != null) {
                                ConsoleActivity.log(line + "\n")
                                line = reader.readLine()
                            }
                        } catch (e: Exception) {
                            handlers.forEach { it.error(ServerError.UNKNOWN, e.message.toString()) }
                            handlers.forEach { it.stop() }
                        }
                    }
                    handlers.forEach { it.stop() }
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
            handlers.forEach { it.error(ServerError.UNKNOWN, e.message.toString()) }
            kill()
        }
    }

    class ServerFiles(private val dataDir: String) {
        val dataDirectory: File
            get() = File(Environment.getExternalStorageDirectory().path + "/PocketMine-MP")

        val phar: File
            get() = File(dataDirectory, "PocketMine-MP.phar")

        val pharIsExist: Boolean
            get() = phar.exists()

        val appDirectory: File
            get() = File(dataDir)

        val php: File
            get() = File(appDirectory, "php")

        val killer: File
            get() = File(appDirectory, "killall")

        val settingsFile: File
            get() = File(dataDirectory, "php.ini")

        val serverSetting: File
            get() = File(dataDirectory, "server.properties")
    }
}