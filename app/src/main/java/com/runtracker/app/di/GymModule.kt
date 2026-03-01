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
    fun provideExerciseDao(database: GoSteadyDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    @Singleton
    fun provideGymWorkoutDao(database: GoSteadyDatabase): GymWorkoutDao {
        return database.gymWorkoutDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutTemplateDao(database: GoSteadyDatabase): WorkoutTemplateDao {
        return database.workoutTemplateDao()
    }

    @Provides
    @Singleton
    fun provideExerciseHistoryDao(database: GoSteadyDatabase): ExerciseHistoryDao {
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
    fun provideWorkoutPlanDao(database: GoSteadyDatabase): WorkoutPlanDao {
        return database.workoutPlanDao()
    }

    @Provides
    @Singleton
    fun provideScheduledGymWorkoutDao(database: GoSteadyDatabase): ScheduledGymWorkoutDao {
        return database.scheduledGymWorkoutDao()
    }
}
