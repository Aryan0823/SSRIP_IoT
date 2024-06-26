package com.example.ssrip

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class NetworkActivity : AppCompatActivity() {
    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiListView: ListView
    private lateinit var scanButton: Button
    private lateinit var nextButton: Button
    private lateinit var progressBar: ProgressBar
    private var selectedNetwork: String? = null
    private val wifiList = mutableListOf<String>()
    private lateinit var wifiReceiver: BroadcastReceiver
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private lateinit var category: String
    private lateinit var deviceName: String
    private lateinit var userId: String

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
        private const val LOCATION_SETTINGS_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)

        initializeViews()
        initializeManagers()
        setClickListeners()
        initializeWifiReceiver()
    }

    private fun initializeViews() {
        wifiListView = findViewById(R.id.wifiListView)
        scanButton = findViewById(R.id.scanButton)
        nextButton = findViewById(R.id.nextButton)
        progressBar = findViewById(R.id.progressBar)

        category = intent.getStringExtra("CATEGORY") ?: ""
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: ""
        userId = intent.getStringExtra("USER_UID") ?: ""

        nextButton.isEnabled = false
    }

    private fun initializeManagers() {
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private fun setClickListeners() {
        scanButton.setOnClickListener {
            if (checkAndRequestPermissions()) {
                scanWifiNetworks()
            }
        }

        nextButton.setOnClickListener {
            navigateToNextActivity()
        }

        wifiListView.setOnItemClickListener { _, _, position, _ ->
            selectedNetwork = wifiList[position]
            connectToWifi(selectedNetwork!!)
        }
    }

    private fun initializeWifiReceiver() {
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

    private fun checkAndRequestPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }

            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSIONS_REQUEST_CODE)
                return false
            }
        }
        return true
    }

    private fun scanWifiNetworks() {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Please enable Wi-Fi", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !isLocationEnabled()) {
            promptEnableLocation()
            return
        }

        registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        val success = wifiManager.startScan()
        if (!success) {
            scanFailure()
        }
        progressBar.visibility = android.view.View.VISIBLE
    }

    private fun isLocationEnabled(): Boolean {
        val locationMode: Int = try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            return false
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }

    private fun promptEnableLocation() {
        AlertDialog.Builder(this)
            .setTitle("Location Required")
            .setMessage("Please enable location services to scan for Wi-Fi networks.")
            .setPositiveButton("Enable") { _, _ ->
                startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), LOCATION_SETTINGS_REQUEST_CODE)
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        wifiList.clear()
        val results = wifiManager.scanResults
        for (result in results) {
            if (result.SSID.startsWith("SSRIP") && !wifiList.contains(result.SSID)) {
                wifiList.add(result.SSID)
            }
        }
        updateWifiList()
        Toast.makeText(this, "Scan failed. Showing cached results.", Toast.LENGTH_SHORT).show()
    }

    private fun updateWifiList() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, wifiList)
        wifiListView.adapter = adapter
        progressBar.visibility = android.view.View.GONE
        wifiListView.visibility = android.view.View.VISIBLE
        try {
            unregisterReceiver(wifiReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    private fun connectToWifi(networkSSID: String) {
        progressBar.visibility = android.view.View.VISIBLE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectToWifiAndroidQ(networkSSID)
        } else {
            connectToWifiLegacy(networkSSID)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connectToWifiAndroidQ(networkSSID: String) {
        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(networkSSID)
            // .setWpa2Passphrase("YOUR_PASSWORD_HERE")  // Uncomment and set if network is secured
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                connectivityManager.bindProcessToNetwork(network)
                runOnUiThread {
                    Toast.makeText(this@NetworkActivity, "Connected to $networkSSID", Toast.LENGTH_SHORT).show()
                    nextButton.isEnabled = true
                    progressBar.visibility = android.view.View.GONE
                }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                runOnUiThread {
                    Toast.makeText(this@NetworkActivity, "Unable to connect to $networkSSID", Toast.LENGTH_SHORT).show()
                    nextButton.isEnabled = false
                    progressBar.visibility = android.view.View.GONE
                }
            }
        }

        connectivityManager.requestNetwork(request, networkCallback)
    }

    private fun connectToWifiLegacy(networkSSID: String) {
        val wifiConfig = WifiConfiguration()
        wifiConfig.SSID = "\"$networkSSID\""
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)

        val netId = wifiManager.addNetwork(wifiConfig)
        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()

        Handler(Looper.getMainLooper()).postDelayed({
            val info = wifiManager.connectionInfo
            if (info != null && info.ssid == "\"$networkSSID\"") {
                Toast.makeText(this@NetworkActivity, "Connected to $networkSSID", Toast.LENGTH_SHORT).show()
                nextButton.isEnabled = true
            } else {
                Toast.makeText(this@NetworkActivity, "Unable to connect to $networkSSID", Toast.LENGTH_SHORT).show()
                nextButton.isEnabled = false
            }
            progressBar.visibility = android.view.View.GONE
        }, 5000) // 5 seconds delay
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                scanWifiNetworks()
            } else {
                Toast.makeText(this, "Permissions are required to scan Wi-Fi networks", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            if (isLocationEnabled()) {
                scanWifiNetworks()
            } else {
                Toast.makeText(this, "Location is required to scan Wi-Fi networks", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(wifiReceiver)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }
        } catch (e: IllegalArgumentException) {
            // Receiver or callback was not registered
        }
    }
}