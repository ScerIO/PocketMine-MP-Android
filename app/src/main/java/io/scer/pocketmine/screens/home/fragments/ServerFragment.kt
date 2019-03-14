package io.scer.pocketmine.screens.home.fragments

import android.content.Context.WIFI_SERVICE
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import io.scer.pocketmine.R
import io.scer.pocketmine.ServerService
import io.scer.pocketmine.server.*
import kotlinx.android.synthetic.main.fragment_server.*


class ServerFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_server, container, false)

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

    @Suppress("DEPRECATION")
    private fun getIpAddress(): String {
        val wifiManager = context!!.getSystemService(WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        return if (ip == 0) "127.0.0.1" else Formatter.formatIpAddress(ip)
    }

    private val startObserver = ServerBus.listen(StartEvent::class.java).subscribe {
        if (activity == null) return@subscribe

        activity!!.runOnUiThread {
            toggleButtons(true)
        }
    }

    private val stopObserver = ServerBus.listen(StopEvent::class.java).subscribe {
        if (activity == null) return@subscribe

        activity!!.runOnUiThread {
            toggleButtons(false)
        }
        if (service != null) requireActivity().stopService(service)
    }

    private val errorObserver = ServerBus.listen(ErrorEvent::class.java).subscribe {
        if (activity == null) return@subscribe

        when (it.type) {
            Errors.PHAR_NOT_EXIST -> Snackbar.make(view!!, R.string.phar_does_not_exist, Snackbar.LENGTH_LONG).show()
            Errors.UNKNOWN -> Snackbar.make(view!!, "Error: $it.message", Snackbar.LENGTH_LONG).show()
        }
        activity!!.runOnUiThread {
            toggleButtons(false)
        }
        if (service != null) activity!!.stopService(service)
    }

    private val statUpdateObserver = ServerBus.listen(UpdateStatEvent::class.java).subscribe {
        println(it)
    }

    private fun toggleButtons(started: Boolean) {
        start.isEnabled = !started
        stop.isEnabled = started
    }

    override fun onDestroyView() {
        startObserver.dispose()
        stopObserver.dispose()
        errorObserver.dispose()
        statUpdateObserver.dispose()
        super.onDestroyView()
    }

    companion object {
        private var service: Intent? = null
    }
}