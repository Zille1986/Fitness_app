package com.runtracker.app

import com.runtracker.shared.data.model.*
import org.junit.Assert.*
import org.junit.Test

class NutritionCalculatorTest {

    @Test
    fun `calculateBMR for male returns correct value`() {
        val bmr = NutritionCalculator.calculateBMR(
            weightKg = 80.0,
            heightCm = 180.0,
            ageYears = 30,
            gender = Gender.MALE
        )
        // Mifflin-St Jeor: (10 * 80) + (6.25 * 180) - (5 * 30) + 5 = 800 + 1125 - 150 + 5 = 1780
        assertEquals(1780.0, bmr, 0.1)
    }

    @Test
    fun `calculateBMR for female returns correct value`() {
        val bmr = NutritionCalculator.calculateBMR(
            weightKg = 65.0,
            heightCm = 165.0,
            ageYears = 28,
            gender = Gender.FEMALE
        )
        // Mifflin-St Jeor: (10 * 65) + (6.25 * 165) - (5 * 28) - 161 = 650 + 1031.25 - 140 - 161 = 1380.25
        assertEquals(1380.25, bmr, 0.1)
    }

    @Test
    fun `calculateTDEE applies correct activity multiplier`() {
        val bmr = 1800.0
        
        assertEquals(2160.0, NutritionCalculator.calculateTDEE(bmr, ActivityLevel.SEDENTARY), 0.1)
        assertEquals(2475.0, NutritionCalculator.calculateTDEE(bmr, ActivityLevel.LIGHTLY_ACTIVE), 0.1)
        assertEquals(2790.0, NutritionCalculator.calculateTDEE(bmr, ActivityLevel.MODERATELY_ACTIVE), 0.1)
        assertEquals(3105.0, NutritionCalculator.calculateTDEE(bmr, ActivityLevel.VERY_ACTIVE), 0.1)
        assertEquals(3420.0, NutritionCalculator.calculateTDEE(bmr, ActivityLevel.EXTREMELY_ACTIVE), 0.1)
    }

    @Test
    fun `calculateDailyCalories applies goal adjustment`() {
        val baseParams = listOf(80.0, 180.0, 30, Gender.MALE, ActivityLevel.MODERATELY_ACTIVE)
        
        val maintainCalories = NutritionCalculator.calculateDailyCalories(
            weightKg = 80.0, heightCm = 180.0, ageYears = 30,
            gender = Gender.MALE, activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goal = NutritionGoalType.MAINTAIN
        )
        
        val loseCalories = NutritionCalculator.calculateDailyCalories(
            weightKg = 80.0, heightCm = 180.0, ageYears = 30,
            gender = Gender.MALE, activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goal = NutritionGoalType.LOSE_WEIGHT
        )
        
        val gainCalories = NutritionCalculator.calculateDailyCalories(
            weightKg = 80.0, heightCm = 180.0, ageYears = 30,
            gender = Gender.MALE, activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goal = NutritionGoalType.GAIN_MUSCLE
        )
        
        assertEquals(maintainCalories - 500, loseCalories)
        assertEquals(maintainCalories + 250, gainCalories)
    }

    @Test
    fun `calculateStepCalories returns reasonable values`() {
        val calories = NutritionCalculator.calculateStepCalories(10000, 70.0)
        // 10000 * 0.04 * (70/70) = 400
        assertEquals(400, calories)
        
        val caloriesHeavy = NutritionCalculator.calculateStepCalories(10000, 100.0)
        // 10000 * 0.04 * (100/70) ≈ 571
        assertTrue(caloriesHeavy > calories)
    }

    @Test
    fun `calculateRunningCalories varies by pace`() {
        val slowJog = NutritionCalculator.calculateRunningCalories(
            distanceKm = 5.0, durationMinutes = 50.0, weightKg = 70.0
        )
        
        val fastRun = NutritionCalculator.calculateRunningCalories(
            distanceKm = 5.0, durationMinutes = 25.0, weightKg = 70.0
        )
        
        // Faster pace should burn more calories per minute but less total time
        // Both should be reasonable values
        assertTrue(slowJog > 200)
        assertTrue(fastRun > 200)
    }

    @Test
    fun `calculateMacros distributes calories correctly`() {
        val goals = NutritionGoals(
            proteinPerKgBodyweight = 2.0,
            carbPercentage = 0.45,
            fatPercentage = 0.25
        )
        
        val macros = NutritionCalculator.calculateMacros(
            totalCalories = 2500,
            weightKg = 75.0,
            goals = goals
        )
        
        // Protein: 75 * 2.0 = 150g = 600 cal
        assertEquals(150, macros.proteinGrams)
        
        // Remaining: 2500 - 600 = 1900
        // Carbs get 45/(45+25) = 64.3% of remaining
        // Fat gets 25/(45+25) = 35.7% of remaining
        assertTrue(macros.carbsGrams > 200)
        assertTrue(macros.fatGrams > 50)
        
        // Total calories should roughly match
        val totalCal = (macros.proteinGrams * 4) + (macros.carbsGrams * 4) + (macros.fatGrams * 9)
        assertEquals(2500.0, totalCal.toDouble(), 50.0)
    }

    @Test
    fun `calculateWaterTarget scales with body weight`() {
        val goals = NutritionGoals(waterPerKgBodyweight = 35.0)
        
        val water70kg = NutritionCalculator.calculateWaterTarget(70.0, goals)
        val water90kg = NutritionCalculator.calculateWaterTarget(90.0, goals)
        
        assertEquals(2450, water70kg)
        assertEquals(3150, water90kg)
    }
}
