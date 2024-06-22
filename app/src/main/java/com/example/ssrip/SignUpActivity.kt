package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var firstNameInputLayout: TextInputLayout
    private lateinit var lastNameInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var phoneNumberInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var termsCheckbox: MaterialCheckBox

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        firstNameInputLayout = findViewById(R.id.firstNameInputLayout)
        lastNameInputLayout = findViewById(R.id.lastNameInputLayout)
        emailInputLayout = findViewById(R.id.emailInputLayout)
        phoneNumberInputLayout = findViewById(R.id.phoneNumberInputLayout)
        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        termsCheckbox = findViewById(R.id.termsCheckbox)

        findViewById<View>(R.id.signUpButton).setOnClickListener { registerUser() }
        findViewById<View>(R.id.signInLinkTextView).setOnClickListener { navigateToSignIn() }
    }

    private fun registerUser() {
        val firstName = firstNameInputLayout.editText?.text.toString().trim()
        val lastName = lastNameInputLayout.editText?.text.toString().trim()
        val email = emailInputLayout.editText?.text.toString().trim()
        val phoneNumber = phoneNumberInputLayout.editText?.text.toString().trim()
        val password = passwordInputLayout.editText?.text.toString().trim()

        if (TextUtils.isEmpty(firstName)) {
            firstNameInputLayout.error = "First Name is required"
            return
        }

        if (TextUtils.isEmpty(lastName)) {
            lastNameInputLayout.error = "Last Name is required"
            return
        }

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.error = "Email is required"
            return
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            phoneNumberInputLayout.error = "Phone Number is required"
            return
        }

        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.error = "Password is required"
            return
        }

        if (!termsCheckbox.isChecked) {
            Toast.makeText(this, "You must agree to the terms of use", Toast.LENGTH_LONG).show()
            return
        }

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
                            Toast.makeText(this, "User registered successfully", Toast.LENGTH_LONG).show()
                            navigateToSignIn()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to register user", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}
