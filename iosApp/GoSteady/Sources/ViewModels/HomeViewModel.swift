import Foundation
import SwiftUI
import SwiftData
import HealthKit

@Observable
final class HomeViewModel {
    var userName: String = "Athlete"
    var readinessScore: Int = 0
    var readinessLabel: String = "No data"
    var sleepHours: Double? = nil
    var todaySteps: Int = 0
    var todayCalories: Double = 0
    var activeStreak: Int = 0
    var weeklyDistanceKm: Double = 0
    var weeklyDurationFormatted: String = "0h 0m"
    var weeklyWorkoutCount: Int = 0
    var weeklyGoalProgress: Float = 0
    var recentWorkouts: [RecentWorkoutData] = []
    var upcomingWorkout: UpcomingWorkoutData? = nil
    var weeklyActivity: [DayActivity] = []
    var isLoading: Bool = true
    var restingHeartRate: Int? = nil
    var hrv: Double? = nil

    private let healthService = HealthKitService()
    private var modelContext: ModelContext?

    var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 0..<12: return "Good morning"
        case 12..<17: return "Good afternoon"
        default: return "Good evening"
        }
    }

    func load(modelContext: ModelContext) async {
        self.modelContext = modelContext
        isLoading = true

        // Load user profile
        loadUserProfile(modelContext: modelContext)

        // Load runs and gym workouts
        loadWeeklyStats(modelContext: modelContext)
        loadRecentWorkouts(modelContext: modelContext)
        loadUpcomingWorkout(modelContext: modelContext)
        buildWeeklyActivityGrid(modelContext: modelContext)
        calculateStreak(modelContext: modelContext)

        // Load HealthKit data
        await loadHealthData()

        calculateReadiness()
        isLoading = false
    }

    private func loadUserProfile(modelContext: ModelContext) {
        let descriptor = FetchDescriptor<UserProfile>()
        if let profile = try? modelContext.fetch(descriptor).first {
            userName = profile.name.isEmpty ? "Athlete" : profile.name
        }
    }

    private func loadWeeklyStats(modelContext: ModelContext) {
        let calendar = Calendar.current
        let startOfWeek = calendar.dateInterval(of: .weekOfYear, for: Date())?.start ?? Date()

        // Runs this week
        var runDescriptor = FetchDescriptor<Run>(
            predicate: #Predicate<Run> { $0.startTime >= startOfWeek && $0.isCompleted }
        )
        runDescriptor.sortBy = [SortDescriptor(\.startTime, order: .reverse)]

        let runs = (try? modelContext.fetch(runDescriptor)) ?? []
        let runDistanceKm = runs.reduce(0.0) { $0 + $1.distanceKm }
        let runDurationMs = runs.reduce(Int64(0)) { $0 + $1.durationMillis }

        // Gym workouts this week
        var gymDescriptor = FetchDescriptor<GymWorkout>(
            predicate: #Predicate<GymWorkout> { $0.startTime >= startOfWeek && $0.isCompleted }
        )
        gymDescriptor.sortBy = [SortDescriptor(\.startTime, order: .reverse)]

        let gymWorkouts = (try? modelContext.fetch(gymDescriptor)) ?? []
        let gymDurationMs = gymWorkouts.reduce(Int64(0)) { $0 + $1.durationMillis }

        weeklyDistanceKm = runDistanceKm
        weeklyWorkoutCount = runs.count + gymWorkouts.count

        let totalMs = runDurationMs + gymDurationMs
        let totalMinutes = totalMs / 60000
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60
        weeklyDurationFormatted = "\(hours)h \(minutes)m"

        // Weekly goal
        let profileDescriptor = FetchDescriptor<UserProfile>()
        if let profile = try? modelContext.fetch(profileDescriptor).first {
            let goal = profile.weeklyGoalKm
            weeklyGoalProgress = goal > 0 ? Float(runDistanceKm / goal).clamped(to: 0...1) : 0
        }
    }

    private func loadRecentWorkouts(modelContext: ModelContext) {
        var runDescriptor = FetchDescriptor<Run>(
            predicate: #Predicate<Run> { $0.isCompleted }
        )
        runDescriptor.sortBy = [SortDescriptor(\.startTime, order: .reverse)]
        runDescriptor.fetchLimit = 5

        let runs = (try? modelContext.fetch(runDescriptor)) ?? []

        var gymDescriptor = FetchDescriptor<GymWorkout>(
            predicate: #Predicate<GymWorkout> { $0.isCompleted }
        )
        gymDescriptor.sortBy = [SortDescriptor(\.startTime, order: .reverse)]
        gymDescriptor.fetchLimit = 5

        let gymWorkouts = (try? modelContext.fetch(gymDescriptor)) ?? []

        var workouts: [RecentWorkoutData] = []
        for run in runs {
            workouts.append(RecentWorkoutData(
                id: run.id,
                sportIcon: AppTheme.SportIcon.running,
                sportColor: AppTheme.running,
                title: String(format: "%.2f km Run", run.distanceKm),
                duration: run.durationFormatted,
                keyMetric: "\(run.avgPaceFormatted)/km",
                date: run.startTime
            ))
        }
        for workout in gymWorkouts {
            workouts.append(RecentWorkoutData(
                id: workout.id,
                sportIcon: AppTheme.SportIcon.gym,
                sportColor: AppTheme.gym,
                title: workout.name,
                duration: workout.durationFormatted,
                keyMetric: "\(workout.exercises.count) exercises",
                date: workout.startTime
            ))
        }

        recentWorkouts = workouts.sorted { $0.date > $1.date }.prefix(5).map { $0 }
    }

    private func loadUpcomingWorkout(modelContext: ModelContext) {
        let descriptor = FetchDescriptor<TrainingPlan>(
            predicate: #Predicate<TrainingPlan> { $0.isActive }
        )
        guard let plan = try? modelContext.fetch(descriptor).first else {
            upcomingWorkout = nil
            return
        }

        let today = Calendar.current.component(.weekday, from: Date())
        let nextWorkout = plan.weeklySchedule
            .filter { !$0.isCompleted }
            .min(by: {
                let diff0 = ($0.dayOfWeek - today + 7) % 7
                let diff1 = ($1.dayOfWeek - today + 7) % 7
                return diff0 < diff1
            })

        if let next = nextWorkout {
            upcomingWorkout = UpcomingWorkoutData(
                workoutType: next.workoutType.rawValue.replacingOccurrences(of: "_", with: " ").capitalized,
                description: next.workoutDescription,
                targetDistance: next.targetDistanceMeters.map { String(format: "%.1f km", $0 / 1000) }
            )
        }
    }

    private func buildWeeklyActivityGrid(modelContext: ModelContext) {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        var activities: [DayActivity] = []

        for dayOffset in (0..<7).reversed() {
            guard let date = calendar.date(byAdding: .day, value: -dayOffset, to: today) else { continue }
            let nextDate = calendar.date(byAdding: .day, value: 1, to: date)!

            var runDescriptor = FetchDescriptor<Run>(
                predicate: #Predicate<Run> { $0.startTime >= date && $0.startTime < nextDate && $0.isCompleted }
            )
            runDescriptor.fetchLimit = 10
            let dayRuns = (try? modelContext.fetch(runDescriptor)) ?? []

            var gymDescriptor = FetchDescriptor<GymWorkout>(
                predicate: #Predicate<GymWorkout> { $0.startTime >= date && $0.startTime < nextDate && $0.isCompleted }
            )
            gymDescriptor.fetchLimit = 10
            let dayGym = (try? modelContext.fetch(gymDescriptor)) ?? []

            let dayName = calendar.shortWeekdaySymbols[calendar.component(.weekday, from: date) - 1]
            let workoutCount = dayRuns.count + dayGym.count
            let isToday = calendar.isDateInToday(date)

            activities.append(DayActivity(
                day: dayName,
                workoutCount: workoutCount,
                isToday: isToday,
                date: date
            ))
        }

        weeklyActivity = activities
    }

    private func calculateStreak(modelContext: ModelContext) {
        let calendar = Calendar.current
        var streak = 0
        var checkDate = calendar.startOfDay(for: Date())

        for _ in 0..<365 {
            let nextDate = calendar.date(byAdding: .day, value: 1, to: checkDate)!
            var runDescriptor = FetchDescriptor<Run>(
                predicate: #Predicate<Run> { $0.startTime >= checkDate && $0.startTime < nextDate && $0.isCompleted }
            )
            runDescriptor.fetchLimit = 1
            let hasRun = ((try? modelContext.fetch(runDescriptor)) ?? []).count > 0

            var gymDescriptor = FetchDescriptor<GymWorkout>(
                predicate: #Predicate<GymWorkout> { $0.startTime >= checkDate && $0.startTime < nextDate && $0.isCompleted }
            )
            gymDescriptor.fetchLimit = 1
            let hasGym = ((try? modelContext.fetch(gymDescriptor)) ?? []).count > 0

            if hasRun || hasGym {
                streak += 1
                checkDate = calendar.date(byAdding: .day, value: -1, to: checkDate)!
            } else {
                break
            }
        }

        activeStreak = streak
    }

    private func loadHealthData() async {
        guard HealthKitService.isAvailable else { return }

        do {
            try await healthService.requestAuthorization()
            todaySteps = await healthService.fetchTodaySteps()
            todayCalories = await healthService.fetchTodayCalories()
            sleepHours = await healthService.fetchLastNightSleep()?.totalHours
            restingHeartRate = await healthService.fetchRestingHeartRate()
            hrv = await healthService.fetchLatestHRV()
        } catch {
            print("Health data load error: \(error)")
        }
    }

    private func calculateReadiness() {
        var score = 50 // baseline
        var factors = 0

        // Sleep factor (0-30 points)
        if let sleep = sleepHours {
            factors += 1
            if sleep >= 8 { score += 30 }
            else if sleep >= 7 { score += 25 }
            else if sleep >= 6 { score += 15 }
            else if sleep >= 5 { score += 5 }
            else { score -= 10 }
        }

        // HRV factor (0-20 points)
        if let hrv {
            factors += 1
            if hrv >= 60 { score += 20 }
            else if hrv >= 40 { score += 10 }
            else if hrv >= 20 { score += 5 }
            else { score -= 5 }
        }

        // Resting heart rate factor
        if let rhr = restingHeartRate {
            factors += 1
            if rhr < 60 { score += 10 }
            else if rhr < 70 { score += 5 }
            else { score -= 5 }
        }

        if factors == 0 {
            readinessScore = 0
            readinessLabel = "No data"
        } else {
            readinessScore = max(0, min(100, score))
            switch readinessScore {
            case 80...: readinessLabel = "Excellent"
            case 60..<80: readinessLabel = "Good"
            case 40..<60: readinessLabel = "Moderate"
            default: readinessLabel = "Low"
            }
        }
    }
}

// MARK: - Data Models

struct RecentWorkoutData: Identifiable {
    let id: UUID
    let sportIcon: String
    let sportColor: Color
    let title: String
    let duration: String
    let keyMetric: String
    let date: Date
}

struct UpcomingWorkoutData {
    let workoutType: String
    let description: String
    let targetDistance: String?
}

struct DayActivity: Identifiable {
    let id = UUID()
    let day: String
    let workoutCount: Int
    let isToday: Bool
    let date: Date
}

// MARK: - Clamp Extension

extension Comparable {
    func clamped(to limits: ClosedRange<Self>) -> Self {
        min(max(self, limits.lowerBound), limits.upperBound)
    }
}
