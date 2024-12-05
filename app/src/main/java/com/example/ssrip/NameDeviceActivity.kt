package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class NameDeviceActivity : BaseActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var userEmail: String
    private lateinit var category: String
    private lateinit var categoryTextView: TextView
    private lateinit var deviceNameEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var existingDevicesRecyclerView: RecyclerView

    private var selectedExistingDevice: String? = null
    private lateinit var existingDevicesAdapter: ExistingDevicesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_name_device)

        initializeViews()
        setupListeners()
        fetchExistingDevices()
    }

    private fun initializeViews() {
        db = FirebaseFirestore.getInstance()
        val userDetails = sessionManager.getUserDetails()
        userId = userDetails[SessionManager.KEY_USER_ID] ?: return
        userEmail = userDetails[SessionManager.KEY_EMAIL] ?: return
        category = intent.getStringExtra("CATEGORY") ?: return

        categoryTextView = findViewById(R.id.categoryTextView)
        deviceNameEditText = findViewById(R.id.deviceNameEditText)
        submitButton = findViewById(R.id.submitButton)
        progressBar = findViewById(R.id.progressBar)
        existingDevicesRecyclerView = findViewById(R.id.existingDevicesRecyclerView)

        categoryTextView.text = "Selected Category: $category"
        existingDevicesRecyclerView.layoutManager = LinearLayoutManager(this)

        // Setup listener for device name edit text to clear existing device selection
        deviceNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && selectedExistingDevice != null) {
                // Clear existing device selection when user starts typing
                existingDevicesAdapter.clearSelection()
                selectedExistingDevice = null
            }
        }

        // Add text change listener to clear existing device selection when typing
        deviceNameEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true && selectedExistingDevice != null) {
                    existingDevicesAdapter.clearSelection()
                    selectedExistingDevice = null
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupListeners() {
        submitButton.setOnClickListener {
            val deviceName = deviceNameEditText.text.toString().trim()

            // Check if an existing device is selected
            if (selectedExistingDevice != null) {
                proceedToNetworkActivity(selectedExistingDevice!!)
            }
            // Check if a new device name is entered
            else if (deviceName.isNotEmpty()) {
                checkDeviceName(deviceName)
            }
            // No device selected or name entered
            else {
                Toast.makeText(this, "Please enter a device name or select an existing device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchExistingDevices() {
        val collectionName = if (category == "OutdoorSensors") "OutdoorSensors" else category

        db.collection("Data").document(userId).collection(collectionName)
            .get()
            .addOnSuccessListener { documents ->
                val deviceNames = documents.map { it.id }
                if (deviceNames.isEmpty()) {
                    existingDevicesRecyclerView.visibility = View.GONE
                    findViewById<TextView>(R.id.orTextView).visibility = View.GONE
                } else {
                    existingDevicesAdapter = ExistingDevicesAdapter(deviceNames) { selectedDevice ->
                        // Clear the edit text when an existing device is selected
                        deviceNameEditText.text.clear()
                        selectedExistingDevice = selectedDevice
                    }
                    existingDevicesRecyclerView.adapter = existingDevicesAdapter
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching devices: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkDeviceName(deviceName: String) {
        showProgressBar()
        db.collection("Data").document(userId).collection(category).document(deviceName).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    hideProgressBar()
                    Toast.makeText(this, "Device name already exists. Please choose another name.", Toast.LENGTH_LONG).show()
                } else {
                    createDevice(deviceName)
                }
            }
            .addOnFailureListener { e ->
                hideProgressBar()
                Toast.makeText(this, "Error checking device name: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createDevice(deviceName: String) {
        val deviceData = when (category) {
            "AC" -> hashMapOf(
                "deviceName" to deviceName,
                "deviceStatus" to "OFF",
                "roomTemperature" to 0,
                "setTemperature" to 0
            )
            "Fan" -> hashMapOf(
                "deviceName" to deviceName,
                "deviceStatus" to "OFF",
                "roomTemperature" to 0,
                "setFanSpeed" to 0
            )
            "Light" -> hashMapOf(
                "deviceName" to deviceName,
                "deviceStatus" to "OFF",
                "roomLight" to 0,
                "setBrightness" to 0
            )
            "Humidifier" -> hashMapOf(
                "deviceName" to deviceName,
                "deviceStatus" to "OFF",
                "roomHumidity" to 0,
                "roomTemperature" to 0,
                "setHumidity" to 0
            )
            "OutdoorSensors" -> hashMapOf(
                "deviceName" to "outdoor",
                "timestamp" to "",
                "temperature" to 0,
                "humidity" to 0,
                "light" to 0
            )
            else -> hashMapOf(
                "deviceName" to deviceName,
                "deviceStatus" to "OFF"
            )
        }

        val collectionName = if (category == "OutdoorSensors") "OutdoorSensors" else category

        db.collection("Data").document(userId).collection(collectionName).document(deviceName)
            .set(deviceData)
            .addOnSuccessListener {
                proceedToNetworkActivity(deviceName)
            }
            .addOnFailureListener { e ->
                hideProgressBar()
                Toast.makeText(this, "Error adding device: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun proceedToNetworkActivity(deviceName: String) {
        hideProgressBar()
        val intent = Intent(this, NetworkActivity::class.java).apply {
            putExtra("CATEGORY", category)
            putExtra("DEVICE_NAME", deviceName)
            putExtra("USER_UID", userId)
            putExtra("USER_EMAIL", userEmail)
        }
        startActivity(intent)
        finish()
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
        submitButton.isEnabled = false
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
        submitButton.isEnabled = true
    }

    // Adapter for existing devices
    inner class ExistingDevicesAdapter(
        private val devices: List<String>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<ExistingDevicesAdapter.DeviceViewHolder>() {

        private var selectedPosition = RecyclerView.NO_POSITION

        fun clearSelection() {
            val previousSelected = selectedPosition
            selectedPosition = RecyclerView.NO_POSITION
            if (previousSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelected)
            }
        }

        inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val deviceNameTextView: TextView = view.findViewById(R.id.deviceNameTextView)

            fun bind(deviceName: String, position: Int) {
                deviceNameTextView.text = deviceName
                itemView.isSelected = position == selectedPosition

                itemView.setOnClickListener {
                    // Clear the edit text when an existing device is selected
                    deviceNameEditText.text.clear()

                    // Update selected position
                    val previousSelected = selectedPosition
                    selectedPosition = adapterPosition

                    // Notify items that need to be redrawn
                    if (previousSelected != RecyclerView.NO_POSITION) {
                        notifyItemChanged(previousSelected)
                    }
                    notifyItemChanged(selectedPosition)

                    // Call the click listener
                    onItemClick(deviceName)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_device, parent, false)
            return DeviceViewHolder(view)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            holder.bind(devices[position], position)
        }

        override fun getItemCount() = devices.size
    }

    override fun onNetworkAvailable() {
        Toast.makeText(this, "Network connection restored", Toast.LENGTH_SHORT).show()
    }

    override fun onNetworkLost() {
        Toast.makeText(this, "Network connection lost", Toast.LENGTH_SHORT).show()
    }
}