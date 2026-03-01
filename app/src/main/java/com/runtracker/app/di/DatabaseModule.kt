package com.runtracker.app.di

import android.content.Context
import com.runtracker.shared.data.db.GoSteadyDatabase
import com.runtracker.shared.location.LocationTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GoSteadyDatabase {
        return GoSteadyDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideLocationTracker(@ApplicationContext context: Context): LocationTracker {
        return LocationTracker(context)
    }
}
