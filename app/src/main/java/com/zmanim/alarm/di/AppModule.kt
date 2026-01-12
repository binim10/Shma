package com.zmanim.alarm.di

import android.content.Context
import com.zmanim.alarm.data.datastore.AlarmPreferences
import com.zmanim.alarm.domain.LocationProvider
import com.zmanim.alarm.domain.ZmanimCalculator
import com.zmanim.alarm.service.AlarmScheduler
import com.zmanim.alarm.service.provider.InternalAlarmProvider
import com.zmanim.alarm.service.provider.SleepAsAndroidProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAlarmPreferences(
        @ApplicationContext context: Context
    ): AlarmPreferences {
        return AlarmPreferences(context)
    }

    @Provides
    @Singleton
    fun provideZmanimCalculator(): ZmanimCalculator {
        return ZmanimCalculator()
    }

    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context
    ): LocationProvider {
        return LocationProvider(context)
    }

    @Provides
    @Singleton
    fun provideInternalAlarmProvider(
        @ApplicationContext context: Context
    ): InternalAlarmProvider {
        return InternalAlarmProvider(context)
    }

    @Provides
    @Singleton
    fun provideSleepAsAndroidProvider(
        @ApplicationContext context: Context
    ): SleepAsAndroidProvider {
        return SleepAsAndroidProvider(context)
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        @ApplicationContext context: Context,
        alarmPreferences: AlarmPreferences,
        zmanimCalculator: ZmanimCalculator,
        locationProvider: LocationProvider,
        internalAlarmProvider: InternalAlarmProvider,
        sleepAsAndroidProvider: SleepAsAndroidProvider
    ): AlarmScheduler {
        return AlarmScheduler(
            context,
            alarmPreferences,
            zmanimCalculator,
            locationProvider,
            internalAlarmProvider,
            sleepAsAndroidProvider
        )
    }
}
