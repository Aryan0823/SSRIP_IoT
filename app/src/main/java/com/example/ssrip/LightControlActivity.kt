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

class LightControlActivity : BaseActivity() {
    private lateinit var deviceSelector: Spinner
    private lateinit var powerSwitch: Switch
    private lateinit var brightnessSeekBar: SeekBar
    private lateinit var brightnessTextView: TextView
    private lateinit var roomLightValue: TextView
    private lateinit var powerConsumption: TextView

    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var userId: String
    private var lightDevices: MutableList<String> = mutableListOf()
    private var deviceListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_light_control)

        initializeViews()
        setupListeners()

        db = FirebaseFirestore.getInstance()
        functions = Firebase.functions
        userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: ""
        if (userId.isNotEmpty()) {
            fetchLightDevices()
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews() {
        deviceSelector = findViewById(R.id.deviceSelector)
        powerSwitch = findViewById(R.id.switch3)
        brightnessSeekBar = findViewById(R.id.seekBar)
        brightnessTextView = findViewById(R.id.textView3)
        roomLightValue = findViewById(R.id.roomLightValue)
        powerConsumption = findViewById(R.id.materialCardView3)
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

        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateLightBrightness(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun fetchLightDevices() {
        db.collection("Data").document(userId).collection("Light")
            .get()
            .addOnSuccessListener { documents ->
                lightDevices.clear()
                for (document in documents) {
                    lightDevices.add(document.id)
                }
                updateDeviceSelector()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching light devices: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDeviceSelector() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lightDevices)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSelector.adapter = adapter

        if (lightDevices.isNotEmpty()) {
            setupDeviceListener(lightDevices[0])
        } else {
            displayNoDeviceMessage()
        }
    }

    private fun setupDeviceListener(deviceName: String) {
        deviceListener?.remove()

        val deviceRef = db.collection("Data").document(userId)
            .collection("Light").document(deviceName)

        deviceListener = deviceRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(this, "Error fetching light data: ${e.message}", Toast.LENGTH_SHORT).show()
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
            roomLightValue.text = "${it["roomLight"]} lux"
            brightnessSeekBar.progress = (it["setBrightness"] as? Long)?.toInt() ?: 0
            brightnessTextView.text = "${brightnessSeekBar.progress}%"
            powerConsumption.text = "Check power Consumption"
        }
    }

    private fun displayNoDeviceMessage() {
        powerSwitch.isChecked = false
        roomLightValue.text = "-- lux"
        brightnessSeekBar.progress = 0
        brightnessTextView.text = "0%"
        powerConsumption.text = "Check power Consumption"
        Toast.makeText(this, "No light device data available", Toast.LENGTH_SHORT).show()
    }

    private fun updateDevicePowerState(isOn: Boolean) {
        val selectedDevice = deviceSelector.selectedItem as? String ?: return
        val roomLight = roomLightValue.text.toString().replace(" lux", "").toDoubleOrNull() ?: 0.0

        val data = hashMapOf(
            "userId" to userId,
            "deviceName" to selectedDevice,
            "turnOn" to isOn,
            "roomLight" to roomLight
        )

        functions
            .getHttpsCallable("toggleLight")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<String, Any>
                when (response?.get("status") as? String) {
                    "confirmation_required" -> showLightToggleConfirmationDialog(selectedDevice)
                    "on" -> {
                        powerSwitch.isChecked = true
                        brightnessSeekBar.progress = 50
                        brightnessTextView.text = "50%"
                        Toast.makeText(this, "Light turned on", Toast.LENGTH_SHORT).show()
                    }
                    "off" -> {
                        powerSwitch.isChecked = false
                        brightnessSeekBar.progress = 0
                        brightnessTextView.text = "0%"
                        Toast.makeText(this, "Light turned off", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLightToggleConfirmationDialog(deviceName: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Light Operation")
            .setMessage("Are you sure you want to turn on the Light?")
            .setPositiveButton("Yes") { _, _ ->
                confirmLightToggle(deviceName, true)
            }
            .setNegativeButton("No") { _, _ ->
                confirmLightToggle(deviceName, false)
            }
            .show()
    }

    private fun confirmLightToggle(deviceName: String, confirm: Boolean) {
        val data = hashMapOf(
            "userId" to userId,
            "deviceName" to deviceName,
            "confirm" to confirm
        )

        functions
            .getHttpsCallable("confirmLightToggle")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<String, Any>
                runOnUiThread {
                    powerSwitch.isChecked = response?.get("status") as? String == "on"
                    if (confirm) {
                        brightnessSeekBar.progress = 50
                        brightnessTextView.text = "50%"
                    }
                    Toast.makeText(this, response?.get("message") as? String, Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLightBrightness(brightness: Int) {
        val selectedDevice = deviceSelector.selectedItem as? String ?: return

        val data = hashMapOf(
            "userId" to userId,
            "deviceName" to selectedDevice,
            "newBrightness" to brightness
        )

        functions
            .getHttpsCallable("updateLightBrightness")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<String, Any>
                when (response?.get("status") as? String) {
                    "updated" -> {
                        brightnessTextView.text = "$brightness%"
                        Toast.makeText(this, response["message"] as? String, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onNetworkAvailable() {
        Toast.makeText(this, "Network connection restored", Toast.LENGTH_SHORT).show()
        fetchLightDevices()
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
            fetchLightDevices()
        }
    }
}