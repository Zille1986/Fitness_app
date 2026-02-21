package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

@Entity(tableName = "daily_nutrition")
@TypeConverters(Converters::class)
data class DailyNutrition(
    @PrimaryKey
    val date: Long, // Start of day timestamp
    val targetCalories: Int = 2000,
    val consumedCalories: Int = 0,
    val burnedCalories: Int = 0,
    val targetProteinGrams: Int = 150,
    val consumedProteinGrams: Int = 0,
    val targetCarbsGrams: Int = 250,
    val consumedCarbsGrams: Int = 0,
    val targetFatGrams: Int = 65,
    val consumedFatGrams: Int = 0,
    val targetFiberGrams: Int = 30,
    val consumedFiberGrams: Int = 0,
    val waterMl: Int = 0,
    val targetWaterMl: Int = 2500,
    val stepCount: Int = 0,
    val stepCaloriesBurned: Int = 0,
    val workoutCaloriesBurned: Int = 0,
    val meals: List<MealEntry> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val netCalories: Int get() = consumedCalories - burnedCalories
    val remainingCalories: Int get() = targetCalories - consumedCalories + burnedCalories
    val calorieProgress: Float get() = (consumedCalories.toFloat() / targetCalories).coerceIn(0f, 1.5f)
    val proteinProgress: Float get() = (consumedProteinGrams.toFloat() / targetProteinGrams).coerceIn(0f, 1.5f)
    val carbsProgress: Float get() = (consumedCarbsGrams.toFloat() / targetCarbsGrams).coerceIn(0f, 1.5f)
    val fatProgress: Float get() = (consumedFatGrams.toFloat() / targetFatGrams).coerceIn(0f, 1.5f)
    val waterProgress: Float get() = (waterMl.toFloat() / targetWaterMl).coerceIn(0f, 1.5f)
}

data class MealEntry(
    val id: String,
    val name: String,
    val mealType: MealType,
    val calories: Int,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val fiberGrams: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)

enum class MealType {
    BREAKFAST,
    MORNING_SNACK,
    LUNCH,
    AFTERNOON_SNACK,
    DINNER,
    EVENING_SNACK
}

@Entity(tableName = "nutrition_goals")
data class NutritionGoals(
    @PrimaryKey
    val id: Int = 1,
    val goal: NutritionGoalType = NutritionGoalType.MAINTAIN,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATELY_ACTIVE,
    val proteinPerKgBodyweight: Double = 1.6, // grams per kg
    val carbPercentage: Double = 0.45, // 45% of calories
    val fatPercentage: Double = 0.25, // 25% of calories
    val dailyStepGoal: Int = 10000,
    val waterPerKgBodyweight: Double = 35.0, // ml per kg
    val mealFrequency: Int = 4, // meals per day
    val updatedAt: Long = System.currentTimeMillis()
)

enum class NutritionGoalType {
    LOSE_WEIGHT,      // -500 cal deficit
    LOSE_WEIGHT_SLOW, // -250 cal deficit
    MAINTAIN,         // maintenance
    GAIN_MUSCLE,      // +250 cal surplus
    BULK              // +500 cal surplus
}

enum class ActivityLevel {
    SEDENTARY,        // Little to no exercise, desk job
    LIGHTLY_ACTIVE,   // Light exercise 1-3 days/week
    MODERATELY_ACTIVE,// Moderate exercise 3-5 days/week
    VERY_ACTIVE,      // Hard exercise 6-7 days/week
    EXTREMELY_ACTIVE  // Very hard exercise, physical job
}

val ActivityLevel.multiplier: Double
    get() = when (this) {
        ActivityLevel.SEDENTARY -> 1.2
        ActivityLevel.LIGHTLY_ACTIVE -> 1.375
        ActivityLevel.MODERATELY_ACTIVE -> 1.55
        ActivityLevel.VERY_ACTIVE -> 1.725
        ActivityLevel.EXTREMELY_ACTIVE -> 1.9
    }

val ActivityLevel.displayName: String
    get() = when (this) {
        ActivityLevel.SEDENTARY -> "Sedentary"
        ActivityLevel.LIGHTLY_ACTIVE -> "Lightly Active"
        ActivityLevel.MODERATELY_ACTIVE -> "Moderately Active"
        ActivityLevel.VERY_ACTIVE -> "Very Active"
        ActivityLevel.EXTREMELY_ACTIVE -> "Extremely Active"
    }

val NutritionGoalType.displayName: String
    get() = when (this) {
        NutritionGoalType.LOSE_WEIGHT -> "Lose Weight"
        NutritionGoalType.LOSE_WEIGHT_SLOW -> "Lose Weight (Slow)"
        NutritionGoalType.MAINTAIN -> "Maintain Weight"
        NutritionGoalType.GAIN_MUSCLE -> "Build Muscle"
        NutritionGoalType.BULK -> "Bulk Up"
    }

val NutritionGoalType.calorieAdjustment: Int
    get() = when (this) {
        NutritionGoalType.LOSE_WEIGHT -> -500
        NutritionGoalType.LOSE_WEIGHT_SLOW -> -250
        NutritionGoalType.MAINTAIN -> 0
        NutritionGoalType.GAIN_MUSCLE -> 250
        NutritionGoalType.BULK -> 500
    }
