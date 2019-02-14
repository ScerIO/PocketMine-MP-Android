package io.scer.pocketmine.utils

import android.os.AsyncTask
import org.json.JSONObject
import java.net.URL
import kotlin.collections.HashMap

class AsyncRequest : AsyncTask<String, Void, HashMap<String, JSONObject>?>() {
    override fun doInBackground(vararg channels: String): HashMap<String, JSONObject>? {
        val map = HashMap<String, JSONObject>()
        channels.forEach { channel ->
            try {
                val body = URL("https://update.pmmp.io/api?channel=$channel").readText()
                map[channel] = JSONObject(body)
            } catch (e: Exception) {
                // Network error or pmmp.io is down
            }
        }
        return map
    }
}