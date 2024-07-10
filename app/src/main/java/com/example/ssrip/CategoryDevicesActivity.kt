package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
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
        val outdoorSensorDevice = intent.getStringExtra("OUTDOOR_SENSOR_DEVICE") ?: ""

        tvCategory.text = category
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devices)
        lvDevices.adapter = adapter

        lvDevices.setOnItemClickListener { _, _, position, _ ->
            val deviceName = devices[position]
            redirectToAppropriateActivity(category, deviceName, outdoorSensorDevice)
        }
    }

    private fun redirectToAppropriateActivity(category: String, deviceName: String, outdoorSensorDevice: String) {
        val intent = when (category) {
            "AC" -> Intent(this, AcControlActivity::class.java)
            "Fan" -> Intent(this, FanControlActivity::class.java)
            "Light" -> Intent(this, LightControlActivity::class.java)
            "Humidifier" -> Intent(this, HumidifierControlActivity::class.java)
            else -> return // If the category doesn't match, we don't redirect
        }

        // Pass category, device name, and outdoor sensor information to the next activity
        intent.putExtra("CATEGORY", category)
        intent.putExtra("DEVICE_NAME", deviceName)
        intent.putExtra("OUTDOOR_SENSOR_CATEGORY", "OutdoorSensors")
        intent.putExtra("OUTDOOR_SENSOR_DEVICE", outdoorSensorDevice)

        startActivity(intent)
    }
}