package com.zmanim.alarm.service.provider

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zmanim.alarm.receiver.AlarmReceiver
import com.zmanim.alarm.service.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Internal alarm provider using Android's native AlarmManager.
 * This is the default alarm provider for the app.
 */
class InternalAlarmProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmProvider {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun scheduleAlarm(alarmTime: ZonedDateTime, message: String): Boolean {
        if (!isAvailable()) {
            return false
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_ALARM_TRIGGER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmScheduler.ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            alarmTime.toInstant().toEpochMilli(),
            getPendingIntentForMainActivity()
        )

        // Use setAlarmClock for maximum precision and to show in system UI
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        return true
    }

    override suspend fun cancelAlarm() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_ALARM_TRIGGER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmScheduler.ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    override suspend fun isAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun getProviderName(): String = "Internal Alarm"

    private fun getPendingIntentForMainActivity(): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
