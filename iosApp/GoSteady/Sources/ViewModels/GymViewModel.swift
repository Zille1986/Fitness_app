import SwiftUI
import SwiftData

// MARK: - Gym ViewModel

@Observable
final class GymViewModel {

    // MARK: - State

    var recentWorkouts: [RecentWorkoutInfo] = []
    var templates: [TemplateInfo] = []
    var weeklyVolume: [Float] = Array(repeating: 0, count: 7)
    var totalWeeklyVolume: Float = 0
    var totalWeeklySets: Int = 0
    var nextWorkout: NextWorkoutInfo?
    var muscleGroupStats: [MuscleGroupStat] = []
    var isLoading = true
    var errorMessage: String?
    var totalWorkoutCount: Int = 0

    // Exercise library
    var allExercises: [Exercise] = []
    var filteredExercises: [Exercise] = []
    var exerciseSearchQuery: String = "" {
        didSet { filterExercises() }
    }
    var selectedMuscleGroupFilter: MuscleGroup? {
        didSet { filterExercises() }
    }
    var selectedEquipmentFilter: Equipment? {
        didSet { filterExercises() }
    }

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

        loadRecentWorkouts()
        loadTemplates()
        loadWeeklyStats()
        loadNextWorkout()
        loadExercises()
        loadMuscleGroupStats()

