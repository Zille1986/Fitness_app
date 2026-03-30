import Foundation
import Observation
import Combine

// MARK: - Readiness & Training Models

struct ReadinessScore: Equatable {
    let overall: Double // 0-100
    let hrvScore: Double
    let sleepScore: Double
    let recentLoadScore: Double
    let moodScore: Double
    let recommendation: ReadinessLevel

    enum ReadinessLevel: String {
        case fullyReady = "Fully Ready"
        case ready = "Ready"
        case moderate = "Moderate"
        case fatigued = "Fatigued"
        case needsRest = "Needs Rest"
    }
}

struct TrainingLoad: Equatable {
    let acuteLoad: Double // last 7 days
    let chronicLoad: Double // last 28 days
    let ratio: Double // acute / chronic (sweet spot: 0.8-1.3)
    let status: TrainingStatus

    enum TrainingStatus: String {
        case detraining = "Detraining"
        case maintenance = "Maintenance"
        case optimal = "Optimal"
        case overreaching = "Overreaching"
        case overtraining = "Overtraining"
    }
}

struct WorkoutAdjustment: Equatable {
    let adjustedIntensity: Double // multiplier, e.g. 0.8 for 80%
    let adjustedVolume: Double // multiplier
    let reason: String
    let suggestedType: String? // e.g. "recovery run" instead of intervals
}

struct RecoveryRecommendation: Equatable, Identifiable {
    let id = UUID()
    let category: RecoveryCategory
    let title: String
    let description: String
    let priority: Priority

    enum RecoveryCategory: String {
        case sleep = "Sleep"
        case nutrition = "Nutrition"
        case activeRecovery = "Active Recovery"
        case stretching = "Stretching"
        case rest = "Rest"
        case hydration = "Hydration"
    }

    enum Priority: String, Comparable {
        case low = "Low"
        case medium = "Medium"
        case high = "High"

        static func < (lhs: Priority, rhs: Priority) -> Bool {
            let order: [Priority] = [.low, .medium, .high]
            return order.firstIndex(of: lhs)! < order.firstIndex(of: rhs)!
        }
    }
}

struct ProgressiveOverloadSuggestion: Equatable, Identifiable {
    let id = UUID()
    let exerciseName: String
    let currentWeight: Double?
    let suggestedWeight: Double?
    let currentReps: Int?
    let suggestedReps: Int?
    let currentSets: Int?
    let suggestedSets: Int?
    let rationale: String
}

struct WorkoutHistoryEntry {
    let date: Date
    let type: String
    let durationMinutes: Int
    let distanceMeters: Double?
    let avgHeartRate: Int?
    let calories: Int?
    let perceivedExertion: Int? // 1-10 RPE
    let load: Double // calculated training load
}

// MARK: - Smart Trainer Service

@Observable
final class SmartTrainerService {

    var currentReadiness: ReadinessScore?
    var currentTrainingLoad: TrainingLoad?
    var recoveryRecommendations: [RecoveryRecommendation] = []
    var lastError: String?

    private let healthKitService: HealthKitService
    private var workoutHistory: [WorkoutHistoryEntry] = []

    init(healthKitService: HealthKitService) {
        self.healthKitService = healthKitService
    }

    // MARK: - Readiness Scoring

