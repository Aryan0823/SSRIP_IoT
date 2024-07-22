package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class SignUpActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var phoneEditText: EditText

    private lateinit var signupButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var signInLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        firstNameEditText = findViewById(R.id.firstNameInput)
        lastNameEditText = findViewById(R.id.lastNameInput)
        emailEditText = findViewById(R.id.emailInput)
        phoneEditText = findViewById(R.id.phoneNumberInput)
        passwordEditText = findViewById(R.id.passwordInput)

        signupButton = findViewById(R.id.signUpButton)
        progressBar = findViewById(R.id.progressBar)
        signInLink = findViewById(R.id.signInLink)

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val firstName = firstNameEditText.text.toString().trim()
            val lastName = lastNameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = ProgressBar.VISIBLE

            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val userData = hashMapOf(
                            "firstname" to firstName,
                            "lastname" to lastName,
                            "phonenumber" to phone,
                            "current date" to Date(),
                            "email" to email
                        )

                        db.collection("users").document(it.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                progressBar.visibility = ProgressBar.GONE
                                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = ProgressBar.GONE
                                Toast.makeText(this, "Failed to store user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        signInLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}