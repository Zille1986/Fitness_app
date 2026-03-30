import Foundation
import SwiftData

@Model
final class DailyNutrition {
    @Attribute(.unique) var date: Date
    var targetCalories: Int
    var consumedCalories: Int
    var burnedCalories: Int
    var targetProteinGrams: Int
    var consumedProteinGrams: Int
    var targetCarbsGrams: Int
    var consumedCarbsGrams: Int
    var targetFatGrams: Int
    var consumedFatGrams: Int
    var targetFiberGrams: Int
    var consumedFiberGrams: Int
    var waterMl: Int
    var targetWaterMl: Int
    var stepCount: Int
    var stepCaloriesBurned: Int
    var workoutCaloriesBurned: Int
    var meals: [MealEntry]
    var updatedAt: Date

    var netCalories: Int { consumedCalories - burnedCalories }
    var remainingCalories: Int { targetCalories - consumedCalories + burnedCalories }
    var calorieProgress: Float { min(Float(consumedCalories) / Float(targetCalories), 1.5) }
    var proteinProgress: Float { min(Float(consumedProteinGrams) / Float(targetProteinGrams), 1.5) }
    var carbsProgress: Float { min(Float(consumedCarbsGrams) / Float(targetCarbsGrams), 1.5) }
    var fatProgress: Float { min(Float(consumedFatGrams) / Float(targetFatGrams), 1.5) }
    var waterProgress: Float { min(Float(waterMl) / Float(targetWaterMl), 1.5) }

    init(
        date: Date,
        targetCalories: Int = 2000,
        consumedCalories: Int = 0,
        burnedCalories: Int = 0,
        targetProteinGrams: Int = 150,
        consumedProteinGrams: Int = 0,
        targetCarbsGrams: Int = 250,
        consumedCarbsGrams: Int = 0,
        targetFatGrams: Int = 65,
        consumedFatGrams: Int = 0,
        targetFiberGrams: Int = 30,
        consumedFiberGrams: Int = 0,
        waterMl: Int = 0,
        targetWaterMl: Int = 2500,
        stepCount: Int = 0,
        stepCaloriesBurned: Int = 0,
        workoutCaloriesBurned: Int = 0,
        meals: [MealEntry] = [],
        updatedAt: Date = Date()
    ) {
        self.date = date
        self.targetCalories = targetCalories
        self.consumedCalories = consumedCalories
        self.burnedCalories = burnedCalories
        self.targetProteinGrams = targetProteinGrams
        self.consumedProteinGrams = consumedProteinGrams
        self.targetCarbsGrams = targetCarbsGrams
        self.consumedCarbsGrams = consumedCarbsGrams
        self.targetFatGrams = targetFatGrams
        self.consumedFatGrams = consumedFatGrams
        self.targetFiberGrams = targetFiberGrams
        self.consumedFiberGrams = consumedFiberGrams
        self.waterMl = waterMl
        self.targetWaterMl = targetWaterMl
        self.stepCount = stepCount
        self.stepCaloriesBurned = stepCaloriesBurned
        self.workoutCaloriesBurned = workoutCaloriesBurned
        self.meals = meals
        self.updatedAt = updatedAt
    }
}

struct MealEntry: Codable, Hashable, Identifiable {
    var id: String
    var name: String
    var mealType: MealType
    var calories: Int
    var proteinGrams: Double
    var carbsGrams: Double
    var fatGrams: Double
    var fiberGrams: Double
    var timestamp: Date
    var notes: String?

    init(
        id: String = UUID().uuidString,
        name: String,
        mealType: MealType,
        calories: Int,
        proteinGrams: Double = 0.0,
        carbsGrams: Double = 0.0,
        fatGrams: Double = 0.0,
        fiberGrams: Double = 0.0,
        timestamp: Date = Date(),
        notes: String? = nil
    ) {
        self.id = id
        self.name = name
        self.mealType = mealType
        self.calories = calories
        self.proteinGrams = proteinGrams
        self.carbsGrams = carbsGrams
        self.fatGrams = fatGrams
        self.fiberGrams = fiberGrams
        self.timestamp = timestamp
        self.notes = notes
    }
}

enum MealType: String, Codable, CaseIterable {
    case breakfast = "BREAKFAST"
    case morningSnack = "MORNING_SNACK"
    case lunch = "LUNCH"
    case afternoonSnack = "AFTERNOON_SNACK"
    case dinner = "DINNER"
    case eveningSnack = "EVENING_SNACK"
}

@Model
final class NutritionGoals {
    @Attribute(.unique) var id: Int
    var goal: NutritionGoalType
    var activityLevel: ActivityLevel
    var proteinPerKgBodyweight: Double
    var carbPercentage: Double
    var fatPercentage: Double
    var dailyStepGoal: Int
    var waterPerKgBodyweight: Double
    var mealFrequency: Int
    var updatedAt: Date

