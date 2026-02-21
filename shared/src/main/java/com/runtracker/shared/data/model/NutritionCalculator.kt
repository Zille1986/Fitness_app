package com.runtracker.shared.data.model

object NutritionCalculator {

    /**
     * Calculate Basal Metabolic Rate using Mifflin-St Jeor equation
     */
    fun calculateBMR(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        gender: Gender?
    ): Double {
        val base = (10 * weightKg) + (6.25 * heightCm) - (5 * ageYears)
        return when (gender) {
            Gender.MALE -> base + 5
            Gender.FEMALE -> base - 161
            else -> base - 78 // Average
        }
    }

    /**
     * Calculate Total Daily Energy Expenditure
     */
    fun calculateTDEE(
        bmr: Double,
        activityLevel: ActivityLevel
    ): Double {
        return bmr * activityLevel.multiplier
    }

    /**
     * Calculate daily calorie target based on goals
     */
    fun calculateDailyCalories(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        gender: Gender?,
        activityLevel: ActivityLevel,
        goal: NutritionGoalType
    ): Int {
        val bmr = calculateBMR(weightKg, heightCm, ageYears, gender)
        val tdee = calculateTDEE(bmr, activityLevel)
        return (tdee + goal.calorieAdjustment).toInt()
    }

    /**
     * Calculate calories burned from steps
     * Average: 0.04 calories per step per kg of body weight
     */
    fun calculateStepCalories(steps: Int, weightKg: Double): Int {
        return (steps * 0.04 * (weightKg / 70)).toInt()
    }

    /**
     * Calculate calories burned from running
     * MET value for running varies by pace
     */
    fun calculateRunningCalories(
        distanceKm: Double,
        durationMinutes: Double,
        weightKg: Double
    ): Int {
        val speedKmH = if (durationMinutes > 0) (distanceKm / durationMinutes) * 60 else 0.0
        val met = when {
            speedKmH < 6 -> 6.0   // Jogging
            speedKmH < 8 -> 8.3   // Running 8 km/h
            speedKmH < 10 -> 9.8  // Running 10 km/h
            speedKmH < 12 -> 11.0 // Running 12 km/h
            speedKmH < 14 -> 11.8 // Running 14 km/h
            else -> 12.8          // Running 16+ km/h
        }
        return ((met * 3.5 * weightKg / 200) * durationMinutes).toInt()
    }

    /**
     * Calculate calories burned from gym workout
     * Based on workout duration and intensity (volume)
     */
    fun calculateGymCalories(
        durationMinutes: Double,
        totalVolume: Double,
        weightKg: Double
    ): Int {
        // Base MET for weight training is 3-6 depending on intensity
        val intensityFactor = when {
            totalVolume < 5000 -> 3.5   // Light
            totalVolume < 10000 -> 4.5  // Moderate
            totalVolume < 20000 -> 5.5  // Intense
            else -> 6.5                  // Very intense
        }
        return ((intensityFactor * 3.5 * weightKg / 200) * durationMinutes).toInt()
    }

    /**
     * Calculate macro targets based on calories and goals
     */
    fun calculateMacros(
        totalCalories: Int,
        weightKg: Double,
        goals: NutritionGoals
    ): MacroTargets {
        // Protein: based on body weight
        val proteinGrams = (weightKg * goals.proteinPerKgBodyweight).toInt()
        val proteinCalories = proteinGrams * 4

        // Remaining calories for carbs and fat
        val remainingCalories = totalCalories - proteinCalories
        
        // Distribute remaining between carbs and fat
        val carbCalories = (remainingCalories * (goals.carbPercentage / (goals.carbPercentage + goals.fatPercentage))).toInt()
        val fatCalories = remainingCalories - carbCalories

        val carbsGrams = carbCalories / 4
        val fatGrams = fatCalories / 9
        val fiberGrams = (totalCalories / 1000.0 * 14).toInt() // 14g per 1000 calories

        return MacroTargets(
            calories = totalCalories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams,
            fiberGrams = fiberGrams
        )
    }

    /**
     * Calculate water intake target
     */
    fun calculateWaterTarget(weightKg: Double, goals: NutritionGoals): Int {
        return (weightKg * goals.waterPerKgBodyweight).toInt()
    }

    /**
     * Adjust calories based on today's activity
     */
    fun adjustCaloriesForActivity(
        baseCalories: Int,
        stepCount: Int,
        stepGoal: Int,
        workoutCalories: Int
    ): Int {
        // Add back workout calories (eat more on active days)
        val workoutAdjustment = (workoutCalories * 0.5).toInt() // Eat back 50% of workout calories
        
        // Adjust for steps above/below goal
        val stepDifference = stepCount - stepGoal
        val stepAdjustment = when {
            stepDifference > 5000 -> 150   // Very active day
            stepDifference > 2000 -> 75    // Active day
            stepDifference < -5000 -> -100 // Very sedentary day
            stepDifference < -2000 -> -50  // Sedentary day
            else -> 0
        }
        
        return baseCalories + workoutAdjustment + stepAdjustment
    }
}

data class MacroTargets(
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val fiberGrams: Int
)
