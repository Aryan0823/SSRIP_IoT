package com.example.ssrip

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.data.isNotEmpty().let {
            handleDataMessage(remoteMessage.data)
        }

        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveFCMTokenToFirestore(token)
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val title = data["title"]
        val message = data["message"]

        when (type) {
            "fan_alert" -> showNotification("Fan Alert", message)
            "fan_speed_update" -> showNotification("Fan Speed Update", message)
            "fan_status_update" -> showNotification("Fan Status Update", message)
            "ac_status_update" -> showNotification("AC Status Update", message)
            "ac_temperature_alert" -> showNotification("AC Temperature Alert", message)
            "room_temperature_alert" -> showNotification("Room Temperature Alert", message)
            "light_status_update" -> showNotification("Light Status Update", message)
            "light_brightness_update" -> showNotification("Light Brightness Update", message)
            "room_light_alert" -> showNotification("Room Light Alert", message)
            "humidifier_status_update" -> showNotification("Humidifier Status Update", message)
            "humidifier_humidity_update" -> showNotification("Humidifier Humidity Update", message)
            "room_humidity_alert" -> showNotification("Room Humidity Alert", message)
            else -> showNotification(title, message)
        }
    }

    private fun showNotification(title: String?, message: String?) {
        val channelId = "SMART_HOME_CONTROL_CHANNEL"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Smart Home Control Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun saveFCMTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    println("FCM token updated successfully")
                }
                .addOnFailureListener { e ->
                    println("Error updating FCM token: ${e.message}")
                }
        }
    }
}