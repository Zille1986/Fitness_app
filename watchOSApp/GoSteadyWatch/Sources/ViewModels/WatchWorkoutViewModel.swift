import Foundation
import Observation
import WatchKit

@Observable
final class WatchWorkoutViewModel {

    // Summary display
    var showSummary = false
    var summaryData: WorkoutSummaryData?

    // HR Zones
    var maxHR: Int = 190
    var currentZone: HRZone = .zone1
    var hrAlert: ZoneAlert = .inZone
    var targetHRMin: Int?
    var targetHRMax: Int?

    // HIIT State
    var hiitTemplate: HIITTemplate?
    var hiitPhase: HIITPhase = .warmup
    var hiitCurrentExerciseIndex: Int = 0
    var hiitCurrentRound: Int = 1
    var hiitRemainingSeconds: Int = 0
    var hiitPhaseDuration: Int = 0
    var hiitTotalElapsed: TimeInterval = 0
    var hiitIsPaused: Bool = false
    var hiitIsComplete: Bool = false
    var hiitCurrentExerciseName: String = "Get Ready"
    var hiitNextExerciseName: String?
    var hiitCaloriesEstimate: Int = 0

    var hiitPhaseProgress: Double {
        guard hiitPhaseDuration > 0 else { return 0 }
        return 1.0 - (Double(hiitRemainingSeconds) / Double(hiitPhaseDuration))
    }

    // MARK: - HR Zone Calculation

    func updateZone(heartRate: Int) {
        for zone in HRZone.allCases.reversed() {
            let range = zone.range(maxHR: maxHR)
            if heartRate >= range.lowerBound {
                currentZone = zone
                break
            }
        }

        if let min = targetHRMin, let max = targetHRMax {
            if heartRate < min {
                hrAlert = .tooLow
            } else if heartRate > max {
                hrAlert = .tooHigh
            } else {
                hrAlert = .inZone
            }
        }
    }

    func setTargetZone(_ zone: HRZone) {
        let range = zone.range(maxHR: maxHR)
        targetHRMin = range.lowerBound
        targetHRMax = range.upperBound
    }

    // MARK: - Summary

    func showSummaryWith(data: WorkoutSummaryData) {
        summaryData = data
        showSummary = true
    }

    func dismissSummary() {
        showSummary = false
        summaryData = nil
    }

    // MARK: - HIIT Logic

    func startHIIT(template: HIITTemplate) {
        hiitTemplate = template
        hiitPhase = .warmup
        hiitCurrentExerciseIndex = 0
        hiitCurrentRound = 1
        hiitRemainingSeconds = template.warmupSeconds
        hiitPhaseDuration = template.warmupSeconds
        hiitTotalElapsed = 0
        hiitIsPaused = false
        hiitIsComplete = false
        hiitCurrentExerciseName = "Warm Up"
        hiitNextExerciseName = template.exercises.first
        hiitCaloriesEstimate = 0
    }

    func hiitTick() {
        guard !hiitIsPaused, !hiitIsComplete else { return }
        hiitRemainingSeconds -= 1
        hiitTotalElapsed += 1

        // Estimate calories
        let calPerSec: Double
        switch hiitPhase {
        case .work: calPerSec = 10.0 / 60.0
        case .rest: calPerSec = 4.0 / 60.0
        default: calPerSec = 3.0 / 60.0
        }
        hiitCaloriesEstimate = max(hiitCaloriesEstimate, Int(hiitTotalElapsed * calPerSec))

        // Countdown haptics
        if hiitRemainingSeconds <= 3 && hiitRemainingSeconds > 0 {
            WKInterfaceDevice.current().play(.click)
        }

        if hiitRemainingSeconds <= 0 {
            advanceHIITPhase()
        }
    }

    func toggleHIITPause() {
        hiitIsPaused.toggle()
    }

    private func advanceHIITPhase() {
        guard let template = hiitTemplate else {
            hiitIsComplete = true
            return
        }

        switch hiitPhase {
        case .warmup:
            hiitPhase = .work
            hiitCurrentExerciseIndex = 0
            let duration = template.workSeconds
            hiitRemainingSeconds = duration
            hiitPhaseDuration = duration
            hiitCurrentExerciseName = template.exercises[0]
            hiitNextExerciseName = template.exercises.count > 1 ? template.exercises[1] : nil
            WKInterfaceDevice.current().play(.start)

        case .work:
            let nextIndex = hiitCurrentExerciseIndex + 1
            if nextIndex < template.exercises.count {
                // Rest between exercises
                hiitPhase = .rest
                hiitRemainingSeconds = template.restSeconds
                hiitPhaseDuration = template.restSeconds
                hiitCurrentExerciseName = "Rest"
                hiitNextExerciseName = template.exercises[nextIndex]
                WKInterfaceDevice.current().play(.stop)
            } else if hiitCurrentRound < template.rounds {
                // Rest between rounds
                hiitPhase = .rest
                hiitRemainingSeconds = template.restSeconds
                hiitPhaseDuration = template.restSeconds
                hiitCurrentExerciseName = "Rest"
                hiitNextExerciseName = template.exercises[0]
                WKInterfaceDevice.current().play(.stop)
            } else {
                // Cooldown
                hiitPhase = .cooldown
                hiitRemainingSeconds = template.cooldownSeconds
                hiitPhaseDuration = template.cooldownSeconds
                hiitCurrentExerciseName = "Cool Down"
                hiitNextExerciseName = nil
                WKInterfaceDevice.current().play(.notification)
            }

        case .rest:
            let nextIndex = hiitCurrentExerciseIndex + 1
            let (newIndex, newRound): (Int, Int)
            if nextIndex < template.exercises.count {
                newIndex = nextIndex
                newRound = hiitCurrentRound
            } else {
                newIndex = 0
                newRound = hiitCurrentRound + 1
            }

            hiitPhase = .work
            hiitCurrentExerciseIndex = newIndex
            hiitCurrentRound = newRound
            hiitRemainingSeconds = template.workSeconds
            hiitPhaseDuration = template.workSeconds
            hiitCurrentExerciseName = template.exercises[newIndex]
            hiitNextExerciseName = (newIndex + 1 < template.exercises.count) ? template.exercises[newIndex + 1] : nil
            WKInterfaceDevice.current().play(.start)

        case .cooldown:
            hiitPhase = .complete
            hiitIsComplete = true
            hiitRemainingSeconds = 0
            WKInterfaceDevice.current().play(.success)

        case .complete:
            break
        }
    }

    func buildHIITSessionData() -> [String: Any] {
        [
            "templateId": hiitTemplate?.id ?? "",
            "templateName": hiitTemplate?.name ?? "",
            "totalDurationMs": Int(hiitTotalElapsed * 1000),
            "exerciseCount": hiitTemplate?.exercises.count ?? 0,
            "roundsCompleted": hiitCurrentRound,
            "totalRounds": hiitTemplate?.rounds ?? 0,
            "caloriesEstimate": hiitCaloriesEstimate,
            "isCompleted": hiitIsComplete,
            "source": "watch"
        ]
    }
}
