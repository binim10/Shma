# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Cellar/android-sdk/24.3.3/tools/proguard/proguard-android.txt

# Keep KosherKotlin classes
-keep class com.kosherjava.zmanim.** { *; }
-dontwarn com.kosherjava.zmanim.**

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Keep data classes
-keep @kotlinx.serialization.Serializable class * { *; }