    init(
        id: Int = 1,
        goal: NutritionGoalType = .maintain,
        activityLevel: ActivityLevel = .moderatelyActive,
        proteinPerKgBodyweight: Double = 1.6,
        carbPercentage: Double = 0.45,
        fatPercentage: Double = 0.25,
        dailyStepGoal: Int = 10000,
        waterPerKgBodyweight: Double = 35.0,
        mealFrequency: Int = 4,
        updatedAt: Date = Date()
    ) {
        self.id = id
        self.goal = goal
        self.activityLevel = activityLevel
        self.proteinPerKgBodyweight = proteinPerKgBodyweight
        self.carbPercentage = carbPercentage
        self.fatPercentage = fatPercentage
        self.dailyStepGoal = dailyStepGoal
        self.waterPerKgBodyweight = waterPerKgBodyweight
        self.mealFrequency = mealFrequency
        self.updatedAt = updatedAt
    }
}

enum NutritionGoalType: String, Codable, CaseIterable {
    case loseWeight = "LOSE_WEIGHT"
    case loseWeightSlow = "LOSE_WEIGHT_SLOW"
    case maintain = "MAINTAIN"
    case gainMuscle = "GAIN_MUSCLE"
    case bulk = "BULK"

    var displayName: String {
        switch self {
        case .loseWeight: return "Lose Weight"
        case .loseWeightSlow: return "Lose Weight (Slow)"
        case .maintain: return "Maintain Weight"
        case .gainMuscle: return "Build Muscle"
        case .bulk: return "Bulk Up"
        }
    }

    var calorieAdjustment: Int {
        switch self {
        case .loseWeight: return -500
        case .loseWeightSlow: return -250
        case .maintain: return 0
        case .gainMuscle: return 250
        case .bulk: return 500
        }
    }
}

enum ActivityLevel: String, Codable, CaseIterable {
    case sedentary = "SEDENTARY"
    case lightlyActive = "LIGHTLY_ACTIVE"
    case moderatelyActive = "MODERATELY_ACTIVE"
    case veryActive = "VERY_ACTIVE"
    case extremelyActive = "EXTREMELY_ACTIVE"

    var multiplier: Double {
        switch self {
        case .sedentary: return 1.2
        case .lightlyActive: return 1.375
        case .moderatelyActive: return 1.55
        case .veryActive: return 1.725
        case .extremelyActive: return 1.9
        }
    }

    var displayName: String {
        switch self {
        case .sedentary: return "Sedentary"
        case .lightlyActive: return "Lightly Active"
        case .moderatelyActive: return "Moderately Active"
        case .veryActive: return "Very Active"
        case .extremelyActive: return "Extremely Active"
        }
    }
}

struct MealSuggestion: Codable, Hashable, Identifiable {
    var id: String
    var name: String
    var suggestionDescription: String
    var mealType: MealType
    var calories: Int
    var proteinGrams: Double
    var carbsGrams: Double
    var fatGrams: Double
    var fiberGrams: Double
    var tags: [String]
    var ingredients: [String]
    var prepTimeMinutes: Int
    var isQuickMeal: Bool
    var isHighProtein: Bool
    var isLowCarb: Bool
    var isVegetarian: Bool
    var isVegan: Bool

    init(
        id: String,
        name: String,
        suggestionDescription: String,
        mealType: MealType,
        calories: Int,
        proteinGrams: Double,
        carbsGrams: Double,
        fatGrams: Double,
        fiberGrams: Double = 0.0,
        tags: [String] = [],
        ingredients: [String] = [],
        prepTimeMinutes: Int = 15,
        isQuickMeal: Bool = false,
        isHighProtein: Bool = false,
        isLowCarb: Bool = false,
        isVegetarian: Bool = false,
        isVegan: Bool = false
    ) {
        self.id = id
        self.name = name
        self.suggestionDescription = suggestionDescription
        self.mealType = mealType
        self.calories = calories
        self.proteinGrams = proteinGrams
        self.carbsGrams = carbsGrams
        self.fatGrams = fatGrams
        self.fiberGrams = fiberGrams
        self.tags = tags
        self.ingredients = ingredients
        self.prepTimeMinutes = prepTimeMinutes
        self.isQuickMeal = isQuickMeal
        self.isHighProtein = isHighProtein
        self.isLowCarb = isLowCarb
        self.isVegetarian = isVegetarian
        self.isVegan = isVegan
    }
}

struct DailySuggestions: Codable {
    var date: Date
    var targetCalories: Int
    var targetProtein: Int
    var remainingCalories: Int
    var remainingProtein: Int
    var dayType: DayType
    var suggestions: [MealSuggestion]
    var tips: [String]
}

enum DayType: String, Codable, CaseIterable {
    case restDay = "REST_DAY"
    case lightActivity = "LIGHT_ACTIVITY"
    case cardioDay = "CARDIO_DAY"
    case strengthDay = "STRENGTH_DAY"
    case highActivity = "HIGH_ACTIVITY"
}
