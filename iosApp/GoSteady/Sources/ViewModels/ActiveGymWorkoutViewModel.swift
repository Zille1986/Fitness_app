import SwiftUI
import SwiftData
import Combine

// MARK: - Active Gym Workout ViewModel

@Observable
final class ActiveGymWorkoutViewModel {

    // MARK: - State

    var workout: GymWorkout?
    var elapsedSeconds: TimeInterval = 0
    var isRestTimerActive = false
    var restTimeRemaining: Int = 0
    var isLoading = true
    var isFinished = false
    var showAddExercise = false
    var exercisePBs: [UUID: ExercisePBInfo] = [:]
    var exerciseLastWorkouts: [UUID: LastWorkoutInfo] = [:]

    var elapsedFormatted: String {
        let total = Int(elapsedSeconds)
        let hours = total / 3600
        let minutes = (total % 3600) / 60
        let seconds = total % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }

    var restTimeFormatted: String {
        let minutes = restTimeRemaining / 60
        let seconds = restTimeRemaining % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    var completedSetsCount: Int {
        workout?.exercises.reduce(0) { $0 + $1.completedSets } ?? 0
    }

    var totalVolumeFormatted: String {
        let vol = workout?.exercises.reduce(0.0) { $0 + $1.totalVolume } ?? 0
        return String(format: "%.0f kg", vol)
    }

    // MARK: - Private

    private var modelContext: ModelContext?
    private var timerCancellable: AnyCancellable?
    private var restTimerCancellable: AnyCancellable?

    // MARK: - Init

    init() {}

    func configure(modelContext: ModelContext) {
        self.modelContext = modelContext
    }

    // MARK: - Create Workout from Template

    func createWorkoutFromTemplate(templateId: UUID) {
        guard let modelContext else { return }
        isLoading = true

        do {
            let descriptor = FetchDescriptor<WorkoutTemplate>(
                predicate: #Predicate<WorkoutTemplate> { t in t.id == templateId }
            )
            guard let template = try modelContext.fetch(descriptor).first else {
                isLoading = false
                return
            }

            let exercises = template.exercises.sorted(by: { $0.orderIndex < $1.orderIndex }).map { templateExercise in
                WorkoutExercise(
                    exerciseId: templateExercise.exerciseId,
                    exerciseName: templateExercise.exerciseName,
                    sets: (1...templateExercise.sets).map { setNum in
                        WorkoutSet(
                            setNumber: setNum,
                            targetReps: templateExercise.targetRepsMin
                        )
                    },
                    restSeconds: templateExercise.restSeconds,
                    orderIndex: templateExercise.orderIndex
                )
            }

            let newWorkout = GymWorkout(
                name: template.name,
                startTime: Date(),
                exercises: exercises,
                templateId: template.id
            )

            modelContext.insert(newWorkout)
            try modelContext.save()

            // Update template usage
            template.lastUsed = Date()
            template.timesUsed += 1
            try modelContext.save()

            workout = newWorkout
            isLoading = false

            startTimer()
            loadExerciseHistory()
        } catch {
            isLoading = false
        }
    }

    // MARK: - Create Blank Workout

    func createBlankWorkout(name: String = "Quick Workout") {
        guard let modelContext else { return }

        let newWorkout = GymWorkout(
            name: name,
            startTime: Date()
        )

        modelContext.insert(newWorkout)
        do {
            try modelContext.save()
            workout = newWorkout
            isLoading = false
            startTimer()
        } catch {
            isLoading = false
        }
    }

    // MARK: - Timer

    private func startTimer() {
        timerCancellable = Timer.publish(every: 1.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self, let workout = self.workout else { return }
                self.elapsedSeconds = Date().timeIntervalSince(workout.startTime)
            }
    }

    // MARK: - Exercise Management

    func addExercise(_ exercise: Exercise) {
        guard var currentWorkout = workout else { return }

        let workoutExercise = WorkoutExercise(
            exerciseId: exercise.id,
            exerciseName: exercise.name,
            sets: [WorkoutSet(setNumber: 1)],
            restSeconds: 90,
            orderIndex: currentWorkout.exercises.count,
            videoFileName: exercise.videoFileName
        )

        currentWorkout.exercises.append(workoutExercise)
        workout = currentWorkout
        saveWorkout()
        loadExerciseHistoryFor(exerciseId: exercise.id)
    }

    func removeExercise(at index: Int) {
        guard var currentWorkout = workout else { return }
        guard index < currentWorkout.exercises.count else { return }

        currentWorkout.exercises.remove(at: index)
        for i in currentWorkout.exercises.indices {
            currentWorkout.exercises[i].orderIndex = i
        }
        workout = currentWorkout
        saveWorkout()
    }

    // MARK: - Set Management

    func addSet(exerciseIndex: Int) {
        guard var currentWorkout = workout else { return }
        guard exerciseIndex < currentWorkout.exercises.count else { return }

        let exercise = currentWorkout.exercises[exerciseIndex]
        let lastSet = exercise.sets.last
        let newSet = WorkoutSet(
            setNumber: exercise.sets.count + 1,
            weight: lastSet?.weight ?? 0,
            targetReps: lastSet?.targetReps ?? 10
        )

        currentWorkout.exercises[exerciseIndex].sets.append(newSet)
        workout = currentWorkout
        saveWorkout()
    }

    func removeSet(exerciseIndex: Int, setIndex: Int) {
        guard var currentWorkout = workout else { return }
        guard exerciseIndex < currentWorkout.exercises.count else { return }
        guard currentWorkout.exercises[exerciseIndex].sets.count > 1 else { return }

        currentWorkout.exercises[exerciseIndex].sets.remove(at: setIndex)
        for i in currentWorkout.exercises[exerciseIndex].sets.indices {
            currentWorkout.exercises[exerciseIndex].sets[i].setNumber = i + 1
        }
        workout = currentWorkout
        saveWorkout()
    }

    func updateSet(exerciseIndex: Int, setIndex: Int, weight: Double?, reps: Int?) {
        guard var currentWorkout = workout else { return }
        guard exerciseIndex < currentWorkout.exercises.count else { return }
        guard setIndex < currentWorkout.exercises[exerciseIndex].sets.count else { return }

        if let weight {
            currentWorkout.exercises[exerciseIndex].sets[setIndex].weight = weight
        }
        if let reps {
            currentWorkout.exercises[exerciseIndex].sets[setIndex].reps = reps
        }
        workout = currentWorkout
        saveWorkout()
    }

    func completeSet(exerciseIndex: Int, setIndex: Int) {
        guard var currentWorkout = workout else { return }
        guard exerciseIndex < currentWorkout.exercises.count else { return }
        guard setIndex < currentWorkout.exercises[exerciseIndex].sets.count else { return }

        currentWorkout.exercises[exerciseIndex].sets[setIndex].isCompleted = true
        currentWorkout.exercises[exerciseIndex].sets[setIndex].completedAt = Date()
        workout = currentWorkout
        saveWorkout()

        // Start rest timer
        let restSeconds = currentWorkout.exercises[exerciseIndex].restSeconds
        startRestTimer(seconds: restSeconds)
    }

    // MARK: - Rest Timer

    func startRestTimer(seconds: Int) {
        restTimerCancellable?.cancel()
        restTimeRemaining = seconds
        isRestTimerActive = true

        restTimerCancellable = Timer.publish(every: 1.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self else { return }
                if self.restTimeRemaining > 0 {
                    self.restTimeRemaining -= 1
                } else {
                    self.isRestTimerActive = false
                    self.restTimerCancellable?.cancel()
                }
            }
    }

    func skipRestTimer() {
        restTimerCancellable?.cancel()
        isRestTimerActive = false
        restTimeRemaining = 0
    }

    func addRestTime(_ seconds: Int) {
        restTimeRemaining += seconds
    }

    // MARK: - Finish / Cancel

    func finishWorkout() {
        guard var currentWorkout = workout else {
            isFinished = true
            return
        }

        let totalVolume = currentWorkout.exercises.reduce(0.0) { $0 + $1.totalVolume }
        let totalSets = currentWorkout.exercises.reduce(0) { $0 + $1.completedSets }
        let totalReps = currentWorkout.exercises.reduce(0) { $0 + $1.totalReps }

        currentWorkout.endTime = Date()
        currentWorkout.isCompleted = true
        currentWorkout.totalVolume = totalVolume
        currentWorkout.totalSets = totalSets
        currentWorkout.totalReps = totalReps

        workout = currentWorkout
        saveWorkout()
        recordExerciseHistory()
        checkPersonalBests()

        timerCancellable?.cancel()
        restTimerCancellable?.cancel()
        isFinished = true
    }

    func cancelWorkout() {
        guard let modelContext, let workout else {
            isFinished = true
            return
        }

        modelContext.delete(workout)
        do {
            try modelContext.save()
        } catch {
            print("Failed to delete workout: \(error)")
        }

        timerCancellable?.cancel()
        restTimerCancellable?.cancel()
        isFinished = true
    }

    // MARK: - Persistence

    private func saveWorkout() {
        guard let modelContext else { return }
        do {
            try modelContext.save()
        } catch {
            print("Failed to save workout: \(error)")
        }
    }

    // MARK: - Exercise History

    private func loadExerciseHistory() {
        guard let workout else { return }
        for exercise in workout.exercises {
            loadExerciseHistoryFor(exerciseId: exercise.exerciseId)
        }
    }

    private func loadExerciseHistoryFor(exerciseId: UUID) {
        guard let modelContext else { return }

        do {
            // Load PBs
            let pbDescriptor = FetchDescriptor<PersonalBest>(
                predicate: #Predicate<PersonalBest> { pb in pb.exerciseId == exerciseId }
            )
            if let pb = try modelContext.fetch(pbDescriptor).first {
                exercisePBs[exerciseId] = ExercisePBInfo(
                    bestWeight: pb.bestWeight,
                    bestReps: pb.bestReps,
                    estimatedOneRepMax: pb.estimatedOneRepMax,
                    achievedDate: pb.achievedDate
                )
            }

            // Load last workout
            var histDescriptor = FetchDescriptor<ExerciseHistory>(
                predicate: #Predicate<ExerciseHistory> { h in h.exerciseId == exerciseId },
                sortBy: [SortDescriptor(\ExerciseHistory.date, order: .reverse)]
            )
            histDescriptor.fetchLimit = 1

            if let lastHistory = try modelContext.fetch(histDescriptor).first {
                exerciseLastWorkouts[exerciseId] = LastWorkoutInfo(
                    bestWeight: lastHistory.bestWeight,
                    bestReps: lastHistory.bestReps,
                    date: lastHistory.date
                )
            }
        } catch {
            print("Failed to load exercise history for \(exerciseId): \(error)")
        }
    }

    private func recordExerciseHistory() {
        guard let modelContext, let workout else { return }

        for exercise in workout.exercises {
            let completedSets = exercise.sets.filter(\.isCompleted)
            guard !completedSets.isEmpty else { continue }

            let bestSet = completedSets.max(by: { $0.weight < $1.weight })
            let totalVolume = exercise.totalVolume
            let bestWeight = bestSet?.weight ?? 0
            let bestReps = bestSet?.reps ?? 0

            // Epley formula: 1RM = weight * (1 + reps / 30)
            let oneRepMax = bestWeight > 0 && bestReps > 0
                ? bestWeight * (1.0 + Double(bestReps) / 30.0)
                : 0.0

            let totalReps = completedSets.reduce(0) { $0 + $1.reps }

            let history = ExerciseHistory(
                exerciseId: exercise.exerciseId,
                workoutId: workout.id,
                date: Date(),
                bestWeight: bestWeight,
                bestReps: bestReps,
                totalVolume: totalVolume,
                totalSets: completedSets.count,
                totalReps: totalReps,
                estimatedOneRepMax: oneRepMax
            )

            modelContext.insert(history)
        }

        do {
            try modelContext.save()
        } catch {
            print("Failed to record exercise history: \(error)")
        }
    }

    private func checkPersonalBests() {
        guard let modelContext, let workout else { return }

        for exercise in workout.exercises {
            let completedSets = exercise.sets.filter(\.isCompleted)
            guard !completedSets.isEmpty else { continue }

            let bestSet = completedSets.max(by: { $0.weight < $1.weight })
            guard let bestWeight = bestSet?.weight, let bestReps = bestSet?.reps, bestWeight > 0 else { continue }

            let oneRepMax = bestWeight * (1.0 + Double(bestReps) / 30.0)

            do {
                let exerciseId = exercise.exerciseId
                let pbDescriptor = FetchDescriptor<PersonalBest>(
                    predicate: #Predicate<PersonalBest> { pb in pb.exerciseId == exerciseId }
                )
                let existingPB = try modelContext.fetch(pbDescriptor).first

                if let existing = existingPB {
                    if oneRepMax > existing.estimatedOneRepMax {
                        existing.bestWeight = bestWeight
                        existing.bestReps = bestReps
                        existing.estimatedOneRepMax = oneRepMax
                        existing.achievedDate = Date()
                    }
                } else {
                    let newPB = PersonalBest(
                        exerciseId: exerciseId,
                        exerciseName: exercise.exerciseName,
                        bestWeight: bestWeight,
                        bestReps: bestReps,
                        estimatedOneRepMax: oneRepMax,
                        achievedDate: Date()
                    )
                    modelContext.insert(newPB)
                }

                try modelContext.save()
            } catch {
                print("Failed to check PB for \(exercise.exerciseName): \(error)")
            }
        }
    }

    // MARK: - 1RM Calculator

    static func calculateOneRepMax(weight: Double, reps: Int) -> Double {
        guard weight > 0, reps > 0 else { return 0 }
        return weight * (1.0 + Double(reps) / 30.0)
    }

    static func oneRepMaxPercentage(weight: Double, oneRepMax: Double) -> Int? {
        guard oneRepMax > 0, weight > 0 else { return nil }
        return Int((weight / oneRepMax) * 100)
    }
}

// MARK: - Supporting Types

struct ExercisePBInfo {
    let bestWeight: Double
    let bestReps: Int
    let estimatedOneRepMax: Double
    let achievedDate: Date

    var bestWeightFormatted: String {
        "\(bestWeight)kg x \(bestReps)"
    }

    var oneRepMaxFormatted: String {
        String(format: "%.1f kg", estimatedOneRepMax)
    }
}

struct LastWorkoutInfo {
    let bestWeight: Double
    let bestReps: Int
    let date: Date
}
