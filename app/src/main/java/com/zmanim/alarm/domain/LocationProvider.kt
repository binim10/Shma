package com.zmanim.alarm.domain

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.zmanim.alarm.data.model.LocationData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Provider for getting the device's current location using FusedLocationProviderClient
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder: Geocoder? = if (Geocoder.isPresent()) {
        Geocoder(context, Locale.getDefault())
    } else null

    /**
     * Gets the current location of the device
     * Requires ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permission
     * @return LocationData or null if location cannot be obtained
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationData? {
        return try {
            // First try to get fresh location
            val cancellationTokenSource = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).await()

            if (location != null) {
                val locationName = getLocationName(location.latitude, location.longitude)
                return LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locationName = locationName
                )
            }

            // Fallback to last known location
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                val locationName = getLocationName(lastLocation.latitude, lastLocation.longitude)
                return LocationData(
                    latitude = lastLocation.latitude,
                    longitude = lastLocation.longitude,
                    locationName = locationName
                )
            }

            null
        } catch (e: Exception) {
            // Permission denied or location services disabled
            null
        }
    }

    /**
     * Gets the last known location without requesting a fresh location
     * This is faster but may be outdated
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): LocationData? {
        return try {
            val lastLocation = fusedLocationClient.lastLocation.await() ?: return null
            val locationName = getLocationName(lastLocation.latitude, lastLocation.longitude)
            LocationData(
                latitude = lastLocation.latitude,
                longitude = lastLocation.longitude,
                locationName = locationName
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets a human-readable location name using reverse geocoding
     */
    private suspend fun getLocationName(latitude: Double, longitude: Double): String? {
        if (geocoder == null) return null

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        val name = address?.locality ?: address?.adminArea ?: address?.countryName
                        continuation.resume(name)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()
                address?.locality ?: address?.adminArea ?: address?.countryName
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if two locations are significantly different (more than ~1km apart)
     * This is used to determine if we need to recalculate Zmanim
     */
    fun isSignificantLocationChange(
        oldLocation: LocationData?,
        newLocation: LocationData
    ): Boolean {
        if (oldLocation == null) return true

        val distance = calculateDistance(
            oldLocation.latitude,
            oldLocation.longitude,
            newLocation.latitude,
            newLocation.longitude
        )

        // Consider it significant if more than 1000 meters (1km) apart
        return distance > 1000.0
    }

    /**
     * Calculates distance between two coordinates using Haversine formula
     * @return Distance in meters
     */
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}
