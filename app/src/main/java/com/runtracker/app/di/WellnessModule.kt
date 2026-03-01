package com.runtracker.app.di

import com.runtracker.shared.data.db.MentalHealthDao
import com.runtracker.shared.data.db.GoSteadyDatabase
import com.runtracker.shared.data.repository.FormReviewRepository
import com.runtracker.shared.data.repository.MentalHealthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WellnessModule {

    @Provides
    @Singleton
    fun provideMentalHealthDao(database: GoSteadyDatabase): MentalHealthDao {
        return database.mentalHealthDao()
    }

    @Provides
    @Singleton
    fun provideMentalHealthRepository(mentalHealthDao: MentalHealthDao): MentalHealthRepository {
        return MentalHealthRepository(mentalHealthDao)
    }

    @Provides
    @Singleton
    fun provideFormReviewRepository(): FormReviewRepository {
        return FormReviewRepository()
    }
}
