import SwiftUI
import SwiftData

// MARK: - Training Plan ViewModel

@Observable
final class TrainingPlanViewModel {

    // MARK: - State

    var plans: [TrainingPlanInfo] = []
    var activePlan: TrainingPlan?
    var preBuiltPlans: [PreBuiltPlan] = PreBuiltPlan.allPlans
    var isLoading = true
    var errorMessage: String?
    var isGenerating = false
    var selectedWeekNumber: Int = 1
    var weeklyScheduleForActivePlan: [DaySchedule] = []

    // Create plan state
    var showCreatePlanSheet = false
    var newPlanGoalType: GoalType = .improve10K
    var newPlanSelectedDays: Set<Int> = [2, 4, 6, 7] // Tue, Thu, Sat, Sun
    var newPlanWeeklyKm: String = "30"
    var newPlanLongRunKm: String = "12"

    // MARK: - Dependencies

    private var modelContext: ModelContext?

    // MARK: - Init

    init() {}

    func configure(modelContext: ModelContext) {
        self.modelContext = modelContext
        loadData()
    }

    // MARK: - Data Loading

    func loadData() {
        isLoading = true
        errorMessage = nil

        loadPlans()
        loadActivePlan()

        isLoading = false
    }

    func refresh() {
        loadData()
    }

    private func loadPlans() {
        guard let modelContext else { return }

        do {
            let descriptor = FetchDescriptor<TrainingPlan>(
                sortBy: [SortDescriptor(\TrainingPlan.createdAt, order: .reverse)]
            )
            let fetchedPlans = try modelContext.fetch(descriptor)

            let formatter = DateFormatter()
            formatter.dateFormat = "MMM d"

            plans = fetchedPlans.map { plan in
                let weeks = Calendar.current.dateComponents([.weekOfYear], from: plan.startDate, to: plan.endDate).weekOfYear ?? 0
                let completedWorkouts = plan.weeklySchedule.filter(\.isCompleted).count
                let totalWorkouts = plan.weeklySchedule.count

                return TrainingPlanInfo(
                    id: plan.id,
                    name: plan.name,
                    description: plan.planDescription,
                    goalType: plan.goalType,
                    weeks: weeks,
                    startDate: formatter.string(from: plan.startDate),
                    endDate: formatter.string(from: plan.endDate),
                    isActive: plan.isActive,
                    completedWorkouts: completedWorkouts,
                    totalWorkouts: totalWorkouts,
                    targetDistance: plan.targetDistance
                )
            }
        } catch {
            errorMessage = "Failed to load plans: \(error.localizedDescription)"
        }
    }

    private func loadActivePlan() {
        guard let modelContext else { return }

        do {
            let descriptor = FetchDescriptor<TrainingPlan>(
                predicate: #Predicate<TrainingPlan> { $0.isActive }
            )
            activePlan = try modelContext.fetch(descriptor).first
            if let plan = activePlan {
                buildWeeklySchedule(for: plan)
            }
        } catch {
            activePlan = nil
        }
    }

    // MARK: - Weekly Schedule

    private func buildWeeklySchedule(for plan: TrainingPlan) {
        let weekWorkouts = plan.weeklySchedule.filter { $0.weekNumber == selectedWeekNumber }
        let dayNames = ["", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]

        weeklyScheduleForActivePlan = (1...7).map { day in
            let workout = weekWorkouts.first { $0.dayOfWeek == day }
            return DaySchedule(
                dayOfWeek: day,
                dayName: day < dayNames.count ? dayNames[day] : "?",
                workout: workout
            )
        }
    }

    func selectWeek(_ week: Int) {
        selectedWeekNumber = week
        if let plan = activePlan {
            buildWeeklySchedule(for: plan)
        }
    }

    var totalWeeksInActivePlan: Int {
        guard let plan = activePlan else { return 0 }
        return Calendar.current.dateComponents([.weekOfYear], from: plan.startDate, to: plan.endDate).weekOfYear ?? 0
    }

    // MARK: - Plan Management

    func setActivePlan(_ planId: UUID) {
        guard let modelContext else { return }

        do {
            // Deactivate all plans
            let allPlans = try modelContext.fetch(FetchDescriptor<TrainingPlan>())
            for plan in allPlans {
                plan.isActive = false
            }

            // Activate selected plan
            let descriptor = FetchDescriptor<TrainingPlan>(
                predicate: #Predicate<TrainingPlan> { p in p.id == planId }
            )
            if let plan = try modelContext.fetch(descriptor).first {
                plan.isActive = true
            }

            try modelContext.save()
            loadData()
        } catch {
            errorMessage = "Failed to set active plan: \(error.localizedDescription)"
        }
    }

