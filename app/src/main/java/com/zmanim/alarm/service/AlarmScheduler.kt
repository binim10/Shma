package com.zmanim.alarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zmanim.alarm.data.datastore.AlarmPreferences
import com.zmanim.alarm.domain.LocationProvider
import com.zmanim.alarm.domain.ZmanimCalculator
import com.zmanim.alarm.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for scheduling and canceling alarms using AlarmManager
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmPreferences: AlarmPreferences,
    private val zmanimCalculator: ZmanimCalculator,
    private val locationProvider: LocationProvider
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

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

        // Schedule the alarm
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            alarmTime.toInstant().toEpochMilli(),
            getPendingIntentForMainActivity()
        )

        // Use setAlarmClock for maximum precision and to show in system UI
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        // Save the scheduled alarm time
        alarmPreferences.setLastScheduledAlarm(alarmTime)

        return true
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
    fun cancelAlarm() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
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
