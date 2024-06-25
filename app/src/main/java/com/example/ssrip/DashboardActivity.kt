package com.example.ssrip

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DashboardActivity : AppCompatActivity() {

    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Get the UID from the intent
        uid = intent.getStringExtra("USER_UID") ?: ""

        val fab: FloatingActionButton = findViewById(R.id.floatingActionButton)
        fab.setOnClickListener { showDevicePopup() }
    }

    private fun showDevicePopup() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.popup_device_selection)

        val acButton: Button = dialog.findViewById(R.id.acButton)
        val humidifierButton: Button = dialog.findViewById(R.id.humidifierButton)
        val lightButton: Button = dialog.findViewById(R.id.lightButton)
        val fanButton: Button = dialog.findViewById(R.id.fanButton)

        acButton.setOnClickListener {
            startActivity(Intent(this, AddDeviceActivity::class.java).apply {
                putExtra("USER_UID", uid)
            })
            dialog.dismiss()
        }

        humidifierButton.setOnClickListener {
            startActivity(Intent(this, AddDeviceActivity::class.java).apply {
                putExtra("USER_UID", uid)
            })
            dialog.dismiss()
        }

        lightButton.setOnClickListener {
            startActivity(Intent(this, AddDeviceActivity::class.java).apply {
                putExtra("USER_UID", uid)
            })
            dialog.dismiss()
        }

        fanButton.setOnClickListener {
            startActivity(Intent(this, AddDeviceActivity::class.java).apply {
                putExtra("USER_UID", uid)
            })
            dialog.dismiss()
        }

        dialog.show()
    }
}
