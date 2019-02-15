package io.scer.pocketmine.screens.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import io.scer.pocketmine.ServerService
import io.scer.pocketmine.server.*
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import io.scer.pocketmine.R


class ServerFragment : Fragment() {
    private lateinit var start: MaterialButton
    private lateinit var stop: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_server, container, false)

        start = view.findViewById(R.id.start)
        stop = view.findViewById(R.id.stop)
        val ip = view.findViewById<TextView>(io.scer.pocketmine.R.id.ip)

        val isStarted = Server.getInstance().isRunning
        toggleButtons(isStarted)

        start.setOnClickListener {
            service = Intent(activity, ServerService::class.java)
            ContextCompat.startForegroundService(context!!, service!!)
        }

        stop.setOnClickListener {
            Server.getInstance().sendCommand("stop")
        }

        ip.text = getIpAddress()

        return view
    }

    private fun getIpAddress(): String {
        val wifiManager = context!!.getSystemService(WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        return if (ip == 0) "127.0.0.1" else Formatter.formatIpAddress(ip)
    }

    override fun onDestroyView() {
        startObserver.dispose()
        stopObserver.dispose()
        errorObserver.dispose()
        super.onDestroyView()
    }

    private val startObserver = ServerBus.listen(StartEvent::class.java).subscribe {
        activity!!.runOnUiThread {
            toggleButtons(true)
            Snackbar.make(view!!, "Started", Snackbar.LENGTH_LONG).show()
        }
    }

    private val stopObserver = ServerBus.listen(StopEvent::class.java).subscribe {
        activity!!.runOnUiThread {
            toggleButtons(false)
            Snackbar.make(view!!, "Exited", Snackbar.LENGTH_LONG).show()
        }
        if (service != null) requireActivity().stopService(service)
    }

    private val errorObserver = ServerBus.listen(ErrorEvent::class.java).subscribe {
        when (it.type) {
            Errors.PHAR_NOT_EXIST -> Snackbar.make(view!!, R.string.phar_does_not_exist, Snackbar.LENGTH_LONG).show()
            Errors.UNKNOWN -> Snackbar.make(view!!, "Error: $it.message", Snackbar.LENGTH_LONG).show()
        }
        activity!!.runOnUiThread {
            toggleButtons(false)
        }
        if (service != null) requireActivity().stopService(service)
    }

    private fun toggleButtons(started: Boolean) {
        start.isEnabled = !started
        stop.isEnabled = started
    }

    companion object {
        fun newInstance() = ServerFragment()
        private var service: Intent? = null
    }
}