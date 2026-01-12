# Sleep as Android Integration

## Overview
This integration adds support for syncing Zmanim alarms to the Sleep as Android app, providing users with a choice between the internal alarm system and Sleep as Android's advanced alarm features.

## Architecture

### 1. AlarmProvider Interface
- **Location**: `service/provider/AlarmProvider.kt`
- **Purpose**: Abstraction layer for different alarm scheduling mechanisms
- **Methods**:
  - `scheduleAlarm(alarmTime, message)`: Schedule an alarm
  - `cancelAlarm()`: Cancel scheduled alarms
  - `isAvailable()`: Check if provider is available
  - `getProviderName()`: Get human-readable provider name

### 2. Implementations

#### InternalAlarmProvider
- **Location**: `service/provider/InternalAlarmProvider.kt`
- **Purpose**: Wraps existing AlarmManager functionality
- **Features**:
  - Uses `AlarmManager.setAlarmClock()` for precise timing
  - Checks for SCHEDULE_EXACT_ALARM permission on Android 12+
  - Maintains compatibility with existing alarm infrastructure

#### SleepAsAndroidProvider
- **Location**: `service/provider/SleepAsAndroidProvider.kt`
- **Purpose**: Integrates with Sleep as Android via Intent API
- **Implementation Details**:
  - Uses `AlarmClock.ACTION_SET_ALARM` intent
  - Targets package: `com.urbandroid.sleep`
  - Required extras:
    - `EXTRA_HOUR`: Alarm hour
    - `EXTRA_MINUTES`: Alarm minutes
    - `EXTRA_MESSAGE`: "Zmanim: Kriyat Shema"
    - `EXTRA_SKIP_UI`: true (background operation)
  - Checks app installation via PackageManager
  - Graceful fallback if app not installed

### 3. Updated Components

#### AlarmScheduler
- **Changes**:
  - Injects both `InternalAlarmProvider` and `SleepAsAndroidProvider`
  - Uses `getAlarmProvider()` to select appropriate provider based on user settings
  - Automatic fallback to internal provider if selected provider unavailable
  - `cancelAlarm()` now cancels from both providers to prevent orphaned alarms

#### AlarmPreferences & Data Model
- **New Field**: `alarmProviderType: AlarmProviderType`
- **Enum Values**:
  - `INTERNAL`: Use internal AlarmManager
  - `SLEEP_AS_ANDROID`: Use Sleep as Android app
- **Default**: `INTERNAL` for backward compatibility
- **New Method**: `setAlarmProviderType(type)`

#### MainViewModel
- **New Methods**:
  - `updateAlarmProvider(type)`: Switch alarm providers
  - `manualSyncToSleepAsAndroid()`: Force immediate sync
  - `isCurrentProviderAvailable()`: Check provider status
  - `getCurrentProviderName()`: Get active provider name

#### MainActivity UI
- **New Component**: `ProviderSelectorCard`
- **Features**:
  - Radio button selector for alarm provider
  - Real-time detection of Sleep as Android installation
  - Warning card when Sleep as Android not installed
  - "Install Sleep as Android" button linking to Play Store
  - "Sync Now" button for manual synchronization
  - Disabled UI elements when Sleep as Android not available

### 4. Utility Functions
- **Location**: `util/AlarmProviderUtil.kt`
- **Functions**:
  - `isSleepAsAndroidInstalled()`: Check app installation
  - `openSleepAsAndroidInPlayStore()`: Open Play Store page
  - `getSleepAsAndroidNotAvailableMessage()`: Error message helper

## User Flow

### Switching to Sleep as Android
1. User navigates to "Alarm Provider" card
2. Selects "Sleep as Android" radio button
3. If app not installed:
   - Warning appears with installation button
   - Automatically falls back to internal alarm
4. If app installed:
   - Provider switches to Sleep as Android
   - If alarm enabled, immediately reschedules with new provider
   - "Sync Now" button becomes available

### Manual Sync
1. User taps "Sync Now to Sleep as Android"
2. App calculates current alarm time
3. Sends intent to Sleep as Android with:
   - Calculated hour and minute
   - Custom message: "Zmanim: Kriyat Shema"
   - Skip UI flag for seamless operation
