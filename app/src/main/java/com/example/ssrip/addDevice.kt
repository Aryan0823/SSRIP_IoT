package com.example.ssrip

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity

class addDevice : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_FINE_LOCATION = 2
    private val REQUEST_BLUETOOTH_PERMISSIONS = 3
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private val deviceList: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        val buttonScan: Button = findViewById(R.id.button_scan)
        val listViewDevices: ListView = findViewById(R.id.listview_devices)

        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        listViewDevices.adapter = deviceListAdapter

        buttonScan.setOnClickListener {
            checkPermissionsAndScan()
        }
    }

    private fun checkPermissionsAndScan() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if BLUETOOTH_ADMIN permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            // Request BLUETOOTH_ADMIN permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADMIN), REQUEST_ENABLE_BT)
        } else {
            // Bluetooth is supported and BLUETOOTH_ADMIN permission is granted
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                checkBluetoothPermissions()
            }
        }
    }


    private fun checkBluetoothPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        deviceList.clear()
        deviceListAdapter.notifyDataSetChanged()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(receiver, filter)
            try {
                bluetoothAdapter?.startDiscovery()
            } catch (e: SecurityException) {
                Toast.makeText(this, "Permission denied for Bluetooth scanning", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Bluetooth and Location permissions are required for scanning", Toast.LENGTH_SHORT).show()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                try {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        val deviceName = device?.name
                        val deviceAddress = device?.address // MAC address
                        if (deviceName != null && deviceAddress != null) {
                            val deviceInfo = "$deviceName\n$deviceAddress"
                            if (!deviceList.contains(deviceInfo)) {
                                deviceList.add(deviceInfo)
                                deviceListAdapter.notifyDataSetChanged()
                            }
                        }
                    } else {
                        ActivityCompat.requestPermissions(
                            this@addDevice,
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                            REQUEST_BLUETOOTH_PERMISSIONS
                        )
                    }
                } catch (e: SecurityException) {
                    Toast.makeText(context, "Permission denied for accessing Bluetooth device information", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, now enable Bluetooth
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                } else {
                    Toast.makeText(this, "BLUETOOTH_ADMIN permission is required to enable Bluetooth", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_FINE_LOCATION, REQUEST_BLUETOOTH_PERMISSIONS -> {
                if ((grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED })) {
                    startScanning()
                } else {
                    Toast.makeText(this, "Bluetooth and Location permissions are required for scanning", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                checkBluetoothPermissions()
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to scan for devices", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: SecurityException) {
            // Handle exception if permission was revoked while the activity is running
        }
    }
}
