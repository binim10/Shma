package com.zmanim.alarm.domain

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import com.zmanim.alarm.data.model.LocationData
import com.zmanim.alarm.data.model.ZmanimData
import java.time.ZonedDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for calculating Zmanim (Jewish prayer times) using KosherKotlin library
 */
@Singleton
class ZmanimCalculator @Inject constructor() {

    /**
     * Calculates Zmanim for today based on the provided location
     * @param location The location for which to calculate Zmanim
     * @return ZmanimData containing the calculated times, or null if calculation fails
     */
    fun calculateTodayZmanim(location: LocationData): ZmanimData? {
        return try {
            val geoLocation = GeoLocation(
                location.locationName ?: "Current Location",
                location.latitude,
                location.longitude,
                0.0, // elevation - can be enhanced later
                TimeZone.getDefault()
            )

            val calendar = ComplexZmanimCalendar(geoLocation)
            calendar.calendar = Calendar.getInstance()

            // Get Sof Zman Shma for both opinions
            val shmaMGA = calendar.sofZmanShmaMGA?.let { date ->
                ZonedDateTime.ofInstant(date.toInstant(), TimeZone.getDefault().toZoneId())
            }

            val shmaGRA = calendar.sofZmanShmaGRA?.let { date ->
                ZonedDateTime.ofInstant(date.toInstant(), TimeZone.getDefault().toZoneId())
            }

            ZmanimData(
                sofZmanShmaMGA = shmaMGA,
                sofZmanShmaGRA = shmaGRA,
                location = location,
                calculatedAt = ZonedDateTime.now()
            )
        } catch (e: Exception) {
            // Log error in production
            null
        }
    }

    /**
     * Calculates Zmanim for tomorrow based on the provided location
     * This is used to schedule the next day's alarm
     */
    fun calculateTomorrowZmanim(location: LocationData): ZmanimData? {
        return try {
            val geoLocation = GeoLocation(
                location.locationName ?: "Current Location",
                location.latitude,
                location.longitude,
                0.0,
                TimeZone.getDefault()
            )

            val calendar = ComplexZmanimCalendar(geoLocation)
            val tomorrowCalendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            calendar.calendar = tomorrowCalendar

            val shmaMGA = calendar.sofZmanShmaMGA?.let { date ->
                ZonedDateTime.ofInstant(date.toInstant(), TimeZone.getDefault().toZoneId())
            }

            val shmaGRA = calendar.sofZmanShmaGRA?.let { date ->
                ZonedDateTime.ofInstant(date.toInstant(), TimeZone.getDefault().toZoneId())
            }

            ZmanimData(
                sofZmanShmaMGA = shmaMGA,
                sofZmanShmaGRA = shmaGRA,
                location = location,
                calculatedAt = ZonedDateTime.now()
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates the alarm time (X minutes before the earliest SZKS)
     * @param zmanimData The Zmanim data
     * @param minutesBefore Number of minutes before SZKS to trigger the alarm
     * @return The alarm time, or null if calculation is not possible
     */
    fun calculateAlarmTime(zmanimData: ZmanimData, minutesBefore: Int): ZonedDateTime? {
        val earliestSZKS = zmanimData.getEarliestSZKS() ?: return null
        return earliestSZKS.minusMinutes(minutesBefore.toLong())
    }
}