    func calculateReadiness(
        userMood: Int? = nil, // 1-5 scale
        perceivedStress: Int? = nil // 1-5 scale
    ) async -> ReadinessScore {
        // Fetch HRV
        let hrv = await healthKitService.fetchLatestHRV()
        let hrvScore = calculateHRVScore(hrv: hrv)

        // Fetch sleep
        let sleep = await healthKitService.fetchLastNightSleep()
        let sleepScore = calculateSleepScore(sleep: sleep)

        // Calculate recent training load
        let recentLoadScore = calculateRecentLoadScore()

        // Mood/stress score
        let moodScore = calculateMoodScore(mood: userMood, stress: perceivedStress)

        // Weighted average
        let overall = (hrvScore * 0.30) + (sleepScore * 0.30) + (recentLoadScore * 0.25) + (moodScore * 0.15)

        let level: ReadinessScore.ReadinessLevel
        switch overall {
        case 80...: level = .fullyReady
        case 65..<80: level = .ready
        case 50..<65: level = .moderate
        case 35..<50: level = .fatigued
        default: level = .needsRest
        }

        let score = ReadinessScore(
            overall: overall,
            hrvScore: hrvScore,
            sleepScore: sleepScore,
            recentLoadScore: recentLoadScore,
            moodScore: moodScore,
            recommendation: level
        )

        currentReadiness = score
        recoveryRecommendations = generateRecoveryRecommendations(readiness: score, sleep: sleep, hrv: hrv)

        return score
    }

    private func calculateHRVScore(hrv: Double?) -> Double {
        guard let hrv else { return 50 } // neutral if no data

        // HRV scoring: higher is generally better
        // Average adult HRV (SDNN) is 50-100ms
        // Athletes often higher: 60-150ms
        switch hrv {
        case 100...: return 95
        case 80..<100: return 85
        case 60..<80: return 70
        case 40..<60: return 55
        case 20..<40: return 35
        default: return 20
        }
    }

    private func calculateSleepScore(sleep: SleepData?) -> Double {
        guard let sleep else { return 40 } // penalize missing sleep data

        var score: Double = 0

        // Total sleep scoring (7-9 hours optimal)
        switch sleep.totalHours {
        case 8.0...9.0: score += 50
        case 7.0..<8.0: score += 45
        case 6.0..<7.0: score += 30
        case 5.0..<6.0: score += 20
        default: score += 10
        }

        // Deep sleep scoring (1-2 hours optimal, ~20% of total)
        let deepRatio = sleep.deepHours / max(sleep.totalHours, 1)
        switch deepRatio {
        case 0.20...: score += 25
        case 0.15..<0.20: score += 20
        case 0.10..<0.15: score += 15
        default: score += 5
        }

        // REM sleep scoring (~20-25% optimal)
        let remRatio = sleep.remHours / max(sleep.totalHours, 1)
        switch remRatio {
        case 0.20...: score += 25
        case 0.15..<0.20: score += 20
        case 0.10..<0.15: score += 15
        default: score += 5
        }

        return min(score, 100)
    }

    private func calculateRecentLoadScore() -> Double {
        let load = calculateTrainingLoad()

        // Optimal ACR ratio is 0.8-1.3
        switch load.ratio {
        case 0.8...1.3: return 80
        case 0.5..<0.8: return 60 // slightly undertrained
        case 1.3..<1.5: return 50 // slightly overreaching
        case 1.5..<2.0: return 30 // significant overreaching
        case 2.0...: return 15 // danger zone
        default: return 70 // very low training, just starting
        }
    }

    private func calculateMoodScore(mood: Int?, stress: Int?) -> Double {
        var score: Double = 60 // default neutral

        if let mood {
            // mood: 1=terrible, 5=great
            score = Double(mood) * 20.0
        }

        if let stress {
            // stress: 1=none, 5=extreme
            let stressPenalty = Double(stress - 1) * 10.0
            score = max(0, score - stressPenalty)
        }

        return min(max(score, 0), 100)
    }

    // MARK: - Training Load

