package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class DashboardActivity : BaseActivity() {
    private lateinit var logoutButton: Button
    private lateinit var welcomeTextView: TextView
    private lateinit var addDeviceButton: FloatingActionButton
    private lateinit var btnAC: ImageButton
    private lateinit var btnFan: ImageButton
    private lateinit var btnHumidifier: ImageButton
    private lateinit var btnLight: ImageButton
    private lateinit var tvOutdoorTemperature: TextView
    private lateinit var tvOutdoorHumidity: TextView
    private lateinit var tvOutdoorLight: TextView
    private lateinit var db: FirebaseFirestore
    private var outdoorSensorListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        initializeViews()
        setupListeners()
        displayUserInfo()
        db = FirebaseFirestore.getInstance()
        setupOutdoorSensorListener()
    }

    private fun initializeViews() {
        logoutButton = findViewById(R.id.btnSignOut)
        welcomeTextView = findViewById(R.id.tvWelcome)
        addDeviceButton = findViewById(R.id.btnAddDevice)
        btnAC = findViewById(R.id.btnAC)
        btnFan = findViewById(R.id.btnFan)
        btnHumidifier = findViewById(R.id.btnHumidifier)
        btnLight = findViewById(R.id.btnLight)
        tvOutdoorTemperature = findViewById(R.id.tvOutdoorTemperature)
        tvOutdoorHumidity = findViewById(R.id.tvOutdoorHumidity)
        tvOutdoorLight = findViewById(R.id.tvOutdoorLight)
    }

    private fun setupListeners() {
        logoutButton.setOnClickListener { logout() }
        addDeviceButton.setOnClickListener {
            startActivity(Intent(this, AddDeviceCategoryActivity::class.java))
        }
        btnAC.setOnClickListener { openControlActivity("AC") }
        btnFan.setOnClickListener { openControlActivity("Fan") }
        btnHumidifier.setOnClickListener { openControlActivity("Humidifier") }
        btnLight.setOnClickListener { openControlActivity("Light") }
    }

    private fun displayUserInfo() {
        val userDetails = getUserDetails()
        val email = userDetails[SessionManager.KEY_EMAIL]
        welcomeTextView.text = "Welcome, $email"
    }

    private fun setupOutdoorSensorListener() {
        val userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: return
        val deviceRef = db.collection("Data").document(userId)
            .collection("OutdoorSensors").document("outdoor")

        outdoorSensorListener = deviceRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(this, "Error fetching outdoor sensor data: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val temperature = snapshot.getDouble("temperature")
                val humidity = snapshot.getDouble("humidity")
                val light = snapshot.getDouble("light")

                tvOutdoorTemperature.text = "Temperature: ${temperature?.toInt()}°C"
                tvOutdoorHumidity.text = "Humidity: ${humidity?.toInt()}%"
                tvOutdoorLight.text = "Light: ${light?.toInt()} lux"

                Toast.makeText(this, "Outdoor data updated", Toast.LENGTH_SHORT).show()
            } else {
                displayNoSensorMessage()
                showCreateOutdoorDeviceDialog()
            }
        }
    }

    private fun displayNoSensorMessage() {
        tvOutdoorTemperature.text = "Temperature: --"
        tvOutdoorHumidity.text = "Humidity: --"
        tvOutdoorLight.text = "Light: --"
    }

    private fun showCreateOutdoorDeviceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Outdoor Device Not Found")
            .setMessage("Would you like to create an outdoor device?")
            .setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(this, AddDeviceCategoryActivity::class.java))
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun openControlActivity(category: String) {
        val intent = when (category) {
            "AC" -> Intent(this, AcControlActivity::class.java)
            "Fan" -> Intent(this, FanControlActivity::class.java)
            "Light" -> Intent(this, LightControlActivity::class.java)
            "Humidifier" -> Intent(this, HumidifierControlActivity::class.java)
            else -> return // If the category doesn't match, we don't redirect
        }

        // Pass category to the next activity
        intent.putExtra("CATEGORY", category)

        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        setupOutdoorSensorListener()
    }

    override fun onPause() {
        super.onPause()
        outdoorSensorListener?.remove()
    }

    override fun onNetworkAvailable() {
        Toast.makeText(this, "Network connection restored", Toast.LENGTH_SHORT).show()
        setupOutdoorSensorListener()
    }

    override fun onNetworkLost() {
        Toast.makeText(this, "Network connection lost", Toast.LENGTH_SHORT).show()
    }
}