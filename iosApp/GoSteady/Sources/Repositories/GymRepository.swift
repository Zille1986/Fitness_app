import Foundation
import SwiftData

final class GymRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Workouts

    func fetchAllWorkouts(sortedBy order: SortOrder = .reverse) -> [GymWorkout] {
        let descriptor = FetchDescriptor<GymWorkout>(sortBy: [SortDescriptor(\.startTime, order: order)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchRecentWorkouts(limit: Int = 10) -> [GymWorkout] {
        var descriptor = FetchDescriptor<GymWorkout>(sortBy: [SortDescriptor(\.startTime, order: .reverse)])
        descriptor.fetchLimit = limit
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchWorkoutById(_ id: UUID) -> GymWorkout? {
        let descriptor = FetchDescriptor<GymWorkout>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    func fetchActiveWorkout() -> GymWorkout? {
        let descriptor = FetchDescriptor<GymWorkout>(predicate: #Predicate { $0.isCompleted == false })
        return try? context.fetch(descriptor).first
    }

    func fetchWorkoutsInRange(start: Date, end: Date) -> [GymWorkout] {
        let descriptor = FetchDescriptor<GymWorkout>(
            predicate: #Predicate { $0.startTime >= start && $0.startTime <= end },
            sortBy: [SortDescriptor(\.startTime, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchCompletedWorkoutsInRange(start: Date, end: Date) -> [GymWorkout] {
        let descriptor = FetchDescriptor<GymWorkout>(
            predicate: #Predicate { $0.startTime >= start && $0.startTime <= end && $0.isCompleted == true },
            sortBy: [SortDescriptor(\.startTime, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func insertWorkout(_ workout: GymWorkout) {
        context.insert(workout)
        try? context.save()
    }

    func updateWorkout(_ workout: GymWorkout) {
        try? context.save()
    }

    func deleteWorkout(_ workout: GymWorkout) {
        context.delete(workout)
        try? context.save()
    }

    // MARK: - Templates

    func fetchAllTemplates() -> [WorkoutTemplate] {
        let descriptor = FetchDescriptor<WorkoutTemplate>(sortBy: [SortDescriptor(\.name)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchDefaultTemplates() -> [WorkoutTemplate] {
        let descriptor = FetchDescriptor<WorkoutTemplate>(
            predicate: #Predicate { $0.isDefault == true },
            sortBy: [SortDescriptor(\.name)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchCustomTemplates() -> [WorkoutTemplate] {
        let descriptor = FetchDescriptor<WorkoutTemplate>(
            predicate: #Predicate { $0.isDefault == false },
            sortBy: [SortDescriptor(\.name)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchTemplateById(_ id: UUID) -> WorkoutTemplate? {
        let descriptor = FetchDescriptor<WorkoutTemplate>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    func insertTemplate(_ template: WorkoutTemplate) {
        context.insert(template)
        try? context.save()
    }

    func updateTemplate(_ template: WorkoutTemplate) {
        try? context.save()
    }

    func deleteTemplate(_ template: WorkoutTemplate) {
        context.delete(template)
        try? context.save()
    }

    func incrementTemplateUsage(_ template: WorkoutTemplate) {
        template.timesUsed += 1
        template.lastUsed = Date()
        try? context.save()
    }

    func templateCount() -> Int {
        let descriptor = FetchDescriptor<WorkoutTemplate>()
        return (try? context.fetchCount(descriptor)) ?? 0
    }

    // MARK: - Exercise History

    func fetchHistoryForExercise(_ exerciseId: UUID) -> [ExerciseHistory] {
        let descriptor = FetchDescriptor<ExerciseHistory>(
            predicate: #Predicate { $0.exerciseId == exerciseId },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchRecentHistoryForExercise(_ exerciseId: UUID, limit: Int = 10) -> [ExerciseHistory] {
        var descriptor = FetchDescriptor<ExerciseHistory>(
            predicate: #Predicate { $0.exerciseId == exerciseId },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        descriptor.fetchLimit = limit
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchBestOneRepMax(exerciseId: UUID) -> ExerciseHistory? {
        let descriptor = FetchDescriptor<ExerciseHistory>(
            predicate: #Predicate { $0.exerciseId == exerciseId },
            sortBy: [SortDescriptor(\.estimatedOneRepMax, order: .reverse)]
        )
        return try? context.fetch(descriptor).first
    }

    func fetchBestWeight(exerciseId: UUID) -> ExerciseHistory? {
        let descriptor = FetchDescriptor<ExerciseHistory>(
            predicate: #Predicate { $0.exerciseId == exerciseId },
            sortBy: [SortDescriptor(\.bestWeight, order: .reverse)]
        )
        return try? context.fetch(descriptor).first
    }

    func fetchPersonalRecords() -> [ExerciseHistory] {
        let descriptor = FetchDescriptor<ExerciseHistory>(
            predicate: #Predicate { $0.isPersonalRecord == true },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func insertHistory(_ history: ExerciseHistory) {
        context.insert(history)
        try? context.save()
    }

    func updateHistory(_ history: ExerciseHistory) {
        try? context.save()
    }

    /// Record exercise history from completed sets and detect personal records.
    func recordExerciseHistory(
        exerciseId: UUID,
        workoutId: UUID,
        sets: [WorkoutSet]
    ) -> GymPBResult? {
        let completed = sets.filter(\.isCompleted)
        guard !completed.isEmpty else { return nil }

        let bestSet = completed.max(by: { ($0.weight * Double($0.reps)) < ($1.weight * Double($1.reps)) })!
        let totalVolume = completed.reduce(0.0) { $0 + $1.weight * Double($1.reps) }
        let totalReps = completed.reduce(0) { $0 + $1.reps }
        let estimated1RM = OneRepMaxCalculator.calculate(weight: bestSet.weight, reps: bestSet.reps)

        let previousBest = fetchBestOneRepMax(exerciseId: exerciseId)
        let isPR = previousBest == nil || estimated1RM > (previousBest?.estimatedOneRepMax ?? 0)

        let history = ExerciseHistory(
            exerciseId: exerciseId,
            workoutId: workoutId,
            date: Date(),
            bestWeight: bestSet.weight,
            bestReps: bestSet.reps,
            totalVolume: totalVolume,
            totalSets: completed.count,
            totalReps: totalReps,
            estimatedOneRepMax: estimated1RM,
            isPersonalRecord: isPR
        )
        insertHistory(history)

        return isPR ? GymPBResult(
            exerciseId: exerciseId,
            weight: bestSet.weight,
            reps: bestSet.reps,
            estimatedOneRepMax: estimated1RM
        ) : nil
    }

    // MARK: - Progression

    func progressionSuggestion(
        exerciseId: UUID,
        exerciseName: String,
        targetReps: ClosedRange<Int> = 8...12
    ) -> ProgressionSuggestion {
        let history = fetchRecentHistoryForExercise(exerciseId, limit: 5)
        return ProgressionEngine.suggestProgression(
            exerciseId: exerciseId,
            exerciseName: exerciseName,
            recentHistory: history,
            targetReps: targetReps
        )
    }

    // MARK: - Weekly Stats

    func weeklyGymStats() -> GymWeeklyStats {
        let range = Date.thisWeekRange
        let workouts = fetchCompletedWorkoutsInRange(start: range.start, end: range.end)
        let totalVolume = workouts.reduce(0.0) { $0 + $1.totalVolume }
        return GymWeeklyStats(
            weekStartDate: range.start,
            totalVolume: totalVolume,
            workoutCount: workouts.count
        )
    }
}

struct GymPBResult {
    let exerciseId: UUID
    let weight: Double
    let reps: Int
    let estimatedOneRepMax: Double
}

struct GymWeeklyStats {
    let weekStartDate: Date
    let totalVolume: Double
    let workoutCount: Int

    var totalVolumeFormatted: String {
        FormatUtils.formatVolume(totalVolume)
    }
}
