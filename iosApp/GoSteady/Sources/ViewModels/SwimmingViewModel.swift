import Foundation
import Observation
import SwiftData

// MARK: - ViewModel-specific Types

struct SwimmingTrainingPlanSummary: Identifiable {
    let id: String
    let name: String
    let description: String
    let goalType: String
    let weekProgress: Int
    let totalWeeks: Int
    let isActive: Bool
}

// MARK: - ViewModel

@Observable
final class SwimmingViewModel {
    // State
    var recentWorkouts: [SwimmingWorkoutSummary] = []
    var weeklyStats: SwimmingWeeklyStats?
    var activePlan: SwimmingTrainingPlanSummary?
    var availablePlans: [SwimmingTrainingPlanSummary] = []
    var strokeBreakdown: [StrokeBreakdownItem] = []
    var isLoading = true
    var errorMessage: String?
    var selectedFilter: SwimFilterType = .all

    enum SwimFilterType: String, CaseIterable {
        case all = "All"
        case pool = "Pool"
        case openWater = "Open Water"
    }

    // Repositories (injected)
    private var swimmingRepository: SwimmingRepository?

    init() {}

    func configure(swimmingRepository: SwimmingRepository) {
        self.swimmingRepository = swimmingRepository
        loadData()
    }

    func loadData() {
        guard let swimmingRepository else { return }
        isLoading = true
        errorMessage = nil

        let rawWorkouts = swimmingRepository.fetchRecentCompleted(limit: 10)
        self.recentWorkouts = rawWorkouts.map { Self.toSummary($0) }
        self.weeklyStats = swimmingRepository.weeklyStats()

        if let plan = swimmingRepository.fetchActivePlan() {
            self.activePlan = Self.toPlanSummary(plan)
        }
        self.availablePlans = swimmingRepository.fetchAllPlans().map { Self.toPlanSummary($0) }
        self.strokeBreakdown = computeStrokeBreakdown(from: recentWorkouts)
        self.isLoading = false
    }

    private static func toSummary(_ w: SwimmingWorkout) -> SwimmingWorkoutSummary {
        SwimmingWorkoutSummary(
            id: w.id.uuidString,
            startTime: w.startTime,
            endTime: w.endTime,
            swimType: w.swimType,
            poolLength: w.poolLength,
            distanceMeters: w.distanceMeters,
            durationMillis: w.durationMillis,
            laps: w.laps,
            avgPaceSecondsPer100m: w.avgPaceSecondsPer100m,
            bestPaceSecondsPer100m: w.bestPaceSecondsPer100m,
            caloriesBurned: w.caloriesBurned,
            strokeType: w.strokeType,
            notes: w.notes,
            splits: w.splits
        )
    }

    private static func toPlanSummary(_ p: SwimmingTrainingPlan) -> SwimmingTrainingPlanSummary {
        let completedCount = p.weeklySchedule.filter(\.isCompleted).count
        let totalCount = p.weeklySchedule.count
        return SwimmingTrainingPlanSummary(
            id: p.id.uuidString,
            name: p.name,
            description: p.planDescription,
            goalType: p.goalType.rawValue,
            weekProgress: totalCount > 0 ? completedCount : 0,
            totalWeeks: max(p.weeklySchedule.map(\.weekNumber).max() ?? 1, 1),
            isActive: p.isActive
        )
    }

    var filteredWorkouts: [SwimmingWorkoutSummary] {
        switch selectedFilter {
        case .all:
            return recentWorkouts
        case .pool:
            return recentWorkouts.filter { $0.swimType == .pool }
        case .openWater:
            return recentWorkouts.filter { $0.swimType != .pool }
        }
    }

