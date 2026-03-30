import Foundation
import SwiftUI
import SwiftData

@Observable
final class OnboardingViewModel {
    var currentStep: Int = 0
    var name: String = ""
    var age: String = ""
    var weight: String = ""
    var height: String = ""
    var gender: Gender? = nil
    var fitnessGoals: Set<OnboardingGoal> = []
    var nutritionGoal: NutritionGoalType = .maintain
    var activityLevel: ActivityLevel = .moderatelyActive
    var weeklyRunGoalKm: String = "20"
    var locationPermissionGranted: Bool = false
    var healthPermissionGranted: Bool = false
    var notificationPermissionGranted: Bool = false
    var isComplete: Bool = false

    let totalSteps = 4

    var canProceed: Bool {
        switch currentStep {
        case 0: return true // Welcome
        case 1: return !name.isEmpty // Personal Info
        case 2: return !fitnessGoals.isEmpty // Goals
        case 3: return true // Permissions (optional)
        default: return true
        }
    }

    var progressValue: Float {
        Float(currentStep + 1) / Float(totalSteps)
    }

    func nextStep() {
        if currentStep < totalSteps - 1 {
            withAnimation(.easeInOut(duration: 0.3)) {
                currentStep += 1
            }
        }
    }

    func previousStep() {
        if currentStep > 0 {
            withAnimation(.easeInOut(duration: 0.3)) {
                currentStep -= 1
            }
        }
    }

    func toggleGoal(_ goal: OnboardingGoal) {
        if fitnessGoals.contains(goal) {
            fitnessGoals.remove(goal)
        } else {
            fitnessGoals.insert(goal)
        }
    }

    func requestLocationPermission() {
        // CLLocationManager permission request would go here
        locationPermissionGranted = true
    }

    func requestHealthPermission() async {
        let service = HealthKitService()
        do {
            try await service.requestAuthorization()
            healthPermissionGranted = true
        } catch {
            healthPermissionGranted = false
        }
    }

    func requestNotificationPermission() async {
        do {
            let granted = try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])
            notificationPermissionGranted = granted
        } catch {
            notificationPermissionGranted = false
        }
    }

    func completeOnboarding(modelContext: ModelContext) {
        // Save user profile
        let profile = UserProfile(
            name: name.isEmpty ? "Athlete" : name,
            age: Int(age),
            weight: Double(weight),
            height: Double(height),
            gender: gender,
            weeklyGoalKm: Double(weeklyRunGoalKm) ?? 20.0,
            isOnboardingComplete: true
        )
        modelContext.insert(profile)

        // Save nutrition goals
        let nutritionGoals = NutritionGoals(
            goal: nutritionGoal,
            activityLevel: activityLevel
        )
        modelContext.insert(nutritionGoals)

        isComplete = true
    }
}

// MARK: - Onboarding Goal

enum OnboardingGoal: String, CaseIterable, Identifiable, Hashable {
    case running = "Running"
    case gym = "Gym / Weights"
    case swimming = "Swimming"
    case cycling = "Cycling"
    case loseWeight = "Lose Weight"
    case buildMuscle = "Build Muscle"
    case improveEndurance = "Improve Endurance"
    case stayActive = "Stay Active"
    case trainForRace = "Train for a Race"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .running: return AppTheme.SportIcon.running
        case .gym: return AppTheme.SportIcon.gym
        case .swimming: return AppTheme.SportIcon.swimming
        case .cycling: return AppTheme.SportIcon.cycling
        case .loseWeight: return "scalemass.fill"
        case .buildMuscle: return "figure.strengthtraining.traditional"
        case .improveEndurance: return "heart.circle.fill"
        case .stayActive: return "figure.walk"
        case .trainForRace: return "trophy.fill"
        }
    }

    var color: Color {
        switch self {
        case .running: return AppTheme.running
        case .gym: return AppTheme.gym
        case .swimming: return AppTheme.swimming
        case .cycling: return AppTheme.cycling
        case .loseWeight: return AppTheme.secondary
        case .buildMuscle: return AppTheme.hiit
        case .improveEndurance: return AppTheme.accent
        case .stayActive: return AppTheme.tertiary
        case .trainForRace: return AppTheme.warning
        }
    }
}
