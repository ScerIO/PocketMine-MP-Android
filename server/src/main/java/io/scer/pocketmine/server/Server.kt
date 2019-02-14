package io.scer.pocketmine.server

import java.io.*
import java.nio.charset.Charset

class Server(val files: Files) {
    private var process: Process? = null
    private var stdin: OutputStream? = null
    private var stdout: InputStream? = null

    companion object {
        private var instance : Server? = null
        fun makeInstance(files: Files): Server {
            if (instance == null)
                instance = Server(files)

            return instance!!
        }
        fun getInstance(): Server {
            return instance!!
        }
    }

    private fun execCommand(command: String) {
        Runtime.getRuntime().exec(command)
    }

    fun sendCommand(command: String) {
        try {
            if (!isRunning) return
            ServerBus.Log.message("> $command\n")
            stdin!!.write((command + "\n").toByteArray())
            stdin!!.flush()
        } catch (ignored: Exception) {}
    }

    fun kill() {
        try {
            execCommand( "${files.killer} php")
        } catch (ignored: Exception) {}
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
            ServerBus.publish(ErrorEvent(null, Errors.PHAR_NOT_EXIST))
        }
        val builder = ProcessBuilder(
                files.php.toString(), "-c",
                files.settingsFile.toString(),
                files.phar.toString(),
                "--no-wizard",
                "--settings.enable-dev-builds=1",
                "--enable-ansi",
                "--console.title-tick=0"
        )
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
                    ServerBus.publish(StartEvent())
                    while (isRunning) {
                        try {
                            var line = reader.readLine()
                            while (line != null) {
                                ServerBus.Log.message(line + "\n")
                                line = reader.readLine()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ServerBus.publish(ErrorEvent(e.message.toString(), Errors.UNKNOWN))
                        }
                    }
                    ServerBus.publish(StopEvent())
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
            ServerBus.publish(ErrorEvent(e.message.toString(), Errors.UNKNOWN))
            kill()
        }
    }

    class Files(
            val dataDirectory: File,
            val phar: File,
            val appDirectory: File,
            val php: File,
            val killer: File,
            val settingsFile: File,
            val serverSetting: File
    ) {
        val pharIsExist: Boolean
            get() = phar.exists()
    }
}