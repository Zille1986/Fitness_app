import Foundation
import Observation
import SwiftData
import UIKit

// MARK: - MealType Extensions

extension MealType {
    var displayName: String {
        switch self {
        case .breakfast: return "Breakfast"
        case .morningSnack: return "Morning Snack"
        case .lunch: return "Lunch"
        case .afternoonSnack: return "Afternoon Snack"
        case .dinner: return "Dinner"
        case .eveningSnack: return "Evening Snack"
        }
    }

    var icon: String {
        switch self {
        case .breakfast: return "sunrise"
        case .morningSnack: return "cup.and.saucer"
        case .lunch: return "sun.max"
        case .afternoonSnack: return "leaf"
        case .dinner: return "moon.stars"
        case .eveningSnack: return "moon.zzz"
        }
    }
}

// MARK: - DayType Extensions

extension DayType {
    var displayName: String {
        switch self {
        case .strengthDay: return "Strength Training Day"
        case .cardioDay: return "Cardio Day"
        case .highActivity: return "High Activity Day"
        case .lightActivity: return "Light Activity Day"
        case .restDay: return "Rest Day"
        }
    }

    var icon: String {
        switch self {
        case .strengthDay: return "dumbbell"
        case .cardioDay: return "figure.run"
        case .highActivity: return "flame"
        case .lightActivity: return "figure.walk"
        case .restDay: return "figure.mind.and.body"
        }
    }

    var color: String {
        switch self {
        case .strengthDay: return "7C4DFF"
        case .cardioDay: return "00BCD4"
        case .highActivity: return "FF5722"
        case .lightActivity: return "4CAF50"
        case .restDay: return "9E9E9E"
        }
    }
}

// MARK: - TDEE Calculator

enum TDEECalculator {
    static func calculateDailyCalories(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        isMale: Bool,
        activityLevel: ActivityLevel,
        goal: NutritionGoalType
    ) -> Int {
        // Mifflin-St Jeor equation
        let bmr: Double
        if isMale {
            bmr = 10 * weightKg + 6.25 * heightCm - 5 * Double(ageYears) + 5
        } else {
            bmr = 10 * weightKg + 6.25 * heightCm - 5 * Double(ageYears) - 161
        }
        let tdee = bmr * activityLevel.multiplier
        return Int(tdee) + goal.calorieAdjustment
    }

    static func calculateMacros(
        calories: Int,
        weightKg: Double,
        proteinPerKg: Double,
        carbPercentage: Double,
        fatPercentage: Double
    ) -> (protein: Int, carbs: Int, fat: Int) {
        let proteinGrams = Int(weightKg * proteinPerKg)
        let proteinCalories = proteinGrams * 4
        let remainingCalories = calories - proteinCalories
        let carbGrams = Int(Double(remainingCalories) * carbPercentage / 4.0)
        let fatGrams = Int(Double(remainingCalories) * fatPercentage / 9.0)
        return (proteinGrams, carbGrams, fatGrams)
    }
}

// MARK: - Meal Suggestion Engine (iOS)

enum MealSuggestionEngine {
    static func generateSuggestions(
        nutrition: DailyNutrition,
        goals: NutritionGoals?,
        dayType: DayType,
        mealType: MealType
    ) -> [MealSuggestion] {
        let remaining = nutrition.remainingCalories
        let remainingProtein = nutrition.targetProteinGrams - nutrition.consumedProteinGrams
        let all = mealDatabase.filter { $0.mealType == mealType && $0.calories <= remaining + 100 }

        let result: [MealSuggestion]
        switch dayType {
        case .strengthDay where remainingProtein > 30:
            result = Array(all.filter(\.isHighProtein).prefix(5))
        case .cardioDay:
            result = Array(all.sorted { $0.carbsGrams > $1.carbsGrams }.prefix(5))
        case .restDay:
            result = Array(all.filter { $0.calories < remaining / 2 }.prefix(5))
        default:
            if remainingProtein > 40 {
                result = Array(all.filter(\.isHighProtein).prefix(5))
            } else {
                result = Array(all.prefix(5))
            }
        }
        return result.isEmpty ? Array(all.prefix(5)) : result
    }

