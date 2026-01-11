package com.zmanim.alarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZmanimAlarmApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Zmanim Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Zmanim alarms"
                enableVibration(true)
                setShowBadge(true)
            }

            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Alarm Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service notification for alarm ringing"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val ALARM_CHANNEL_ID = "zmanim_alarm_channel"
        const val SERVICE_CHANNEL_ID = "alarm_service_channel"
    }
}
