import Foundation
import SwiftData

final class CyclingRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Workouts

    func fetchAllWorkouts() -> [CyclingWorkout] {
        let descriptor = FetchDescriptor<CyclingWorkout>(sortBy: [SortDescriptor(\.startTime, order: .reverse)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchWorkoutById(_ id: UUID) -> CyclingWorkout? {
        let descriptor = FetchDescriptor<CyclingWorkout>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    func fetchWorkoutsInRange(start: Date, end: Date) -> [CyclingWorkout] {
        let descriptor = FetchDescriptor<CyclingWorkout>(
            predicate: #Predicate { $0.startTime >= start && $0.startTime <= end },
            sortBy: [SortDescriptor(\.startTime, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchRecentCompleted(limit: Int = 10) -> [CyclingWorkout] {
        var descriptor = FetchDescriptor<CyclingWorkout>(
            predicate: #Predicate { $0.isCompleted == true },
            sortBy: [SortDescriptor(\.startTime, order: .reverse)]
        )
        descriptor.fetchLimit = limit
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchActiveWorkout() -> CyclingWorkout? {
        let descriptor = FetchDescriptor<CyclingWorkout>(predicate: #Predicate { $0.isCompleted == false })
        return try? context.fetch(descriptor).first
    }

    func insert(_ workout: CyclingWorkout) {
        context.insert(workout)
        try? context.save()
    }

    func update(_ workout: CyclingWorkout) {
        try? context.save()
    }

    func delete(_ workout: CyclingWorkout) {
        context.delete(workout)
        try? context.save()
    }

    func totalDistanceSince(_ date: Date) -> Double {
        fetchWorkoutsInRange(start: date, end: Date())
            .filter(\.isCompleted)
            .reduce(0) { $0 + $1.distanceMeters }
    }

    func workoutCountSince(_ date: Date) -> Int {
        fetchWorkoutsInRange(start: date, end: Date())
            .filter(\.isCompleted)
            .count
    }

    func averagePowerSince(_ date: Date) -> Double {
        let workouts = fetchWorkoutsInRange(start: date, end: Date())
            .filter(\.isCompleted)
            .compactMap(\.avgPowerWatts)
        guard !workouts.isEmpty else { return 0 }
        return Double(workouts.reduce(0, +)) / Double(workouts.count)
    }

    // MARK: - Training Plans

    func fetchAllPlans() -> [CyclingTrainingPlan] {
        let descriptor = FetchDescriptor<CyclingTrainingPlan>(sortBy: [SortDescriptor(\.startDate, order: .reverse)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchActivePlan() -> CyclingTrainingPlan? {
        let descriptor = FetchDescriptor<CyclingTrainingPlan>(predicate: #Predicate { $0.isActive == true })
        return try? context.fetch(descriptor).first
    }

    func fetchPlanById(_ id: UUID) -> CyclingTrainingPlan? {
        let descriptor = FetchDescriptor<CyclingTrainingPlan>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    func insertPlan(_ plan: CyclingTrainingPlan) {
        context.insert(plan)
        try? context.save()
    }

    func updatePlan(_ plan: CyclingTrainingPlan) {
        try? context.save()
    }

    func deletePlan(_ plan: CyclingTrainingPlan) {
        context.delete(plan)
        try? context.save()
    }

    func activatePlan(_ plan: CyclingTrainingPlan) {
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

    func weeklyStats() -> CyclingWeeklyStats {
        let weekStart = Date().startOfWeek
        let totalDistance = totalDistanceSince(weekStart)
        let count = workoutCountSince(weekStart)
        let avgPower = averagePowerSince(weekStart)
        return CyclingWeeklyStats(
            totalDistanceMeters: totalDistance,
            workoutCount: count,
            averagePowerWatts: avgPower,
            weekStartDate: weekStart
        )
    }
}

struct CyclingWeeklyStats {
    let totalDistanceMeters: Double
    let workoutCount: Int
    let averagePowerWatts: Double
    let weekStartDate: Date

    var totalDistanceKm: Double { totalDistanceMeters / 1000.0 }
}