    func deletePlan(_ planId: UUID) {
        guard let modelContext else { return }

        do {
            let descriptor = FetchDescriptor<TrainingPlan>(
                predicate: #Predicate<TrainingPlan> { p in p.id == planId }
            )
            if let plan = try modelContext.fetch(descriptor).first {
                modelContext.delete(plan)
                try modelContext.save()
                loadData()
            }
        } catch {
            errorMessage = "Failed to delete plan: \(error.localizedDescription)"
        }
    }

    // MARK: - Create Plan

    func createPlan() {
        guard let modelContext else { return }
        isGenerating = true

        let goalType = newPlanGoalType
        let selectedDays = Array(newPlanSelectedDays).sorted()
        let weeklyKm = Double(newPlanWeeklyKm) ?? 30
        let longRunKm = Double(newPlanLongRunKm) ?? 12

        let (name, description, targetDistance, weeks) = planDefaults(for: goalType)

        let startDate = Date()
        let endDate = Calendar.current.date(byAdding: .weekOfYear, value: weeks, to: startDate) ?? startDate

        var schedule: [ScheduledWorkout] = []

        for week in 1...weeks {
            let weekMultiplier = weeklyKm * (1.0 + Double(week - 1) * 0.05)

            for (index, day) in selectedDays.enumerated() {
                let workoutType: WorkoutType
                let workoutDescription: String
                var targetDuration: Int?
                var targetDistance: Double?

                if index == selectedDays.count - 1 {
                    // Last day of the week = long run
                    workoutType = .longRun
                    let distance = longRunKm * (1.0 + Double(week - 1) * 0.08)
                    targetDistance = distance * 1000
                    targetDuration = Int(distance / 5.5 * 60) // ~5:30/km
                    workoutDescription = String(format: "Long run: %.1f km at easy pace", distance)
                } else if index == 0 && selectedDays.count >= 3 {
                    // First day = speed work
                    workoutType = week % 2 == 0 ? .tempoRun : .intervalTraining
                    targetDuration = 35 + week
                    workoutDescription = workoutType == .tempoRun
                        ? "Tempo run at threshold pace for \(targetDuration!) minutes"
                        : "Interval session: 6x800m with 400m recovery jog"
                } else if index == 1 && selectedDays.count >= 4 {
                    // Second day = moderate effort
                    workoutType = .easyRun
                    let distance = weekMultiplier * 0.25
                    targetDistance = distance * 1000
                    targetDuration = Int(distance / 5.5 * 60)
                    workoutDescription = String(format: "Easy run: %.1f km at comfortable pace", distance)
                } else {
                    // Recovery / easy run
                    workoutType = .recoveryRun
                    targetDuration = 25 + week
                    workoutDescription = "Recovery run: gentle pace, focus on form"
                }

                let workout = ScheduledWorkout(
                    dayOfWeek: day,
                    weekNumber: week,
                    workoutType: workoutType,
                    targetDistanceMeters: targetDistance,
                    targetDurationMinutes: targetDuration,
                    workoutDescription: workoutDescription
                )
                schedule.append(workout)
            }
        }

        let plan = TrainingPlan(
            name: name,
            planDescription: description,
            goalType: goalType,
            targetDistance: targetDistance,
            startDate: startDate,
            endDate: endDate,
            weeklySchedule: schedule,
            isActive: true
        )

        // Deactivate existing plans
        do {
            let allPlans = try modelContext.fetch(FetchDescriptor<TrainingPlan>())
            for existing in allPlans {
                existing.isActive = false
            }
        } catch {}

        modelContext.insert(plan)

        do {
            try modelContext.save()
            isGenerating = false
            showCreatePlanSheet = false
            loadData()
        } catch {
            errorMessage = "Failed to create plan: \(error.localizedDescription)"
            isGenerating = false
        }
    }

