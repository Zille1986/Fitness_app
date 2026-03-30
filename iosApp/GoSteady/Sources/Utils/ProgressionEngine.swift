import Foundation
import SwiftData

struct ProgressionEngine {

    private static let minSessionsForProgression = 2
    private static let weightIncrementKg = 2.5
    private static let weightIncrementLb = 5.0

    // MARK: - Suggest Progression

    static func suggestProgression(
        exerciseId: UUID,
        exerciseName: String,
        recentHistory: [ExerciseHistory],
        targetReps: ClosedRange<Int> = 8...12,
        useMetric: Bool = true
    ) -> ProgressionSuggestion {
        let increment = useMetric ? weightIncrementKg : weightIncrementLb

        guard !recentHistory.isEmpty else {
            return ProgressionSuggestion(
                exerciseId: exerciseId,
                exerciseName: exerciseName,
                suggestionType: .maintain,
                currentWeight: 0,
                currentReps: targetReps.lowerBound,
                suggestedWeight: 0,
                suggestedReps: targetReps.lowerBound,
                confidence: 0,
                reasoning: "No history available. Start with a comfortable weight."
            )
        }

        let lastSession = recentHistory[0]

        guard recentHistory.count >= minSessionsForProgression else {
            return ProgressionSuggestion(
                exerciseId: exerciseId,
                exerciseName: exerciseName,
                suggestionType: .maintain,
                currentWeight: lastSession.bestWeight,
                currentReps: lastSession.bestReps,
                suggestedWeight: lastSession.bestWeight,
                suggestedReps: lastSession.bestReps,
                confidence: 0.5,
                reasoning: "Need more sessions to make a confident suggestion. Keep current weight."
            )
        }

        // Analyze recent performance
        let recent3 = Array(recentHistory.prefix(3))
        let avgReps = Double(recent3.map(\.bestReps).reduce(0, +)) / Double(recent3.count)

        // Check for declining performance (deload needed)
        if recentHistory.count >= 3 {
            let declining = recentHistory[0].estimatedOneRepMax < recentHistory[1].estimatedOneRepMax &&
                            recentHistory[1].estimatedOneRepMax < recentHistory[2].estimatedOneRepMax
            if declining {
                return ProgressionSuggestion(
                    exerciseId: exerciseId,
                    exerciseName: exerciseName,
                    suggestionType: .deload,
                    currentWeight: lastSession.bestWeight,
                    currentReps: lastSession.bestReps,
                    suggestedWeight: lastSession.bestWeight * 0.9,
                    suggestedReps: targetReps.upperBound,
                    confidence: 0.8,
                    reasoning: "Performance declining. Consider a deload week at 90% weight."
                )
            }
        }

        // If hitting top of rep range consistently, increase weight
        if avgReps >= Double(targetReps.upperBound - 1) {
            return ProgressionSuggestion(
                exerciseId: exerciseId,
                exerciseName: exerciseName,
                suggestionType: .increaseWeight,
                currentWeight: lastSession.bestWeight,
                currentReps: lastSession.bestReps,
                suggestedWeight: lastSession.bestWeight + increment,
                suggestedReps: targetReps.lowerBound,
                confidence: 0.85,
                reasoning: "Consistently hitting \(targetReps.upperBound) reps. Time to increase weight by \(FormatUtils.formatWeight(increment, useMetric: useMetric))."
            )
        }

        // If in middle of rep range, increase reps
        if avgReps >= Double(targetReps.lowerBound) && avgReps < Double(targetReps.upperBound - 1) {
            return ProgressionSuggestion(
                exerciseId: exerciseId,
                exerciseName: exerciseName,
                suggestionType: .increaseReps,
                currentWeight: lastSession.bestWeight,
                currentReps: lastSession.bestReps,
                suggestedWeight: lastSession.bestWeight,
                suggestedReps: lastSession.bestReps + 1,
                confidence: 0.75,
                reasoning: "Good progress! Try to add 1 more rep this session."
            )
        }

        // Below rep range, maintain
        return ProgressionSuggestion(
            exerciseId: exerciseId,
            exerciseName: exerciseName,
            suggestionType: .maintain,
            currentWeight: lastSession.bestWeight,
            currentReps: lastSession.bestReps,
            suggestedWeight: lastSession.bestWeight,
            suggestedReps: targetReps.lowerBound,
            confidence: 0.7,
            reasoning: "Focus on hitting \(targetReps.lowerBound) reps before progressing."
        )
    }

    // MARK: - Starting Weight Suggestion

    static func suggestStartingWeight(
        oneRepMax: Double?,
        targetReps: Int,
        useMetric: Bool = true
    ) -> Double {
        guard let orm = oneRepMax, orm > 0 else { return 0 }
        let percentage = OneRepMaxCalculator.percentageOfMax(forReps: targetReps)
        let rawWeight = orm * percentage * 0.9 // Start conservative
        let increment = useMetric ? weightIncrementKg : weightIncrementLb
        return (rawWeight / increment).rounded(.down) * increment
    }

    // MARK: - Detect Deload Need

    /// Returns true if the last 3+ sessions show declining estimated 1RM.
    static func needsDeload(recentHistory: [ExerciseHistory]) -> Bool {
        guard recentHistory.count >= 3 else { return false }
        return recentHistory[0].estimatedOneRepMax < recentHistory[1].estimatedOneRepMax &&
               recentHistory[1].estimatedOneRepMax < recentHistory[2].estimatedOneRepMax
    }

    // MARK: - Progress Trend

    static func detectTrend(recentHistory: [ExerciseHistory]) -> ProgressTrend {
        guard recentHistory.count >= 2 else { return .new }
        if recentHistory.count >= 3 {
            let improving = recentHistory[0].estimatedOneRepMax > recentHistory[1].estimatedOneRepMax &&
                            recentHistory[1].estimatedOneRepMax > recentHistory[2].estimatedOneRepMax
            if improving { return .improving }

            let declining = recentHistory[0].estimatedOneRepMax < recentHistory[1].estimatedOneRepMax &&
                            recentHistory[1].estimatedOneRepMax < recentHistory[2].estimatedOneRepMax
            if declining { return .declining }
        }
        if recentHistory[0].estimatedOneRepMax > recentHistory[1].estimatedOneRepMax {
            return .improving
        }
        return .maintaining
    }
}
