import Foundation
import Observation
import WatchKit

@Observable
final class WatchGymViewModel {

    var exercises: [GymExercise] = defaultExercises
    var currentExerciseIndex: Int = 0
    var currentSet: Int = 1
    var restTimerSeconds: Int = 0
    var restTimerRunning: Bool = false
    var defaultRestDuration: Int = 90
    var isWorkoutStarted: Bool = false

    var currentExercise: GymExercise? {
        guard currentExerciseIndex < exercises.count else { return nil }
        return exercises[currentExerciseIndex]
    }

    var completedSetsTotal: Int {
        exercises.reduce(0) { $0 + $1.completedSets }
    }

    var totalSetsTotal: Int {
        exercises.reduce(0) { $0 + $1.targetSets }
    }

    private var restTimer: Timer?

    // MARK: - Actions

    func startWorkout() {
        isWorkoutStarted = true
        currentExerciseIndex = 0
        currentSet = 1
        for i in exercises.indices {
            exercises[i].completedSets = 0
        }
    }

    func logSet() {
        guard currentExerciseIndex < exercises.count else { return }
        exercises[currentExerciseIndex].completedSets += 1
        currentSet = exercises[currentExerciseIndex].completedSets + 1
        WKInterfaceDevice.current().play(.click)

        // Auto-advance if all sets done
        if exercises[currentExerciseIndex].completedSets >= exercises[currentExerciseIndex].targetSets {
            startRestTimer()
            // Move to next exercise after rest
        } else {
            startRestTimer()
        }
    }

    func nextExercise() {
        stopRestTimer()
        if currentExerciseIndex < exercises.count - 1 {
            currentExerciseIndex += 1
            currentSet = exercises[currentExerciseIndex].completedSets + 1
            WKInterfaceDevice.current().play(.directionUp)
        }
    }

    func previousExercise() {
        stopRestTimer()
        if currentExerciseIndex > 0 {
            currentExerciseIndex -= 1
            currentSet = exercises[currentExerciseIndex].completedSets + 1
            WKInterfaceDevice.current().play(.directionDown)
        }
    }

    func adjustWeight(by delta: Double) {
        guard currentExerciseIndex < exercises.count else { return }
        exercises[currentExerciseIndex].weight = max(0, exercises[currentExerciseIndex].weight + delta)
    }

    func adjustReps(by delta: Int) {
        guard currentExerciseIndex < exercises.count else { return }
        exercises[currentExerciseIndex].targetReps = max(1, exercises[currentExerciseIndex].targetReps + delta)
    }

    // MARK: - Rest Timer

    func startRestTimer() {
        restTimerSeconds = defaultRestDuration
        restTimerRunning = true
        restTimer?.invalidate()
        restTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self else { return }
            if self.restTimerSeconds > 0 {
                self.restTimerSeconds -= 1
                if self.restTimerSeconds == 3 || self.restTimerSeconds == 2 || self.restTimerSeconds == 1 {
                    WKInterfaceDevice.current().play(.click)
                }
            } else {
                self.stopRestTimer()
                WKInterfaceDevice.current().play(.notification)
                // Auto-advance to next exercise if current is complete
                if let ex = self.currentExercise, ex.completedSets >= ex.targetSets {
                    self.nextExercise()
                }
            }
        }
    }

    func stopRestTimer() {
        restTimer?.invalidate()
        restTimer = nil
        restTimerRunning = false
        restTimerSeconds = 0
    }

    func skipRest() {
        stopRestTimer()
        if let ex = currentExercise, ex.completedSets >= ex.targetSets {
            nextExercise()
        }
    }

    func resetWorkout() {
        stopRestTimer()
        isWorkoutStarted = false
        currentExerciseIndex = 0
        currentSet = 1
        exercises = Self.defaultExercises
    }

    // MARK: - Default Exercises

    static let defaultExercises: [GymExercise] = [
        GymExercise(name: "Bench Press", targetSets: 4, targetReps: 8, weight: 60),
        GymExercise(name: "Squat", targetSets: 4, targetReps: 8, weight: 80),
        GymExercise(name: "Deadlift", targetSets: 3, targetReps: 5, weight: 100),
        GymExercise(name: "Overhead Press", targetSets: 3, targetReps: 10, weight: 40),
        GymExercise(name: "Barbell Row", targetSets: 3, targetReps: 10, weight: 50),
        GymExercise(name: "Pull Ups", targetSets: 3, targetReps: 8, weight: 0),
        GymExercise(name: "Dips", targetSets: 3, targetReps: 10, weight: 0),
        GymExercise(name: "Lunges", targetSets: 3, targetReps: 12, weight: 20)
    ]

    deinit {
        restTimer?.invalidate()
    }
}