    private func planDefaults(for goalType: GoalType) -> (name: String, description: String, targetDistance: Double?, weeks: Int) {
        switch goalType {
        case .first5K:
            return ("Couch to 5K", "Build up to running 5 kilometers", 5000, 8)
        case .improve5K:
            return ("5K Improvement", "Improve your 5K time with structured training", 5000, 8)
        case .first10K:
            return ("10K Training Plan", "Build endurance for your first 10K", 10000, 10)
        case .improve10K:
            return ("10K Improvement", "Get faster at 10K with speed and endurance work", 10000, 10)
        case .halfMarathon:
            return ("Half Marathon Plan", "Train for 21.1 km with progressive overload", 21097, 14)
        case .marathon:
            return ("Marathon Training", "Full marathon preparation with periodization", 42195, 18)
        case .generalFitness:
            return ("General Fitness", "Improve overall running fitness", nil, 8)
        case .weightLoss:
            return ("Running for Weight Loss", "Consistent running to support weight loss", nil, 12)
        case .custom:
            return ("Custom Plan", "Your personalized training plan", nil, 8)
        }
    }

    // MARK: - Create from Pre-Built

    func createFromPreBuilt(_ preBuilt: PreBuiltPlan) {
        newPlanGoalType = preBuilt.goalType
        showCreatePlanSheet = true
    }

    func markWorkoutComplete(workoutId: String) {
        guard let plan = activePlan else { return }

        if let index = plan.weeklySchedule.firstIndex(where: { $0.id == workoutId }) {
            plan.weeklySchedule[index].isCompleted = true
            guard let modelContext else { return }
            do {
                try modelContext.save()
                loadData()
            } catch {
                errorMessage = "Failed to update workout: \(error.localizedDescription)"
            }
        }
    }
}

// MARK: - Data Models

struct TrainingPlanInfo: Identifiable {
    let id: UUID
    let name: String
    let description: String
    let goalType: GoalType
    let weeks: Int
    let startDate: String
    let endDate: String
    let isActive: Bool
    let completedWorkouts: Int
    let totalWorkouts: Int
    let targetDistance: Double?

    var progress: Double {
        guard totalWorkouts > 0 else { return 0 }
        return Double(completedWorkouts) / Double(totalWorkouts)
    }

    var targetDistanceFormatted: String? {
        targetDistance.map { String(format: "%.0f K", $0 / 1000) }
    }
}

struct DaySchedule: Identifiable {
    let id = UUID()
    let dayOfWeek: Int
    let dayName: String
    let workout: ScheduledWorkout?

    var hasWorkout: Bool { workout != nil }
    var isCompleted: Bool { workout?.isCompleted ?? false }
    var isRestDay: Bool { workout == nil || workout?.workoutType == .restDay }
}

struct PreBuiltPlan: Identifiable {
    let id = UUID()
    let name: String
    let subtitle: String
    let goalType: GoalType
    let weeks: Int
    let daysPerWeek: Int
    let icon: String
    let color: Color

    static let allPlans: [PreBuiltPlan] = [
        PreBuiltPlan(name: "Couch to 5K", subtitle: "Beginner-friendly, 8 weeks", goalType: .first5K, weeks: 8, daysPerWeek: 3, icon: "figure.walk", color: .green),
        PreBuiltPlan(name: "5K Speed", subtitle: "Improve your 5K time", goalType: .improve5K, weeks: 8, daysPerWeek: 4, icon: "bolt.fill", color: .orange),
        PreBuiltPlan(name: "10K Builder", subtitle: "First 10K in 10 weeks", goalType: .first10K, weeks: 10, daysPerWeek: 4, icon: "figure.run", color: .blue),
        PreBuiltPlan(name: "10K PR", subtitle: "Faster 10K with intervals", goalType: .improve10K, weeks: 10, daysPerWeek: 5, icon: "flame.fill", color: .red),
        PreBuiltPlan(name: "Half Marathon", subtitle: "21.1 km in 14 weeks", goalType: .halfMarathon, weeks: 14, daysPerWeek: 4, icon: "trophy.fill", color: .purple),
        PreBuiltPlan(name: "Marathon", subtitle: "Full 42.2 km prep, 18 weeks", goalType: .marathon, weeks: 18, daysPerWeek: 5, icon: "star.fill", color: .yellow),
        PreBuiltPlan(name: "General Fitness", subtitle: "Stay fit with regular runs", goalType: .generalFitness, weeks: 8, daysPerWeek: 3, icon: "heart.fill", color: .pink),
        PreBuiltPlan(name: "Weight Loss Runner", subtitle: "Consistent running for results", goalType: .weightLoss, weeks: 12, daysPerWeek: 4, icon: "scalemass.fill", color: .mint),
    ]
}
