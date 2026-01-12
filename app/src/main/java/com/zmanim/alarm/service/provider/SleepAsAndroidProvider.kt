package com.zmanim.alarm.service.provider

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Alarm provider that integrates with Sleep as Android app.
 * Uses Intent API to schedule alarms in the external Sleep as Android app.
 *
 * Reference: https://sleep.urbandroid.org/docs/devs/intent_api.html
 */
class SleepAsAndroidProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmProvider {

    override suspend fun scheduleAlarm(alarmTime: ZonedDateTime, message: String): Boolean {
        if (!isAvailable()) {
            Log.w(TAG, "Sleep as Android is not installed or not available")
            return false
        }

        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                // Target Sleep as Android specifically
                setPackage(SLEEP_AS_ANDROID_PACKAGE)

                // Required extras
                putExtra(AlarmClock.EXTRA_HOUR, alarmTime.hour)
                putExtra(AlarmClock.EXTRA_MINUTES, alarmTime.minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)

                // Additional useful extras
                putExtra(AlarmClock.EXTRA_VIBRATE, true)
                putExtra(AlarmClock.EXTRA_RINGTONE, AlarmClock.VALUE_RINGTONE_SILENT) // Let Sleep as Android use its own alarm sound

                // Mark this alarm as replaceable so we can update it daily
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
            Log.i(TAG, "Successfully scheduled alarm in Sleep as Android for ${alarmTime.hour}:${alarmTime.minute}")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm in Sleep as Android", e)
            return false
        }
    }

    override suspend fun cancelAlarm() {
        // Sleep as Android doesn't provide a standardized way to cancel alarms via Intent
        // The app will handle updating the alarm when a new time is set
        // Users can manually dismiss alarms in the Sleep as Android app
        Log.i(TAG, "Cancel requested - Sleep as Android will be updated on next schedule")
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SLEEP_AS_ANDROID_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun getProviderName(): String = "Sleep as Android"

    companion object {
        private const val TAG = "SleepAsAndroidProvider"
        const val SLEEP_AS_ANDROID_PACKAGE = "com.urbandroid.sleep"
    }
}