    static func generateTips(
        nutrition: DailyNutrition,
        dayType: DayType,
        stepGoal: Int
    ) -> [String] {
        var tips: [String] = []
        let hour = Calendar.current.component(.hour, from: Date())

        if nutrition.waterProgress < 0.5 {
            tips.append("You're behind on water intake. Try to drink a glass every hour.")
        }
        if nutrition.proteinProgress < 0.3 && hour > 12 {
            tips.append("Protein intake is low. Consider a high-protein snack or meal.")
        }
        switch dayType {
        case .strengthDay:
            tips.append("Strength day! Aim for 1.6-2.2g protein per kg bodyweight.")
            if nutrition.carbsProgress < 0.5 {
                tips.append("Carbs fuel your lifts. Don't skip them on training days.")
            }
        case .cardioDay:
            tips.append("Cardio day! Prioritize carbs for energy and recovery.")
            tips.append("Remember to replenish electrolytes after your run.")
        case .restDay:
            tips.append("Rest day - focus on recovery nutrition and sleep.")
        default:
            break
        }
        if nutrition.stepCount < stepGoal / 2 && hour > 14 {
            tips.append("You're behind on steps. A short walk can boost metabolism.")
        } else if nutrition.stepCount > Int(Double(stepGoal) * 1.5) {
            tips.append("Great activity today! You've earned some extra calories.")
        }
        if nutrition.remainingCalories < 200 && hour < 18 {
            tips.append("Running low on calories. Save room for dinner!")
        }
        return Array(tips.prefix(3))
    }

    static func determineDayType(
        hasRun: Bool,
        hasGymWorkout: Bool,
        stepCount: Int,
        stepGoal: Int
    ) -> DayType {
        if hasGymWorkout { return .strengthDay }
        if hasRun { return .cardioDay }
        if stepCount > Int(Double(stepGoal) * 1.5) { return .highActivity }
        if stepCount > stepGoal / 2 { return .lightActivity }
        return .restDay
    }

