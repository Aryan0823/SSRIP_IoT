package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class AddDeviceCategoryActivity : BaseActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var progressBar: ProgressBar
    private lateinit var outdoorDeviceButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device_category)

        db = FirebaseFirestore.getInstance()
        progressBar = findViewById(R.id.progressBar)
        outdoorDeviceButton = findViewById(R.id.mainButton)

        setupCategoryButtons()
        checkOutdoorDeviceExistence()
    }

    private fun setupCategoryButtons() {
        val categories = listOf("AC", "Humidifier", "Light", "Fan")
        categories.forEach { category ->
            val buttonId = "${category.toLowerCase()}Button"
            findViewById<Button>(resources.getIdentifier(buttonId, "id", packageName))
                .setOnClickListener { checkCategoryAndProceed(category) }
        }

        outdoorDeviceButton.setOnClickListener { handleOutdoorDeviceClick() }
    }

    private fun checkOutdoorDeviceExistence() {
        showProgressBar()
        val userId = sessionManager.getUserDetails()[SessionManager.KEY_USER_ID] ?: return
        val outdoorDeviceRef = db.collection("Data").document(userId)
            .collection("OutdoorSensors").document("outdoor")

        outdoorDeviceRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                outdoorDeviceButton.isEnabled = false
                outdoorDeviceButton.text = "Outdoor Device (Already Exists)"
            } else {
                outdoorDeviceButton.isEnabled = true
                outdoorDeviceButton.text = "Add Outdoor Device"
            }
            hideProgressBar()
        }.addOnFailureListener { e ->
            hideProgressBar()
            showError("Error checking outdoor device: ${e.message}")
        }
    }

    private fun handleOutdoorDeviceClick() {
        val userId = sessionManager.getUserDetails()[SessionManager.KEY_USER_ID] ?: return
        val outdoorDeviceRef = db.collection("Data").document(userId)
            .collection("OutdoorSensors").document("outdoor")

        showProgressBar()
        outdoorDeviceRef.get().addOnSuccessListener { documentSnapshot ->
            hideProgressBar()
            if (!documentSnapshot.exists()) {
                // Redirect to AddDeviceActivity with OutdoorSensors category and outdoor device name
                val intent = Intent(this, AddDeviceActivity::class.java).apply {
                    putExtra("CATEGORY", "OutdoorSensors")
                    putExtra("DEVICE_NAME", "outdoor")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Outdoor device already exists", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            hideProgressBar()
            showError("Error checking outdoor device: ${e.message}")
        }
    }

    private fun checkCategoryAndProceed(category: String) {
        showProgressBar()
        val userId = sessionManager.getUserDetails()[SessionManager.KEY_USER_ID] ?: return
        val userDocRef = db.collection("Data").document(userId)

        userDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (!documentSnapshot.exists()) {
                createUserDocumentAndProceed(userDocRef, category)
            } else {
                createCategoryAndProceed(category, userDocRef)
            }
        }.addOnFailureListener { e ->
            hideProgressBar()
            showError("Error accessing Firestore: ${e.message}")
        }
    }

    private fun createUserDocumentAndProceed(userDocRef: DocumentReference, category: String) {
        userDocRef.set(mapOf("created" to true))
            .addOnSuccessListener {
                createCategoryAndProceed(category, userDocRef)
            }
            .addOnFailureListener { e ->
                hideProgressBar()
                showError("Error creating user document: ${e.message}")
            }
    }

    private fun createCategoryAndProceed(category: String, userDocRef: DocumentReference) {
        val collectionName = if (category == "OutDoor Device") "OutdoorSensors" else category
        userDocRef.collection(collectionName).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    userDocRef.collection(collectionName)
                        .document("placeholder")
                        .set(mapOf("placeholder" to false))
                        .addOnSuccessListener {
                            hideProgressBar()
                            startNameDeviceActivity(category)
                        }
                        .addOnFailureListener { e ->
                            hideProgressBar()
                            showError("Error creating category: ${e.message}")
                        }
                } else {
                    hideProgressBar()
                    startNameDeviceActivity(category)
                }
            }
            .addOnFailureListener { e ->
                hideProgressBar()
                showError("Error checking category: ${e.message}")
            }
    }

    private fun startNameDeviceActivity(category: String) {
        val intent = Intent(this, NameDeviceActivity::class.java).apply {
            putExtra("CATEGORY", category)
        }
        startActivity(intent)
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onNetworkAvailable() {
        checkOutdoorDeviceExistence()
    }

    override fun onNetworkLost() {
        Toast.makeText(this, "Network connection lost", Toast.LENGTH_SHORT).show()
    }
}