4. Sleep as Android receives and schedules the alarm

### Daily Recalculation (Automatic)
1. WorkManager triggers daily location check
2. If location changed significantly OR
3. TimeZoneReceiver/BootReceiver triggers recalculation
4. `AlarmScheduler.rescheduleAlarm()` called
5. Automatically uses selected provider
6. Sleep as Android alarms update without user intervention

## Safety & Validation

### Installation Check
- `PackageManager.getPackageInfo()` verifies Sleep as Android presence
- Graceful fallback prevents alarm scheduling failures
- UI provides clear feedback about app availability

### Permission Handling
- Internal alarms: Requires `SCHEDULE_EXACT_ALARM` (Android 12+)
- Sleep as Android: No additional permissions needed
- Existing permission infrastructure reused

### Cancellation Safety
- When switching providers, cancels from BOTH providers
- Prevents duplicate alarms
- Ensures clean state when disabling alarms

## Testing Checklist

### Unit Testing
- [ ] AlarmProvider interface contract
- [ ] InternalAlarmProvider scheduling logic
- [ ] SleepAsAndroidProvider intent construction
- [ ] Provider selection logic in AlarmScheduler
- [ ] Fallback behavior when provider unavailable

### Integration Testing
- [ ] Switch from Internal to Sleep as Android with alarm enabled
- [ ] Switch from Sleep as Android to Internal with alarm enabled
- [ ] Manual sync button functionality
- [ ] Automatic daily recalculation with Sleep as Android
- [ ] Boot receiver with Sleep as Android provider
- [ ] Timezone change with Sleep as Android provider

### UI Testing
- [ ] Provider selector displays correctly
- [ ] Warning appears when Sleep as Android not installed
- [ ] Radio buttons reflect current selection
- [ ] Sync button only visible when appropriate
- [ ] Play Store link opens correctly

### Edge Cases
- [ ] Sleep as Android uninstalled after being selected
- [ ] Rapid provider switching
- [ ] Alarm enabled/disabled while switching providers
- [ ] Location permission denied with Sleep as Android
- [ ] Sleep as Android intent rejected/failed

## Future Enhancements

1. **Bidirectional Sync**: Listen for alarm dismissals in Sleep as Android
2. **Advanced Settings**: Pass additional Sleep as Android options
3. **Multiple Alarms**: Support different Zmanim times
4. **Smart Sleep Tracking**: Integrate with Sleep as Android's sleep tracking
5. **Statistics**: Show sync success rate and history

## API Reference

### Sleep as Android Intent API
- **Documentation**: https://sleep.urbandroid.org/docs/devs/intent_api.html
- **Action**: `android.intent.action.SET_ALARM`
- **Package**: `com.urbandroid.sleep`
- **Key Extras**:
  - `android.intent.extra.alarm.HOUR`
  - `android.intent.extra.alarm.MINUTES`
  - `android.intent.extra.alarm.MESSAGE`
  - `android.intent.extra.alarm.SKIP_UI`
  - `android.intent.extra.alarm.VIBRATE`

## Files Changed

### New Files
1. `service/provider/AlarmProvider.kt`
2. `service/provider/InternalAlarmProvider.kt`
3. `service/provider/SleepAsAndroidProvider.kt`
4. `util/AlarmProviderUtil.kt`

### Modified Files
1. `data/model/ZmanimData.kt` - Added `AlarmProviderType` enum
2. `data/datastore/AlarmPreferences.kt` - Added provider type persistence
3. `service/AlarmScheduler.kt` - Refactored to use provider pattern
4. `ui/MainViewModel.kt` - Added provider management methods
5. `ui/MainActivity.kt` - Added `ProviderSelectorCard` UI component

## Migration Notes

### Backward Compatibility
- Existing users automatically default to `INTERNAL` provider
- No data migration required
- Existing alarms continue functioning normally

### Version Compatibility
- Minimum SDK: 26 (unchanged)
- Target SDK: 34 (unchanged)
- Sleep as Android: Any recent version supporting standard alarm intents
