# Zmanim Alarm App

A production-ready Android application that allows users to set alarms relative to "Sof Zman Kriyat Shema" (SZKS) based on their current GPS location.

## 📥 Download APK

### Option 1: Download from GitHub Actions (Recommended)
1. Go to the [Actions tab](../../actions/workflows/build-apk.yml) of this repository
2. Click on the latest successful workflow run (green checkmark)
3. Scroll down to "Artifacts" section
4. Download **zmanim-alarm-debug.apk**
5. Transfer to your Android device and install

**Note**: You may need to enable "Install from Unknown Sources" in your Android settings.

### Option 2: Build Locally
If you have Android Studio installed:
```bash
git clone <repository-url>
cd Shma
./gradlew assembleDebug
```
The APK will be in `app/build/outputs/apk/debug/`

## Features

### Core Functionality
- **Dynamic Zmanim Calculation**: Automatically calculates SZKS (Magen Avraham and Gra) based on the device's current location using the KosherKotlin library
- **Smart Alarm Scheduling**: Set alarms for X minutes before the earliest SZKS time
- **Location-Aware**: Automatically recalculates when location changes significantly (>1km)
- **Reliable Scheduling**: Uses `AlarmManager.setAlarmClock()` for maximum precision and Google Play compliance

### Reliability Features
- **Boot Persistence**: Automatically reschedules alarms after device reboot
- **DST Handling**: Automatically adjusts for Daylight Saving Time changes
- **Timezone Support**: Handles timezone changes seamlessly
- **Battery Optimization**: Minimal battery impact with efficient background processing

### User Experience
- **Modern UI**: Material 3 design with Jetpack Compose
- **Dark/Light Theme**: Automatic theme switching based on system settings
- **Lock Screen Alarm**: Full-screen alarm that appears on the lock screen
- **Customizable Offset**: Configure how many minutes before SZKS to trigger the alarm (5-60 minutes)

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with ViewModel and StateFlow
- **Dependency Injection**: Hilt
- **Zmanim Library**: KosherKotlin (io.github.kdroidfilter:kosherkotlin)
- **Location Services**: Google Play Services (FusedLocationProviderClient)
- **Data Persistence**: DataStore Preferences
- **Background Work**: WorkManager for periodic location checks
- **Alarm Scheduling**: AlarmManager with exact alarm permissions

## Architecture

### Project Structure
```
com.zmanim.alarm/
├── data/
│   ├── datastore/     # DataStore for settings persistence
│   └── model/         # Data classes (ZmanimData, LocationData, AlarmSettings)
├── domain/            # Business logic
│   ├── ZmanimCalculator    # Zmanim calculation using KosherKotlin
│   └── LocationProvider    # Location services wrapper
├── service/           # Android services
│   ├── AlarmScheduler      # Alarm scheduling logic
│   └── AlarmRingingService # Foreground service for alarm
├── receiver/          # BroadcastReceivers
│   ├── AlarmReceiver       # Handles alarm triggers
│   ├── BootReceiver        # Reschedules after boot
│   └── TimeZoneReceiver    # Handles DST changes
├── worker/            # WorkManager workers
│   └── LocationCheckWorker # Periodic location checking
├── ui/                # UI layer
│   ├── MainActivity        # Main screen
│   ├── MainViewModel       # Main screen ViewModel
│   ├── alarm/             # Alarm ringing screen
│   └── theme/             # Material 3 theme
└── di/                # Hilt dependency injection modules
```

## Permissions

The app requires the following permissions:

### Essential
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: For calculating Zmanim based on current location
- `POST_NOTIFICATIONS`: For alarm notifications (Android 13+)
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`: For precise alarm scheduling (Android 12+)

### System
- `RECEIVE_BOOT_COMPLETED`: To reschedule alarms after device reboot
- `WAKE_LOCK`: To ensure alarm triggers reliably
- `VIBRATE`: For alarm vibration
- `FOREGROUND_SERVICE`: For alarm ringing service

## Privacy & Security

- **Local-Only Processing**: All Zmanim calculations happen on-device; no location data is transmitted
- **No Network Required**: Works completely offline (except for initial Google Play Services setup)
- **Minimal Data Collection**: Only stores alarm settings and last known location locally
- **No Backup**: Sensitive data is excluded from cloud backups

## Google Play Compliance

This app is designed to meet Google Play's requirements:

1. ✅ **Exact Alarm Permission**: Properly declared and justified for alarm app functionality
2. ✅ **Runtime Permissions**: All dangerous permissions requested at runtime with clear explanations
3. ✅ **Background Location**: Not used; only foreground location access
4. ✅ **Foreground Service**: Uses `specialUse` type with proper justification
5. ✅ **Privacy Policy**: No data collection = simplified privacy requirements

## Building the App

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or newer
- Android SDK 34
- Gradle 8.4

### Build Steps

1. Clone the repository:
```bash
git clone https://github.com/binim10/Shma.git
cd Shma
```

2. Open the project in Android Studio

3. Sync Gradle files

4. Build and run:
```bash
./gradlew assembleDebug
```

Or use Android Studio's Run button.

### Release Build

For production builds:

1. Generate a signing key:
```bash
keytool -genkey -v -keystore my-release-key.keystore -alias alias_name -keyalg RSA -keysize 2048 -validity 10000
```

2. Create `keystore.properties` in the project root:
```properties
storeFile=my-release-key.keystore
storePassword=your_password
keyAlias=alias_name
keyPassword=your_password
```

3. Build release APK:
```bash
./gradlew assembleRelease
```

## Usage

1. **First Launch**: Grant location and notification permissions
2. **Enable Alarm**: Toggle the "Enable Zmanim Alarm" switch
3. **Adjust Timing**: Use the slider to set how many minutes before SZKS you want the alarm
4. **View Next Alarm**: The app displays when the next alarm will trigger
5. **When Alarm Rings**: Dismiss or snooze the alarm from the full-screen interface

## Testing Checklist

- [ ] Alarm triggers at correct time
- [ ] Alarm reschedules after boot
- [ ] Alarm reschedules after timezone change
- [ ] Alarm updates when location changes significantly
- [ ] Lock screen alarm displays correctly
- [ ] Permissions requested properly
- [ ] Settings persist across app restarts
- [ ] Dark/light theme works correctly

## Future Enhancements

- Multiple alarm support
- Different zmanim types (Chatzos, Shkiah, etc.)
- Custom ringtone selection
- Weekly schedule (enable/disable specific days)
- Notification history
- Widget support
- Backup/restore settings

## License

Copyright © 2026. All rights reserved.

## Acknowledgments

- [KosherKotlin](https://github.com/kdroidfilter/KosherKotlin) for Zmanim calculations
- [KosherJava](https://github.com/KosherJava/zmanim) - the original Java library
