package io.scer.pocketmine.screens.fragments

import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import io.reactivex.disposables.Disposable
import io.scer.pocketmine.R
import io.scer.pocketmine.server.Server
import io.scer.pocketmine.server.ServerBus
import java.util.*
import kotlin.concurrent.schedule

class ConsoleFragment : Fragment() {
    private lateinit var editCommand: EditText
    private lateinit var labelLog: TextView
    private lateinit var scroll: ScrollView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_console, container, false)
        setHasOptionsMenu(true)

        scroll = view.findViewById(R.id.scroll)
        labelLog = view.findViewById(R.id.labelLog)
        val send: ImageButton = view.findViewById(R.id.send)
        editCommand = view.findViewById(R.id.editCommand)
        val commandRoot = view.findViewById<View>(R.id.command_root)
        val serverDisabled = view.findViewById<View>(R.id.server_disabled)

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

        if (!Server.getInstance().isRunning) {
            commandRoot.visibility = View.GONE
            serverDisabled.visibility = View.VISIBLE
        }

        messageObserver = ServerBus.Log.listen.subscribe{
            if (activity == null) return@subscribe

            activity!!.runOnUiThread {
                labelLog.text = it
                Timer().schedule(100) {
                    if (activity == null) return@schedule

                    activity!!.runOnUiThread {
                        scroll.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }
            }
        }

        return view
    }

    override fun onDestroyView() {
        messageObserver.dispose()
        super.onDestroyView()
    }

    private lateinit var messageObserver: Disposable

    private fun sendCommand() {
        Server.getInstance().sendCommand(editCommand.text.toString())
        editCommand.text.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.console, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear -> {
                labelLog.text = ""
                ServerBus.Log.clear()
                Snackbar.make(view!!, R.string.console_cleaned, Snackbar.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun newInstance() = ConsoleFragment()
    }
}