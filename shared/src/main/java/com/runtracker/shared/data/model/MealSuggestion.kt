package com.runtracker.shared.data.model

data class MealSuggestion(
    val id: String,
    val name: String,
    val description: String,
    val mealType: MealType,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double = 0.0,
    val tags: List<String> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val prepTimeMinutes: Int = 15,
    val isQuickMeal: Boolean = false,
    val isHighProtein: Boolean = false,
    val isLowCarb: Boolean = false,
    val isVegetarian: Boolean = false,
    val isVegan: Boolean = false
)

data class DailySuggestions(
    val date: Long,
    val targetCalories: Int,
    val targetProtein: Int,
    val remainingCalories: Int,
    val remainingProtein: Int,
    val dayType: DayType,
    val suggestions: List<MealSuggestion>,
    val tips: List<String>
)

enum class DayType {
    REST_DAY,
    LIGHT_ACTIVITY,
    CARDIO_DAY,
    STRENGTH_DAY,
    HIGH_ACTIVITY
}

object MealSuggestionEngine {

    fun generateSuggestions(
        dailyNutrition: DailyNutrition,
        goals: NutritionGoals,
        dayType: DayType,
        currentMealType: MealType
    ): List<MealSuggestion> {
        val remainingCalories = dailyNutrition.remainingCalories
        val remainingProtein = dailyNutrition.targetProteinGrams - dailyNutrition.consumedProteinGrams
        
        // Filter suggestions based on remaining macros and meal type
        val allSuggestions = getMealDatabase()
            .filter { it.mealType == currentMealType }
            .filter { it.calories <= remainingCalories + 100 } // Allow slight overflow
        
        // Prioritize based on day type and remaining macros
        return when {
            dayType == DayType.STRENGTH_DAY && remainingProtein > 30 -> {
                allSuggestions.filter { it.isHighProtein }.take(5)
            }
            dayType == DayType.CARDIO_DAY -> {
                allSuggestions.sortedByDescending { it.carbsGrams }.take(5)
            }
            dayType == DayType.REST_DAY -> {
                allSuggestions.filter { it.calories < remainingCalories / 2 }.take(5)
            }
            remainingProtein > 40 -> {
                allSuggestions.filter { it.isHighProtein }.take(5)
            }
            else -> allSuggestions.take(5)
        }.ifEmpty { allSuggestions.take(5) }
    }

    fun generateDailyTips(
        dailyNutrition: DailyNutrition,
        dayType: DayType,
        stepCount: Int,
        stepGoal: Int
    ): List<String> {
        val tips = mutableListOf<String>()
        
        // Hydration tips
        if (dailyNutrition.waterProgress < 0.5f) {
            tips.add("💧 You're behind on water intake. Try to drink a glass every hour.")
        }
        
        // Protein tips
        if (dailyNutrition.proteinProgress < 0.3f && 
            java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) > 12) {
            tips.add("🥩 Protein intake is low. Consider a high-protein snack or meal.")
        }
        
        // Activity-based tips
        when (dayType) {
            DayType.STRENGTH_DAY -> {
                tips.add("💪 Strength day! Aim for 1.6-2.2g protein per kg bodyweight.")
                if (dailyNutrition.carbsProgress < 0.5f) {
                    tips.add("🍚 Carbs fuel your lifts. Don't skip them on training days.")
                }
            }
            DayType.CARDIO_DAY -> {
                tips.add("🏃 Cardio day! Prioritize carbs for energy and recovery.")
                tips.add("🧂 Remember to replenish electrolytes after your run.")
            }
            DayType.REST_DAY -> {
                tips.add("😴 Rest day - focus on recovery nutrition and sleep.")
            }
            else -> {}
        }
        
