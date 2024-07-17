package com.example.ssrip

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : BaseActivity() {
    private lateinit var progressBar: View
    private lateinit var loginCard: CardView
    private lateinit var loginButton: View
    private lateinit var signUpButton: View

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)
        loginCard = findViewById(R.id.loginCard)
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signUpButton)

        checkNotificationPermission()
        checkLoginStatus()

        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // Request FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                saveFCMTokenToFirestore(token)
            } else {
                println("Error getting FCM token: ${task.exception?.message}")
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkLoginStatus() {
        progressBar.isVisible = true
        loginCard.isVisible = false
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userId = user.uid
            val intent = Intent(this, DashboardActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        } else {
            progressBar.isVisible = false
            loginCard.isVisible = true
        }
    }

    private fun saveFCMTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    println("FCM token updated successfully")
                }
                .addOnFailureListener { e ->
                    println("Error updating FCM token: ${e.message}")
                }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                // You can handle post-permission granted tasks here if needed
            } else {
                // Permission denied
                // Optionally inform the user about the necessity of the permission
            }
        }
    }
}