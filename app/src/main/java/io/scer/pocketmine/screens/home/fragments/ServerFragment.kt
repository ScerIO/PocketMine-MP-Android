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
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.snackbar.Snackbar
import io.scer.pocketmine.R
import io.scer.pocketmine.ServerService
import io.scer.pocketmine.server.*
import kotlinx.android.synthetic.main.fragment_server.*

class ServerFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_server, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isStarted = Server.getInstance().isRunning
        toggleButtons(isStarted)

        start.setOnClickListener {
            service = Intent(activity, ServerService::class.java)
            ContextCompat.startForegroundService(requireContext(), service!!)
        }

        stop.setOnClickListener {
            Server.getInstance().sendCommand("stop")
        }

        dataSet = LineDataSet(ArrayList<Entry>(), null)
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.secondaryColor)
        dataSet.setDrawCircles(false)
        lineData = LineData(dataSet)
        chart_processor.description.isEnabled = false
        chart_processor.data = lineData
        chart_processor.setScaleEnabled(false)
        chart_processor.setTouchEnabled(false)
        chart_processor.isDragEnabled = false
        chart_processor.setDrawBorders(true)
        chart_processor.legend.isEnabled = false
        val leftAxis = chart_processor.axisLeft
        leftAxis.axisMaximum = 100f
        leftAxis.valueFormatter = PercentFormatter()
        leftAxis.setDrawGridLines(false)
        val rightAxis = chart_processor.axisRight
        rightAxis.isEnabled = false
        val xAxis = chart_processor.xAxis
        xAxis.isEnabled = false

        ip.text = getIpAddress()
    }

    @Suppress("DEPRECATION")
    private fun getIpAddress(): String {
        val wifiManager = requireContext().applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        return if (ip == 0) "127.0.0.1" else Formatter.formatIpAddress(ip)
    }

    private val startObserver = ServerBus.listen(StartEvent::class.java).subscribe({
        if (activity == null) return@subscribe

        requireActivity().runOnUiThread {
            toggleButtons(true)
        }
    }, ::handleError)

    private val stopObserver = ServerBus.listen(StopEvent::class.java).subscribe({
        if (activity == null) return@subscribe

        requireActivity().runOnUiThread {
            toggleButtons(false)
        }
        if (service != null) requireActivity().stopService(service)
    }, ::handleError)

    private val errorObserver = ServerBus.listen(ErrorEvent::class.java).subscribe ({
        if (activity == null) return@subscribe

        when (it.type) {
            Errors.PHAR_NOT_EXIST -> Snackbar.make(requireView(), R.string.phar_does_not_exist, Snackbar.LENGTH_LONG).show()
            Errors.UNKNOWN -> Snackbar.make(requireView(), "Error: $it.message", Snackbar.LENGTH_LONG).show()
        }
        requireActivity().runOnUiThread {
            toggleButtons(false)
        }
        if (service != null) requireActivity().stopService(service)
    }, ::handleError)

    private lateinit var dataSet: LineDataSet
    private lateinit var lineData: LineData
    private var lastIndex: Int = 0
    private val statUpdateObserver = ServerBus.listen(UpdateStatEvent::class.java).subscribe ({
        if (activity == null || !it.state.containsKey("Load")) return@subscribe

        requireActivity().runOnUiThread {
            if (chart_processor == null) return@runOnUiThread

            val processor = it.state.getValue("Load").replace("%", "").toFloat()
            println(it)

            if (lastIndex >= 5) {
                dataSet.removeFirst()
                chart_processor.xAxis.axisMinimum = lastIndex.toFloat()
            }

            if (lineData.entryCount > 0) {
                lastIndex++
                lineData.addEntry(Entry((lastIndex).toFloat(), processor), 0)
            } else {
                lineData.addEntry(Entry(0f, processor), 0)
            }

            dataSet.notifyDataSetChanged()
            chart_processor.notifyDataSetChanged()
            chart_processor.invalidate()
        }
    }, ::handleError)

    private fun toggleButtons(isStarted: Boolean) {
        if (start.isEnabled != !isStarted) {
            start.isEnabled = !isStarted
        }
        if (stop.isEnabled != isStarted) {
            stop.isEnabled = isStarted
        }
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