    static var mealDatabase: [MealSuggestion] {
        [
            MealSuggestion(id: "b1", name: "Greek Yogurt Parfait", suggestionDescription: "Greek yogurt with berries, granola, and honey", mealType: .breakfast, calories: 350, proteinGrams: 25, carbsGrams: 40, fatGrams: 10, fiberGrams: 4, tags: ["quick", "high-protein"], ingredients: ["Greek yogurt", "Mixed berries", "Granola", "Honey"], isQuickMeal: true, isHighProtein: true),
            MealSuggestion(id: "b2", name: "Protein Oatmeal", suggestionDescription: "Oatmeal with protein powder, banana, and almond butter", mealType: .breakfast, calories: 450, proteinGrams: 30, carbsGrams: 55, fatGrams: 12, fiberGrams: 8, tags: ["high-protein", "pre-workout"], ingredients: ["Oats", "Protein powder", "Banana", "Almond butter"], isHighProtein: true),
            MealSuggestion(id: "b3", name: "Egg White Omelette", suggestionDescription: "Egg whites with spinach, tomatoes, and feta", mealType: .breakfast, calories: 280, proteinGrams: 28, carbsGrams: 8, fatGrams: 14, fiberGrams: 2, tags: ["low-carb", "high-protein"], ingredients: ["Egg whites", "Spinach", "Tomatoes", "Feta cheese"], isHighProtein: true, isLowCarb: true, isVegetarian: true),
            MealSuggestion(id: "b4", name: "Avocado Toast with Eggs", suggestionDescription: "Whole grain toast with avocado and poached eggs", mealType: .breakfast, calories: 420, proteinGrams: 18, carbsGrams: 35, fatGrams: 24, fiberGrams: 8, tags: ["balanced"], ingredients: ["Whole grain bread", "Avocado", "Eggs"], isVegetarian: true),
            MealSuggestion(id: "b5", name: "Protein Smoothie Bowl", suggestionDescription: "Thick smoothie with protein, topped with nuts and seeds", mealType: .breakfast, calories: 380, proteinGrams: 28, carbsGrams: 42, fatGrams: 12, fiberGrams: 6, tags: ["quick", "post-workout"], ingredients: ["Protein powder", "Frozen berries", "Banana", "Almond milk", "Chia seeds"], isQuickMeal: true, isHighProtein: true),
            MealSuggestion(id: "l1", name: "Grilled Chicken Salad", suggestionDescription: "Mixed greens with grilled chicken, quinoa, and vinaigrette", mealType: .lunch, calories: 450, proteinGrams: 40, carbsGrams: 30, fatGrams: 18, fiberGrams: 6, tags: ["high-protein", "low-carb"], ingredients: ["Chicken breast", "Mixed greens", "Quinoa", "Cherry tomatoes", "Olive oil"], isHighProtein: true),
            MealSuggestion(id: "l2", name: "Turkey Wrap", suggestionDescription: "Whole wheat wrap with turkey, avocado, and vegetables", mealType: .lunch, calories: 480, proteinGrams: 35, carbsGrams: 40, fatGrams: 20, fiberGrams: 8, tags: ["quick", "balanced"], ingredients: ["Whole wheat wrap", "Turkey breast", "Avocado", "Lettuce", "Tomato"], isQuickMeal: true, isHighProtein: true),
            MealSuggestion(id: "l3", name: "Salmon Poke Bowl", suggestionDescription: "Fresh salmon over rice with edamame and vegetables", mealType: .lunch, calories: 520, proteinGrams: 35, carbsGrams: 50, fatGrams: 18, fiberGrams: 5, tags: ["omega-3", "post-workout"], ingredients: ["Salmon", "Sushi rice", "Edamame", "Cucumber", "Avocado", "Soy sauce"], isHighProtein: true),
            MealSuggestion(id: "l4", name: "Chicken Stir-Fry", suggestionDescription: "Chicken with mixed vegetables and brown rice", mealType: .lunch, calories: 500, proteinGrams: 38, carbsGrams: 48, fatGrams: 14, fiberGrams: 6, tags: ["balanced", "meal-prep"], ingredients: ["Chicken breast", "Broccoli", "Bell peppers", "Brown rice", "Soy sauce"], isHighProtein: true),
            MealSuggestion(id: "l5", name: "Lentil Soup with Bread", suggestionDescription: "Hearty lentil soup with whole grain bread", mealType: .lunch, calories: 420, proteinGrams: 22, carbsGrams: 60, fatGrams: 10, fiberGrams: 16, tags: ["high-fiber", "vegan"], ingredients: ["Lentils", "Carrots", "Celery", "Tomatoes", "Whole grain bread"], isVegetarian: true, isVegan: true),
            MealSuggestion(id: "d1", name: "Grilled Salmon with Vegetables", suggestionDescription: "Salmon fillet with roasted asparagus and sweet potato", mealType: .dinner, calories: 550, proteinGrams: 42, carbsGrams: 35, fatGrams: 25, fiberGrams: 6, tags: ["omega-3", "recovery"], ingredients: ["Salmon fillet", "Asparagus", "Sweet potato", "Olive oil", "Lemon"], isHighProtein: true),
            MealSuggestion(id: "d2", name: "Lean Beef Stir-Fry", suggestionDescription: "Lean beef with broccoli and brown rice", mealType: .dinner, calories: 580, proteinGrams: 45, carbsGrams: 45, fatGrams: 20, fiberGrams: 5, tags: ["high-protein", "iron-rich"], ingredients: ["Lean beef", "Broccoli", "Brown rice", "Garlic", "Ginger"], isHighProtein: true),
            MealSuggestion(id: "d3", name: "Chicken Breast with Quinoa", suggestionDescription: "Herb-crusted chicken with quinoa and steamed vegetables", mealType: .dinner, calories: 520, proteinGrams: 48, carbsGrams: 38, fatGrams: 16, fiberGrams: 6, tags: ["high-protein", "clean-eating"], ingredients: ["Chicken breast", "Quinoa", "Zucchini", "Bell peppers", "Herbs"], isHighProtein: true),
            MealSuggestion(id: "d4", name: "Shrimp Pasta", suggestionDescription: "Whole wheat pasta with shrimp and garlic sauce", mealType: .dinner, calories: 550, proteinGrams: 35, carbsGrams: 55, fatGrams: 18, fiberGrams: 7, tags: ["carb-loading"], ingredients: ["Whole wheat pasta", "Shrimp", "Garlic", "Olive oil", "Parsley"], isHighProtein: true),
            MealSuggestion(id: "d5", name: "Tofu Buddha Bowl", suggestionDescription: "Crispy tofu with roasted vegetables and tahini dressing", mealType: .dinner, calories: 480, proteinGrams: 25, carbsGrams: 45, fatGrams: 22, fiberGrams: 10, tags: ["vegan", "high-fiber"], ingredients: ["Tofu", "Sweet potato", "Chickpeas", "Kale", "Tahini"], isHighProtein: true, isVegetarian: true, isVegan: true),
            MealSuggestion(id: "s1", name: "Protein Shake", suggestionDescription: "Whey protein with almond milk", mealType: .morningSnack, calories: 180, proteinGrams: 25, carbsGrams: 8, fatGrams: 4, tags: ["quick", "post-workout"], ingredients: ["Whey protein", "Almond milk"], isQuickMeal: true, isHighProtein: true, isLowCarb: true),
            MealSuggestion(id: "s2", name: "Apple with Almond Butter", suggestionDescription: "Sliced apple with natural almond butter", mealType: .afternoonSnack, calories: 220, proteinGrams: 6, carbsGrams: 28, fatGrams: 12, fiberGrams: 5, tags: ["quick", "natural"], ingredients: ["Apple", "Almond butter"], isQuickMeal: true, isVegetarian: true),
            MealSuggestion(id: "s3", name: "Cottage Cheese with Berries", suggestionDescription: "Low-fat cottage cheese with mixed berries", mealType: .eveningSnack, calories: 180, proteinGrams: 20, carbsGrams: 15, fatGrams: 4, fiberGrams: 2, tags: ["casein", "before-bed"], ingredients: ["Cottage cheese", "Mixed berries"], isQuickMeal: true, isHighProtein: true, isVegetarian: true),
            MealSuggestion(id: "s4", name: "Mixed Nuts", suggestionDescription: "Handful of mixed nuts (almonds, walnuts, cashews)", mealType: .afternoonSnack, calories: 200, proteinGrams: 6, carbsGrams: 8, fatGrams: 18, fiberGrams: 2, tags: ["healthy-fats"], ingredients: ["Almonds", "Walnuts", "Cashews"], isQuickMeal: true, isVegetarian: true, isVegan: true),
            MealSuggestion(id: "s5", name: "Greek Yogurt", suggestionDescription: "Plain Greek yogurt with a drizzle of honey", mealType: .morningSnack, calories: 150, proteinGrams: 18, carbsGrams: 12, fatGrams: 3, tags: ["quick", "probiotic"], ingredients: ["Greek yogurt", "Honey"], isQuickMeal: true, isHighProtein: true, isVegetarian: true),
            MealSuggestion(id: "s6", name: "Protein Bar", suggestionDescription: "High-protein, low-sugar protein bar", mealType: .afternoonSnack, calories: 220, proteinGrams: 20, carbsGrams: 22, fatGrams: 8, fiberGrams: 3, tags: ["convenient", "on-the-go"], ingredients: ["Protein bar"], isQuickMeal: true, isHighProtein: true),
            MealSuggestion(id: "s7", name: "Hard Boiled Eggs", suggestionDescription: "Two hard boiled eggs with salt and pepper", mealType: .morningSnack, calories: 140, proteinGrams: 12, carbsGrams: 1, fatGrams: 10, tags: ["keto-friendly", "meal-prep"], ingredients: ["Eggs"], isQuickMeal: true, isHighProtein: true, isLowCarb: true, isVegetarian: true),
        ]
    }
}

