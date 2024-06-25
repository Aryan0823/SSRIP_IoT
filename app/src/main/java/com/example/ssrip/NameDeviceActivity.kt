package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NameDeviceActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var category: String
    private lateinit var categoryTextView: TextView
    private lateinit var deviceNameEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_name_device)

        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        category = intent.getStringExtra("CATEGORY") ?: return

        categoryTextView = findViewById(R.id.categoryTextView)
        deviceNameEditText = findViewById(R.id.deviceNameEditText)
        submitButton = findViewById(R.id.submitButton)
        progressBar = findViewById(R.id.progressBar)

        // Display the selected category
        categoryTextView.text = "Selected Category: $category"

        submitButton.setOnClickListener {
            val deviceName = deviceNameEditText.text.toString().trim()
            if (deviceName.isNotEmpty()) {
                checkDeviceName(deviceName)
            } else {
                Toast.makeText(this, "Please enter a device name", Toast.LENGTH_SHORT).show()
            }
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
        val deviceData = hashMapOf(
            "name" to deviceName,
            "category" to category,
            "deviceStatus" to "OFF"  // Adding deviceStatus field
        )

        db.collection("Data").document(userId).collection(category).document(deviceName)
            .set(deviceData)
            .addOnSuccessListener {
                hideProgressBar()
                Toast.makeText(this, "Device added successfully", Toast.LENGTH_SHORT).show()
                // Passing category and device name to AddDeviceActivity
                val intent = Intent(this, AddDeviceActivity::class.java).apply {
                    putExtra("CATEGORY", category)
                    putExtra("DEVICE_NAME", deviceName)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                hideProgressBar()
                Toast.makeText(this, "Error adding device: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
        submitButton.isEnabled = false
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
        submitButton.isEnabled = true
    }
}