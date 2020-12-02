package com.example.cors

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.cors.ntrip.NtripClient
import java.text.DecimalFormat


class MainActivity : AppCompatActivity() {
    val TAG = "NTAG"
    var df = DecimalFormat()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    fun btnStartService(v: View) {
        val NetworkProtocol = "ntripv1"
        val SERVERIP = "182.92.228.123"
        val SERVERPORT = 2101
        val USERNAME = "864650052180087"
        val PASSWORD = "qJNn3PdT"
        val MOUNTPOINT = "30000013"
        val ntripClient = NtripClient()
        ntripClient.addDataListener { buffer ->
            //                Log.d(TAG, "onDataReceived" + buffer?.let { String(it) })
        }
        ntripClient.setTimeOut(2000)
        ntripClient.connect(SERVERIP, SERVERPORT, USERNAME, PASSWORD, MOUNTPOINT)

    }


}