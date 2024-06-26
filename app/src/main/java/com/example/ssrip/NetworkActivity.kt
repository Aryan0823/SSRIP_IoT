package com.example.ssrip

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class NetworkActivity : AppCompatActivity() {
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiListView: ListView
    private lateinit var scanButton: Button
    private lateinit var nextButton: Button
    private lateinit var progressBar: ProgressBar
    private var selectedNetwork: String? = null
    private val wifiList = mutableListOf<String>()
    private lateinit var wifiReceiver: BroadcastReceiver

    private lateinit var category: String
    private lateinit var deviceName: String
    private lateinit var userId: String

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)

        // Initialize views
        wifiListView = findViewById(R.id.wifiListView)
        scanButton = findViewById(R.id.scanButton)
        nextButton = findViewById(R.id.nextButton)
        progressBar = findViewById(R.id.progressBar)

        // Get extras from intent
        category = intent.getStringExtra("CATEGORY") ?: ""
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: ""
        userId = intent.getStringExtra("USER_UID") ?: ""

        // Initialize WifiManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        scanButton.setOnClickListener {
            if (checkPermissions()) {
                scanWifiNetworks()
            } else {
                requestPermissions()
            }
        }

        nextButton.setOnClickListener {
            navigateToNextActivity()
        }

        wifiListView.setOnItemClickListener { _, _, position, _ ->
            selectedNetwork = wifiList[position]
            connectToWifi(selectedNetwork!!)
        }

        // Initially disable the next button
        nextButton.isEnabled = false

        // Initialize WifiReceiver
        wifiReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    scanSuccess()
                } else {
                    scanFailure()
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanWifiNetworks()
            } else {
                Toast.makeText(this, "Permission denied. Cannot scan Wi-Fi networks.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun scanWifiNetworks() {
        registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan()
        progressBar.visibility = android.view.View.VISIBLE
    }

    private fun scanSuccess() {
        wifiList.clear()
        val results = wifiManager.scanResults
        for (result in results) {
            if (result.SSID.startsWith("SSRIP") && !wifiList.contains(result.SSID)) {
                wifiList.add(result.SSID)
            }
        }
        updateWifiList()
    }

    private fun scanFailure() {
        scanSuccess() // Fallback to using cached results
    }

    private fun updateWifiList() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, wifiList)
        wifiListView.adapter = adapter
        progressBar.visibility = android.view.View.GONE
        wifiListView.visibility = android.view.View.VISIBLE
        unregisterReceiver(wifiReceiver)
    }

    private fun connectToWifi(networkSSID: String) {
        progressBar.visibility = android.view.View.VISIBLE
        val conf = WifiConfiguration()
        conf.SSID = "\"$networkSSID\""
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)

        val networkId = wifiManager.addNetwork(conf)
        wifiManager.disconnect()
        wifiManager.enableNetwork(networkId, true)
        wifiManager.reconnect()

        // Check connection after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            if (isConnectedToWifi(networkSSID)) {
                Toast.makeText(this, "Connected to $networkSSID", Toast.LENGTH_SHORT).show()
                nextButton.isEnabled = true
            } else {
                Toast.makeText(this, "Not connected. Please try again.", Toast.LENGTH_SHORT).show()
                nextButton.isEnabled = false
            }
            progressBar.visibility = android.view.View.GONE
        }, 5000) // 5 seconds delay to allow connection to establish
    }

    private fun isConnectedToWifi(ssid: String): Boolean {
        val connectionInfo = wifiManager.connectionInfo
        return connectionInfo != null && connectionInfo.ssid == "\"$ssid\""
    }

    private fun navigateToNextActivity() {
        val intent = Intent(this, AddDeviceActivity::class.java).apply {
            putExtra("CATEGORY", category)
            putExtra("DEVICE_NAME", deviceName)
            putExtra("USER_UID", userId)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(wifiReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered
        }
    }
}