import Foundation
import SwiftUI
import SwiftData

@Observable
final class ProfileViewModel {
    var name: String = ""
    var age: String = ""
    var weight: String = ""
    var height: String = ""
    var gender: Gender? = nil
    var fitnessLevel: FitnessLevel = .intermediate
    var restingHeartRate: String = ""
    var maxHeartRate: String = ""
    var weeklyGoalKm: String = "20"
    var preferredUnits: Units = .metric
    var isStravaConnected: Bool = false
    var isEditing: Bool = false
    var isSaved: Bool = false

    // Computed body stats
    var bmi: Double? {
        guard let w = Double(weight), let h = Double(height), h > 0 else { return nil }
        let heightM = h / 100.0
        return w / (heightM * heightM)
    }

    var bmiCategory: String {
        guard let bmi else { return "N/A" }
        switch bmi {
        case ..<18.5: return "Underweight"
        case 18.5..<25: return "Normal"
        case 25..<30: return "Overweight"
        default: return "Obese"
        }
    }

    var bmiColor: Color {
        guard let bmi else { return AppTheme.onSurfaceVariant }
        switch bmi {
        case ..<18.5: return AppTheme.warning
        case 18.5..<25: return AppTheme.success
        case 25..<30: return AppTheme.secondary
        default: return AppTheme.error
        }
    }

    var estimatedMaxHR: Int {
        if let maxHR = Int(maxHeartRate), maxHR > 0 { return maxHR }
        let ageVal = Int(age) ?? 30
        return 220 - ageVal
    }

    private var modelContext: ModelContext?

    func load(modelContext: ModelContext) {
        self.modelContext = modelContext
        let descriptor = FetchDescriptor<UserProfile>()
        guard let profile = try? modelContext.fetch(descriptor).first else { return }

        name = profile.name
        age = profile.age.map(String.init) ?? ""
        weight = profile.weight.map { String(format: "%.1f", $0) } ?? ""
        height = profile.height.map { String(format: "%.0f", $0) } ?? ""
        gender = profile.gender
        restingHeartRate = profile.restingHeartRate.map(String.init) ?? ""
        maxHeartRate = profile.maxHeartRate.map(String.init) ?? ""
        weeklyGoalKm = String(format: "%.0f", profile.weeklyGoalKm)
        preferredUnits = profile.preferredUnits
        isStravaConnected = profile.stravaAccessToken != nil
    }

    func saveProfile() {
        guard let modelContext else { return }
        let descriptor = FetchDescriptor<UserProfile>()
        let profile: UserProfile

        if let existing = try? modelContext.fetch(descriptor).first {
            profile = existing
        } else {
            profile = UserProfile()
            modelContext.insert(profile)
        }

        profile.name = name
        profile.age = Int(age)
        profile.weight = Double(weight)
        profile.height = Double(height)
        profile.gender = gender
        profile.restingHeartRate = Int(restingHeartRate)
        profile.maxHeartRate = Int(maxHeartRate)
        profile.weeklyGoalKm = Double(weeklyGoalKm) ?? 20.0
        profile.preferredUnits = preferredUnits
        profile.updatedAt = Date()

        isEditing = false
        isSaved = true

        // Reset flag after short delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { [weak self] in
            self?.isSaved = false
        }
    }

    func toggleEditing() {
        if isEditing {
            saveProfile()
        } else {
            isEditing = true
        }
    }

    func setFitnessLevel(_ level: FitnessLevel) {
        fitnessLevel = level
    }
}
