# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Cellar/android-sdk/24.3.3/tools/proguard/proguard-android.txt

# =============================================================================
# CRITICAL: Java Time API - Required for ZonedDateTime in DataStore
# =============================================================================
# Without these, DataStore fails to deserialize ZonedDateTime and app crashes
-keep class java.time.** { *; }
-keepclassmembers class java.time.** { *; }
-dontwarn java.time.**

# =============================================================================
# App Data Models - CRITICAL for DataStore serialization
# =============================================================================
-keep class com.zmanim.alarm.data.model.** { *; }
-keepclassmembers class com.zmanim.alarm.data.model.** {
    <fields>;
    <init>(...);
}

# =============================================================================
# Alarm Providers
# =============================================================================
-keep class com.zmanim.alarm.service.provider.** { *; }
-keepclassmembers class com.zmanim.alarm.service.provider.** {
    <methods>;
}

# =============================================================================
# KosherJava Zmanim Library
# =============================================================================
-keep class com.kosherjava.zmanim.** { *; }
-dontwarn com.kosherjava.zmanim.**

# =============================================================================
# Hilt Dependency Injection
# =============================================================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Hilt generated code
-keep class **_HiltModules { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# Keep @Inject annotated constructors and fields
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# =============================================================================
# DataStore
# =============================================================================
-keep class androidx.datastore.*.** { *; }
-keepclassmembers class androidx.datastore.preferences.core.** { *; }

# =============================================================================
# Kotlin & Coroutines
# =============================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlin.Metadata { *; }

# =============================================================================
# Android Components
# =============================================================================
# Keep Application class
-keep class com.zmanim.alarm.ZmanimAlarmApplication { *; }

# Keep BroadcastReceivers
-keep class * extends android.content.BroadcastReceiver {
    public <init>(...);
}

# Keep Services
-keep class * extends android.app.Service {
    public <init>(...);
}

# =============================================================================
# Debugging Support
# =============================================================================
# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable

# Keep annotations for reflection
-keepattributes *Annotation*

# Keep generic signatures for reflection
-keepattributes Signature
