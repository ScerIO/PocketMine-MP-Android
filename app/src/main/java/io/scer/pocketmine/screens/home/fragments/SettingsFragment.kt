package io.scer.pocketmine.screens.home.fragments

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
import io.scer.pocketmine.R
import io.scer.pocketmine.server.Properties

class SettingsFragment : Fragment() {
    private var properties: Properties? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val content: LinearLayout = view.findViewById(R.id.content)

        try {
            properties = Properties()
            visualize(content)
        } catch (e: Exception) {
            val propertiesNotExist = view.findViewById<View>(R.id.properties_not_exist)
            propertiesNotExist.visibility = View.VISIBLE
        }

        return view
    }

    private fun visualize(rootLayout: LinearLayout) {
        for (entry in properties!!.getMap()) {
            val key = entry.key
            val value: Any = entry.value

            val view = View(context)
            view.layoutParams = ViewGroup.LayoutParams(1, resources.getDimension(R.dimen.settings_spacing).toInt())

            when (value) {
                is Boolean -> rootLayout.addView(buildSwitch(key, value))
                is String -> rootLayout.addView(buildEditText(key, value))
            }

            rootLayout.addView(view)
        }
    }

    private fun buildSwitch(key: String, value: Boolean): Switch {
        val switch = Switch(context)
        switch.height = resources.getDimension(R.dimen.settings_entity_height).toInt()
        switch.setTextColor(ContextCompat.getColor(requireContext(), R.color.primaryTextColor))
        switch.text = key
        switch.isChecked = value
        switch.textSize = 14F
        switch.setOnCheckedChangeListener { _, isChecked ->
            properties!!.set(key, isChecked)
        }

        return switch
    }

    private fun buildEditText(key: String, value: String): LinearLayout {
        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL

        val textView = TextView(context)
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primaryTextColor))
        textView.text = key

        root.addView(textView)

        val editText = EditText(context)
        editText.height = resources.getDimension(R.dimen.settings_entity_height).toInt()
        editText.setText(value)
        editText.textSize = 14F
        editText.maxLines = 1

        if (value.toIntOrNull() != null) {
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {}

            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, before: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, after: Int) {
                val text = charSequence.toString()
                if (text.isEmpty()) return
                properties!!.set(key, text)
            }
        })

        root.addView(editText)

        return root
    }

    override fun onDestroy() {
        properties?.write()
        super.onDestroy()
    }
}