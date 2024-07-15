package com.example.ssrip

import android.os.Bundle
import android.view.View
import android.widget.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LightControlActivity : BaseActivity() {
    private lateinit var deviceSelector: Spinner
    private lateinit var powerSwitch: Switch
    private lateinit var brightnessSeekBar: SeekBar
    private lateinit var brightnessTextView: TextView
    private lateinit var roomTempValue: TextView
    private lateinit var outsideTempValue: TextView
    private lateinit var powerConsumption: TextView

    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String
    private var lightDevices: MutableList<String> = mutableListOf()
    private var deviceListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_light_control)

        initializeViews()
        setupListeners()

        db = FirebaseFirestore.getInstance()
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
        roomTempValue = findViewById(R.id.roomTempValue)
        outsideTempValue = findViewById(R.id.outsideTempValue)
        powerConsumption = findViewById(R.id.powerconsuption)

        brightnessSeekBar.max = 100 // Set max brightness to 100%
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
                    updateBrightness(progress)
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
            roomTempValue.text = "${it["roomLight"]} lux"
            brightnessSeekBar.progress = (it["setBrightness"] as? Long)?.toInt() ?: 0
            brightnessTextView.text = "${brightnessSeekBar.progress} %"
            // Note: outdoorTemperature is not in the provided structure, so we'll leave it out
            outsideTempValue.text = "-- °C"
            powerConsumption.text = "Check power Consumption"
        }
    }

    private fun displayNoDeviceMessage() {
        powerSwitch.isChecked = false
        roomTempValue.text = "-- lux"
        outsideTempValue.text = "-- °C"
        brightnessSeekBar.progress = 0
        brightnessTextView.text = "0 %"
        powerConsumption.text = "Check power Consumption"
        Toast.makeText(this, "No light device data available", Toast.LENGTH_SHORT).show()
    }

    private fun updateDevicePowerState(isOn: Boolean) {
        val selectedDevice = deviceSelector.selectedItem as? String ?: return
        db.collection("Data").document(userId).collection("Light").document(selectedDevice)
            .update("deviceStatus", if (isOn) "ON" else "OFF")
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error updating power state: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBrightness(brightness: Int) {
        val selectedDevice = deviceSelector.selectedItem as? String ?: return
        db.collection("Data").document(userId).collection("Light").document(selectedDevice)
            .update("setBrightness", brightness)
            .addOnSuccessListener {
                brightnessTextView.text = "$brightness %"
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error updating brightness: ${exception.message}", Toast.LENGTH_SHORT).show()
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