package com.example.ssrip

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class CategoryDevicesActivity : BaseActivity() {

    private lateinit var tvCategory: TextView
    private lateinit var lvDevices: ListView
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_devices)

        tvCategory = findViewById(R.id.tvCategory)
        lvDevices = findViewById(R.id.lvDevices)

        db = FirebaseFirestore.getInstance()

        val category = intent.getStringExtra("CATEGORY") ?: return
        val devices = intent.getStringArrayListExtra("DEVICES") ?: return

        tvCategory.text = category
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devices)
        lvDevices.adapter = adapter

        lvDevices.setOnItemClickListener { _, _, position, _ ->
            val deviceName = devices[position]
            toggleDeviceStatus(category, deviceName)
        }
    }

    private fun toggleDeviceStatus(category: String, deviceName: String) {
        val userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: return
        val deviceRef = db.collection("Data").document(userId)
            .collection(category).document(deviceName)

        deviceRef.get().addOnSuccessListener { document ->
            val currentStatus = document.getString("status") ?: "off"
            val newStatus = if (currentStatus == "on") "off" else "on"

            deviceRef.update("status", newStatus)
                .addOnSuccessListener {
                    Toast.makeText(this, "Device status updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

}