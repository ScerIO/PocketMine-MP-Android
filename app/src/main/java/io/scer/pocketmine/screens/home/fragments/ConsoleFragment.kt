package io.scer.pocketmine.screens.home.fragments

import android.os.Bundle
import android.view.*
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import io.reactivex.disposables.Disposable
import io.scer.pocketmine.R
import io.scer.pocketmine.server.Server
import io.scer.pocketmine.server.ServerBus
import io.scer.pocketmine.server.StopEvent
import kotlinx.android.synthetic.main.fragment_console.*
import java.util.*
import kotlin.concurrent.schedule

class ConsoleFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_console, container, false)
        setHasOptionsMenu(true)

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

        if (Server.getInstance().isRunning) toggleCommandLine(true)

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

    private fun toggleCommandLine(enable: Boolean) {
        command_root.visibility = if (enable) View.VISIBLE else View.GONE
        command_root.visibility = if (enable) View.GONE else View.VISIBLE
    }

    private val stopObserver = ServerBus.listen(StopEvent::class.java).subscribe {
        if (activity == null) return@subscribe

        activity!!.runOnUiThread {
            toggleCommandLine(false)
        }
    }

    override fun onDestroyView() {
        messageObserver.dispose()
        stopObserver.dispose()
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
}