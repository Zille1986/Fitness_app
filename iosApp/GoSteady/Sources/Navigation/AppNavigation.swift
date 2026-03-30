import SwiftUI

// MARK: - Route Enum

enum AppRoute: Hashable {
    // Main tabs
    case home
    case activity
    case training
    case nutritionDashboard
    case profile

    // Activity sub-screens
    case runningDashboard
    case gymDashboard
    case swimmingDashboard
    case cyclingDashboard
    case hiitDashboard

    // Running
    case activeRun
    case runDetail(id: UUID)
    case runHistory

    // Gym
    case activeWorkout(id: UUID)
    case workoutDetail(id: UUID)
    case exerciseLibrary
    case createTemplate
    case editTemplate(id: UUID)
    case gymHistory
    case exerciseAnalysis

    // Swimming
    case activeSwim(swimType: String, poolLength: String?)
    case createSwimmingPlan

    // Cycling
    case activeCycling(cyclingType: String)
    case createCyclingPlan

    // HIIT
    case activeHIIT(templateId: String)
    case hiitSummary(sessionId: UUID)

    // Training Plans
    case trainingPlans
    case planDetail(id: UUID)
    case workoutPreview(planId: UUID, workoutId: String)
    case customWorkouts
    case workoutBuilder
    case customPlans
    case planBuilder
    case workoutPlan

    // Nutrition
    case nutritionSettings
    case foodScanner

    // Profile sub-screens
    case settings
    case analytics
    case achievements
    case gamification
    case progressPhotos
    case bodyAnalysis

    // AI Coach
    case aiCoach

    // Strava
    case stravaImport
    case stravaSettings

    // Wellness / Mindfulness
    case wellnessDashboard
    case mindfulness

    // Safety
    case safety

    // Health Sync
    case healthSync
}

// MARK: - Deep Link Handler

enum DeepLink {
    case stravaCallback(code: String)
    case workout(id: UUID)
    case run(id: UUID)
    case openTab(AppTab)

    static func parse(url: URL) -> DeepLink? {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            return nil
        }

        // Strava OAuth callback
        if components.host == "localhost" && components.path.hasPrefix("/callback") {
            if let code = components.queryItems?.first(where: { $0.name == "code" })?.value {
                return .stravaCallback(code: code)
            }
        }

        // GoSteady deep links: gosteady://workout/{id}
        if components.scheme == "gosteady" {
            switch components.host {
            case "workout":
                if let idString = components.path.split(separator: "/").first,
                   let id = UUID(uuidString: String(idString)) {
                    return .workout(id: id)
                }
            case "run":
                if let idString = components.path.split(separator: "/").first,
                   let id = UUID(uuidString: String(idString)) {
                    return .run(id: id)
                }
            case "home":
                return .openTab(.home)
            case "activity":
                return .openTab(.activity)
            case "training":
                return .openTab(.training)
            case "nutrition":
                return .openTab(.nutrition)
            case "profile":
                return .openTab(.profile)
            default:
                break
            }
        }

        return nil
    }
}

// MARK: - App Tab

enum AppTab: String, CaseIterable, Identifiable {
    case home = "Home"
    case activity = "Activity"
    case training = "Training"
    case nutrition = "Nutrition"
    case profile = "Profile"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .home: return "house.fill"
        case .activity: return "figure.run"
        case .training: return "calendar.badge.clock"
        case .nutrition: return "fork.knife"
        case .profile: return "person.circle.fill"
        }
    }

    var selectedIcon: String {
        switch self {
        case .home: return "house.fill"
        case .activity: return "figure.run"
        case .training: return "calendar.badge.clock"
        case .nutrition: return "fork.knife"
        case .profile: return "person.circle.fill"
        }
    }
}

// MARK: - Router

@Observable
final class AppRouter {
    var selectedTab: AppTab = .home
    var homePath: [AppRoute] = []
    var activityPath: [AppRoute] = []
    var trainingPath: [AppRoute] = []
    var nutritionPath: [AppRoute] = []
    var profilePath: [AppRoute] = []

    func navigate(to route: AppRoute) {
        switch selectedTab {
        case .home: homePath.append(route)
        case .activity: activityPath.append(route)
        case .training: trainingPath.append(route)
        case .nutrition: nutritionPath.append(route)
        case .profile: profilePath.append(route)
        }
    }

    func popToRoot() {
        switch selectedTab {
        case .home: homePath.removeAll()
        case .activity: activityPath.removeAll()
        case .training: trainingPath.removeAll()
        case .nutrition: nutritionPath.removeAll()
        case .profile: profilePath.removeAll()
        }
    }

    func handleDeepLink(_ deepLink: DeepLink) {
        switch deepLink {
        case .stravaCallback:
            selectedTab = .profile
            profilePath = [.settings, .stravaSettings]
        case .workout(let id):
            selectedTab = .activity
            activityPath = [.workoutDetail(id: id)]
        case .run(let id):
            selectedTab = .activity
            activityPath = [.runDetail(id: id)]
        case .openTab(let tab):
            selectedTab = tab
        }
    }
}
