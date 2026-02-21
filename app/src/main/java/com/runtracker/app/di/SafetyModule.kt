package com.runtracker.app.di

import com.runtracker.shared.data.db.RunTrackerDatabase
import com.runtracker.shared.data.db.SafetyDao
import com.runtracker.shared.data.repository.SafetyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SafetyModule {

    @Provides
    @Singleton
    fun provideSafetyDao(database: RunTrackerDatabase): SafetyDao {
        return database.safetyDao()
    }

    @Provides
    @Singleton
    fun provideSafetyRepository(safetyDao: SafetyDao): SafetyRepository {
        return SafetyRepository(safetyDao)
    }
}
