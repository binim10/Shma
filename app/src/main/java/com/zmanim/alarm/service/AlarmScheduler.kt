package com.zmanim.alarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zmanim.alarm.data.datastore.AlarmPreferences
import com.zmanim.alarm.data.model.AlarmProviderType
import com.zmanim.alarm.domain.LocationProvider
import com.zmanim.alarm.domain.ZmanimCalculator
import com.zmanim.alarm.receiver.AlarmReceiver
import com.zmanim.alarm.service.provider.AlarmProvider
import com.zmanim.alarm.service.provider.InternalAlarmProvider
import com.zmanim.alarm.service.provider.SleepAsAndroidProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for scheduling and canceling alarms using different providers
 * (Internal AlarmManager or external apps like Sleep as Android)
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmPreferences: AlarmPreferences,
    private val zmanimCalculator: ZmanimCalculator,
    private val locationProvider: LocationProvider,
    private val internalAlarmProvider: InternalAlarmProvider,
    private val sleepAsAndroidProvider: SleepAsAndroidProvider
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Gets the appropriate alarm provider based on user settings
     */
    private suspend fun getAlarmProvider(): AlarmProvider {
        val settings = alarmPreferences.alarmSettings.first()
        return when (settings.alarmProviderType) {
            AlarmProviderType.INTERNAL -> internalAlarmProvider
            AlarmProviderType.SLEEP_AS_ANDROID -> {
                // If Sleep as Android is not available, fallback to internal
                if (sleepAsAndroidProvider.isAvailable()) {
                    sleepAsAndroidProvider
                } else {
                    internalAlarmProvider
                }
            }
        }
    }

    /**
     * Schedules the Zmanim alarm based on current settings and location
     * @return true if successfully scheduled, false otherwise
     */
    suspend fun scheduleAlarm(): Boolean {
        val settings = alarmPreferences.alarmSettings.first()

        if (!settings.isEnabled) {
            cancelAlarm()
            return false
        }

        // Get current location
        val location = locationProvider.getCurrentLocation()
            ?: settings.lastLocation
            ?: return false

        // Save location for future use
        alarmPreferences.setLastLocation(location)

        // Calculate tomorrow's Zmanim (since today's may have already passed)
        val now = ZonedDateTime.now()
        val todayZmanim = zmanimCalculator.calculateTodayZmanim(location)
        val tomorrowZmanim = zmanimCalculator.calculateTomorrowZmanim(location)

        // Check if today's alarm time hasn't passed yet
        val todayAlarmTime = todayZmanim?.let {
            zmanimCalculator.calculateAlarmTime(it, settings.minutesBefore)
        }

        val alarmTime = if (todayAlarmTime != null && todayAlarmTime.isAfter(now)) {
            todayAlarmTime
        } else {
            // Use tomorrow's alarm time
            tomorrowZmanim?.let {
                zmanimCalculator.calculateAlarmTime(it, settings.minutesBefore)
            } ?: return false
        }

        // Use the appropriate provider to schedule the alarm
        val provider = getAlarmProvider()
        val message = "Zmanim: Kriyat Shema"
        val success = provider.scheduleAlarm(alarmTime, message)

        if (success) {
            // Save the scheduled alarm time
            alarmPreferences.setLastScheduledAlarm(alarmTime)
        }

        return success
    }

    /**
     * Schedules the next day's alarm after the current one has triggered
     * This is called from AlarmReceiver after the alarm goes off
     */
    suspend fun scheduleNextDayAlarm(): Boolean {
        return scheduleAlarm()
    }

    /**
     * Cancels the currently scheduled alarm
     */
    suspend fun cancelAlarm() {
        // Cancel from both providers to ensure no alarms are left
        // This is important when switching providers or disabling alarms
        internalAlarmProvider.cancelAlarm()
        sleepAsAndroidProvider.cancelAlarm()
    }

    /**
     * Checks if the app can schedule exact alarms (required for Android 12+)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Checks if the currently selected alarm provider is available
     */
    suspend fun isCurrentProviderAvailable(): Boolean {
        val provider = getAlarmProvider()
        return provider.isAvailable()
    }

    /**
     * Gets the name of the currently selected provider
     */
    suspend fun getCurrentProviderName(): String {
        val provider = getAlarmProvider()
        return provider.getProviderName()
    }

    /**
     * Reschedules the alarm (used when settings change or location changes)
     */
    suspend fun rescheduleAlarm(): Boolean {
        cancelAlarm()
        return scheduleAlarm()
    }

    private fun getPendingIntentForMainActivity(): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ALARM_REQUEST_CODE = 1001
        const val ACTION_ALARM_TRIGGER = "com.zmanim.alarm.ACTION_ALARM_TRIGGER"
    }
}
