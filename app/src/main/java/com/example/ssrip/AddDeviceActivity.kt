package com.example.ssrip

import android.os.Bundle
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

class AddDeviceActivity : AppCompatActivity() {

    private lateinit var tvCategory: TextView
    private lateinit var tvDeviceName: TextView
    private lateinit var etIpAddress: EditText
    private lateinit var btnScanNetworks: Button
    private lateinit var tvAvailableNetworks: TextView
    private lateinit var etSsid: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var category: String
    private lateinit var deviceName: String
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        initializeViews()
        setInitialData()
        setupListeners()
    }

    private fun initializeViews() {
        tvCategory = findViewById(R.id.tvCategory)
        tvDeviceName = findViewById(R.id.tvDeviceName)
        etIpAddress = findViewById(R.id.etIpAddress)
        btnScanNetworks = findViewById(R.id.btnScanNetworks)
        tvAvailableNetworks = findViewById(R.id.tvAvailableNetworks)
        etSsid = findViewById(R.id.etSsid)
        etPassword = findViewById(R.id.etPassword)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setInitialData() {
        category = intent.getStringExtra("CATEGORY") ?: ""
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: ""
        userId = intent.getStringExtra("USER_UID") ?: ""

        tvCategory.text = "Category: $category"
        tvDeviceName.text = "Device Name: $deviceName"
    }

    private fun setupListeners() {
        btnScanNetworks.setOnClickListener {
            scanWifiNetworks()
        }

        btnSubmit.setOnClickListener {
            submitWifiCredentials()
        }
    }

    private fun scanWifiNetworks() {
        val ipAddress = etIpAddress.text.toString()
        if (ipAddress.isEmpty()) {
            Toast.makeText(this, "Please enter an IP address", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val networks = performNetworkScan(ipAddress)
                withContext(Dispatchers.Main) {
                    displayNetworks(networks)
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddDeviceActivity, "Error scanning networks: ${e.message}", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private suspend fun performNetworkScan(ipAddress: String): List<String> = withContext(Dispatchers.IO) {
        val url = URL("http://$ipAddress/scan")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 5000

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val jsonArray = JSONArray(response)
            return@withContext List(jsonArray.length()) { i ->
                val network = jsonArray.getJSONObject(i)
                "${network.getString("ssid")} (${network.getInt("rssi")} dBm)"
            }
        } else {
            throw Exception("HTTP error code: $responseCode")
        }
    }

    private fun displayNetworks(networks: List<String>) {
        tvAvailableNetworks.text = networks.joinToString("\n")
    }

    private fun submitWifiCredentials() {
        val ipAddress = etIpAddress.text.toString()
        val ssid = etSsid.text.toString()
        val password = etPassword.text.toString()

        if (ipAddress.isEmpty() || ssid.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = sendCredentialsToESP(ipAddress, ssid, password)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@AddDeviceActivity, "Credentials sent successfully", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddDeviceActivity, "Failed to send credentials", Toast.LENGTH_LONG).show()
                    }
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddDeviceActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private suspend fun sendCredentialsToESP(ipAddress: String, ssid: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val url = URL("http://$ipAddress/setting?ssid=$ssid&pass=$password&deviceName=$deviceName&category=$category&uid=$userId")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val jsonResponse = JSONObject(response)
            return@withContext jsonResponse.getBoolean("success")
        } else {
            throw Exception("HTTP error code: $responseCode")
        }
    }
}