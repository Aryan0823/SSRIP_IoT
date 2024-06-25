package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class AddDeviceCategoryActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device_category)

        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        progressBar = findViewById(R.id.progressBar)

        val acButton: Button = findViewById(R.id.acButton)
        val humidifierButton: Button = findViewById(R.id.humidifierButton)
        val lightButton: Button = findViewById(R.id.lightButton)
        val fanButton: Button = findViewById(R.id.fanButton)

        acButton.setOnClickListener { checkCategoryAndProceed("AC") }
        humidifierButton.setOnClickListener { checkCategoryAndProceed("Humidifier") }
        lightButton.setOnClickListener { checkCategoryAndProceed("Light") }
        fanButton.setOnClickListener { checkCategoryAndProceed("Fan") }
    }

    private fun checkCategoryAndProceed(category: String) {
        showProgressBar()

        val userDocRef = db.collection("Data").document(userId)

        userDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (!documentSnapshot.exists()) {
                // Create the user document if it doesn't exist
                userDocRef.set(mapOf("created" to true))
                    .addOnSuccessListener {
                        createCategoryAndProceed(category, userDocRef)
                    }
                    .addOnFailureListener { e ->
                        hideProgressBar()
                        showError("Error creating user document: ${e.message}")
                    }
            } else {
                createCategoryAndProceed(category, userDocRef)
            }
        }.addOnFailureListener { e ->
            hideProgressBar()
            showError("Error accessing Firestore: ${e.message}")
        }
    }

    private fun createCategoryAndProceed(category: String, userDocRef: DocumentReference) {
        userDocRef.collection(category).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    userDocRef.collection(category)
                        .document("placeholder")
                        .set(mapOf("placeholder" to true))
                        .addOnSuccessListener {
                            hideProgressBar()
                            startAddDeviceActivity(category)
                        }
                        .addOnFailureListener { e ->
                            hideProgressBar()
                            showError("Error creating category: ${e.message}")
                        }
                } else {
                    hideProgressBar()
                    startAddDeviceActivity(category)
                }
            }
            .addOnFailureListener { e ->
                hideProgressBar()
                showError("Error checking category: ${e.message}")
            }
    }

    private fun startAddDeviceActivity(category: String) {
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
}