    func saveManualSwim(distanceMeters: Double, durationMillis: Int64, strokeType: StrokeType, swimType: SwimType, notes: String?) {
        guard let swimmingRepository else { return }
        let pacePer100m = distanceMeters > 0 ? (Double(durationMillis) / 1000.0) / (distanceMeters / 100.0) : 0.0
        let workout = SwimmingWorkout(
            startTime: Date().addingTimeInterval(-Double(durationMillis) / 1000.0),
            endTime: Date(),
            swimType: swimType,
            poolLength: swimType == .pool ? .shortCourseMeters : nil,
            distanceMeters: distanceMeters,
            durationMillis: durationMillis,
            laps: swimType == .pool ? Int(distanceMeters / 25.0) : 0,
            avgPaceSecondsPer100m: pacePer100m,
            bestPaceSecondsPer100m: pacePer100m,
            caloriesBurned: estimateCalories(distance: distanceMeters, duration: durationMillis),
            strokeType: strokeType,
            notes: notes,
            isCompleted: true
        )
        swimmingRepository.insert(workout)
        loadData()
    }

    func deleteWorkout(_ workout: SwimmingWorkoutSummary) {
        guard let swimmingRepository, let uuid = UUID(uuidString: workout.id) else { return }
        if let dbWorkout = swimmingRepository.fetchWorkoutById(uuid) {
            swimmingRepository.delete(dbWorkout)
        }
        recentWorkouts.removeAll { $0.id == workout.id }
    }

    // MARK: - Helpers

    private func computeStrokeBreakdown(from workouts: [SwimmingWorkoutSummary]) -> [StrokeBreakdownItem] {
        var counts: [StrokeType: Double] = [:]
        for w in workouts {
            counts[w.strokeType, default: 0] += w.distanceMeters
        }
        let total = counts.values.reduce(0, +)
        guard total > 0 else { return [] }
        return counts.map { StrokeBreakdownItem(strokeType: $0.key, distanceMeters: $0.value, percentage: $0.value / total) }
            .sorted { $0.percentage > $1.percentage }
    }

    private func estimateCalories(distance: Double, duration: Int64) -> Int {
        // ~7 cal/min swimming at moderate pace
        let minutes = Double(duration) / 60000.0
        return Int(minutes * 7.0)
    }

    static func formatPace(_ secondsPer100m: Double) -> String {
        guard secondsPer100m > 0, !secondsPer100m.isInfinite, !secondsPer100m.isNaN else { return "--:--" }
        let minutes = Int(secondsPer100m / 60)
        let seconds = Int(secondsPer100m.truncatingRemainder(dividingBy: 60))
        return String(format: "%d:%02d", minutes, seconds)
    }

    static func formatDuration(_ millis: Int64) -> String {
        let totalSeconds = millis / 1000
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            return String(format: "%d:%02d", minutes, seconds)
        }
    }
}

// MARK: - Supporting Types

struct SwimmingWorkoutSummary: Identifiable {
    let id: String
    let startTime: Date
    let endTime: Date?
    let swimType: SwimType
    let poolLength: PoolLength?
    let distanceMeters: Double
    let durationMillis: Int64
    let laps: Int
    let avgPaceSecondsPer100m: Double
    let bestPaceSecondsPer100m: Double
    let caloriesBurned: Int
    let strokeType: StrokeType
    let notes: String?
    let splits: [SwimSplit]

    var distanceFormatted: String {
        if distanceMeters >= 1000 {
            return String(format: "%.2f km", distanceMeters / 1000.0)
        }
        return String(format: "%.0f m", distanceMeters)
    }

    var durationFormatted: String {
        SwimmingViewModel.formatDuration(durationMillis)
    }

    var avgPaceFormatted: String {
        SwimmingViewModel.formatPace(avgPaceSecondsPer100m)
    }

    var dateFormatted: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d"
        return formatter.string(from: startTime)
    }

    var fullDateFormatted: String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: startTime)
    }
}

struct StrokeBreakdownItem: Identifiable {
    var id: String { strokeType.rawValue }
    let strokeType: StrokeType
    let distanceMeters: Double
    let percentage: Double
}

