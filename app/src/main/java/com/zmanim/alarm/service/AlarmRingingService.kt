package com.zmanim.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.zmanim.alarm.R
import com.zmanim.alarm.ZmanimAlarmApplication
import com.zmanim.alarm.ui.alarm.AlarmRingingActivity

/**
 * Foreground service that runs while the alarm is ringing
 * This ensures the alarm continues even with battery optimizations
 */
class AlarmRingingService : Service() {

    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RINGING -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startVibration()
            }
            ACTION_STOP_RINGING -> {
                stopVibration()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopVibration()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ZmanimAlarmApplication.SERVICE_CHANNEL_ID)
            .setContentTitle(getString(R.string.time_for_shema))
            .setContentText(getString(R.string.time_for_shema))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .build()
    }

    private fun startVibration() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 500, 500, 500, 500, 500)
                val effect = VibrationEffect.createWaveform(pattern, 0)
                it.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val pattern = longArrayOf(0, 500, 500, 500, 500, 500)
                it.vibrate(pattern, 0)
            }
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    companion object {
        const val ACTION_START_RINGING = "com.zmanim.alarm.ACTION_START_RINGING"
        const val ACTION_STOP_RINGING = "com.zmanim.alarm.ACTION_STOP_RINGING"
        private const val NOTIFICATION_ID = 2001
    }
}
