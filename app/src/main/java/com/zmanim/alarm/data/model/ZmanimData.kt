package com.zmanim.alarm.data.model

import java.time.ZonedDateTime

/**
 * Enum representing different alarm provider types
 */
enum class AlarmProviderType {
    INTERNAL,
    SLEEP_AS_ANDROID
}

/**
 * Data class representing Zmanim (Jewish prayer times) for a specific day
 */
data class ZmanimData(
    val sofZmanShmaMGA: ZonedDateTime?,
    val sofZmanShmaGRA: ZonedDateTime?,
    val location: LocationData,
    val calculatedAt: ZonedDateTime = ZonedDateTime.now()
) {
    /**
     * Returns the earliest Sof Zman Kriyat Shema between MGA and GRA
     */
    fun getEarliestSZKS(): ZonedDateTime? {
        return when {
            sofZmanShmaMGA == null && sofZmanShmaGRA == null -> null
            sofZmanShmaMGA == null -> sofZmanShmaGRA
            sofZmanShmaGRA == null -> sofZmanShmaMGA
            sofZmanShmaMGA.isBefore(sofZmanShmaGRA) -> sofZmanShmaMGA
            else -> sofZmanShmaGRA
        }
    }
}

/**
 * Data class representing location information
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val locationName: String? = null
)

/**
 * Data class representing alarm settings
 */
data class AlarmSettings(
    val isEnabled: Boolean = false,
    val minutesBefore: Int = 30,
    val lastLocation: LocationData? = null,
    val lastScheduledAlarm: ZonedDateTime? = null,
    val alarmProviderType: AlarmProviderType = AlarmProviderType.INTERNAL
)
