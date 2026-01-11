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
 * BroadcastReceiver that reschedules alarms after device boot
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                // Reschedule alarms after boot
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        alarmScheduler.scheduleAlarm()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
