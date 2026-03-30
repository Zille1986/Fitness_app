import Foundation
import SwiftData

final class NutritionRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Daily Nutrition

    func fetchTodayNutrition() -> DailyNutrition? {
        let today = Date().startOfDay
        let descriptor = FetchDescriptor<DailyNutrition>(predicate: #Predicate { $0.date == today })
        return try? context.fetch(descriptor).first
    }

    func fetchNutritionForDate(_ date: Date) -> DailyNutrition? {
        let dayStart = date.startOfDay
        let descriptor = FetchDescriptor<DailyNutrition>(predicate: #Predicate { $0.date == dayStart })
        return try? context.fetch(descriptor).first
    }

    func fetchRecentNutrition(days: Int = 7) -> [DailyNutrition] {
        let cutoff = Calendar.current.date(byAdding: .day, value: -days, to: Date())?.startOfDay ?? Date()
        let descriptor = FetchDescriptor<DailyNutrition>(
            predicate: #Predicate { $0.date >= cutoff },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchNutritionInRange(start: Date, end: Date) -> [DailyNutrition] {
        let s = start.startOfDay
        let e = end.startOfDay
        let descriptor = FetchDescriptor<DailyNutrition>(
            predicate: #Predicate { $0.date >= s && $0.date <= e },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func getOrCreateTodayNutrition(
        userWeight: Double?,
        userHeight: Double?,
        userAge: Int?,
        userGender: Gender?,
        goals: NutritionGoals?
    ) -> DailyNutrition {
        if let existing = fetchTodayNutrition() { return existing }

        let weight = userWeight ?? 70.0
        let height = userHeight ?? 170.0
        let age = userAge ?? 30
        let activityLevel = goals?.activityLevel ?? .moderatelyActive
        let goalType = goals?.goal ?? .maintain
        let proteinPerKg = goals?.proteinPerKgBodyweight ?? 1.6
        let carbPct = goals?.carbPercentage ?? 0.45
        let fatPct = goals?.fatPercentage ?? 0.25

        let calories = NutritionCalculator.calculateDailyCalories(
            weightKg: weight, heightCm: height, ageYears: age,
            gender: userGender, activityLevel: activityLevel, goal: goalType
        )
        let macros = NutritionCalculator.calculateMacros(
            totalCalories: calories, weightKg: weight,
            proteinPerKg: proteinPerKg, carbPercentage: carbPct, fatPercentage: fatPct
        )
        let waterTarget = NutritionCalculator.calculateWaterTarget(
            weightKg: weight, mlPerKg: goals?.waterPerKgBodyweight ?? 35.0
        )

        let nutrition = DailyNutrition(
            date: Date().startOfDay,
            targetCalories: macros.calories,
            targetProteinGrams: macros.proteinGrams,
            targetCarbsGrams: macros.carbsGrams,
            targetFatGrams: macros.fatGrams,
            targetFiberGrams: macros.fiberGrams,
            targetWaterMl: waterTarget
        )
        context.insert(nutrition)
        try? context.save()
        return nutrition
    }

    func updateNutrition(_ nutrition: DailyNutrition) {
        nutrition.updatedAt = Date()
        try? context.save()
    }

    // MARK: - Meal Management

    func addMeal(_ meal: MealEntry) {
        guard let today = fetchTodayNutrition() else { return }
        today.meals.append(meal)
        today.consumedCalories += meal.calories
        today.consumedProteinGrams += Int(meal.proteinGrams)
        today.consumedCarbsGrams += Int(meal.carbsGrams)
        today.consumedFatGrams += Int(meal.fatGrams)
        today.consumedFiberGrams += Int(meal.fiberGrams)
        updateNutrition(today)
    }

    func removeMeal(mealId: String) {
        guard let today = fetchTodayNutrition() else { return }
        guard let meal = today.meals.first(where: { $0.id == mealId }) else { return }
        today.meals.removeAll { $0.id == mealId }
        today.consumedCalories = max(0, today.consumedCalories - meal.calories)
        today.consumedProteinGrams = max(0, today.consumedProteinGrams - Int(meal.proteinGrams))
        today.consumedCarbsGrams = max(0, today.consumedCarbsGrams - Int(meal.carbsGrams))
        today.consumedFatGrams = max(0, today.consumedFatGrams - Int(meal.fatGrams))
        today.consumedFiberGrams = max(0, today.consumedFiberGrams - Int(meal.fiberGrams))
        updateNutrition(today)
    }

    func addWater(ml: Int) {
        guard let today = fetchTodayNutrition() else { return }
        today.waterMl += ml
        updateNutrition(today)
    }

    func updateStepCount(steps: Int, weightKg: Double) {
        guard let today = fetchTodayNutrition() else { return }
        let stepCalories = NutritionCalculator.calculateStepCalories(steps: steps, weightKg: weightKg)
        today.stepCount = steps
        today.stepCaloriesBurned = stepCalories
        today.burnedCalories = stepCalories + today.workoutCaloriesBurned
        updateNutrition(today)
    }

    func addWorkoutCalories(_ calories: Int) {
        guard let today = fetchTodayNutrition() else { return }
        today.workoutCaloriesBurned += calories
        today.burnedCalories = today.stepCaloriesBurned + today.workoutCaloriesBurned
        updateNutrition(today)
    }

    func setWorkoutCalories(_ calories: Int) {
        guard let today = fetchTodayNutrition() else { return }
        today.workoutCaloriesBurned = calories
        today.burnedCalories = today.stepCaloriesBurned + calories
        updateNutrition(today)
    }

    // MARK: - Nutrition Goals

    func fetchGoals() -> NutritionGoals? {
        let descriptor = FetchDescriptor<NutritionGoals>()
        return try? context.fetch(descriptor).first
    }

    func saveGoals(_ goals: NutritionGoals) {
        goals.updatedAt = Date()
        if fetchGoals() == nil {
            context.insert(goals)
        }
        try? context.save()
    }

    // MARK: - Stats

    func weeklyAverages() -> NutritionWeeklyStats {
        let recent = fetchRecentNutrition(days: 7)
        let count = max(1, recent.count)
        let avgCalories = recent.reduce(0) { $0 + $1.consumedCalories } / count
        let avgProtein = recent.reduce(0) { $0 + $1.consumedProteinGrams } / count
        let totalSteps = recent.reduce(0) { $0 + $1.stepCount }
        return NutritionWeeklyStats(avgCalories: avgCalories, avgProtein: avgProtein, totalSteps: totalSteps)
    }
}

struct NutritionWeeklyStats {
    let avgCalories: Int
    let avgProtein: Int
    let totalSteps: Int
}
