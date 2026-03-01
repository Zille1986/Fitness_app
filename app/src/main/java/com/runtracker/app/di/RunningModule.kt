package com.runtracker.app.di

import com.runtracker.shared.data.db.PersonalBestDao
import com.runtracker.shared.data.db.RunDao
import com.runtracker.shared.data.db.GoSteadyDatabase
import com.runtracker.shared.data.repository.PersonalBestRepository
import com.runtracker.shared.data.repository.RunRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RunningModule {

    @Provides
    @Singleton
    fun provideRunDao(database: GoSteadyDatabase): RunDao {
        return database.runDao()
    }

    @Provides
    @Singleton
    fun provideRunRepository(runDao: RunDao): RunRepository {
        return RunRepository(runDao)
    }

    @Provides
    @Singleton
    fun providePersonalBestDao(database: GoSteadyDatabase): PersonalBestDao {
        return database.personalBestDao()
    }

    @Provides
    @Singleton
    fun providePersonalBestRepository(personalBestDao: PersonalBestDao): PersonalBestRepository {
        return PersonalBestRepository(personalBestDao)
    }
}
