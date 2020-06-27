package io.scer.pocketmine.server

import java.io.*
import java.nio.charset.Charset

val availableStats = listOf("PocketMine-MP", "Online", "Memory", "U", "TPS", "Load")

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
            stdin!!.write((command + "\r\n").toByteArray())
            stdin!!.flush()
        } catch (ignored: Exception) {}
    }

    fun kill() {
        if (!isRunning) {
            ServerBus.Log.message("> Server is not running")
            return
        }
        try {
            process?.destroy()
            process = null
            ServerBus.Log.message("> Server killed\n")
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
        val binary = File(files.appDirectory.toString() + "/php")
        binary.setExecutable(true, true)
        val builder = ProcessBuilder(
                files.php.toString(), "-c",
                files.settingsFile.toString(),
                files.phar.toString(),
                "--no-wizard",
                "--settings.enable-dev-builds=1",
                "--enable-ansi",
                "--console.title-tick=1"
        )
                .redirectErrorStream(true)
                .directory(files.dataDirectory)
        builder.environment()["TMPDIR"] = files.dataDirectory.toString() + "/tmp"
        try {
            process = builder.start()
            stdout = process!!.inputStream
            stdin = process!!.outputStream
            Thread {
                val reader = BufferedReader(InputStreamReader(stdout!!, Charset.forName("UTF-8")))
                ServerBus.publish(StartEvent())
                while (isRunning) {
                    try {
                        val buffer = CharArray(8192)
                        var size: Int
                        do {
                            size = reader.read(buffer, 0, buffer.size)
                            var stringBuilder = StringBuilder()
                            for (i in 0 until size) {
                                val character = buffer[i]
                                if (character == '\r') { }
                                else if (character == '\n' || character == '\u0007') {
                                    val line = stringBuilder.toString()
                                    if (character == '\u0007' && line.startsWith("\u001B]0;")) {
                                        val statsArray = line.substring(4).split(" | ")
                                        ServerBus.publish(UpdateStatEvent(associateStats(statsArray)))
                                    } else {
                                        ServerBus.Log.message(line + "\n")
                                    }
                                    stringBuilder = StringBuilder()
                                } else {
                                    stringBuilder.append(buffer[i])
                                }
                            }
                        } while (size != -1)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ServerBus.publish(ErrorEvent(e.message.toString(), Errors.UNKNOWN))
                    }
                }
                ServerBus.publish(StopEvent())
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
            ServerBus.publish(ErrorEvent(e.message.toString(), Errors.UNKNOWN))
            kill()
        }
    }

    private fun associateStats(stats: List<String>): HashMap<String, String> {
        val result = HashMap<String, String>()

        fun removeNameFromStat(fullStat: String, name: String): String {
            return fullStat.substring(fullStat.indexOf(name) + name.length)
        }

        stats.forEach { fullStat ->
            availableStats.forEach { statName ->
                if (fullStat.contains("$statName ")) {
                    result[statName] = removeNameFromStat(fullStat, "$statName ")
                }
            }
        }

        return result
    }

    class Files(
            val dataDirectory: File,
            val phar: File,
            val appDirectory: File,
            val php: File,
            val settingsFile: File,
            val serverSetting: File
    ) {
        val pharIsExist: Boolean
            get() = phar.exists()
    }
}