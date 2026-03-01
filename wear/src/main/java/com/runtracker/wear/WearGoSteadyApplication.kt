package com.runtracker.wear

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class WearGoSteadyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val trackingChannel = NotificationChannel(
                TRACKING_CHANNEL_ID,
                "Run Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows notification while tracking a run"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(trackingChannel)
        }
    }

    companion object {
        const val TRACKING_CHANNEL_ID = "wear_tracking_channel"
    }
}
