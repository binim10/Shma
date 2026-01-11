package com.zmanim.alarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zmanim.alarm.data.datastore.AlarmPreferences
import com.zmanim.alarm.data.model.AlarmSettings
import com.zmanim.alarm.data.model.ZmanimData
import com.zmanim.alarm.domain.LocationProvider
import com.zmanim.alarm.domain.ZmanimCalculator
import com.zmanim.alarm.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * ViewModel for the main screen
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val alarmPreferences: AlarmPreferences,
    private val zmanimCalculator: ZmanimCalculator,
    private val locationProvider: LocationProvider,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    val alarmSettings = alarmPreferences.alarmSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlarmSettings()
        )

    init {
        loadZmanimData()
    }

    fun loadZmanimData() {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading

            try {
                val settings = alarmSettings.value
                val location = locationProvider.getCurrentLocation()
                    ?: settings.lastLocation

                if (location == null) {
                    _uiState.value = MainUiState.Error("Location unavailable. Please enable location services.")
                    return@launch
                }

                // Save location
                alarmPreferences.setLastLocation(location)

                // Calculate today's Zmanim
                val zmanimData = zmanimCalculator.calculateTodayZmanim(location)

                if (zmanimData == null) {
                    _uiState.value = MainUiState.Error("Error calculating Zmanim. Please try again.")
                    return@launch
                }

                val nextAlarmTime = if (settings.isEnabled) {
                    calculateNextAlarmTime(zmanimData, settings.minutesBefore)
                } else null

                _uiState.value = MainUiState.Success(
                    zmanimData = zmanimData,
                    nextAlarmTime = nextAlarmTime
                )
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun toggleAlarm(enabled: Boolean) {
        viewModelScope.launch {
            alarmPreferences.setAlarmEnabled(enabled)

            if (enabled) {
                val success = alarmScheduler.scheduleAlarm()
                if (!success) {
                    _uiState.value = MainUiState.Error("Failed to schedule alarm. Please check permissions.")
                    alarmPreferences.setAlarmEnabled(false)
                }
            } else {
                alarmScheduler.cancelAlarm()
                alarmPreferences.clearLastScheduledAlarm()
            }

            loadZmanimData()
        }
    }

    fun updateMinutesBefore(minutes: Int) {
        viewModelScope.launch {
            alarmPreferences.setMinutesBefore(minutes)

            if (alarmSettings.value.isEnabled) {
                alarmScheduler.rescheduleAlarm()
            }

            loadZmanimData()
        }
    }

    fun onLocationPermissionGranted() {
        _permissionState.value = _permissionState.value.copy(locationGranted = true)
        loadZmanimData()
    }

    fun onNotificationPermissionGranted() {
        _permissionState.value = _permissionState.value.copy(notificationGranted = true)
    }

    fun checkExactAlarmPermission(): Boolean {
        return alarmScheduler.canScheduleExactAlarms()
    }

    private fun calculateNextAlarmTime(zmanimData: ZmanimData, minutesBefore: Int): ZonedDateTime? {
        val now = ZonedDateTime.now()
        val todayAlarmTime = zmanimCalculator.calculateAlarmTime(zmanimData, minutesBefore)

        return if (todayAlarmTime != null && todayAlarmTime.isAfter(now)) {
            todayAlarmTime
        } else {
            alarmSettings.value.lastScheduledAlarm
        }
    }
}

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(
        val zmanimData: ZmanimData,
        val nextAlarmTime: ZonedDateTime?
    ) : MainUiState()
    data class Error(val message: String) : MainUiState()
}

data class PermissionState(
    val locationGranted: Boolean = false,
    val notificationGranted: Boolean = false
)
