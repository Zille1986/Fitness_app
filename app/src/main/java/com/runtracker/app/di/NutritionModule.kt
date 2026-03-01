package com.runtracker.app.di

import com.runtracker.shared.data.db.DailyNutritionDao
import com.runtracker.shared.data.db.NutritionGoalsDao
import com.runtracker.shared.data.db.GoSteadyDatabase
import com.runtracker.shared.data.repository.NutritionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NutritionModule {

    @Provides
    @Singleton
    fun provideDailyNutritionDao(database: GoSteadyDatabase): DailyNutritionDao {
        return database.dailyNutritionDao()
    }

    @Provides
    @Singleton
    fun provideNutritionGoalsDao(database: GoSteadyDatabase): NutritionGoalsDao {
        return database.nutritionGoalsDao()
    }

    @Provides
    @Singleton
    fun provideNutritionRepository(
        dailyNutritionDao: DailyNutritionDao,
        nutritionGoalsDao: NutritionGoalsDao
    ): NutritionRepository {
        return NutritionRepository(dailyNutritionDao, nutritionGoalsDao)
    }
}
