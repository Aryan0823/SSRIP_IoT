package com.example.ssrip

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.NetworkRequest
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException

class AddDeviceActivity : AppCompatActivity() {

    private lateinit var tvCategory: TextView
    private lateinit var tvDeviceName: TextView
    private lateinit var etIpAddress: EditText
//    private lateinit var btnScanNetworks: Button
//    private lateinit var tvAvailableNetworks: TextView
    private lateinit var etSsid: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    private lateinit var category: String
    private lateinit var deviceName: String
    private lateinit var userId: String

    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private var currentSSID: String? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val TAG = "AddDeviceActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        initializeViews()
        setInitialData()
        setupListeners()
        initializeWifiManager()
    }

    private fun initializeViews() {
        tvCategory = findViewById(R.id.tvCategory)
        tvDeviceName = findViewById(R.id.tvDeviceName)
        etIpAddress = findViewById(R.id.etIpAddress)
//        btnScanNetworks = findViewById(R.id.btnScanNetworks)
//        tvAvailableNetworks = findViewById(R.id.tvAvailableNetworks)
        etSsid = findViewById(R.id.etSsid)
        etPassword = findViewById(R.id.etPassword)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)

        val noSpaceFilter = getNoSpaceInputFilter()
        etIpAddress.filters = arrayOf(noSpaceFilter)
        etSsid.filters = arrayOf(noSpaceFilter)
        etPassword.filters = arrayOf(noSpaceFilter)
    }

    private fun getNoSpaceInputFilter(): InputFilter {
        return InputFilter { source, _, _, _, _, _ ->
            source.filterNot { it.isWhitespace() }
        }
    }

    private fun setInitialData() {
        category = intent.getStringExtra("CATEGORY") ?: ""
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: ""
        userId = intent.getStringExtra("USER_UID") ?: ""

        tvCategory.text = "Category: $category"
        tvDeviceName.text = "Device Name: $deviceName"
    }

    private fun setupListeners() {
//        btnScanNetworks.setOnClickListener {
//            scanWifiNetworks()
//        }

        btnSubmit.setOnClickListener {
            submitWifiCredentials()
        }
    }

    private fun initializeWifiManager() {
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        currentSSID = wifiManager.connectionInfo.ssid.replace("\"", "")
        Log.d(TAG, "Current SSID: $currentSSID")
    }

//    private fun scanWifiNetworks() {
//        val ipAddress = etIpAddress.text.toString()
//        if (ipAddress.isEmpty()) {
//            updateStatus("Please enter an IP address")
//            return
//        }
//
//        progressBar.visibility = View.VISIBLE
//        updateStatus("Scanning networks...")
//        coroutineScope.launch {
//            try {
//                ensureSSRIPConnection()
//                val networks = performNetworkScan(ipAddress)
//                displayNetworks(networks)
//                updateStatus("Scan complete")
//            } catch (e: Exception) {
//                Log.e(TAG, "Error scanning networks", e)
//                when (e) {
//                    is SocketTimeoutException -> updateStatus("Connection timed out. Please try again.")
//                    else -> updateStatus("Error scanning networks: ${e.message}")
//                }
//                ensureSSRIPConnection() // Try to reconnect to SSRIP
//            } finally {
//                progressBar.visibility = View.GONE
//            }
//        }
//    }

    private suspend fun ensureSSRIPConnection() {
        withContext(Dispatchers.IO) {
            if (!isConnectedToSSRIP()) {
                Log.d(TAG, "Not connected to SSRIP, attempting to reconnect")
                connectToSSRIP()
            }
        }
    }

    private fun isConnectedToSSRIP(): Boolean {
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connManager.activeNetwork
        val capabilities = connManager.getNetworkCapabilities(network)
        val ssid = wifiManager.connectionInfo.ssid.replace("\"", "")
        return capabilities != null &&
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                ssid.startsWith("SSRIP")
    }

    private fun connectToSSRIP() {
        val ssidToConnect = currentSSID ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssidToConnect)
                .build()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    connectivityManager.bindProcessToNetwork(network)
                    Log.d(TAG, "Connected to $ssidToConnect")
                }
            })
        } else {
            val wifiConfig = WifiConfiguration()
            wifiConfig.SSID = "\"$ssidToConnect\""
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            val netId = wifiManager.addNetwork(wifiConfig)
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()
        }
    }

//    private suspend fun performNetworkScan(ipAddress: String): List<String> = withContext(Dispatchers.IO) {
//        val url = URL("http://$ipAddress/scan")
//        val connection = url.openConnection() as HttpURLConnection
//        connection.requestMethod = "POST"
//        connection.connectTimeout = 10000 // 10 seconds timeout
//
//        val responseCode = connection.responseCode
//        if (responseCode == HttpURLConnection.HTTP_OK) {
//            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
//            val jsonArray = JSONArray(response)
//            return@withContext List(jsonArray.length()) { i ->
//                val network = jsonArray.getJSONObject(i)
//                "${network.getString("ssid")} (${network.getInt("rssi")} dBm)"
//            }
//        } else {
//            throw Exception("HTTP error code: $responseCode")
//        }
//    }

//    private fun displayNetworks(networks: List<String>) {
//        tvAvailableNetworks.text = networks.joinToString("\n")
//    }

    private fun submitWifiCredentials() {
        val ipAddress = etIpAddress.text.toString()
        val ssid = etSsid.text.toString()
        val password = etPassword.text.toString()

        if (ipAddress.isEmpty() || ssid.isEmpty() || password.isEmpty()) {
            updateStatus("Please fill in all fields")
            return
        }

        progressBar.visibility = View.VISIBLE
        updateStatus("Submitting credentials...")
        coroutineScope.launch {
            try {
                ensureSSRIPConnection()
                val success = sendCredentialsToESP(ipAddress, ssid, password)
                if (success) {
                    updateStatus("Credentials sent successfully")
                    navigateToWifiSwitchActivity(ssid, password)
                } else {
                    updateStatus("Failed to send credentials")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting credentials", e)
                updateStatus("Error: ${e.message}")
                ensureSSRIPConnection() // Try to reconnect to SSRIP
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun sendCredentialsToESP(ipAddress: String, ssid: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val url = URL("http://$ipAddress/setting?ssid=$ssid&pass=$password&deviceName=$deviceName&category=$category&uid=$userId")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000 // 10 seconds timeout

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val jsonResponse = JSONObject(response)
            return@withContext jsonResponse.getBoolean("success")
        } else {
            throw Exception("HTTP error code: $responseCode")
        }
    }

    private fun navigateToWifiSwitchActivity(ssid: String, password: String) {
        val intent = Intent(this, WifiSwitchActivity::class.java).apply {
            putExtra("SSID", ssid)
            putExtra("PASSWORD", password)
            putExtra("CATEGORY", category)
            putExtra("DEVICE_NAME", deviceName)
        }
        startActivity(intent)
        finish()
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            tvStatus.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
