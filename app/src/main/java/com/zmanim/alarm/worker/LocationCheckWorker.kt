package com.zmanim.alarm.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zmanim.alarm.data.datastore.AlarmPreferences
import com.zmanim.alarm.domain.LocationProvider
import com.zmanim.alarm.service.AlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that periodically checks if location has changed significantly
 * and reschedules the alarm if needed
 */
@HiltWorker
class LocationCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationProvider: LocationProvider,
    private val alarmPreferences: AlarmPreferences,
    private val alarmScheduler: AlarmScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val settings = alarmPreferences.alarmSettings.first()

            if (!settings.isEnabled) {
                return Result.success()
            }

            val currentLocation = locationProvider.getLastKnownLocation()
                ?: return Result.retry()

            val lastLocation = settings.lastLocation

            // Check if location has changed significantly (>1km)
            if (locationProvider.isSignificantLocationChange(lastLocation, currentLocation)) {
                // Save new location
                alarmPreferences.setLastLocation(currentLocation)

                // Reschedule alarm with new location
                alarmScheduler.rescheduleAlarm()
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "location_check_worker"
    }
}
