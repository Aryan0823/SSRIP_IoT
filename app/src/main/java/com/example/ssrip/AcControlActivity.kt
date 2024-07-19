package com.example.ssrip

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.appcompat.app.AlertDialog
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class AcControlActivity : BaseActivity() {
    private lateinit var deviceSelector: Spinner
    private lateinit var powerSwitch: Switch
    private lateinit var setTempTextView: TextView
    private lateinit var decreaseTemp: FloatingActionButton
    private lateinit var increaseTemp: FloatingActionButton
    private lateinit var roomTempValue: TextView
    private lateinit var outsideTempValue: TextView
    private lateinit var powerConsumption: TextView

    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var userId: String
    private var acDevices: MutableList<String> = mutableListOf()
    private var deviceListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ac_control)

        initializeViews()
        setupListeners()

        db = FirebaseFirestore.getInstance()
        functions = Firebase.functions
        userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: ""
        if (userId.isNotEmpty()) {
            fetchAcDevices()
            fetchOutsideTemperature()
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews() {
        deviceSelector = findViewById(R.id.deviceSelector)
        powerSwitch = findViewById(R.id.switch3)
        setTempTextView = findViewById(R.id.setTemp)
        decreaseTemp = findViewById(R.id.floatingActionButton2)
        increaseTemp = findViewById(R.id.floatingActionButton3)
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

        decreaseTemp.setOnClickListener {
            updateTemperature(-1)
        }

        increaseTemp.setOnClickListener {
            updateTemperature(1)
        }
    }

    private fun fetchAcDevices() {
        db.collection("Data").document(userId).collection("AC")
            .get()
            .addOnSuccessListener { documents ->
                acDevices.clear()
                for (document in documents) {
                    acDevices.add(document.id)
                }
                updateDeviceSelector()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching AC devices: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDeviceSelector() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, acDevices)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSelector.adapter = adapter

        if (acDevices.isNotEmpty()) {
            setupDeviceListener(acDevices[0])
        } else {
            displayNoDeviceMessage()
        }
    }

    private fun setupDeviceListener(deviceName: String) {
        deviceListener?.remove()

        val deviceRef = db.collection("Data").document(userId)
            .collection("AC").document(deviceName)

        deviceListener = deviceRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(this, "Error fetching AC data: ${e.message}", Toast.LENGTH_SHORT).show()
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
            setTempTextView.text = "${it["setTemperature"]} °C"
            powerSwitch.isChecked = it["deviceStatus"] as? String == "ON"
            roomTempValue.text = "${it["roomTemperature"]} °C"
        }
    }

    private fun displayNoDeviceMessage() {
        setTempTextView.text = "-- °C"
        powerSwitch.isChecked = false
        roomTempValue.text = "-- °C"
        outsideTempValue.text = "-- °C"
        powerConsumption.text = "-- W"
        Toast.makeText(this, "No AC device data available", Toast.LENGTH_SHORT).show()
    }

    private fun updateDevicePowerState(isOn: Boolean) {
        val selectedDevice = deviceSelector.selectedItem as? String ?: return
        val roomTemp = roomTempValue.text.toString().replace(" °C", "").toFloatOrNull() ?: 0f

        val data = hashMapOf(
            "userId" to userId,
            "deviceName" to selectedDevice,
            "turnOn" to isOn,
            "roomTemperature" to roomTemp
        )

        functions
            .getHttpsCallable("toggleAC")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<String, Any>
                when (response?.get("status") as? String) {
                    "confirmation_required" -> showConfirmationDialog(selectedDevice)
                    "on" -> {
                        powerSwitch.isChecked = true
                        Toast.makeText(this, "AC turned on", Toast.LENGTH_SHORT).show()
                        recordPowerState(selectedDevice, true)
                    }
                    "off" -> {
                        powerSwitch.isChecked = false
                        Toast.makeText(this, "AC turned off", Toast.LENGTH_SHORT).show()
                        recordPowerState(selectedDevice, false)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun recordPowerState(deviceName: String, isOn: Boolean) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val date = dateFormat.format(calendar.time)
        val time = timeFormat.format(calendar.time)

        val deviceStatus = if (isOn) "ON" else "OFF"

        val powerStateData = hashMapOf(
            "deviceStatus" to deviceStatus,
            "timestamp" to calendar.timeInMillis
        )

        db.collection("Data").document(userId)
            .collection("AC").document(deviceName)
            .collection(date).document(time)
            .set(powerStateData)
            .addOnSuccessListener {
                Log.d("AcControlActivity", "Power state recorded successfully")
            }
            .addOnFailureListener { e ->
                Log.e("AcControlActivity", "Error recording power state", e)
            }
    }

    private fun showConfirmationDialog(deviceName: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm AC Operation")
            .setMessage("Are you sure you want to turn on the AC?")
            .setPositiveButton("Yes") { _, _ ->
                confirmAcToggle(deviceName, true)
            }
            .setNegativeButton("No") { _, _ ->
                confirmAcToggle(deviceName, false)
            }
            .show()
    }

    private fun confirmAcToggle(deviceName: String, confirm: Boolean) {
        val data = hashMapOf(
            "userId" to userId,
            "deviceName" to deviceName,
            "confirm" to confirm
        )

        functions
            .getHttpsCallable("confirmAC")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<String, Any>
                runOnUiThread {
                    val isOn = response?.get("status") as? String == "on"
                    powerSwitch.isChecked = isOn
                    Toast.makeText(this, response?.get("message") as? String, Toast.LENGTH_SHORT).show()
                    recordPowerState(deviceName, isOn)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTemperature(change: Int) {
        val currentTemp = setTempTextView.text.toString().replace(" °C", "").toIntOrNull() ?: return
        val newTemp = currentTemp + change

        val selectedDevice = deviceSelector.selectedItem as? String ?: return
        db.collection("Data").document(userId).collection("AC").document(selectedDevice)
            .update("setTemperature", newTemp)
            .addOnSuccessListener {
                setTempTextView.text = "$newTemp °C"
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error updating temperature: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchOutsideTemperature() {
        val outdoorRef = db.collection("Data").document(userId)
            .collection("OutdoorSensors").document("outdoor")

        outdoorRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("AcControlActivity", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val temperature = snapshot.getDouble("temperature") ?: Double.NaN
                outsideTempValue.text = if (temperature.isNaN()) "-- °C" else "$temperature °C"
            } else {
                Log.d("AcControlActivity", "Current data: null")
            }
        }
    }

    override fun onNetworkAvailable() {
        Toast.makeText(this, "Network connection restored", Toast.LENGTH_SHORT).show()
        fetchAcDevices()
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
            fetchAcDevices()
            fetchOutsideTemperature()
        }
    }
}