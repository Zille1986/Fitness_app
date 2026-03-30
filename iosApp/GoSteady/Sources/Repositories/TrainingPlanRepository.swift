import Foundation
import SwiftData

final class TrainingPlanRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Fetch

    func fetchAll() -> [TrainingPlan] {
        let descriptor = FetchDescriptor<TrainingPlan>(sortBy: [SortDescriptor(\.startDate, order: .reverse)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchActivePlan() -> TrainingPlan? {
        let descriptor = FetchDescriptor<TrainingPlan>(predicate: #Predicate { $0.isActive == true })
        return try? context.fetch(descriptor).first
    }

    func fetchById(_ id: UUID) -> TrainingPlan? {
        let descriptor = FetchDescriptor<TrainingPlan>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    // MARK: - Create / Update / Delete

    func insert(_ plan: TrainingPlan) {
        context.insert(plan)
        try? context.save()
    }

    func update(_ plan: TrainingPlan) {
        try? context.save()
    }

    func delete(_ plan: TrainingPlan) {
        context.delete(plan)
        try? context.save()
    }

    // MARK: - Activation

    func setActivePlan(_ plan: TrainingPlan) {
        deactivateAllPlans()
        plan.isActive = true
        try? context.save()
    }

    func deactivateAllPlans() {
        let plans = fetchAll().filter(\.isActive)
        for plan in plans {
            plan.isActive = false
        }
        try? context.save()
    }

    // MARK: - Today's Workout

    func todaysWorkout() -> ScheduledWorkout? {
        guard let plan = fetchActivePlan() else { return nil }

        let cal = Calendar.current
        let dayOfWeek = cal.component(.weekday, from: Date())
        let daysSinceStart = cal.dateComponents([.day], from: plan.startDate.startOfDay, to: Date().startOfDay).day ?? 0
        let currentWeek = (daysSinceStart / 7) + 1

        return plan.weeklySchedule.first { workout in
            workout.weekNumber == currentWeek && workout.dayOfWeek == dayOfWeek
        }
    }

    /// Returns the next upcoming workout and how many days from now.
    func nextUpcomingWorkout() -> (workout: ScheduledWorkout, daysFromNow: Int)? {
        guard let plan = fetchActivePlan() else { return nil }

        let cal = Calendar.current
        let now = Date()
        guard now <= plan.endDate else { return nil }

        let daysSinceStart = cal.dateComponents([.day], from: plan.startDate.startOfDay, to: now.startOfDay).day ?? 0
        let totalDays = cal.dateComponents([.day], from: plan.startDate.startOfDay, to: plan.endDate.startOfDay).day ?? 0
        let totalWeeks = (totalDays / 7) + 1

        for daysAhead in 0...60 {
            guard let targetDate = cal.date(byAdding: .day, value: daysAhead, to: now) else { continue }
            let targetDayOfWeek = cal.component(.weekday, from: targetDate)
            let targetDaysSinceStart = daysSinceStart + daysAhead
            let targetWeek = (targetDaysSinceStart / 7) + 1

            if targetWeek > totalWeeks { break }

            if let workout = plan.weeklySchedule.first(where: {
                $0.weekNumber == targetWeek && $0.dayOfWeek == targetDayOfWeek && !$0.isCompleted
            }) {
                return (workout, daysAhead)
            }
        }
        return nil
    }

    /// Mark a scheduled workout as completed with a reference run ID.
    func markWorkoutCompleted(planId: UUID, workoutId: String, completedRunId: UUID) {
        guard let plan = fetchById(planId) else { return }
        if let index = plan.weeklySchedule.firstIndex(where: { $0.id == workoutId }) {
            plan.weeklySchedule[index].isCompleted = true
            plan.weeklySchedule[index].completedRunId = completedRunId
            try? context.save()
        }
    }
}
