package io.scer.pocketmine.screens.fragments

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import io.scer.pocketmine.R
import io.scer.pocketmine.server.Properties

class SettingsFragment : Fragment() {
    private var properties: Properties? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val rootLayout: LinearLayout = view.findViewById(R.id.settingsRootLayout)

        try {
            properties = Properties()
            visualize(rootLayout)
        } catch (e: Exception) {
            Snackbar.make(view, R.string.settings_does_not_exist, Snackbar.LENGTH_LONG).show()
        }
        return view
    }

    private fun visualize(rootLayout: LinearLayout) {
        for (entry in properties!!.getMap()) {
            val key = entry.key
            val value: Any = entry.value

            val view = View(context)
            view.layoutParams = ViewGroup.LayoutParams(1, 50)

            rootLayout.addView(view)

            when (value) {
                is Boolean -> {
                    val switch = Switch(context)
                    switch.text = key
                    switch.isChecked = value
                    switch.textSize = 14F
                    switch.setOnCheckedChangeListener { _, isChecked ->
                        properties!!.set(key, isChecked)
                    }

                    rootLayout.addView(switch)
                }
                is String -> {
                    val textView = TextView(context)
                    textView.text = key
                    textView.setTextColor(ContextCompat.getColor(context!!, R.color.colorAccent))

                    rootLayout.addView(textView)

                    val editText = EditText(context)
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

                    rootLayout.addView(editText)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        properties?.write()
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}