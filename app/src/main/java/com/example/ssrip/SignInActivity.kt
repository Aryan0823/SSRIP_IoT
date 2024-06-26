package com.example.ssrip

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignInActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var signInButton: Button
    private lateinit var forgotPasswordText: TextView
    private lateinit var signUpLink: TextView
    private lateinit var loadingProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Initialize views
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        signInButton = findViewById(R.id.signInButton)
        forgotPasswordText = findViewById(R.id.forgotPasswordText)
        signUpLink = findViewById(R.id.signUpLink)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        // Set click listeners
        signInButton.setOnClickListener {
            signIn()
        }

        forgotPasswordText.setOnClickListener {
            // Handle forgot password action here
            // For now, we'll just show a toast
            Toast.makeText(this, "Forgot password functionality not implemented yet", Toast.LENGTH_SHORT).show()
        }

        signUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    private fun signIn() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            emailInput.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            passwordInput.requestFocus()
            return
        }

        showLoading()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                hideLoading()
                if (task.isSuccessful) {
                    // Sign in success, return to MainActivity
                    setResult(RESULT_OK)
                    finish()
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showLoading() {
        loadingProgressBar.visibility = View.VISIBLE
        signInButton.isEnabled = false
    }

    private fun hideLoading() {
        loadingProgressBar.visibility = View.GONE
        signInButton.isEnabled = true
    }
}