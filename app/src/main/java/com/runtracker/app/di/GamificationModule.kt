package com.runtracker.app.di

import com.runtracker.shared.data.db.GamificationDao
import com.runtracker.shared.data.db.RunTrackerDatabase
import com.runtracker.shared.data.repository.GamificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GamificationModule {

    @Provides
    @Singleton
    fun provideGamificationDao(database: RunTrackerDatabase): GamificationDao {
        return database.gamificationDao()
    }

    @Provides
    @Singleton
    fun provideGamificationRepository(gamificationDao: GamificationDao): GamificationRepository {
        return GamificationRepository(gamificationDao)
    }
}