    func calculateTrainingLoad() -> TrainingLoad {
        let now = Date()
        let sevenDaysAgo = Calendar.current.date(byAdding: .day, value: -7, to: now)!
        let twentyEightDaysAgo = Calendar.current.date(byAdding: .day, value: -28, to: now)!

        let acuteEntries = workoutHistory.filter { $0.date >= sevenDaysAgo }
        let chronicEntries = workoutHistory.filter { $0.date >= twentyEightDaysAgo }

        let acuteLoad = acuteEntries.reduce(0.0) { $0 + $1.load }
        let chronicLoad = chronicEntries.isEmpty ? 0 : chronicEntries.reduce(0.0) { $0 + $1.load } / 4.0 // weekly average

        let ratio = chronicLoad > 0 ? acuteLoad / chronicLoad : 0

        let status: TrainingLoad.TrainingStatus
        switch ratio {
        case ..<0.5: status = .detraining
        case 0.5..<0.8: status = .maintenance
        case 0.8...1.3: status = .optimal
        case 1.3..<1.5: status = .overreaching
        default: status = .overtraining
        }

        let load = TrainingLoad(
            acuteLoad: acuteLoad,
            chronicLoad: chronicLoad,
            ratio: ratio,
            status: status
        )

        currentTrainingLoad = load
        return load
    }

    // MARK: - Workout Adjustment

    func adjustWorkout(
        plannedType: String,
        plannedIntensity: Double, // 0-1
        plannedDurationMinutes: Int
    ) async -> WorkoutAdjustment {
        let readiness: ReadinessScore
        if let current = currentReadiness {
            readiness = current
        } else {
            readiness = await calculateReadiness()
        }

        switch readiness.recommendation {
        case .fullyReady:
            return WorkoutAdjustment(
                adjustedIntensity: plannedIntensity * 1.05,
                adjustedVolume: 1.0,
                reason: "You're fully recovered. Slight intensity bump recommended.",
                suggestedType: nil
            )

        case .ready:
            return WorkoutAdjustment(
                adjustedIntensity: plannedIntensity,
                adjustedVolume: 1.0,
                reason: "You're in good shape. Proceed as planned.",
                suggestedType: nil
            )

        case .moderate:
            return WorkoutAdjustment(
                adjustedIntensity: plannedIntensity * 0.9,
                adjustedVolume: 0.9,
                reason: "Moderate readiness detected. Reduce intensity by 10% to manage fatigue.",
                suggestedType: plannedType == "INTERVAL_TRAINING" ? "TEMPO_RUN" : nil
            )

        case .fatigued:
            return WorkoutAdjustment(
                adjustedIntensity: plannedIntensity * 0.75,
                adjustedVolume: 0.75,
                reason: "You're showing signs of fatigue. Significantly reduced workout recommended.",
                suggestedType: "EASY_RUN"
            )

        case .needsRest:
            return WorkoutAdjustment(
                adjustedIntensity: 0.5,
                adjustedVolume: 0.5,
                reason: "Rest is strongly recommended. If you must train, keep it very light.",
                suggestedType: "RECOVERY_RUN"
            )
        }
    }

    // MARK: - Record Workout

    func recordWorkout(
        date: Date = Date(),
        type: String,
        durationMinutes: Int,
        distanceMeters: Double? = nil,
        avgHeartRate: Int? = nil,
        calories: Int? = nil,
        perceivedExertion: Int? = nil
    ) {
        let load = calculateWorkoutLoad(
            durationMinutes: durationMinutes,
            avgHeartRate: avgHeartRate,
            perceivedExertion: perceivedExertion,
            distanceMeters: distanceMeters
        )

        let entry = WorkoutHistoryEntry(
            date: date,
            type: type,
            durationMinutes: durationMinutes,
            distanceMeters: distanceMeters,
            avgHeartRate: avgHeartRate,
            calories: calories,
            perceivedExertion: perceivedExertion,
            load: load
        )

        workoutHistory.append(entry)
        _ = calculateTrainingLoad()
    }

