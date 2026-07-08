package com.example.smartemergencyapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification: Notification =
            NotificationCompat.Builder(this, "emergency_channel")
                .setContentTitle("Smart Emergency App")
                .setContentText("Voice detection running...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "emergency_channel",
                "Emergency Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {

        return null
    }
}