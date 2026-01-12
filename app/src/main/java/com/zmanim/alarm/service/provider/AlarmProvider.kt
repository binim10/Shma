package com.zmanim.alarm.service.provider

import java.time.ZonedDateTime

/**
 * Interface for different alarm scheduling providers.
 * Allows abstraction between internal AlarmManager and external apps like Sleep as Android.
 */
interface AlarmProvider {
    /**
     * Schedules an alarm for the specified time.
     *
     * @param alarmTime The exact time when the alarm should trigger
     * @param message The message/label to show with the alarm
     * @return true if scheduling was successful, false otherwise
     */
    suspend fun scheduleAlarm(alarmTime: ZonedDateTime, message: String): Boolean

    /**
     * Cancels any previously scheduled alarm.
     */
    suspend fun cancelAlarm()

    /**
     * Checks if this provider is available/usable on the current device.
     * For example, external apps need to be installed.
     *
     * @return true if the provider can be used, false otherwise
     */
    suspend fun isAvailable(): Boolean

    /**
     * Gets a human-readable name for this provider.
     */
    fun getProviderName(): String
}
