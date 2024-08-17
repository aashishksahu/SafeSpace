package org.privacymatters.safespace.experimental.main

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class AppNotification: Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            FileTransferNotification.CHANNEL_ID,
            FileTransferNotification.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Shows file transfer progress."

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}