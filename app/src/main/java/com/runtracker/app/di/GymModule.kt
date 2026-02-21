package com.runtracker.app.di

import com.runtracker.shared.data.db.*
import com.runtracker.shared.data.repository.GymRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GymModule {

    @Provides
    @Singleton
    fun provideExerciseDao(database: RunTrackerDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    @Singleton
    fun provideGymWorkoutDao(database: RunTrackerDatabase): GymWorkoutDao {
        return database.gymWorkoutDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutTemplateDao(database: RunTrackerDatabase): WorkoutTemplateDao {
        return database.workoutTemplateDao()
    }

    @Provides
    @Singleton
    fun provideExerciseHistoryDao(database: RunTrackerDatabase): ExerciseHistoryDao {
        return database.exerciseHistoryDao()
    }

    @Provides
    @Singleton
    fun provideGymRepository(
        exerciseDao: ExerciseDao,
        gymWorkoutDao: GymWorkoutDao,
        workoutTemplateDao: WorkoutTemplateDao,
        exerciseHistoryDao: ExerciseHistoryDao
    ): GymRepository {
        return GymRepository(exerciseDao, gymWorkoutDao, workoutTemplateDao, exerciseHistoryDao)
    }

    @Provides
    @Singleton
    fun provideWorkoutPlanDao(database: RunTrackerDatabase): WorkoutPlanDao {
        return database.workoutPlanDao()
    }

    @Provides
    @Singleton
    fun provideScheduledGymWorkoutDao(database: RunTrackerDatabase): ScheduledGymWorkoutDao {
        return database.scheduledGymWorkoutDao()
    }
}
