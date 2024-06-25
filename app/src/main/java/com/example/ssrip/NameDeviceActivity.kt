package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class NameDeviceActivity : AppCompatActivity() {

    private lateinit var categoryValueTextView: TextView
    private lateinit var deviceNameEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var category: String
    private lateinit var uid: String
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_name_device)

        categoryValueTextView = findViewById(R.id.categoryValueTextView)
        deviceNameEditText = findViewById(R.id.deviceNameEditText)
        submitButton = findViewById(R.id.submitButton)
        progressBar = findViewById(R.id.progressBar)

        // Get data from previous activity
        category = intent.getStringExtra("CATEGORY") ?: ""
        uid = intent.getStringExtra("USER_UID") ?: ""

        Log.d("FirestoreDebug", "UID: $uid, Category: $category")

        categoryValueTextView.text = category

        submitButton.setOnClickListener {
            val deviceName = deviceNameEditText.text.toString().trim()
            if (deviceName.isNotEmpty()) {
                checkDeviceNameExists(category, deviceName)
            } else {
                showToast("Please enter a device name")
            }
        }
    }

    private fun checkDeviceNameExists(category: String, deviceName: String) {
        showProgress(true)

        try {
            // Reference to the collection based on category and user ID
            val collectionRef = firestore.collection("users").document(uid).collection("devices").document(category)

            // Query to check if the device name exists
            collectionRef.collection(deviceName).document("info").get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        // Device name already exists
                        showToast("Device name already exists. Please choose another name.")
                        showProgress(false)
                    } else {
                        // Device name does not exist, proceed to add
                        createNewDevice(category, deviceName)
                    }
                }
                .addOnFailureListener { e ->
                    showToast("Error checking device name: ${e.message}")
                    showProgress(false)
                }
        } catch (e: Exception) {
            showToast("An unexpected error occurred: ${e.message}")
            showProgress(false)
        }
    }

    private fun createNewDevice(category: String, deviceName: String) {
        val documentRef = firestore.collection("users").document(uid)
            .collection("devices").document(category)
            .collection(deviceName).document("info")

        val data = hashMapOf(
            "deviceStatus" to "OFF" // Default status when adding a new device
        )

        documentRef.set(data)
            .addOnSuccessListener {
                showToast("Device '$deviceName' added successfully")
                showProgress(false)
                moveToNextActivity(deviceName, category)
            }
            .addOnFailureListener { e ->
                showToast("Error adding device: ${e.message}")
                showProgress(false)
            }
    }



    private fun moveToNextActivity(deviceName: String, category: String) {
        val intent = Intent(this, AddDeviceActivity::class.java).apply {
            putExtra("USER_UID", uid)
            putExtra("DEVICE_NAME", deviceName)
            putExtra("CATEGORY", category)
        }
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        submitButton.isEnabled = !show
    }
}