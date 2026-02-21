package com.runtracker.app.di

import com.runtracker.shared.data.db.*
import com.runtracker.shared.data.repository.BodyAnalysisRepository
import com.runtracker.shared.data.repository.CustomWorkoutRepository
import com.runtracker.shared.data.repository.PersonalizedPlanRepository
import com.runtracker.shared.data.repository.TrainingPlanRepository
import com.runtracker.shared.data.service.PlanGeneratorService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TrainingModule {

    @Provides
    @Singleton
    fun provideTrainingPlanDao(database: RunTrackerDatabase): TrainingPlanDao {
        return database.trainingPlanDao()
    }

    @Provides
    @Singleton
    fun provideTrainingPlanRepository(trainingPlanDao: TrainingPlanDao): TrainingPlanRepository {
        return TrainingPlanRepository(trainingPlanDao)
    }

    @Provides
    @Singleton
    fun provideCustomRunWorkoutDao(database: RunTrackerDatabase): CustomRunWorkoutDao {
        return database.customRunWorkoutDao()
    }

    @Provides
    @Singleton
    fun provideCustomTrainingPlanDao(database: RunTrackerDatabase): CustomTrainingPlanDao {
        return database.customTrainingPlanDao()
    }

    @Provides
    @Singleton
    fun provideCustomWorkoutRepository(
        customRunWorkoutDao: CustomRunWorkoutDao,
        customTrainingPlanDao: CustomTrainingPlanDao
    ): CustomWorkoutRepository {
        return CustomWorkoutRepository(customRunWorkoutDao, customTrainingPlanDao)
    }

    @Provides
    @Singleton
    fun providePersonalizedPlanDao(database: RunTrackerDatabase): PersonalizedPlanDao {
        return database.personalizedPlanDao()
    }

    @Provides
    @Singleton
    fun providePlanGeneratorService(): PlanGeneratorService {
        return PlanGeneratorService()
    }

    @Provides
    @Singleton
    fun providePersonalizedPlanRepository(
        personalizedPlanDao: PersonalizedPlanDao,
        planGeneratorService: PlanGeneratorService,
        bodyAnalysisRepository: BodyAnalysisRepository
    ): PersonalizedPlanRepository {
        return PersonalizedPlanRepository(personalizedPlanDao, planGeneratorService, bodyAnalysisRepository)
    }
}
