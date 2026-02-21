package com.runtracker.app.di

import com.runtracker.shared.data.db.RunTrackerDatabase
import com.runtracker.shared.data.db.SwimmingTrainingPlanDao
import com.runtracker.shared.data.db.SwimmingWorkoutDao
import com.runtracker.shared.data.repository.SwimmingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SwimmingModule {

    @Provides
    @Singleton
    fun provideSwimmingWorkoutDao(database: RunTrackerDatabase): SwimmingWorkoutDao {
        return database.swimmingWorkoutDao()
    }

    @Provides
    @Singleton
    fun provideSwimmingTrainingPlanDao(database: RunTrackerDatabase): SwimmingTrainingPlanDao {
        return database.swimmingTrainingPlanDao()
    }

    @Provides
    @Singleton
    fun provideSwimmingRepository(
        workoutDao: SwimmingWorkoutDao,
        planDao: SwimmingTrainingPlanDao
    ): SwimmingRepository {
        return SwimmingRepository(workoutDao, planDao)
    }
}
