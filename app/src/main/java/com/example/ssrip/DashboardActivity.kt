package com.example.ssrip

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var tvWelcome: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        tvWelcome = findViewById(R.id.tvWelcome)

        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // If not signed in, redirect to login activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        } else {
            // User is signed in, display welcome message
            tvWelcome.text = "Welcome, ${currentUser.email}"
        }
    }

    fun addDevice(view: View) {
        // Check if user is still authenticated
        if (auth.currentUser != null) {
            val addDeviceIntent = Intent(this, AddDeviceCategoryActivity::class.java)
            startActivity(addDeviceIntent)
        } else {
            // If session expired, redirect to login
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    // You can add more functions here for other dashboard functionalities

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if(currentUser == null){
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}