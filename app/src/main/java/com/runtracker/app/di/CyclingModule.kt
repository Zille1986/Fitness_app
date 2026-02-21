package com.runtracker.app.di

import com.runtracker.shared.data.db.CyclingTrainingPlanDao
import com.runtracker.shared.data.db.CyclingWorkoutDao
import com.runtracker.shared.data.db.RunTrackerDatabase
import com.runtracker.shared.data.repository.CyclingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CyclingModule {

    @Provides
    @Singleton
    fun provideCyclingWorkoutDao(database: RunTrackerDatabase): CyclingWorkoutDao {
        return database.cyclingWorkoutDao()
    }

    @Provides
    @Singleton
    fun provideCyclingTrainingPlanDao(database: RunTrackerDatabase): CyclingTrainingPlanDao {
        return database.cyclingTrainingPlanDao()
    }

    @Provides
    @Singleton
    fun provideCyclingRepository(
        workoutDao: CyclingWorkoutDao,
        planDao: CyclingTrainingPlanDao
    ): CyclingRepository {
        return CyclingRepository(workoutDao, planDao)
    }
}
