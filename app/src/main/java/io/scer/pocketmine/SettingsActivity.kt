package io.scer.pocketmine

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import io.scer.pocketmine.utils.ServerProperties
import kotlinx.android.synthetic.main.activity_settings.*
import android.text.Editable
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

class SettingsActivity : AppCompatActivity() {
    private var properties: ServerProperties? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        try {
            properties = ServerProperties()
            visualize()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.settings_does_not_exist, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun visualize() {
        for (entry in properties!!.getMap()) {
            val key = entry.key
            val value: Any = entry.value

            val view = View(this)
            view.layoutParams = ViewGroup.LayoutParams(1, 50)

            settingsRootLayout.addView(view)

            when (value) {
                is Boolean -> {
                    val switch = Switch(this)
                    switch.text = key
                    switch.isChecked = value
                    switch.textSize = 14F
                    switch.setOnCheckedChangeListener { _, isChecked ->
                        properties!!.set(key, isChecked)
                    }

                    settingsRootLayout.addView(switch)
                }
                is String -> {
                    val textView = TextView(this)
                    textView.text = key
                    textView.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))

                    settingsRootLayout.addView(textView)

                    val editText = EditText(this)
                    editText.setText(value)
                    editText.textSize = 14F
                    editText.maxLines = 1

                    if (value.toIntOrNull() != null) {
                        editText.inputType = InputType.TYPE_CLASS_NUMBER
                    } else {
                        editText.inputType = InputType.TYPE_CLASS_TEXT
                    }

                    editText.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(p0: Editable?) {
                        }

                        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        }

                        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                            val text = p0.toString()
                            if (text.isEmpty()) {
                                return
                            }
                            properties!!.set(key, text)
                        }
                    })

                    settingsRootLayout.addView(editText)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        properties?.write()
    }
}