package com.example.ssrip

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import androidx.appcompat.app.AlertDialog

class HumidifierControlActivity : BaseActivity() {
    private lateinit var deviceSelector: Spinner
    private lateinit var powerSwitch: Switch
    private lateinit var setHumidityTextView: TextView
    private lateinit var decreaseHumidity: FloatingActionButton
    private lateinit var increaseHumidity: FloatingActionButton
    private lateinit var roomHumValue: TextView
    private lateinit var outsideHumValue: TextView
    private lateinit var roomTempValue: TextView

    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var userId: String
    private var humidifierDevices: MutableList<String> = mutableListOf()
    private var deviceListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_humidifier_control)

        initializeViews()
        setupListeners()

        db = FirebaseFirestore.getInstance()
        functions = Firebase.functions
        userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: ""
        if (userId.isNotEmpty()) {
            fetchHumidifierDevices()
            fetchOutsideHumidity()
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
        outsideHumValue = findViewById(R.id.outsideHumValue)
        roomTempValue = findViewById(R.id.roomTempValue)
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
            roomTempValue.text = "${it["roomTemperature"]} °C"
        }
    }

    private fun displayNoDeviceMessage() {
        setHumidityTextView.text = "-- %"
        powerSwitch.isChecked = false
        roomHumValue.text = "-- %"
        outsideHumValue.text = "-- %"
        roomTempValue.text = "-- °C"
        Toast.makeText(this, "No humidifier device data available", Toast.LENGTH_SHORT).show()
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
            .getHttpsCallable("toggleHumidifier")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<String, Any>
                when (response?.get("status") as? String) {
                    "confirmation_required" -> showConfirmationDialog(selectedDevice, response["message"] as? String, response["recommendedHumidity"] as? Int)
                    "on" -> powerSwitch.isChecked = true
                    "off" -> powerSwitch.isChecked = false
                }
                Toast.makeText(this, response?.get("message") as? String, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showConfirmationDialog(deviceName: String, message: String?, recommendedHumidity: Int?) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Humidifier Operation")
            .setMessage("$message\nRecommended humidity: $recommendedHumidity%")
            .setPositiveButton("Yes") { _, _ ->
                confirmHumidifierToggle(deviceName, true, recommendedHumidity)
            }
            .setNegativeButton("No") { _, _ ->
                powerSwitch.isChecked = false
            }
            .show()
    }

    private fun confirmHumidifierToggle(deviceName: String, confirm: Boolean, recommendedHumidity: Int?) {
        val data = hashMapOf(
            "userId" to userId,
            "deviceName" to deviceName,
            "confirm" to confirm,
            "recommendedHumidity" to recommendedHumidity
        )

        functions
            .getHttpsCallable("confirmHumidifier")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<String, Any>
                runOnUiThread {
                    powerSwitch.isChecked = response?.get("status") as? String == "on"
                    setHumidityTextView.text = "${response?.get("humidity")} %"
                    Toast.makeText(this, response?.get("message") as? String, Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateHumidity(change: Int) {
        val currentHumidity = setHumidityTextView.text.toString().replace(" %", "").toIntOrNull() ?: return
        val newHumidity = (currentHumidity + change).coerceIn(0, 100)

        val selectedDevice = deviceSelector.selectedItem as? String ?: return
        val roomTemp = roomTempValue.text.toString().replace(" °C", "").toFloatOrNull() ?: 0f

        val data = hashMapOf(
            "userId" to userId,
            "deviceName" to selectedDevice,
            "newHumidity" to newHumidity,
            "roomTemperature" to roomTemp
        )

        functions
            .getHttpsCallable("updateHumidity")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<String, Any>
                when (response?.get("status") as? String) {
                    "confirmation_required" -> showHumidityConfirmationDialog(selectedDevice, response["message"] as? String, newHumidity)
                    "updated" -> {
                        setHumidityTextView.text = "$newHumidity %"
                        Toast.makeText(this, response["message"] as? String, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating humidity: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showHumidityConfirmationDialog(deviceName: String, message: String?, newHumidity: Int) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Humidity Change")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                confirmHumidityChange(deviceName, newHumidity)
            }
            .setNegativeButton("No") { _, _ ->
                // Do nothing, keep the current humidity
            }
            .show()
    }

    private fun confirmHumidityChange(deviceName: String, newHumidity: Int) {
        val data = hashMapOf(
            "userId" to userId,
            "deviceName" to deviceName,
            "newHumidity" to newHumidity
        )

        functions
            .getHttpsCallable("confirmHumidityChange")
            .call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<String, Any>
                runOnUiThread {
                    setHumidityTextView.text = "$newHumidity %"
                    Toast.makeText(this, response?.get("message") as? String, Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
        fetchOutsideHumidity()
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
            fetchOutsideHumidity()
        }
    }
}