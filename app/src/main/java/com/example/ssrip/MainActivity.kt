package com.example.ssrip

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var loginCard: CardView
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        progressBar = findViewById(R.id.progressBar)
        loginCard = findViewById(R.id.loginCard)
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signUpButton)

        // Set click listeners
        loginButton.setOnClickListener { navigateToSignIn() }
        signUpButton.setOnClickListener { navigateToSignUp() }

        // Check user authentication status
        checkUserAuthentication()
    }

    private fun checkUserAuthentication() {
        progressBar.visibility = View.VISIBLE
        loginCard.visibility = View.GONE

        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToDashboard(currentUser.uid)
        } else {
            progressBar.visibility = View.GONE
            loginCard.visibility = View.VISIBLE
        }
    }

    private fun navigateToSignIn() {
        startActivityForResult(Intent(this, SignInActivity::class.java), RC_SIGN_IN)
    }

    private fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    private fun navigateToDashboard(userId: String) {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            putExtra("USER_UID", userId)
        }
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN && resultCode == RESULT_OK) {
            checkUserAuthentication()
        }
    }

    companion object {
        private const val RC_SIGN_IN = 123
    }
}