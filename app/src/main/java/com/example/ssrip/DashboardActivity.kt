package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.view.View
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
    private lateinit var spinnerOutdoorDevices: Spinner
    private lateinit var db: FirebaseFirestore
    private var outdoorSensorListener: ListenerRegistration? = null
    private var deviceList: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        initializeViews()
        setupListeners()
        displayUserInfo()
        db = FirebaseFirestore.getInstance()
        fetchOutdoorDevices()
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
        spinnerOutdoorDevices = findViewById(R.id.spinnerOutdoorDevices)
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

        spinnerOutdoorDevices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedDevice = parent.getItemAtPosition(position) as String
                setupOutdoorSensorListener(selectedDevice)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun displayUserInfo() {
        val userDetails = getUserDetails()
        val email = userDetails[SessionManager.KEY_EMAIL]
        welcomeTextView.text = "Welcome, $email"
    }

    private fun fetchOutdoorDevices() {
        val userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: return
        db.collection("Data").document(userId).collection("OutdoorSensors")
            .get()
            .addOnSuccessListener { documents ->
                deviceList.clear()
                for (document in documents) {
                    deviceList.add(document.id)
                }
                updateSpinner()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching devices: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun updateSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOutdoorDevices.adapter = adapter

        if (deviceList.isNotEmpty()) {
            setupOutdoorSensorListener(deviceList[0])
        } else {
            displayNoSensorMessage()
        }
    }
    private fun displayNoSensorMessage() {
        tvOutdoorTemperature.text = "Temperature: No data"
        tvOutdoorHumidity.text = "Humidity: No data"
        tvOutdoorLight.text = "Light: No data"
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

    private fun setupOutdoorSensorListener(deviceName: String) {
        outdoorSensorListener?.remove()

        val userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: return
        val deviceRef = db.collection("Data").document(userId)
            .collection("OutdoorSensors").document(deviceName)

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

                Toast.makeText(this, "Outdoor data updated for $deviceName", Toast.LENGTH_SHORT).show()
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
            fetchOutdoorDevices()
        }
    }
}