        // Step-based tips
        if (stepCount < stepGoal / 2 && 
            java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) > 14) {
            tips.add("👟 You're behind on steps. A short walk can boost metabolism.")
        } else if (stepCount > stepGoal * 1.5) {
            tips.add("🔥 Great activity today! You've earned some extra calories.")
        }
        
        // Calorie tips
        if (dailyNutrition.remainingCalories < 200 && 
            java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) < 18) {
            tips.add("⚠️ Running low on calories. Save room for dinner!")
        }
        
        return tips.take(3)
    }

    fun determineDayType(
        hasRun: Boolean,
        hasGymWorkout: Boolean,
        stepCount: Int,
        stepGoal: Int
    ): DayType {
        return when {
            hasGymWorkout -> DayType.STRENGTH_DAY
            hasRun -> DayType.CARDIO_DAY
            stepCount > stepGoal * 1.5 -> DayType.HIGH_ACTIVITY
            stepCount > stepGoal * 0.5 -> DayType.LIGHT_ACTIVITY
            else -> DayType.REST_DAY
        }
    }

    private fun getMealDatabase(): List<MealSuggestion> = listOf(
        // Breakfast options
        MealSuggestion(
            id = "b1", name = "Greek Yogurt Parfait",
            description = "Greek yogurt with berries, granola, and honey",
            mealType = MealType.BREAKFAST,
            calories = 350, proteinGrams = 25.0, carbsGrams = 40.0, fatGrams = 10.0,
            fiberGrams = 4.0, isHighProtein = true, isQuickMeal = true,
            tags = listOf("quick", "high-protein"),
            ingredients = listOf("Greek yogurt", "Mixed berries", "Granola", "Honey")
        ),
        MealSuggestion(
            id = "b2", name = "Protein Oatmeal",
            description = "Oatmeal with protein powder, banana, and almond butter",
            mealType = MealType.BREAKFAST,
            calories = 450, proteinGrams = 30.0, carbsGrams = 55.0, fatGrams = 12.0,
            fiberGrams = 8.0, isHighProtein = true,
            tags = listOf("high-protein", "pre-workout"),
            ingredients = listOf("Oats", "Protein powder", "Banana", "Almond butter")
        ),
        MealSuggestion(
            id = "b3", name = "Egg White Omelette",
            description = "Egg whites with spinach, tomatoes, and feta",
            mealType = MealType.BREAKFAST,
            calories = 280, proteinGrams = 28.0, carbsGrams = 8.0, fatGrams = 14.0,
            fiberGrams = 2.0, isHighProtein = true, isLowCarb = true, isVegetarian = true,
            tags = listOf("low-carb", "high-protein"),
            ingredients = listOf("Egg whites", "Spinach", "Tomatoes", "Feta cheese")
        ),
        MealSuggestion(
            id = "b4", name = "Avocado Toast with Eggs",
            description = "Whole grain toast with avocado and poached eggs",
            mealType = MealType.BREAKFAST,
            calories = 420, proteinGrams = 18.0, carbsGrams = 35.0, fatGrams = 24.0,
            fiberGrams = 8.0, isVegetarian = true,
            tags = listOf("balanced"),
            ingredients = listOf("Whole grain bread", "Avocado", "Eggs")
        ),
        MealSuggestion(
            id = "b5", name = "Protein Smoothie Bowl",
            description = "Thick smoothie with protein, topped with nuts and seeds",
            mealType = MealType.BREAKFAST,
            calories = 380, proteinGrams = 28.0, carbsGrams = 42.0, fatGrams = 12.0,
            fiberGrams = 6.0, isHighProtein = true, isQuickMeal = true,
            tags = listOf("quick", "post-workout"),
            ingredients = listOf("Protein powder", "Frozen berries", "Banana", "Almond milk", "Chia seeds")
        ),

        // Lunch options
        MealSuggestion(
            id = "l1", name = "Grilled Chicken Salad",
            description = "Mixed greens with grilled chicken, quinoa, and vinaigrette",
            mealType = MealType.LUNCH,
            calories = 450, proteinGrams = 40.0, carbsGrams = 30.0, fatGrams = 18.0,
            fiberGrams = 6.0, isHighProtein = true,
            tags = listOf("high-protein", "low-carb"),
            ingredients = listOf("Chicken breast", "Mixed greens", "Quinoa", "Cherry tomatoes", "Olive oil")
        ),
        MealSuggestion(
            id = "l2", name = "Turkey Wrap",
            description = "Whole wheat wrap with turkey, avocado, and vegetables",
            mealType = MealType.LUNCH,
            calories = 480, proteinGrams = 35.0, carbsGrams = 40.0, fatGrams = 20.0,
            fiberGrams = 8.0, isHighProtein = true, isQuickMeal = true,
            tags = listOf("quick", "balanced"),
            ingredients = listOf("Whole wheat wrap", "Turkey breast", "Avocado", "Lettuce", "Tomato")
        ),
        MealSuggestion(
            id = "l3", name = "Salmon Poke Bowl",
            description = "Fresh salmon over rice with edamame and vegetables",
            mealType = MealType.LUNCH,
            calories = 520, proteinGrams = 35.0, carbsGrams = 50.0, fatGrams = 18.0,
            fiberGrams = 5.0, isHighProtein = true,
            tags = listOf("omega-3", "post-workout"),
            ingredients = listOf("Salmon", "Sushi rice", "Edamame", "Cucumber", "Avocado", "Soy sauce")
        ),
        MealSuggestion(
            id = "l4", name = "Chicken Stir-Fry",
            description = "Chicken with mixed vegetables and brown rice",
            mealType = MealType.LUNCH,
            calories = 500, proteinGrams = 38.0, carbsGrams = 48.0, fatGrams = 14.0,
            fiberGrams = 6.0, isHighProtein = true,
            tags = listOf("balanced", "meal-prep"),
            ingredients = listOf("Chicken breast", "Broccoli", "Bell peppers", "Brown rice", "Soy sauce")
        ),
        MealSuggestion(
            id = "l5", name = "Lentil Soup with Bread",
            description = "Hearty lentil soup with whole grain bread",
            mealType = MealType.LUNCH,
            calories = 420, proteinGrams = 22.0, carbsGrams = 60.0, fatGrams = 10.0,
            fiberGrams = 16.0, isVegetarian = true, isVegan = true,
            tags = listOf("high-fiber", "vegan"),
            ingredients = listOf("Lentils", "Carrots", "Celery", "Tomatoes", "Whole grain bread")
        ),

        // Dinner options
        MealSuggestion(
            id = "d1", name = "Grilled Salmon with Vegetables",
            description = "Salmon fillet with roasted asparagus and sweet potato",
            mealType = MealType.DINNER,
            calories = 550, proteinGrams = 42.0, carbsGrams = 35.0, fatGrams = 25.0,
            fiberGrams = 6.0, isHighProtein = true,
            tags = listOf("omega-3", "recovery"),
            ingredients = listOf("Salmon fillet", "Asparagus", "Sweet potato", "Olive oil", "Lemon")
        ),
        MealSuggestion(
            id = "d2", name = "Lean Beef Stir-Fry",
            description = "Lean beef with broccoli and brown rice",
            mealType = MealType.DINNER,
            calories = 580, proteinGrams = 45.0, carbsGrams = 45.0, fatGrams = 20.0,
            fiberGrams = 5.0, isHighProtein = true,
            tags = listOf("high-protein", "iron-rich"),
            ingredients = listOf("Lean beef", "Broccoli", "Brown rice", "Garlic", "Ginger")
        ),
        MealSuggestion(
            id = "d3", name = "Chicken Breast with Quinoa",
            description = "Herb-crusted chicken with quinoa and steamed vegetables",
            mealType = MealType.DINNER,
            calories = 520, proteinGrams = 48.0, carbsGrams = 38.0, fatGrams = 16.0,
            fiberGrams = 6.0, isHighProtein = true,
            tags = listOf("high-protein", "clean-eating"),
            ingredients = listOf("Chicken breast", "Quinoa", "Zucchini", "Bell peppers", "Herbs")
        ),
        MealSuggestion(
            id = "d4", name = "Shrimp Pasta",
            description = "Whole wheat pasta with shrimp and garlic sauce",
            mealType = MealType.DINNER,
            calories = 550, proteinGrams = 35.0, carbsGrams = 55.0, fatGrams = 18.0,
            fiberGrams = 7.0, isHighProtein = true,
            tags = listOf("carb-loading"),
            ingredients = listOf("Whole wheat pasta", "Shrimp", "Garlic", "Olive oil", "Parsley")
        ),
        MealSuggestion(
            id = "d5", name = "Tofu Buddha Bowl",
            description = "Crispy tofu with roasted vegetables and tahini dressing",
            mealType = MealType.DINNER,
            calories = 480, proteinGrams = 25.0, carbsGrams = 45.0, fatGrams = 22.0,
            fiberGrams = 10.0, isVegetarian = true, isVegan = true, isHighProtein = true,
            tags = listOf("vegan", "high-fiber"),
            ingredients = listOf("Tofu", "Sweet potato", "Chickpeas", "Kale", "Tahini")
        ),

        // Snack options
        MealSuggestion(
            id = "s1", name = "Protein Shake",
            description = "Whey protein with almond milk",
            mealType = MealType.MORNING_SNACK,
            calories = 180, proteinGrams = 25.0, carbsGrams = 8.0, fatGrams = 4.0,
            isHighProtein = true, isQuickMeal = true, isLowCarb = true,
            tags = listOf("quick", "post-workout"),
            ingredients = listOf("Whey protein", "Almond milk")
        ),
        MealSuggestion(
            id = "s2", name = "Apple with Almond Butter",
            description = "Sliced apple with natural almond butter",
            mealType = MealType.AFTERNOON_SNACK,
            calories = 220, proteinGrams = 6.0, carbsGrams = 28.0, fatGrams = 12.0,
            fiberGrams = 5.0, isQuickMeal = true, isVegetarian = true,
            tags = listOf("quick", "natural"),
            ingredients = listOf("Apple", "Almond butter")
        ),
        MealSuggestion(
            id = "s3", name = "Cottage Cheese with Berries",
            description = "Low-fat cottage cheese with mixed berries",
            mealType = MealType.EVENING_SNACK,
            calories = 180, proteinGrams = 20.0, carbsGrams = 15.0, fatGrams = 4.0,
            fiberGrams = 2.0, isHighProtein = true, isQuickMeal = true, isVegetarian = true,
            tags = listOf("casein", "before-bed"),
            ingredients = listOf("Cottage cheese", "Mixed berries")
        ),
        MealSuggestion(
            id = "s4", name = "Mixed Nuts",
            description = "Handful of mixed nuts (almonds, walnuts, cashews)",
            mealType = MealType.AFTERNOON_SNACK,
            calories = 200, proteinGrams = 6.0, carbsGrams = 8.0, fatGrams = 18.0,
            fiberGrams = 2.0, isQuickMeal = true, isVegetarian = true, isVegan = true,
            tags = listOf("healthy-fats"),
            ingredients = listOf("Almonds", "Walnuts", "Cashews")
        ),
        MealSuggestion(
            id = "s5", name = "Greek Yogurt",
            description = "Plain Greek yogurt with a drizzle of honey",
            mealType = MealType.MORNING_SNACK,
            calories = 150, proteinGrams = 18.0, carbsGrams = 12.0, fatGrams = 3.0,
            isHighProtein = true, isQuickMeal = true, isVegetarian = true,
            tags = listOf("quick", "probiotic"),
            ingredients = listOf("Greek yogurt", "Honey")
        ),
        MealSuggestion(
            id = "s6", name = "Protein Bar",
            description = "High-protein, low-sugar protein bar",
            mealType = MealType.AFTERNOON_SNACK,
            calories = 220, proteinGrams = 20.0, carbsGrams = 22.0, fatGrams = 8.0,
            fiberGrams = 3.0, isHighProtein = true, isQuickMeal = true,
            tags = listOf("convenient", "on-the-go"),
            ingredients = listOf("Protein bar")
        ),
        MealSuggestion(
            id = "s7", name = "Hard Boiled Eggs",
            description = "Two hard boiled eggs with salt and pepper",
            mealType = MealType.MORNING_SNACK,
            calories = 140, proteinGrams = 12.0, carbsGrams = 1.0, fatGrams = 10.0,
            isHighProtein = true, isQuickMeal = true, isLowCarb = true, isVegetarian = true,
            tags = listOf("keto-friendly", "meal-prep"),
            ingredients = listOf("Eggs")
        )
    )
}
