package com.runtracker.app.di

import android.content.Context
import com.runtracker.app.audio.HIITAudioCueManager
import com.runtracker.shared.data.db.HIITDao
import com.runtracker.shared.data.db.GoSteadyDatabase
import com.runtracker.shared.data.repository.HIITRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HIITModule {

    @Provides
    @Singleton
    fun provideHIITDao(database: GoSteadyDatabase): HIITDao {
        return database.hiitDao()
    }

    @Provides
    @Singleton
    fun provideHIITRepository(hiitDao: HIITDao): HIITRepository {
        return HIITRepository(hiitDao)
    }

    @Provides
    @Singleton
    fun provideHIITAudioCueManager(@ApplicationContext context: Context): HIITAudioCueManager {
        return HIITAudioCueManager(context)
    }
}