        isLoading = false
    }

    func refresh() {
        loadData()
    }

    private func loadRecentWorkouts() {
        guard let modelContext else { return }

        do {
            var descriptor = FetchDescriptor<GymWorkout>(
                predicate: #Predicate<GymWorkout> { $0.isCompleted },
                sortBy: [SortDescriptor(\GymWorkout.startTime, order: .reverse)]
            )
            descriptor.fetchLimit = 10

            let workouts = try modelContext.fetch(descriptor)
            totalWorkoutCount = workouts.count

            let formatter = DateFormatter()
            formatter.dateFormat = "MMM d"

            recentWorkouts = workouts.map { workout in
                let totalSets = workout.exercises.reduce(0) { $0 + $1.sets.count }
                return RecentWorkoutInfo(
                    id: workout.id,
                    name: workout.name,
                    date: formatter.string(from: workout.startTime),
                    exerciseCount: workout.exercises.count,
                    totalSets: totalSets,
                    duration: workout.durationFormatted,
                    volume: workout.totalVolume
                )
            }
        } catch {
            errorMessage = "Failed to load workouts: \(error.localizedDescription)"
        }
    }

    private func loadTemplates() {
        guard let modelContext else { return }

        do {
            let descriptor = FetchDescriptor<WorkoutTemplate>(
                sortBy: [SortDescriptor(\WorkoutTemplate.name)]
            )
            let fetchedTemplates = try modelContext.fetch(descriptor)

            templates = fetchedTemplates.map { template in
                TemplateInfo(
                    id: template.id,
                    name: template.name,
                    templateDescription: template.templateDescription,
                    exerciseCount: template.exercises.count,
                    estimatedDuration: template.estimatedDurationMinutes,
                    lastUsed: template.lastUsed,
                    usageCount: template.timesUsed
                )
            }
        } catch {
            errorMessage = "Failed to load templates: \(error.localizedDescription)"
        }
    }

    private func loadWeeklyStats() {
        guard let modelContext else { return }

        do {
            let calendar = Calendar.current
            let now = Date()
            guard let weekStart = calendar.date(from: calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: now)) else { return }
            guard let weekEnd = calendar.date(byAdding: .day, value: 7, to: weekStart) else { return }

            let descriptor = FetchDescriptor<GymWorkout>(
                predicate: #Predicate<GymWorkout> { workout in
                    workout.isCompleted && workout.startTime >= weekStart && workout.startTime < weekEnd
                }
            )

            let weeklyWorkouts = try modelContext.fetch(descriptor)

            var dailyVolume = Array(repeating: Float(0), count: 7)
            var sets = 0

            for workout in weeklyWorkouts {
                let dayOfWeek = calendar.component(.weekday, from: workout.startTime)
                let dayIndex = dayOfWeek == 1 ? 6 : dayOfWeek - 2

                var workoutVolume: Double = 0
                for exercise in workout.exercises {
                    for set in exercise.sets where set.isCompleted {
                        workoutVolume += set.weight * Double(set.reps)
                        sets += 1
                    }
                }
                dailyVolume[dayIndex] += Float(workoutVolume)
            }

            weeklyVolume = dailyVolume
            totalWeeklyVolume = dailyVolume.reduce(0, +)
            totalWeeklySets = sets
        } catch {
            errorMessage = "Failed to load weekly stats: \(error.localizedDescription)"
        }
    }

    private func loadNextWorkout() {
        guard let modelContext else { return }

        do {
            let descriptor = FetchDescriptor<WorkoutTemplate>(
                sortBy: [SortDescriptor(\WorkoutTemplate.lastUsed, order: .forward)]
            )
            let fetchedTemplates = try modelContext.fetch(descriptor)

            if let template = fetchedTemplates.first {
                let formatter = RelativeDateTimeFormatter()
                formatter.unitsStyle = .abbreviated

                let lastPerformed: String? = template.lastUsed.map {
                    formatter.localizedString(for: $0, relativeTo: Date())
                }

                nextWorkout = NextWorkoutInfo(
                    templateId: template.id,
                    name: template.name,
                    exerciseCount: template.exercises.count,
                    lastPerformed: lastPerformed,
                    estimatedDuration: template.estimatedDurationMinutes
                )
            }
        } catch {
            // Silently fail
        }
    }

    private func loadExercises() {
        guard let modelContext else { return }

        do {
            let descriptor = FetchDescriptor<Exercise>(
                sortBy: [SortDescriptor(\Exercise.name)]
            )
            allExercises = try modelContext.fetch(descriptor)
            filterExercises()
        } catch {
            errorMessage = "Failed to load exercises: \(error.localizedDescription)"
        }
    }

    private func loadMuscleGroupStats() {
        guard let modelContext else { return }

        do {
            let descriptor = FetchDescriptor<GymWorkout>(
                predicate: #Predicate<GymWorkout> { $0.isCompleted },
                sortBy: [SortDescriptor(\GymWorkout.startTime, order: .reverse)]
            )
            let workouts = try modelContext.fetch(descriptor)

            var muscleVolume: [String: Double] = [:]
            for workout in workouts.prefix(20) {
                for exercise in workout.exercises {
                    let volume = exercise.totalVolume
                    muscleVolume[exercise.exerciseName, default: 0] += volume
                }
            }

            muscleGroupStats = muscleVolume.sorted { $0.value > $1.value }.prefix(6).map {
                MuscleGroupStat(name: $0.key, totalVolume: $0.value)
            }
        } catch {
            // Silently fail
        }
    }

    // MARK: - Exercise Filtering

    private func filterExercises() {
        filteredExercises = allExercises.filter { exercise in
            let matchesMuscle = selectedMuscleGroupFilter == nil ||
                exercise.muscleGroup == selectedMuscleGroupFilter ||
                exercise.secondaryMuscleGroups.contains(selectedMuscleGroupFilter!)

            let matchesEquipment = selectedEquipmentFilter == nil ||
                exercise.equipment == selectedEquipmentFilter

            let matchesQuery = exerciseSearchQuery.isEmpty ||
                exercise.name.localizedCaseInsensitiveContains(exerciseSearchQuery) ||
                exercise.muscleGroup.displayName.localizedCaseInsensitiveContains(exerciseSearchQuery)

            return matchesMuscle && matchesEquipment && matchesQuery
        }
    }

    // MARK: - Template Management

    func deleteTemplate(_ templateId: UUID) {
        guard let modelContext else { return }

        do {
            let descriptor = FetchDescriptor<WorkoutTemplate>(
                predicate: #Predicate<WorkoutTemplate> { t in t.id == templateId }
            )
            if let template = try modelContext.fetch(descriptor).first {
                modelContext.delete(template)
                try modelContext.save()
                loadTemplates()
            }
        } catch {
            errorMessage = "Failed to delete template: \(error.localizedDescription)"
        }
    }

    func deleteWorkout(_ workoutId: UUID) {
        guard let modelContext else { return }

        do {
            let descriptor = FetchDescriptor<GymWorkout>(
                predicate: #Predicate<GymWorkout> { w in w.id == workoutId }
            )
            if let workout = try modelContext.fetch(descriptor).first {
                modelContext.delete(workout)
                try modelContext.save()
                loadData()
            }
        } catch {
            errorMessage = "Failed to delete workout: \(error.localizedDescription)"
        }
    }
}

// MARK: - Data Models

struct RecentWorkoutInfo: Identifiable {
    let id: UUID
    let name: String
    let date: String
    let exerciseCount: Int
    let totalSets: Int
    let duration: String
    let volume: Double
}

struct TemplateInfo: Identifiable {
    let id: UUID
    let name: String
    let templateDescription: String
    let exerciseCount: Int
    let estimatedDuration: Int
    let lastUsed: Date?
    let usageCount: Int
}

struct NextWorkoutInfo {
    let templateId: UUID
    let name: String
    let exerciseCount: Int
    let lastPerformed: String?
    let estimatedDuration: Int
}

struct MuscleGroupStat: Identifiable {
    let id = UUID()
    let name: String
    let totalVolume: Double
}