    private func calculateWorkoutLoad(
        durationMinutes: Int,
        avgHeartRate: Int?,
        perceivedExertion: Int?,
        distanceMeters: Double?
    ) -> Double {
        // TRIMP-like calculation (Training Impulse)
        // Load = duration * intensity_factor

        var intensityFactor: Double = 1.0

        if let rpe = perceivedExertion {
            // RPE-based: session RPE method
            intensityFactor = Double(rpe) / 5.0 // normalize 1-10 to 0.2-2.0
        } else if let hr = avgHeartRate {
            // HR-based intensity estimation
            // Assuming max HR ~190 (should be personalized)
            let hrReserve = Double(hr - 60) / Double(190 - 60)
            intensityFactor = max(0.5, min(2.0, hrReserve * 2.0))
        }

        return Double(durationMinutes) * intensityFactor
    }

    // MARK: - Recovery Recommendations

    private func generateRecoveryRecommendations(
        readiness: ReadinessScore,
        sleep: SleepData?,
        hrv: Double?
    ) -> [RecoveryRecommendation] {
        var recommendations: [RecoveryRecommendation] = []

        // Sleep recommendations
        if let sleep {
            if sleep.totalHours < 7.0 {
                recommendations.append(RecoveryRecommendation(
                    category: .sleep,
                    title: "Improve Sleep Duration",
                    description: "You got \(sleep.formatted) last night. Aim for 7-9 hours for optimal recovery. Try going to bed 30 minutes earlier.",
                    priority: sleep.totalHours < 6.0 ? .high : .medium
                ))
            }
            if sleep.deepHours < 1.0 {
                recommendations.append(RecoveryRecommendation(
                    category: .sleep,
                    title: "Increase Deep Sleep",
                    description: "Your deep sleep was low. Avoid screens 1 hour before bed and keep your room cool (18-20C).",
                    priority: .medium
                ))
            }
        } else {
            recommendations.append(RecoveryRecommendation(
                category: .sleep,
                title: "Track Your Sleep",
                description: "No sleep data available. Wear your watch to bed for better recovery insights.",
                priority: .medium
            ))
        }

        // HRV recommendations
        if let hrv, hrv < 40 {
            recommendations.append(RecoveryRecommendation(
                category: .rest,
                title: "Low HRV Detected",
                description: "Your HRV of \(Int(hrv))ms suggests your nervous system needs recovery. Consider reducing training intensity today.",
                priority: .high
            ))
        }

        // Training load recommendations
        if let load = currentTrainingLoad {
            switch load.status {
            case .overtraining:
                recommendations.append(RecoveryRecommendation(
                    category: .rest,
                    title: "Overtraining Risk",
                    description: "Your acute:chronic ratio is \(String(format: "%.1f", load.ratio)). Take 1-2 rest days to prevent injury and burnout.",
                    priority: .high
                ))
            case .overreaching:
                recommendations.append(RecoveryRecommendation(
                    category: .activeRecovery,
                    title: "Functional Overreaching",
                    description: "You've been training hard. Include a light recovery session today (walk, yoga, or easy swim).",
                    priority: .medium
                ))
            case .detraining:
                recommendations.append(RecoveryRecommendation(
                    category: .activeRecovery,
                    title: "Training Volume Low",
                    description: "Your recent training volume is below baseline. Gradually increase to maintain fitness.",
                    priority: .low
                ))
            default:
                break
            }
        }

        // General recommendations based on readiness
        if readiness.overall < 50 {
            recommendations.append(RecoveryRecommendation(
                category: .hydration,
                title: "Stay Hydrated",
                description: "When fatigued, hydration becomes even more important. Aim for at least 2.5L of water today.",
                priority: .medium
            ))

            recommendations.append(RecoveryRecommendation(
                category: .nutrition,
                title: "Recovery Nutrition",
                description: "Focus on protein (1.6-2.2g/kg bodyweight) and complex carbs to aid recovery. Include anti-inflammatory foods.",
                priority: .medium
            ))

            recommendations.append(RecoveryRecommendation(
                category: .stretching,
                title: "Mobility Work",
                description: "Spend 10-15 minutes on foam rolling and stretching. Focus on tight areas from recent workouts.",
                priority: .low
            ))
        }

        return recommendations.sorted { $0.priority > $1.priority }
    }

