package com.example.ssrip

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

class FanControlActivity : BaseActivity() {
    private lateinit var deviceSelector: Spinner
    private lateinit var powerSwitch: Switch
    private lateinit var fanSpeedSeekBar: SeekBar
    private lateinit var fanSpeedTextView: TextView
    private lateinit var roomTempValue: TextView
    private lateinit var outsideTempValue: TextView
    private lateinit var powerConsumption: TextView

    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var userId: String
    private var fanDevices: MutableList<String> = mutableListOf()
    private var deviceListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fan_control)

        initializeViews()
        setupListeners()

        db = FirebaseFirestore.getInstance()
        functions = Firebase.functions
        userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: ""
        if (userId.isNotEmpty()) {
            fetchFanDevices()
            fetchOutsideTemperature()
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews() {
        deviceSelector = findViewById(R.id.deviceSelector)
        powerSwitch = findViewById(R.id.switch3)
        fanSpeedSeekBar = findViewById(R.id.seekBar2)
        fanSpeedTextView = findViewById(R.id.textView3)
        roomTempValue = findViewById(R.id.roomTempValue)
        outsideTempValue = findViewById(R.id.outsideTempValue)
        powerConsumption = findViewById(R.id.powerconsuption)
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

        fanSpeedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateFanSpeed(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun fetchFanDevices() {
        db.collection("Data").document(userId).collection("Fan")
            .get()
            .addOnSuccessListener { documents ->
                fanDevices.clear()
                for (document in documents) {
                    fanDevices.add(document.id)
                }
                updateDeviceSelector()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching fan devices: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDeviceSelector() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fanDevices)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSelector.adapter = adapter

        if (fanDevices.isNotEmpty()) {
            setupDeviceListener(fanDevices[0])
        } else {
            displayNoDeviceMessage()
        }
    }

    private fun setupDeviceListener(deviceName: String) {
        deviceListener?.remove()

        val deviceRef = db.collection("Data").document(userId)
            .collection("Fan").document(deviceName)

        deviceListener = deviceRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(this, "Error fetching fan data: ${e.message}", Toast.LENGTH_SHORT).show()
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
            powerSwitch.isChecked = (it["deviceStatus"] as? String == "ON")
            roomTempValue.text = "${it["roomTemperature"]} °C"
            fanSpeedSeekBar.progress = (it["setFanSpeed"] as? Long)?.toInt() ?: 0
            fanSpeedTextView.text = fanSpeedSeekBar.progress.toString()
            powerConsumption.text = "Check power Consumption"
        }
    }

    private fun displayNoDeviceMessage() {
        powerSwitch.isChecked = false
        roomTempValue.text = "-- °C"
        outsideTempValue.text = "-- °C"
        fanSpeedSeekBar.progress = 0
        fanSpeedTextView.text = "0"
        powerConsumption.text = "Check power Consumption"
        Toast.makeText(this, "No fan device data available", Toast.LENGTH_SHORT).show()
    }

    private fun updateDevicePowerState(isOn: Boolean) {
        val selectedDevice = deviceSelector.selectedItem as? String ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("Data").document(userId)
            .collection("Fan").document(selectedDevice)
            .update("deviceStatus", if (isOn) "ON" else "OFF")
            .addOnSuccessListener {
                // The update was successful, but we don't need to do anything here
                // The Firebase Function will handle logging and notifications
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating fan status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showFanToggleConfirmationDialog(deviceName: String, recommendedSpeed: Int) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Fan Operation")
            .setMessage("Are you sure you want to turn on the Fan?")
            .setPositiveButton("Yes") { _, _ ->
                confirmFanToggle(deviceName, true, recommendedSpeed)
            }
            .setNegativeButton("No") { _, _ ->
                confirmFanToggle(deviceName, false, 0)
            }
            .show()
    }

    private fun confirmFanToggle(deviceName: String, confirm: Boolean, speed: Int) {
        val data = hashMapOf(
            "userId" to userId,
            "deviceName" to deviceName,
            "confirm" to confirm,
            "speed" to speed
        )

        functions
            .getHttpsCallable("confirmFanToggle")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<String, Any>
                runOnUiThread {
                    powerSwitch.isChecked = response?.get("status") as? String == "on"
                    if (confirm) {
                        fanSpeedSeekBar.progress = speed
                        fanSpeedTextView.text = speed.toString()
                    }
                    Toast.makeText(this, response?.get("message") as? String, Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFanSpeed(speed: Int) {
        val selectedDevice = deviceSelector.selectedItem as? String ?: return
        val roomTemp = roomTempValue.text.toString().replace(" °C", "").toDoubleOrNull() ?: 0.0

        val data = hashMapOf(
            "userId" to userId,
            "deviceName" to selectedDevice,
            "newSpeed" to speed,
            "roomTemperature" to roomTemp
        )

        functions
            .getHttpsCallable("updateFanSpeed")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<String, Any>
                when (response?.get("status") as? String) {
                    "confirmation_required" -> showFanSpeedConfirmationDialog(
                        selectedDevice,
                        speed,
                        response["recommendedSpeed"] as? Int ?: 0
                    )
                    "updated" -> {
                        fanSpeedTextView.text = speed.toString()
                        Toast.makeText(this, response["message"] as? String, Toast.LENGTH_SHORT).show()
                    }
                    "off" -> {
                        powerSwitch.isChecked = false
                        fanSpeedSeekBar.progress = 0
                        fanSpeedTextView.text = "0"
                        Toast.makeText(this, "Fan turned off", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showFanSpeedConfirmationDialog(deviceName: String, requestedSpeed: Int, recommendedSpeed: Int) {
        AlertDialog.Builder(this)
            .setTitle("Confirm FanSpeed Operation")
            .setMessage("Are you sure you want to Increase Fan Speed?")
            .setPositiveButton("Yes") { _, _ ->
                confirmFanSpeed(deviceName, requestedSpeed)
            }
            .setNegativeButton("No") { _, _ ->
                fanSpeedSeekBar.progress = recommendedSpeed
                fanSpeedTextView.text = recommendedSpeed.toString()
            }
            .show()
    }

    private fun confirmFanSpeed(deviceName: String, confirmedSpeed: Int) {
        val data = hashMapOf(
            "userId" to userId,
            "deviceName" to deviceName,
            "confirmedSpeed" to confirmedSpeed
        )

        functions
            .getHttpsCallable("confirmFanSpeed")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<String, Any>
                runOnUiThread {
                    when (response?.get("status") as? String) {
                        "updated" -> {
                            fanSpeedTextView.text = confirmedSpeed.toString()
                            Toast.makeText(this, response["message"] as? String, Toast.LENGTH_SHORT).show()
                        }
                        "off" -> {
                            powerSwitch.isChecked = false
                            fanSpeedSeekBar.progress = 0
                            fanSpeedTextView.text = "0"
                            Toast.makeText(this, "Fan turned off", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchOutsideTemperature() {
        val outdoorRef = db.collection("Data").document(userId)
            .collection("OutdoorSensors").document("outdoor")

        outdoorRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("FanControlActivity", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val temperature = snapshot.getDouble("temperature") ?: Double.NaN
                outsideTempValue.text = if (temperature.isNaN()) "-- °C" else "$temperature °C"
            } else {
                Log.d("FanControlActivity", "Current data: null")
            }
        }
    }

    override fun onNetworkAvailable() {
        Toast.makeText(this, "Network connection restored", Toast.LENGTH_SHORT).show()
        fetchFanDevices()
        fetchOutsideTemperature()
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
            fetchFanDevices()
            fetchOutsideTemperature()
        }
    }
}