package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : BaseActivity() {

    private lateinit var logoutButton: Button
    private lateinit var welcomeTextView: TextView
    private lateinit var userIdTextView: TextView
    private lateinit var addDeviceButton: Button
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initializeViews()
        setupListeners()
        displayUserInfo()
        db = FirebaseFirestore.getInstance()
    }

    private fun initializeViews() {
        logoutButton = findViewById(R.id.btnSignOut)
        welcomeTextView = findViewById(R.id.tvWelcome)
        userIdTextView = findViewById(R.id.tvUserId)
        addDeviceButton = findViewById(R.id.btnAddDevice)
    }

    private fun setupListeners() {
        logoutButton.setOnClickListener {
            logout()
        }

        addDeviceButton.setOnClickListener {
            val intent = Intent(this, AddDeviceCategoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun displayUserInfo() {
        val userDetails = getUserDetails()
        val userId = userDetails[SessionManager.KEY_USER_ID]
        val email = userDetails[SessionManager.KEY_EMAIL]

        welcomeTextView.text = "Welcome, $email"
        userIdTextView.text = "User ID: $userId"
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    override fun onNetworkAvailable() {
        Toast.makeText(this, "Network connection restored", Toast.LENGTH_SHORT).show()
        refreshData()
    }

    override fun onNetworkLost() {
        Toast.makeText(this, "Network connection lost", Toast.LENGTH_SHORT).show()
    }

    private fun refreshData() {
        val userId = getUserDetails()[SessionManager.KEY_USER_ID]
        if (userId != null) {
            db.collection("Data").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Update UI with fresh data
                        // For example, update device list or user information
                        Toast.makeText(this, "Data refreshed", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "No data found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error refreshing data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}