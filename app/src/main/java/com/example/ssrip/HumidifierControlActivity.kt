package com.example.ssrip

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HumidifierControlActivity : BaseActivity() {
    private lateinit var deviceSelector: Spinner
    private lateinit var powerSwitch: Switch
    private lateinit var setHumidityTextView: TextView
    private lateinit var decreaseHumidity: FloatingActionButton
    private lateinit var increaseHumidity: FloatingActionButton
    private lateinit var roomHumValue: TextView

    private lateinit var powerConsumption: TextView
    private lateinit var outsideHumValue: TextView
    private lateinit var roomTempValue: TextView  // New TextView for room temperature

    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String
    private var humidifierDevices: MutableList<String> = mutableListOf()
    private var deviceListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_humidifier_control)

        initializeViews()
        setupListeners()

        db = FirebaseFirestore.getInstance()
        userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: ""
        if (userId.isNotEmpty()) {
            fetchHumidifierDevices()
            fetchOutsideHumidity() // Add this line to fetch and display the outside humidity
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews() {
        deviceSelector = findViewById(R.id.deviceSelector)
        powerSwitch = findViewById(R.id.switch3)
        setHumidityTextView = findViewById(R.id.textView3)
        decreaseHumidity = findViewById(R.id.floatingActionButton2)
        increaseHumidity = findViewById(R.id.floatingActionButton3)
        roomHumValue = findViewById(R.id.roomHumValue)

        powerConsumption = findViewById(R.id.powerconsuption)
        outsideHumValue = findViewById(R.id.outsideHumValue)
        roomTempValue = findViewById(R.id.roomTempValue)  // Initialize the new TextView
    }

    private fun setupListeners() {
        deviceSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedDevice = parent.getItemAtPosition(position) as String
                setupDeviceListener(selectedDevice)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        powerSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateDevicePowerState(isChecked)
        }

        decreaseHumidity.setOnClickListener {
            updateHumidity(-5)
        }

        increaseHumidity.setOnClickListener {
            updateHumidity(5)
        }
    }

    private fun fetchHumidifierDevices() {
        db.collection("Data").document(userId).collection("Humidifier")
            .get()
            .addOnSuccessListener { documents ->
                humidifierDevices.clear()
                for (document in documents) {
                    humidifierDevices.add(document.id)
                }
                updateDeviceSelector()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching humidifier devices: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDeviceSelector() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, humidifierDevices)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSelector.adapter = adapter

        if (humidifierDevices.isNotEmpty()) {
            setupDeviceListener(humidifierDevices[0])
        } else {
            displayNoDeviceMessage()
        }
    }

    private fun setupDeviceListener(deviceName: String) {
        deviceListener?.remove()

        val deviceRef = db.collection("Data").document(userId)
            .collection("Humidifier").document(deviceName)

        deviceListener = deviceRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(this, "Error fetching humidifier data: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                updateUIWithDeviceData(snapshot.data)
            } else {
                displayNoDeviceMessage()
            }
        }
    }

    private fun updateUIWithDeviceData(data: Map<String, Any>?) {
        data?.let {
            setHumidityTextView.text = "${it["setHumidity"]} %"
            powerSwitch.isChecked = it["deviceStatus"] as? String == "ON"
            roomHumValue.text = "${it["roomHumidity"]} %"
            roomTempValue.text = "${it["roomTemperature"]} °C"  // Update room temperature value

            powerConsumption.text = "Check power Consumption"
        }
    }

    private fun displayNoDeviceMessage() {
        setHumidityTextView.text = "-- %"
        powerSwitch.isChecked = false
        roomHumValue.text = "-- %"
        outsideHumValue.text = "-- %"
        roomTempValue.text = "-- °C"  // Display default message for room temperature
        powerConsumption.text = "Check power Consumption"
        Toast.makeText(this, "No humidifier device data available", Toast.LENGTH_SHORT).show()
    }

    private fun updateDevicePowerState(isOn: Boolean) {
        val selectedDevice = deviceSelector.selectedItem as? String ?: return
        db.collection("Data").document(userId).collection("Humidifier").document(selectedDevice)
            .update("deviceStatus", if (isOn) "ON" else "OFF")
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error updating power state: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateHumidity(change: Int) {
        val currentHumidity = setHumidityTextView.text.toString().replace(" %", "").toIntOrNull() ?: return
        val newHumidity = (currentHumidity + change).coerceIn(0, 100)

        val selectedDevice = deviceSelector.selectedItem as? String ?: return
        db.collection("Data").document(userId).collection("Humidifier").document(selectedDevice)
            .update("setHumidity", newHumidity)
            .addOnSuccessListener {
                setHumidityTextView.text = "$newHumidity %"
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error updating humidity: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchOutsideHumidity() {
        val outdoorRef = db.collection("Data").document(userId)
            .collection("OutdoorSensors").document("outdoor")

        outdoorRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("HumidifierControlActivity", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val humidity = snapshot.getDouble("humidity") ?: Double.NaN
                outsideHumValue.text = if (humidity.isNaN()) "-- %" else "$humidity %"
            } else {
                Log.d("HumidifierControlActivity", "Current data: null")
            }
        }
    }

    override fun onNetworkAvailable() {
        Toast.makeText(this, "Network connection restored", Toast.LENGTH_SHORT).show()
        fetchHumidifierDevices()
        fetchOutsideHumidity() // Fetch outside humidity when network is available
    }

    override fun onNetworkLost() {
        Toast.makeText(this, "Network connection lost", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        deviceListener?.remove()
    }

    override fun onResume() {
        super.onResume()
        if (userId.isNotEmpty()) {
            fetchHumidifierDevices()
            fetchOutsideHumidity() // Fetch outside humidity when resuming the activity
        }
    }
}
