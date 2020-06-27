package io.scer.pocketmine.screens.home.fragments

import android.os.Bundle
import android.view.*
import android.widget.ScrollView
import com.google.android.material.snackbar.Snackbar
import io.reactivex.disposables.Disposable
import io.scer.pocketmine.R
import io.scer.pocketmine.server.Server
import io.scer.pocketmine.server.ServerBus
import io.scer.pocketmine.server.StopEvent
import kotlinx.android.synthetic.main.fragment_console.*
import java.util.*
import kotlin.concurrent.schedule


class ConsoleFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_console, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

            requireActivity().runOnUiThread {
                labelLog.text = it
                Timer().schedule(100) {
                    if (activity == null) return@schedule

                    requireActivity().runOnUiThread delayed@ {
                        if (scroll == null) return@delayed

                        scroll.fullScroll(ScrollView.FOCUS_DOWN)
                        editCommand.isFocusable = true
                        editCommand.requestFocus()
                    }
                }
            }
        }
    }

    private fun toggleCommandLine(enable: Boolean) {
        command_root.visibility = if (enable) View.VISIBLE else View.GONE
        server_disabled.visibility = if (enable) View.GONE else View.VISIBLE
    }

    private val stopObserver = ServerBus.listen(StopEvent::class.java).subscribe({
        if (activity == null) return@subscribe

        requireActivity().runOnUiThread {
            toggleCommandLine(false)
        }
    }, ::handleError)

    override fun onDestroyView() {
        messageObserver.dispose()
        stopObserver.dispose()
        super.onDestroyView()
    }

    private lateinit var messageObserver: Disposable

    private fun sendCommand() {
        Runnable {
            Server.getInstance().sendCommand(editCommand.text.toString())
        }.run()

        editCommand.setText("")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.console, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear -> {
                labelLog.text = ""
                ServerBus.Log.clear()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}