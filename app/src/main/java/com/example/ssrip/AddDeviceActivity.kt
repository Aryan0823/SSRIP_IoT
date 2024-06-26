package com.example.ssrip

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException

class AddDeviceActivity : AppCompatActivity() {

    private lateinit var ipAddressEditText: EditText
    private lateinit var ssidEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var categoryTextView: TextView
    private lateinit var deviceNameTextView: TextView
    private lateinit var submitButton: Button
    private lateinit var scanWifiButton: Button
    private lateinit var availableNetworksTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var uid: String
    private lateinit var category: String
    private lateinit var deviceName: String
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        uid = intent.getStringExtra("USER_UID") ?: ""
        category = intent.getStringExtra("CATEGORY") ?: ""
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: ""

        ipAddressEditText = findViewById(R.id.ipAddressEditText)
        ssidEditText = findViewById(R.id.ssidEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        categoryTextView = findViewById(R.id.categoryTextView)
        deviceNameTextView = findViewById(R.id.deviceNameTextView)
        submitButton = findViewById(R.id.submitButton)
        scanWifiButton = findViewById(R.id.scanWifiButton)
        availableNetworksTextView = findViewById(R.id.availableNetworksTextView)
        progressBar = findViewById(R.id.progressBar)

        // Display category and device name
        categoryTextView.text = "Category: $category"
        deviceNameTextView.text = "Device Name: $deviceName"

        scanWifiButton.setOnClickListener {
            val ipAddress = ipAddressEditText.text.toString()
            if (ipAddress.isNotEmpty()) {
                scanNetworks(ipAddress)
            } else {
                showToast("Please enter an IP address")
            }
        }

        submitButton.setOnClickListener {
            val ipAddress = ipAddressEditText.text.toString()
            val ssid = ssidEditText.text.toString()
            val password = passwordEditText.text.toString()
            if (ipAddress.isNotEmpty() && ssid.isNotEmpty() && password.isNotEmpty()) {
                submitCredentials(ipAddress, ssid, password, category, deviceName, uid)
            } else {
                showToast("Please fill in all fields")
            }
        }
    }

    private fun scanNetworks(ipAddress: String) {
        showProgress(true)
        val request = Request.Builder()
            .url("http://$ipAddress/scan")
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val networkList = parseNetworkList(responseBody)
                    withContext(Dispatchers.Main) {
                        displayNetworks(networkList)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("Error: ${response.code}")
                    }
                }
            } catch (e: IOException) {
                Log.e("AddDeviceActivity", "Network error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Network error: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showProgress(false)
                }
            }
        }
    }

    private fun submitCredentials(ipAddress: String, ssid: String, password: String, category: String, deviceName: String, uid: String) {
        showProgress(true)
        val url = HttpUrl.Builder()
            .scheme("http")
            .host(ipAddress)
            .addPathSegment("setting")
            .addQueryParameter("ssid", ssid)
            .addQueryParameter("pass", password)
            .addQueryParameter("category", category)
            .addQueryParameter("deviceName", deviceName)
            .addQueryParameter("uid", uid)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        showToast("Credentials submitted successfully")
                        // You might want to navigate back to the previous activity or main screen here
                        finish()
                    } else {
                        showToast("Error: ${response.code}")
                    }
                }
            } catch (e: IOException) {
                Log.e("AddDeviceActivity", "Network error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Network error: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showProgress(false)
                }
            }
        }
    }

    private fun parseNetworkList(json: String?): List<WifiNetwork> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<WifiNetwork>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun displayNetworks(networks: List<WifiNetwork>) {
        val networkText = networks.joinToString("\n") { "${it.ssid} (${it.rssi} dBm)" }
        availableNetworksTextView.text = networkText
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        submitButton.isEnabled = !show
        scanWifiButton.isEnabled = !show
        ipAddressEditText.isEnabled = !show
        ssidEditText.isEnabled = !show
        passwordEditText.isEnabled = !show
    }

    data class WifiNetwork(
        val ssid: String,
        val rssi: Int,
        val secure: Int
    )
}