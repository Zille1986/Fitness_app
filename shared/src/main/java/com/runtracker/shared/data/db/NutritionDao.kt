package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.DailyNutrition
import com.runtracker.shared.data.model.NutritionGoals
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyNutritionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyNutrition(nutrition: DailyNutrition)

    @Update
    suspend fun updateDailyNutrition(nutrition: DailyNutrition)

    @Query("SELECT * FROM daily_nutrition WHERE date = :date")
    suspend fun getDailyNutritionByDate(date: Long): DailyNutrition?

    @Query("SELECT * FROM daily_nutrition WHERE date = :date")
    fun getDailyNutritionByDateFlow(date: Long): Flow<DailyNutrition?>

    @Query("SELECT * FROM daily_nutrition ORDER BY date DESC LIMIT :limit")
    fun getRecentNutritionFlow(limit: Int): Flow<List<DailyNutrition>>

    @Query("SELECT * FROM daily_nutrition WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getNutritionInRangeFlow(startDate: Long, endDate: Long): Flow<List<DailyNutrition>>

    @Query("SELECT AVG(consumedCalories) FROM daily_nutrition WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageCaloriesInRange(startDate: Long, endDate: Long): Double?

    @Query("SELECT AVG(consumedProteinGrams) FROM daily_nutrition WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageProteinInRange(startDate: Long, endDate: Long): Double?

    @Query("SELECT SUM(stepCount) FROM daily_nutrition WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalStepsInRange(startDate: Long, endDate: Long): Int?

    @Delete
    suspend fun deleteDailyNutrition(nutrition: DailyNutrition)
}

@Dao
interface NutritionGoalsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: NutritionGoals)

    @Update
    suspend fun updateGoals(goals: NutritionGoals)

    @Query("SELECT * FROM nutrition_goals WHERE id = 1")
    suspend fun getGoals(): NutritionGoals?

    @Query("SELECT * FROM nutrition_goals WHERE id = 1")
    fun getGoalsFlow(): Flow<NutritionGoals?>
}
