package io.scer.pocketmine.screens.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import io.scer.pocketmine.R
import io.scer.pocketmine.ServerService
import io.scer.pocketmine.server.*

class ServerFragment : Fragment() {
    private lateinit var start: MaterialButton
    private lateinit var stop: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_server, container, false)

        start = view.findViewById(R.id.start)
        stop = view.findViewById(R.id.stop)

        val isStarted = Server.getInstance().isRunning
        toggleButtons(isStarted)

        start.setOnClickListener {
            service = Intent(activity, ServerService::class.java)
            ContextCompat.startForegroundService(context!!, service!!)
        }

        stop.setOnClickListener {
            Server.getInstance().sendCommand("stop")
        }

        return view
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
        }
    }

    private val stopObserver = ServerBus.listen(StopEvent::class.java).subscribe {
        activity!!.runOnUiThread {
            toggleButtons(false)
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