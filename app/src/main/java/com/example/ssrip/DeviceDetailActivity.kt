package com.example.ssrip

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class DeviceDetailActivity : BaseActivity() {

    private lateinit var tvDeviceName: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnToggle: Button
    private lateinit var db: FirebaseFirestore

    private lateinit var userId: String
    private lateinit var category: String
    private lateinit var deviceName: String
    private var deviceStatus: String = "off"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)

        tvDeviceName = findViewById(R.id.tvDeviceName)
        tvCategory = findViewById(R.id.tvCategory)
        tvStatus = findViewById(R.id.tvStatus)
        btnToggle = findViewById(R.id.btnToggle)

        db = FirebaseFirestore.getInstance()

        userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: return
        category = intent.getStringExtra("CATEGORY") ?: return
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: return

        fetchDeviceDetails()

        btnToggle.setOnClickListener {
            toggleDeviceStatus()
        }
    }

    private fun fetchDeviceDetails() {
        db.collection("Data").document(userId)
            .collection(category).document(deviceName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    deviceName = document.getString("name") ?: deviceName
                    category = document.getString("category") ?: category
                    deviceStatus = document.getString("deviceStatus") ?: "off"
                    updateUI()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching device details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI() {
        tvDeviceName.text = deviceName
        tvCategory.text = "Category: $category"
        tvStatus.text = "Status: ${deviceStatus.capitalize()}"
        btnToggle.text = if (deviceStatus == "on") "Turn Off" else "Turn On"
    }

    private fun toggleDeviceStatus() {
        val newStatus = if (deviceStatus == "on") "off" else "on"

        db.collection("Data").document(userId)
            .collection(category).document(deviceName)
            .update("deviceStatus", newStatus)
            .addOnSuccessListener {
                deviceStatus = newStatus
                updateUI()
                Toast.makeText(this, "Device status updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}