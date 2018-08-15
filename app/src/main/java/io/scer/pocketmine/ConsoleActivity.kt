package io.scer.pocketmine

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.ScrollView
import io.scer.pocketmine.server.Server
import kotlinx.android.synthetic.main.activity_console.*

class ConsoleActivity : AppCompatActivity(), Handler.Callback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_console)

        instance = this

        labelLog.textSize = MainActivity.fontSize.toFloat()

        send.setOnClickListener {
            Server.getInstance().sendCommand(editCommand.text.toString())
            editCommand.setText("")
        }

        labelLog.text = currentLog
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.console, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear -> {
                labelLog.text = ""
                currentLog = ""
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun handleMessage(message: Message): Boolean {
        return false
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var instance: ConsoleActivity? = null
        var currentLog = ""

        fun log(text: String) {
            currentLog += text
            if (instance != null) {
                instance!!.runOnUiThread {
                    instance!!.labelLog.append(text)
                    instance!!.scroll.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }
}