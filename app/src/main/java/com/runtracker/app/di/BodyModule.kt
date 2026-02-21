package com.runtracker.app.di

import com.runtracker.shared.data.db.BodyScanDao
import com.runtracker.shared.data.db.RunTrackerDatabase
import com.runtracker.shared.data.repository.BodyAnalysisRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BodyModule {

    @Provides
    @Singleton
    fun provideBodyScanDao(database: RunTrackerDatabase): BodyScanDao {
        return database.bodyScanDao()
    }

    @Provides
    @Singleton
    fun provideBodyAnalysisRepository(bodyScanDao: BodyScanDao): BodyAnalysisRepository {
        return BodyAnalysisRepository(bodyScanDao)
    }
}
