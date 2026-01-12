package com.zmanim.alarm.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.zmanim.alarm.service.provider.SleepAsAndroidProvider

/**
 * Utility functions for alarm provider operations
 */
object AlarmProviderUtil {

    /**
     * Checks if Sleep as Android is installed on the device
     */
    fun isSleepAsAndroidInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(
                SleepAsAndroidProvider.SLEEP_AS_ANDROID_PACKAGE,
                0
            )
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Opens the Sleep as Android app page in the Play Store
     */
    fun openSleepAsAndroidInPlayStore(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=${SleepAsAndroidProvider.SLEEP_AS_ANDROID_PACKAGE}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web browser if Play Store is not available
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=${SleepAsAndroidProvider.SLEEP_AS_ANDROID_PACKAGE}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Gets a user-friendly error message when Sleep as Android is not available
     */
    fun getSleepAsAndroidNotAvailableMessage(): String {
        return "Sleep as Android is not installed. Please install it from the Play Store or switch to Internal Alarm."
    }
}
