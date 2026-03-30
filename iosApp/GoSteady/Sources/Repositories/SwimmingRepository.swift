import Foundation
import SwiftData

final class SwimmingRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Workouts

    func fetchAllWorkouts() -> [SwimmingWorkout] {
        let descriptor = FetchDescriptor<SwimmingWorkout>(sortBy: [SortDescriptor(\.startTime, order: .reverse)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchWorkoutById(_ id: UUID) -> SwimmingWorkout? {
        let descriptor = FetchDescriptor<SwimmingWorkout>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    func fetchWorkoutsInRange(start: Date, end: Date) -> [SwimmingWorkout] {
        let descriptor = FetchDescriptor<SwimmingWorkout>(
            predicate: #Predicate { $0.startTime >= start && $0.startTime <= end },
            sortBy: [SortDescriptor(\.startTime, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchRecentCompleted(limit: Int = 10) -> [SwimmingWorkout] {
        var descriptor = FetchDescriptor<SwimmingWorkout>(
            predicate: #Predicate { $0.isCompleted == true },
            sortBy: [SortDescriptor(\.startTime, order: .reverse)]
        )
        descriptor.fetchLimit = limit
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchActiveWorkout() -> SwimmingWorkout? {
        let descriptor = FetchDescriptor<SwimmingWorkout>(predicate: #Predicate { $0.isCompleted == false })
        return try? context.fetch(descriptor).first
    }

    func insert(_ workout: SwimmingWorkout) {
        context.insert(workout)
        try? context.save()
    }

    func update(_ workout: SwimmingWorkout) {
        try? context.save()
    }

    func delete(_ workout: SwimmingWorkout) {
        context.delete(workout)
        try? context.save()
    }

    func totalDistanceSince(_ date: Date) -> Double {
        let workouts = fetchWorkoutsInRange(start: date, end: Date()).filter(\.isCompleted)
        return workouts.reduce(0) { $0 + $1.distanceMeters }
    }

    func workoutCountSince(_ date: Date) -> Int {
        let workouts = fetchWorkoutsInRange(start: date, end: Date()).filter(\.isCompleted)
        return workouts.count
    }

    // MARK: - Training Plans

    func fetchAllPlans() -> [SwimmingTrainingPlan] {
        let descriptor = FetchDescriptor<SwimmingTrainingPlan>(sortBy: [SortDescriptor(\.startDate, order: .reverse)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchActivePlan() -> SwimmingTrainingPlan? {
        let descriptor = FetchDescriptor<SwimmingTrainingPlan>(predicate: #Predicate { $0.isActive == true })
        return try? context.fetch(descriptor).first
    }

    func fetchPlanById(_ id: UUID) -> SwimmingTrainingPlan? {
        let descriptor = FetchDescriptor<SwimmingTrainingPlan>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    func insertPlan(_ plan: SwimmingTrainingPlan) {
        context.insert(plan)
        try? context.save()
    }

    func updatePlan(_ plan: SwimmingTrainingPlan) {
        try? context.save()
    }

    func deletePlan(_ plan: SwimmingTrainingPlan) {
        context.delete(plan)
        try? context.save()
    }

    func activatePlan(_ plan: SwimmingTrainingPlan) {
        let allPlans = fetchAllPlans().filter(\.isActive)
        for p in allPlans { p.isActive = false }
        plan.isActive = true
        try? context.save()
    }

    func markWorkoutCompleted(planId: UUID, workoutId: String, completedWorkoutId: UUID) {
        guard let plan = fetchPlanById(planId) else { return }
        if let index = plan.weeklySchedule.firstIndex(where: { $0.id == workoutId }) {
            plan.weeklySchedule[index].isCompleted = true
            plan.weeklySchedule[index].completedWorkoutId = completedWorkoutId
            try? context.save()
        }
    }

    // MARK: - Weekly Stats

    func weeklyStats() -> SwimmingWeeklyStats {
        let weekStart = Date().startOfWeek
        let workouts = fetchWorkoutsInRange(start: weekStart, end: Date()).filter(\.isCompleted)
        let totalDistance = workouts.reduce(0.0) { $0 + $1.distanceMeters }
        let count = workouts.count
        let avgPace: Double
        if totalDistance > 0 {
            let totalDuration = workouts.reduce(0.0) { $0 + Double($1.durationMillis) / 1000.0 }
            avgPace = totalDuration / (totalDistance / 100.0)
        } else {
            avgPace = 0
        }
        return SwimmingWeeklyStats(totalDistanceMeters: totalDistance, workoutCount: count, averagePace: avgPace, weekStartDate: weekStart)
    }
}

struct SwimmingWeeklyStats {
    let totalDistanceMeters: Double
    let workoutCount: Int
    let averagePace: Double
    let weekStartDate: Date

    var totalDistanceKm: Double { totalDistanceMeters / 1000.0 }
}
