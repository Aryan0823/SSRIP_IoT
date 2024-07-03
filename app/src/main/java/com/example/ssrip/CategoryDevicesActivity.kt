package com.example.ssrip

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class CategoryDevicesActivity : BaseActivity() {

    private lateinit var tvCategory: TextView
    private lateinit var lvDevices: ListView
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_devices)

        tvCategory = findViewById(R.id.tvCategory)
        lvDevices = findViewById(R.id.lvDevices)

        db = FirebaseFirestore.getInstance()

        val category = intent.getStringExtra("CATEGORY") ?: return
        val devices = intent.getStringArrayListExtra("DEVICES") ?: return

        tvCategory.text = category
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devices)
        lvDevices.adapter = adapter

        lvDevices.setOnItemClickListener { _, _, position, _ ->
            val deviceName = devices[position]
        }
    }

        }
    }
}