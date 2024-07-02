package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth

class MainActivity : BaseActivity() {

    private lateinit var progressBar: View
    private lateinit var loginCard: CardView
    private lateinit var loginButton: View
    private lateinit var signUpButton: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)
        loginCard = findViewById(R.id.loginCard)
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signUpButton)

        checkLoginStatus()

        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
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
}