// MARK: - NutritionViewModel

@Observable
final class NutritionViewModel {
    // Dependencies
    private var nutritionRepository: NutritionRepository?
    private var geminiService: GeminiService = GeminiService()

    // State
    var todayNutrition: DailyNutrition?
    var goals: NutritionGoals?
    var dayType: DayType = .restDay
    var currentMealType: MealType = .breakfast
    var mealSuggestions: [MealSuggestion] = []
    var tips: [String] = []
    var isLoading = true
    var errorMessage: String?
    var showingQuickLog = false
    var showingWaterLog = false

    // TDEE Settings
    var tdeeAge: String = "30"
    var tdeeWeight: String = "70"
    var tdeeHeight: String = "170"
    var tdeeMale: Bool = true
    var tdeeActivityLevel: ActivityLevel = .moderatelyActive
    var tdeeGoal: NutritionGoalType = .maintain
    var calculatedTDEE: Int = 0

    // Meal log form
    var mealName: String = ""
    var mealCalories: String = ""
    var mealProtein: String = ""
    var mealCarbs: String = ""
    var mealFat: String = ""
    var mealServingSize: String = ""

    // Photo capture
    var isAnalyzingPhoto = false
    var photoAnalysisResult: FoodAnalysisDisplay?

    // Dietary preferences for suggestions
    var preferVegetarian = false
    var preferVegan = false
    var preferHighProtein = false
    var preferLowCarb = false
    var selectedSuggestionMealType: MealType = .breakfast

