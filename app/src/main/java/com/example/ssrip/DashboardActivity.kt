package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : BaseActivity() {
    private lateinit var logoutButton: Button
    private lateinit var welcomeTextView: TextView
    private lateinit var addDeviceButton: FloatingActionButton
    private lateinit var btnAC: ImageButton
    private lateinit var btnFan: ImageButton
    private lateinit var btnHumidifier: ImageButton
    private lateinit var btnLight: ImageButton
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
        addDeviceButton = findViewById(R.id.btnAddDevice)
        btnAC = findViewById(R.id.btnAC)
        btnFan = findViewById(R.id.btnFan)
        btnHumidifier = findViewById(R.id.btnHumidifier)
        btnLight = findViewById(R.id.btnLight)
    }

    private fun setupListeners() {
        logoutButton.setOnClickListener { logout() }
        addDeviceButton.setOnClickListener {
            startActivity(Intent(this, AddDeviceCategoryActivity::class.java))
        }
        btnAC.setOnClickListener { openCategoryDevices("AC") }
        btnFan.setOnClickListener { openCategoryDevices("Fan") }
        btnHumidifier.setOnClickListener { openCategoryDevices("Humidifier") }
        btnLight.setOnClickListener { openCategoryDevices("Light") }
    }

    private fun displayUserInfo() {
        val userDetails = getUserDetails()
        val email = userDetails[SessionManager.KEY_EMAIL]
        welcomeTextView.text = "Welcome, $email"
    }

    private fun openCategoryDevices(category: String) {
        val userId = getUserDetails()[SessionManager.KEY_USER_ID] ?: return
        db.collection("Data").document(userId).collection(category).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No devices found in $category", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, CategoryDevicesActivity::class.java).apply {
                        putExtra("CATEGORY", category)
                        putStringArrayListExtra("DEVICES", ArrayList(documents.map { it.id }))
                    }
                    startActivity(intent)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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