    // MARK: - Progressive Overload Suggestions

    func suggestProgressiveOverload(
        exerciseName: String,
        currentWeight: Double?,
        currentReps: Int,
        currentSets: Int,
        recentPerformance: [(weight: Double?, reps: Int, date: Date)]
    ) -> ProgressiveOverloadSuggestion {
        // Check if they've been consistent at current load
        let recentAtCurrentLoad = recentPerformance.filter { perf in
            perf.weight == currentWeight && perf.reps >= currentReps
        }

        let readyToProgress = recentAtCurrentLoad.count >= 3

        if readyToProgress {
            if let weight = currentWeight, weight > 0 {
                // First try to increase weight by ~2.5-5%
                let increment: Double
                if weight < 20 {
                    increment = 1.0
                } else if weight < 50 {
                    increment = 2.5
                } else {
                    increment = 5.0
                }

                let newWeight = weight + increment

                return ProgressiveOverloadSuggestion(
                    exerciseName: exerciseName,
                    currentWeight: weight,
                    suggestedWeight: newWeight,
                    currentReps: currentReps,
                    suggestedReps: currentReps,
                    currentSets: currentSets,
                    suggestedSets: currentSets,
                    rationale: "You've successfully completed \(currentReps) reps at \(String(format: "%.1f", weight))kg for \(recentAtCurrentLoad.count) consecutive sessions. Ready to increase weight."
                )
            } else {
                // Bodyweight exercise: increase reps or sets
                let suggestedReps = currentReps + 2

                return ProgressiveOverloadSuggestion(
                    exerciseName: exerciseName,
                    currentWeight: nil,
                    suggestedWeight: nil,
                    currentReps: currentReps,
                    suggestedReps: suggestedReps,
                    currentSets: currentSets,
                    suggestedSets: currentSets,
                    rationale: "You've been consistent at \(currentReps) reps for \(recentAtCurrentLoad.count) sessions. Increase by 2 reps."
                )
            }
        } else {
            return ProgressiveOverloadSuggestion(
                exerciseName: exerciseName,
                currentWeight: currentWeight,
                suggestedWeight: currentWeight,
                currentReps: currentReps,
                suggestedReps: currentReps,
                currentSets: currentSets,
                suggestedSets: currentSets,
                rationale: "Continue at current load. Complete \(3 - recentAtCurrentLoad.count) more sessions at this weight before progressing."
            )
        }
    }

    // MARK: - Weekly Load Summary

    func weeklyLoadSummary() -> (totalLoad: Double, workoutCount: Int, avgDuration: Int, trend: String) {
        let oneWeekAgo = Calendar.current.date(byAdding: .day, value: -7, to: Date())!
        let twoWeeksAgo = Calendar.current.date(byAdding: .day, value: -14, to: Date())!

        let thisWeek = workoutHistory.filter { $0.date >= oneWeekAgo }
        let lastWeek = workoutHistory.filter { $0.date >= twoWeeksAgo && $0.date < oneWeekAgo }

        let thisWeekLoad = thisWeek.reduce(0.0) { $0 + $1.load }
        let lastWeekLoad = lastWeek.reduce(0.0) { $0 + $1.load }

        let avgDuration = thisWeek.isEmpty ? 0 : thisWeek.reduce(0) { $0 + $1.durationMinutes } / thisWeek.count

        let trend: String
        if lastWeekLoad == 0 {
            trend = "Starting"
        } else {
            let change = ((thisWeekLoad - lastWeekLoad) / lastWeekLoad) * 100
            if change > 10 {
                trend = "Increasing (+\(Int(change))%)"
            } else if change < -10 {
                trend = "Decreasing (\(Int(change))%)"
            } else {
                trend = "Stable"
            }
        }

        return (thisWeekLoad, thisWeek.count, avgDuration, trend)
    }
}