    init() {}

    func configure(modelContext: ModelContext) {
        guard nutritionRepository == nil else { return }
        self.nutritionRepository = NutritionRepository(context: modelContext)
        loadData()
    }

    var currentMealTypeAutoDetected: MealType {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 0..<10: return .breakfast
        case 10..<12: return .morningSnack
        case 12..<14: return .lunch
        case 14..<17: return .afternoonSnack
        case 17..<20: return .dinner
        default: return .eveningSnack
        }
    }

    func loadData() {
        isLoading = true
        errorMessage = nil

        Task { @MainActor in
            do {
                guard let nutritionRepository else { return }
                let nutrition = nutritionRepository.getOrCreateTodayNutrition(
                    userWeight: nil, userHeight: nil, userAge: nil, userGender: nil, goals: nil
                )
                let loadedGoals: NutritionGoals? = nil
                self.todayNutrition = nutrition
                self.goals = loadedGoals
                self.currentMealType = currentMealTypeAutoDetected
                self.dayType = MealSuggestionEngine.determineDayType(
                    hasRun: false,
                    hasGymWorkout: false,
                    stepCount: nutrition.stepCount,
                    stepGoal: loadedGoals?.dailyStepGoal ?? 10000
                )
                refreshSuggestions()
                self.isLoading = false
            } catch {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func refreshSuggestions() {
        guard let nutrition = todayNutrition else { return }
        mealSuggestions = MealSuggestionEngine.generateSuggestions(
            nutrition: nutrition,
            goals: goals,
            dayType: dayType,
            mealType: currentMealType
        )
        tips = MealSuggestionEngine.generateTips(
            nutrition: nutrition,
            dayType: dayType,
            stepGoal: goals?.dailyStepGoal ?? 10000
        )
    }

    func logQuickMeal(name: String, calories: Int, protein: Int, carbs: Int, fat: Int) {
        let meal = MealEntry(
            name: name.isEmpty ? "Quick Meal" : name,
            mealType: currentMealTypeAutoDetected,
            calories: calories,
            proteinGrams: Double(protein),
            carbsGrams: Double(carbs),
            fatGrams: Double(fat)
        )
        logMealEntry(meal)
    }

    func logMealFromSuggestion(_ suggestion: MealSuggestion) {
        let meal = MealEntry(
            name: suggestion.name,
            mealType: suggestion.mealType,
            calories: suggestion.calories,
            proteinGrams: suggestion.proteinGrams,
            carbsGrams: suggestion.carbsGrams,
            fatGrams: suggestion.fatGrams,
            fiberGrams: suggestion.fiberGrams
        )
        logMealEntry(meal)
    }

    func logMealFromForm() {
        let cal = Int(mealCalories) ?? 0
        guard cal > 0 else { return }
        let meal = MealEntry(
            name: mealName.isEmpty ? "Meal" : mealName,
            mealType: currentMealTypeAutoDetected,
            calories: cal,
            proteinGrams: Double(mealProtein) ?? 0,
            carbsGrams: Double(mealCarbs) ?? 0,
            fatGrams: Double(mealFat) ?? 0
        )
        logMealEntry(meal)
        clearMealForm()
    }

    func removeMeal(_ mealId: String) {
        nutritionRepository?.removeMeal(mealId: mealId)
        loadData()
    }

    func addWater(_ ml: Int) {
        nutritionRepository?.addWater(ml: ml)
        loadData()
    }

    func analyzePhoto(imageData: Data) {
        isAnalyzingPhoto = true
        Task { @MainActor in
            guard let uiImage = UIImage(data: imageData) else {
                self.errorMessage = "Failed to create image from data"
                self.isAnalyzingPhoto = false
                return
            }
            let result = await geminiService.analyzeFoodImage(image: uiImage)
            switch result {
            case .success(let analysis):
                self.photoAnalysisResult = FoodAnalysisDisplay(
                    foodName: analysis.foodName,
                    calories: analysis.calories,
                    proteinGrams: analysis.proteinGrams,
                    carbsGrams: analysis.carbsGrams,
                    fatGrams: analysis.fatGrams,
                    fiberGrams: analysis.fiberGrams,
                    confidence: .high,
                    portionSize: analysis.portionSize,
                    description: analysis.description
                )
                self.mealName = analysis.foodName
                self.mealCalories = "\(analysis.calories)"
                self.mealProtein = "\(Int(analysis.proteinGrams))"
                self.mealCarbs = "\(Int(analysis.carbsGrams))"
                self.mealFat = "\(Int(analysis.fatGrams))"
            case .error(let message):
                self.errorMessage = message
            }
            self.isAnalyzingPhoto = false
        }
    }

    func calculateTDEE() {
        let weight = Double(tdeeWeight) ?? 70
        let height = Double(tdeeHeight) ?? 170
        let age = Int(tdeeAge) ?? 30
        calculatedTDEE = TDEECalculator.calculateDailyCalories(
            weightKg: weight,
            heightCm: height,
            ageYears: age,
            isMale: tdeeMale,
            activityLevel: tdeeActivityLevel,
            goal: tdeeGoal
        )
    }

    func saveGoals(calories: Int, protein: Int, carbs: Int, fat: Int, water: Int) {
        let totalCal = max(Double(calories), 1.0)
        let goals = NutritionGoals(
            goal: tdeeGoal,
            activityLevel: tdeeActivityLevel,
            proteinPerKgBodyweight: 1.6,
            carbPercentage: Double(carbs) * 4.0 / totalCal,
            fatPercentage: Double(fat) * 9.0 / totalCal
        )
        nutritionRepository?.saveGoals(goals)
        loadData()
    }

    func filteredSuggestions(for mealType: MealType) -> [MealSuggestion] {
        var suggestions = MealSuggestionEngine.mealDatabase.filter { $0.mealType == mealType }
        if preferVegetarian { suggestions = suggestions.filter(\.isVegetarian) }
        if preferVegan { suggestions = suggestions.filter(\.isVegan) }
        if preferHighProtein { suggestions = suggestions.filter(\.isHighProtein) }
        if preferLowCarb { suggestions = suggestions.filter(\.isLowCarb) }

        if let nutrition = todayNutrition {
            suggestions = suggestions.filter { $0.calories <= nutrition.remainingCalories + 100 }
        }
        return suggestions
    }

    // MARK: - Private

    private func logMealEntry(_ meal: MealEntry) {
        nutritionRepository?.addMeal(meal)
        loadData()
    }

    private func clearMealForm() {
        mealName = ""
        mealCalories = ""
        mealProtein = ""
        mealCarbs = ""
        mealFat = ""
        mealServingSize = ""
        photoAnalysisResult = nil
    }
}

// MARK: - ViewModel-specific Display Types

struct FoodAnalysisDisplay {
    var foodName: String
    var calories: Int
    var proteinGrams: Double
    var carbsGrams: Double
    var fatGrams: Double
    var fiberGrams: Double = 0
    var confidence: AnalysisConfidence
    var portionSize: String = "1 serving"
    var description: String = ""
    var itemsDetected: [String] = []
    var suggestions: String = ""
}

enum AnalysisConfidence: String {
    case high, medium, low
}
