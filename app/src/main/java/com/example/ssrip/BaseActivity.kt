package com.example.ssrip

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.content.Context

open class BaseActivity : AppCompatActivity() {
    protected lateinit var sessionManager: SessionManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (!sessionManager.checkLogin() && this !is MainActivity && this !is LoginActivity
            && this !is SignUpActivity && this !is DashboardActivity
        ) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setupNetworkCallback()
    }

    private fun setupNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    onNetworkAvailable()
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    onNetworkLost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerNetworkCallback()
    }

    override fun onPause() {
        super.onPause()
        unregisterNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun unregisterNetworkCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    protected open fun onNetworkAvailable() {
        // Override this in child classes to handle network availability
    }

    protected open fun onNetworkLost() {
        // Override this in child classes to handle network loss
    }

    protected fun getUserDetails(): HashMap<String, String?> {
        return sessionManager.getUserDetails()
    }

    protected fun logout() {
        sessionManager.logoutUser()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}