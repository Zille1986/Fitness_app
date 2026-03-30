import Foundation

struct MacroTargets {
    let calories: Int
    let proteinGrams: Int
    let carbsGrams: Int
    let fatGrams: Int
    let fiberGrams: Int
}

struct NutritionCalculator {

    // MARK: - BMR (Mifflin-St Jeor)

    static func calculateBMR(weightKg: Double, heightCm: Double, ageYears: Int, gender: Gender?) -> Double {
        let base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * Double(ageYears))
        switch gender {
        case .male:   return base + 5
        case .female: return base - 161
        default:      return base - 78
        }
    }

    // MARK: - TDEE

    static func calculateTDEE(bmr: Double, activityLevel: ActivityLevel) -> Double {
        bmr * activityLevel.multiplier
    }

    // MARK: - Daily Calories

    static func calculateDailyCalories(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        gender: Gender?,
        activityLevel: ActivityLevel,
        goal: NutritionGoalType
    ) -> Int {
        let bmr = calculateBMR(weightKg: weightKg, heightCm: heightCm, ageYears: ageYears, gender: gender)
        let tdee = calculateTDEE(bmr: bmr, activityLevel: activityLevel)
        return Int(tdee) + goal.calorieAdjustment
    }

    // MARK: - Step Calories

    static func calculateStepCalories(steps: Int, weightKg: Double) -> Int {
        Int(Double(steps) * 0.04 * (weightKg / 70.0))
    }

    // MARK: - Running Calories

    static func calculateRunningCalories(distanceKm: Double, durationMinutes: Double, weightKg: Double) -> Int {
        guard durationMinutes > 0 else { return 0 }
        let speedKmH = (distanceKm / durationMinutes) * 60.0
        let met: Double
        switch speedKmH {
        case ..<6:   met = 6.0
        case 6..<8:  met = 8.3
        case 8..<10: met = 9.8
        case 10..<12: met = 11.0
        case 12..<14: met = 11.8
        default:      met = 12.8
        }
        return Int((met * 3.5 * weightKg / 200.0) * durationMinutes)
    }

    // MARK: - Gym Calories

    static func calculateGymCalories(durationMinutes: Double, totalVolume: Double, weightKg: Double) -> Int {
        let intensityFactor: Double
        switch totalVolume {
        case ..<5000:   intensityFactor = 3.5
        case 5000..<10000:  intensityFactor = 4.5
        case 10000..<20000: intensityFactor = 5.5
        default:            intensityFactor = 6.5
        }
        return Int((intensityFactor * 3.5 * weightKg / 200.0) * durationMinutes)
    }

    // MARK: - Macro Calculation

    static func calculateMacros(
        totalCalories: Int,
        weightKg: Double,
        proteinPerKg: Double = 1.6,
        carbPercentage: Double = 0.45,
        fatPercentage: Double = 0.25
    ) -> MacroTargets {
        let proteinGrams = Int(weightKg * proteinPerKg)
        let proteinCalories = proteinGrams * 4
        let remainingCalories = totalCalories - proteinCalories
        let carbRatio = carbPercentage / (carbPercentage + fatPercentage)
        let carbCalories = Int(Double(remainingCalories) * carbRatio)
        let fatCalories = remainingCalories - carbCalories
        let carbsGrams = carbCalories / 4
        let fatGrams = fatCalories / 9
        let fiberGrams = Int(Double(totalCalories) / 1000.0 * 14.0)

        return MacroTargets(
            calories: totalCalories,
            proteinGrams: proteinGrams,
            carbsGrams: carbsGrams,
            fatGrams: fatGrams,
            fiberGrams: fiberGrams
        )
    }

    // MARK: - Water Target

    static func calculateWaterTarget(weightKg: Double, mlPerKg: Double = 35.0) -> Int {
        Int(weightKg * mlPerKg)
    }

    // MARK: - Activity Adjustment

    static func adjustCaloriesForActivity(
        baseCalories: Int,
        stepCount: Int,
        stepGoal: Int,
        workoutCalories: Int
    ) -> Int {
        let workoutAdjustment = Int(Double(workoutCalories) * 0.5)
        let stepDifference = stepCount - stepGoal
        let stepAdjustment: Int
        switch stepDifference {
        case 5001...:       stepAdjustment = 150
        case 2001...5000:   stepAdjustment = 75
        case (-4999)...(-2001): stepAdjustment = -50
        case ...(-5000):    stepAdjustment = -100
        default:            stepAdjustment = 0
        }
        return baseCalories + workoutAdjustment + stepAdjustment
    }
}
