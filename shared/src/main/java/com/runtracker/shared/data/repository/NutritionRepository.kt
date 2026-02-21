package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.DailyNutritionDao
import com.runtracker.shared.data.db.NutritionGoalsDao
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.*

class NutritionRepository(
    private val dailyNutritionDao: DailyNutritionDao,
    private val nutritionGoalsDao: NutritionGoalsDao
) {
    // Daily Nutrition
    fun getTodayNutrition(): Flow<DailyNutrition?> {
        return dailyNutritionDao.getDailyNutritionByDateFlow(getStartOfDay())
    }

    suspend fun getTodayNutritionOnce(): DailyNutrition? {
        return dailyNutritionDao.getDailyNutritionByDate(getStartOfDay())
    }

    fun getRecentNutrition(days: Int = 7): Flow<List<DailyNutrition>> {
        return dailyNutritionDao.getRecentNutritionFlow(days)
    }

    fun getNutritionInRange(startDate: Long, endDate: Long): Flow<List<DailyNutrition>> {
        return dailyNutritionDao.getNutritionInRangeFlow(startDate, endDate)
    }

    suspend fun getOrCreateTodayNutrition(
        userProfile: UserProfile?,
        goals: NutritionGoals?
    ): DailyNutrition {
        val today = getStartOfDay()
        val existing = dailyNutritionDao.getDailyNutritionByDate(today)
        
        if (existing != null) return existing

        // Calculate targets based on user profile and goals
        val effectiveGoals = goals ?: NutritionGoals()
        val targets = calculateDailyTargets(userProfile, effectiveGoals)

        val newNutrition = DailyNutrition(
            date = today,
            targetCalories = targets.calories,
            targetProteinGrams = targets.proteinGrams,
            targetCarbsGrams = targets.carbsGrams,
            targetFatGrams = targets.fatGrams,
            targetFiberGrams = targets.fiberGrams,
            targetWaterMl = userProfile?.weight?.let { 
                NutritionCalculator.calculateWaterTarget(it, effectiveGoals) 
            } ?: 2500
        )

        dailyNutritionDao.insertDailyNutrition(newNutrition)
        return newNutrition
    }

    suspend fun updateDailyNutrition(nutrition: DailyNutrition) {
        dailyNutritionDao.updateDailyNutrition(nutrition.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun addMeal(meal: MealEntry) {
        val today = getTodayNutritionOnce() ?: return
        val updatedMeals = today.meals + meal
        val updatedNutrition = today.copy(
            meals = updatedMeals,
            consumedCalories = today.consumedCalories + meal.calories,
            consumedProteinGrams = today.consumedProteinGrams + meal.proteinGrams.toInt(),
            consumedCarbsGrams = today.consumedCarbsGrams + meal.carbsGrams.toInt(),
            consumedFatGrams = today.consumedFatGrams + meal.fatGrams.toInt(),
            consumedFiberGrams = today.consumedFiberGrams + meal.fiberGrams.toInt()
        )
        updateDailyNutrition(updatedNutrition)
    }

    suspend fun removeMeal(mealId: String) {
        val today = getTodayNutritionOnce() ?: return
        val meal = today.meals.find { it.id == mealId } ?: return
        val updatedMeals = today.meals.filter { it.id != mealId }
        val updatedNutrition = today.copy(
            meals = updatedMeals,
            consumedCalories = (today.consumedCalories - meal.calories).coerceAtLeast(0),
            consumedProteinGrams = (today.consumedProteinGrams - meal.proteinGrams.toInt()).coerceAtLeast(0),
            consumedCarbsGrams = (today.consumedCarbsGrams - meal.carbsGrams.toInt()).coerceAtLeast(0),
            consumedFatGrams = (today.consumedFatGrams - meal.fatGrams.toInt()).coerceAtLeast(0),
            consumedFiberGrams = (today.consumedFiberGrams - meal.fiberGrams.toInt()).coerceAtLeast(0)
        )
        updateDailyNutrition(updatedNutrition)
    }

    suspend fun addWater(ml: Int) {
        val today = getTodayNutritionOnce() ?: return
        val updatedNutrition = today.copy(waterMl = today.waterMl + ml)
        updateDailyNutrition(updatedNutrition)
    }

    suspend fun updateStepCount(steps: Int, weightKg: Double) {
        val today = getTodayNutritionOnce() ?: return
        val stepCalories = NutritionCalculator.calculateStepCalories(steps, weightKg)
        val updatedNutrition = today.copy(
            stepCount = steps,
            stepCaloriesBurned = stepCalories,
            burnedCalories = stepCalories + today.workoutCaloriesBurned
        )
        updateDailyNutrition(updatedNutrition)
    }

    suspend fun addWorkoutCalories(calories: Int) {
        val today = getTodayNutritionOnce() ?: return
        val updatedNutrition = today.copy(
            workoutCaloriesBurned = today.workoutCaloriesBurned + calories,
            burnedCalories = today.stepCaloriesBurned + today.workoutCaloriesBurned + calories
        )
        updateDailyNutrition(updatedNutrition)
    }
    
    suspend fun setWorkoutCalories(calories: Int) {
        val today = getTodayNutritionOnce() ?: return
        val updatedNutrition = today.copy(
            workoutCaloriesBurned = calories,
            burnedCalories = today.stepCaloriesBurned + calories
        )
        updateDailyNutrition(updatedNutrition)
    }

    // Nutrition Goals
    fun getGoals(): Flow<NutritionGoals?> {
        return nutritionGoalsDao.getGoalsFlow()
    }

    suspend fun getGoalsOnce(): NutritionGoals? {
        return nutritionGoalsDao.getGoals()
    }

    suspend fun saveGoals(goals: NutritionGoals) {
        nutritionGoalsDao.insertGoals(goals.copy(updatedAt = System.currentTimeMillis()))
    }

    // Stats
    suspend fun getWeeklyAverages(): NutritionWeeklyStats {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekAgo = calendar.timeInMillis
        val now = System.currentTimeMillis()

        return NutritionWeeklyStats(
            avgCalories = dailyNutritionDao.getAverageCaloriesInRange(weekAgo, now)?.toInt() ?: 0,
            avgProtein = dailyNutritionDao.getAverageProteinInRange(weekAgo, now)?.toInt() ?: 0,
            totalSteps = dailyNutritionDao.getTotalStepsInRange(weekAgo, now) ?: 0
        )
    }

    // Helpers
    private fun calculateDailyTargets(
        userProfile: UserProfile?,
        goals: NutritionGoals
    ): MacroTargets {
        val weight = userProfile?.weight ?: 70.0
        val height = userProfile?.height ?: 170.0
        val age = userProfile?.age ?: 30
        val gender = userProfile?.gender

        val calories = NutritionCalculator.calculateDailyCalories(
            weightKg = weight,
            heightCm = height,
            ageYears = age,
            gender = gender,
            activityLevel = goals.activityLevel,
            goal = goals.goal
        )

        return NutritionCalculator.calculateMacros(calories, weight, goals)
    }

    private fun getStartOfDay(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

data class NutritionWeeklyStats(
    val avgCalories: Int,
    val avgProtein: Int,
    val totalSteps: Int
)
