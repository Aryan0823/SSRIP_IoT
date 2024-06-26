package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var firstNameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var phoneNumberInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var termsCheckbox: MaterialCheckBox
    private lateinit var signUpButton: Button
    private lateinit var signInLink: View
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        firstNameInput = findViewById(R.id.firstNameInput)
        lastNameInput = findViewById(R.id.lastNameInput)
        emailInput = findViewById(R.id.emailInput)
        phoneNumberInput = findViewById(R.id.phoneNumberInput)
        passwordInput = findViewById(R.id.passwordInput)
        termsCheckbox = findViewById(R.id.termsCheckbox)
        signUpButton = findViewById(R.id.signUpButton)
        signInLink = findViewById(R.id.signInLink)
        progressBar = findViewById(R.id.progressBar)

        signUpButton.setOnClickListener { registerUser() }
        signInLink.setOnClickListener { navigateToSignIn() }
    }

    private fun registerUser() {
        val firstName = firstNameInput.text.toString().trim()
        val lastName = lastNameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val phoneNumber = phoneNumberInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (TextUtils.isEmpty(firstName)) {
            firstNameInput.error = "First Name is required"
            return
        }

        if (TextUtils.isEmpty(lastName)) {
            lastNameInput.error = "Last Name is required"
            return
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.error = "Email is required"
            return
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            phoneNumberInput.error = "Phone Number is required"
            return
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.error = "Password is required"
            return
        }

        if (!termsCheckbox.isChecked) {
            Toast.makeText(this, "You must agree to the terms of use", Toast.LENGTH_LONG).show()
            return
        }

        showLoading()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid ?: return@addOnCompleteListener

                    val userMap = hashMapOf(
                        "firstname" to firstName,
                        "lastname" to lastName,
                        "email" to email,
                        "mobilenumber" to phoneNumber
                    )

                    firestore.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            hideLoading()
                            Toast.makeText(this, "User registered successfully", Toast.LENGTH_LONG).show()
                            navigateToSignIn()
                        }
                        .addOnFailureListener {
                            hideLoading()
                            Toast.makeText(this, "Failed to register user", Toast.LENGTH_LONG).show()
                        }
                } else {
                    hideLoading()
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        signUpButton.isEnabled = false
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        signUpButton.isEnabled = true
    }
}