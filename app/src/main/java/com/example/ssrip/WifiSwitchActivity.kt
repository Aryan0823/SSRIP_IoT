package com.example.ssrip

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class WifiSwitchActivity : BaseActivity() {
    private lateinit var tvCategory: TextView
    private lateinit var tvDeviceName: TextView
    private lateinit var tvSsid: TextView
    private lateinit var tvPassword: TextView
    private lateinit var btnOpenWifiSettings: Button
    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var originalSsid: String
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            checkAndKillApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_switch)

        initializeViews()
        displayData()
        setupNetworkMonitoring()
    }

    private fun initializeViews() {
        tvCategory = findViewById(R.id.tvCategory)
        tvDeviceName = findViewById(R.id.tvDeviceName)
        tvSsid = findViewById(R.id.tvSsid)
        tvPassword = findViewById(R.id.tvPassword)
        btnOpenWifiSettings = findViewById(R.id.btnOpenWifiSettings)
        btnOpenWifiSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    private fun displayData() {
        tvCategory.text = "Category: ${intent.getStringExtra("CATEGORY")}"
        tvDeviceName.text = "Device Name: ${intent.getStringExtra("DEVICE_NAME")}"
        tvSsid.text = "SSID: ${intent.getStringExtra("SSID")}"
        tvPassword.text = "Password: ${intent.getStringExtra("PASSWORD")}"
    }

    private fun setupNetworkMonitoring() {
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        originalSsid = wifiManager.connectionInfo.ssid.replace("\"", "")

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun checkAndKillApp() {
        val currentSsid = wifiManager.connectionInfo.ssid.replace("\"", "")
        if (currentSsid != originalSsid && currentSsid != "<unknown ssid>") {
            runOnUiThread {
                Toast.makeText(this, "WiFi switched. Closing the app.", Toast.LENGTH_LONG).show()
                // Wait for a moment to show the toast
                Handler(Looper.getMainLooper()).postDelayed({
                    // Kill the app
                    finishAffinity()
                }, 2000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
