package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
        checkOutdoorDevice()
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
        btnAC.setOnClickListener { openCategoryDevices("AC") }
        btnFan.setOnClickListener { openCategoryDevices("Fan") }
        btnHumidifier.setOnClickListener { openCategoryDevices("Humidifier") }
        btnLight.setOnClickListener { openCategoryDevices("Light") }
    }

    private fun displayUserInfo() {
        val userDetails = getUserDetails()
        val email = userDetails[SessionManager.KEY_EMAIL]
        welcomeTextView.text = "Welcome, $email"
    }

    private fun checkOutdoorDevice() {
        val userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: return
        val outdoorSensorRef = db.collection("Data").document(userId).collection("OutdoorSensors")

        outdoorSensorRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documents = task.result
                if (documents != null && !documents.isEmpty) {
                    setupOutdoorSensorListener(userId)
                } else {
                    showCreateOutdoorDeviceDialog()
                    displayNoSensorMessage()
                }
            } else {
                Toast.makeText(this, "Error checking outdoor device: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                displayNoSensorMessage()
            }
        }
    }

    private fun displayNoSensorMessage() {
        tvOutdoorTemperature.text = "Temperature: Sensor not yet created"
        tvOutdoorHumidity.text = "Humidity: Sensor not yet created"
        tvOutdoorLight.text = "Light: Sensor not yet created"
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

    private fun setupOutdoorSensorListener(userId: String) {
        val outdoorSensorRef = db.collection("Data").document(userId).collection("OutdoorSensors")

        outdoorSensorListener = outdoorSensorRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(this, "Error fetching outdoor sensor data: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                for (doc in snapshot.documents) {
                    val temperature = doc.getDouble("Temperature")
                    val humidity = doc.getDouble("Humidity")
                    val light = doc.getDouble("Light")
                    val time = doc.getString("Time")

                    tvOutdoorTemperature.text = "Temperature: ${temperature?.toInt()}°C"
                    tvOutdoorHumidity.text = "Humidity: ${humidity?.toInt()}%"
                    tvOutdoorLight.text = "Light: ${light?.toInt()} lux"

                    Toast.makeText(this, "Outdoor data updated at $time", Toast.LENGTH_SHORT).show()
                }
            } else {
                displayNoSensorMessage()
            }
        }
    }

    private fun openCategoryDevices(category: String) {
        val userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: return
        db.collection("Data").document(userId).collection(category).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No devices found in $category", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, CategoryDevicesActivity::class.java).apply {
                        putExtra("CATEGORY", category)
                        putStringArrayListExtra("DEVICES", ArrayList(documents.map { it.id }))
                    }
                    startActivity(intent)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    override fun onPause() {
        super.onPause()
        outdoorSensorListener?.remove()
    }

    override fun onNetworkAvailable() {
        Toast.makeText(this, "Network connection restored", Toast.LENGTH_SHORT).show()
        refreshData()
    }

    override fun onNetworkLost() {
        Toast.makeText(this, "Network connection lost", Toast.LENGTH_SHORT).show()
    }

    private fun refreshData() {
        val userId = getUserDetails()[SessionManager.KEY_USER_ID]
        if (userId != null) {
            checkOutdoorDevice()
        }
    }
}