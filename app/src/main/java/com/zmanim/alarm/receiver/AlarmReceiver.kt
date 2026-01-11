package com.zmanim.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zmanim.alarm.service.AlarmScheduler
import com.zmanim.alarm.ui.alarm.AlarmRingingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver that handles alarm triggers
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlarmScheduler.ACTION_ALARM_TRIGGER) {
            // Launch the alarm ringing activity
            val alarmIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
            }
            context.startActivity(alarmIntent)

            // Schedule the next day's alarm
            val pendingResult = goAsync()
            scope.launch {
                try {
                    alarmScheduler.scheduleNextDayAlarm()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
