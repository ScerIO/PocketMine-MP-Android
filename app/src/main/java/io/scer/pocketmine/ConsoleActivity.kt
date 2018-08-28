package io.scer.pocketmine

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import io.scer.pocketmine.server.Server
import io.scer.pocketmine.utils.fromHtml
import io.scer.pocketmine.utils.toHTML
import kotlinx.android.synthetic.main.activity_console.*

class ConsoleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_console)

        instance = this

        labelLog.textSize = MainActivity.fontSize

        send.setOnClickListener {
            sendCommand()
        }
        editCommand.setOnKeyListener(View.OnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                sendCommand()
                return@OnKeyListener true
            }
            false
        })

        labelLog.text = currentLog
    }

    private fun sendCommand() {
        Server.getInstance().sendCommand(editCommand.text.toString())
        editCommand.text.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.console, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear -> {
                labelLog.text = ""
                currentLog.clear()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var instance: ConsoleActivity? = null
        var currentLog = SpannableStringBuilder()

        fun log(text: String) {
            currentLog.append(fromHtml(text.toHTML()))
            if (instance === null) return

            instance!!.runOnUiThread {
                instance!!.labelLog.text = currentLog
                instance!!.scroll.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
}