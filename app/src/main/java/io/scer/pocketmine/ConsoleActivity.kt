package io.scer.pocketmine

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.SpannableStringBuilder
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import io.scer.pocketmine.server.Server
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
            val html = Html.fromHtml(text.toHTML())
            currentLog.append(html)
            if (instance != null) {
                instance!!.runOnUiThread {
                    instance!!.labelLog.text = currentLog
                    instance!!.scroll.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }

        private fun splitKeepDelimiter(string: String, regex: Regex): MutableList<String> {
            val result = mutableListOf<String>()
            var start = 0
            regex.findAll(string).forEach {
                if (string.substring(start, it.range.first()).isNotEmpty()) {
                    result.add(string.substring(start, it.range.first()))
                }
                result.add(it.value)
                start = it.range.last() + 1
            }
            if (start != string.length) result.add(string.substring(start))
            return result
        }

        private fun String.toHTML(): String {
            val csi = "\u001B["
            val color = "${csi}38;5;"

            var tokens = 0
            val builder = StringBuilder()
            splitKeepDelimiter(this, Regex("\\u001B\\[[\\dm;]+")).forEach { token ->
                when (token) {
                    "${csi}1m" -> {
                        builder.append("<span style='font-weight:bold'>")
                        ++tokens
                    }
                    "${csi}3m" -> {
                        builder.append("<span style='font-style:italic'>")
                        ++tokens
                    }
                    "${csi}4m" -> {
                        builder.append("<span style='text-decoration:underline'>")
                        ++tokens
                    }
                    "${csi}9m" -> {
                        builder.append("<span style='text-decoration:line-through'>")
                        ++tokens
                    }
                    "${csi}m" -> {
                        builder.append("</span>".repeat(tokens))
                        tokens = 0
                    }

                    "${color}16m" -> {
                        builder.append("<span style='color:#000'>")
                        ++tokens
                    }
                    "${color}19m" -> {
                        builder.append("<span style='color:#00A'>")
                        ++tokens
                    }
                    "${color}34m" -> {
                        builder.append("<span style='color:#0A0'>")
                        ++tokens
                    }
                    "${color}37m" -> {
                        builder.append("<span style='color:#0AA'>")
                        ++tokens
                    }
                    "${color}124m" -> {
                        builder.append("<span style='color:#A00'>")
                        ++tokens
                    }
                    "${color}127m" -> {
                        builder.append("<span style='color:#A0A'>")
                        ++tokens
                    }
                    "${color}214m" -> {
                        builder.append("<span style='color:#FA0'>")
                        ++tokens
                    }
                    "${color}145m" -> {
                        builder.append("<span style='color:#AAA'>")
                        ++tokens
                    }
                    "${color}59m" -> {
                        builder.append("<span style='color:#555'>")
                        ++tokens
                    }
                    "${color}63m" -> {
                        builder.append("<span style='color:#55F'>")
                        ++tokens
                    }
                    "${color}83m" -> {
                        builder.append("<span style='color:#5F5'>")
                        ++tokens
                    }
                    "${color}87m" -> {
                        builder.append("<span style='color:blue'>")
                        ++tokens
                    }
                    "${color}203m" -> {
                        builder.append("<span style='color:#F55'>")
                        ++tokens
                    }
                    "${color}207m" -> {
                        builder.append("<span style='color:#F5F'>")
                        ++tokens
                    }
                    "${color}227m" -> {
                        builder.append("<span style='color:#FF5'>")
                        ++tokens
                    }
                    "${color}231m" -> {
                        builder.append("<span style='color:#FFF'>")
                        ++tokens
                    }
                    else -> builder.append(token)
                }
            }
            builder.append("</span>".repeat(tokens))
            return builder.toString().replace("\n", "<br>")
        }
    }
}