package com.zmanim.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zmanim.alarm.service.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver that reschedules alarms when timezone or time changes
 * This handles DST changes automatically
 */
@AndroidEntryPoint
class TimeZoneReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                // Reschedule alarms when timezone changes (DST)
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        alarmScheduler.rescheduleAlarm()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
