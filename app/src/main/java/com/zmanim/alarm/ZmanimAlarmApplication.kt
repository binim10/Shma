package com.zmanim.alarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZmanimAlarmApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== APPLICATION STARTING ===")
        Log.d(TAG, "Android Version: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")

        setupCrashHandler()

        try {
            createNotificationChannels()
            Log.d(TAG, "=== APPLICATION STARTED SUCCESSFULLY ===")
        } catch (e: Exception) {
            Log.e(TAG, "FATAL: Error in Application.onCreate()", e)
            throw e
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "====================================")
            Log.e(TAG, "UNCAUGHT EXCEPTION - APP CRASH")
            Log.e(TAG, "====================================")
            Log.e(TAG, "Thread: ${thread.name}")
            Log.e(TAG, "Exception: ${throwable.javaClass.name}")
            Log.e(TAG, "Message: ${throwable.message}")
            Log.e(TAG, "Stack trace:")
            throwable.printStackTrace()

            var cause = throwable.cause
            var level = 1
            while (cause != null) {
                Log.e(TAG, "--- Caused by (level $level) ---")
                Log.e(TAG, "Exception: ${cause.javaClass.name}")
                Log.e(TAG, "Message: ${cause.message}")
                cause.printStackTrace()
                cause = cause.cause
                level++
            }

            Log.e(TAG, "====================================")
            defaultHandler?.uncaughtException(thread, throwable)
        }
        Log.d(TAG, "Crash handler installed")
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
        private const val TAG = "ZmanimAlarmApp"
        const val ALARM_CHANNEL_ID = "zmanim_alarm_channel"
        const val SERVICE_CHANNEL_ID = "alarm_service_channel"
    }
}
