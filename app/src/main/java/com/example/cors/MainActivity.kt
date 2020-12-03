package com.example.cors

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cors.ntrip.NtripClient
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.text.DecimalFormat


class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    val TAG = "NTAG"
    var df = DecimalFormat()
    private var connect: Boolean = false
    private val NetworkProtocol = "ntripv1"
    private val SERVERIP = "182.92.228.123"
    private val SERVERPORT = 2101
    private val USERNAME = "864650052180087"
    private val PASSWORD = "qJNn3PdT"
    private val MOUNTPOINT = "30000013"
    private val ntripClient = NtripClient.getInstance()
    var log: StringBuffer = StringBuffer()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnService.text = "未连接"
        textLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        ntripClient.addDataListener { buffer ->

            launch() {
                val size = buffer.size
                textBytes.text = "$size btye"
                log.append(String(buffer))
                textLog.setText(log, TextView.BufferType.EDITABLE)
                scrollView1.fullScroll(View.FOCUS_DOWN)
            }
        }

    }

    fun btnStartService(v: View) {
        connect = !connect
        if (connect) {
            ntripClient.setTimeOut(20000)
            ntripClient.setReconnectIntervalsTime(10000)
            ntripClient.setFreqTime(500)
            ntripClient.connect(SERVERIP, SERVERPORT, USERNAME, PASSWORD, MOUNTPOINT)
            btnService.text = "已连接"
        } else {
            ntripClient.disconnect()
            btnService.text = "未连接"
        }
    }

    var isOpen = true
    fun btnLogOpen(v: View) {

        isOpen = !isOpen
        ntripClient.setPrintLog(isOpen)
        if (isOpen) {
            btnlog.text = "log开"
        } else {
            btnlog.text = "log关"
        }

    }


}