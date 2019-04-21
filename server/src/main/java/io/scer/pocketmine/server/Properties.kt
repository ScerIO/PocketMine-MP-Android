package io.scer.pocketmine.server

import java.lang.IndexOutOfBoundsException
import java.lang.String.valueOf
import java.text.SimpleDateFormat
import java.util.*

class Properties {
    private val config = LinkedHashMap<String, Any>()

    init {
        val content = Server.getInstance().files.serverSetting.readText(Charsets.UTF_8)
        for (line in content.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val property = line.split("=")
            try {
                val key = property[0]
                val value = property[1].trim()
                when (value.toLowerCase()) {
                    "on", "true", "yes" -> this.config[key] = true
                    "off", "false", "no" -> this.config[key] = false
                    else -> this.config[key] = value
                }
            } catch (e: IndexOutOfBoundsException) {
                continue
            }
        }
    }

    fun getMap(): LinkedHashMap<String, Any> = config

    fun get(key: String): Any? = config[key]

    fun set(key: String, value: Any) {
        config[key] = value
    }

    fun write() {
        var content = "#Properties Config file\r\n#" + SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault()).format(Date()) + "\r\n"
        config.forEach { entry ->
            val key = entry.key
            var value: Any = entry.value
            if (value is Boolean) {
                when (value) {
                    true -> value = "on"
                    false -> value = "off"
                }
            }
            content += valueOf(key) + "=" + valueOf(value) + "\r\n"
        }
        Server.getInstance().files.serverSetting.writeText(content)
    }
}