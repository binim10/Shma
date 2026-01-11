package com.zmanim.alarm.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zmanim.alarm.data.model.AlarmSettings
import com.zmanim.alarm.data.model.LocationData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alarm_preferences")

@Singleton
class AlarmPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val IS_ENABLED = booleanPreferencesKey("is_enabled")
        val MINUTES_BEFORE = intPreferencesKey("minutes_before")
        val LAST_LATITUDE = doublePreferencesKey("last_latitude")
        val LAST_LONGITUDE = doublePreferencesKey("last_longitude")
        val LAST_LOCATION_NAME = stringPreferencesKey("last_location_name")
        val LAST_SCHEDULED_ALARM = longPreferencesKey("last_scheduled_alarm")
    }

    val alarmSettings: Flow<AlarmSettings> = context.dataStore.data.map { preferences ->
        val lastLat = preferences[PreferencesKeys.LAST_LATITUDE]
        val lastLon = preferences[PreferencesKeys.LAST_LONGITUDE]
        val lastLocation = if (lastLat != null && lastLon != null) {
            LocationData(
                latitude = lastLat,
                longitude = lastLon,
                locationName = preferences[PreferencesKeys.LAST_LOCATION_NAME]
            )
        } else null

        val lastScheduledMillis = preferences[PreferencesKeys.LAST_SCHEDULED_ALARM]
        val lastScheduled = lastScheduledMillis?.let {
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        }

        AlarmSettings(
            isEnabled = preferences[PreferencesKeys.IS_ENABLED] ?: false,
            minutesBefore = preferences[PreferencesKeys.MINUTES_BEFORE] ?: 30,
            lastLocation = lastLocation,
            lastScheduledAlarm = lastScheduled
        )
    }

    suspend fun setAlarmEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ENABLED] = enabled
        }
    }

    suspend fun setMinutesBefore(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MINUTES_BEFORE] = minutes
        }
    }

    suspend fun setLastLocation(location: LocationData) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_LATITUDE] = location.latitude
            preferences[PreferencesKeys.LAST_LONGITUDE] = location.longitude
            location.locationName?.let {
                preferences[PreferencesKeys.LAST_LOCATION_NAME] = it
            }
        }
    }

    suspend fun setLastScheduledAlarm(time: ZonedDateTime) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SCHEDULED_ALARM] = time.toInstant().toEpochMilli()
        }
    }

    suspend fun clearLastScheduledAlarm() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.LAST_SCHEDULED_ALARM)
        }
    }
}
