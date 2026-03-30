import Foundation
import Observation

// MARK: - Wellness Enums

enum WellnessStatus: String, CaseIterable {
    case excellent, good, fair, needsAttention

    var label: String {
        switch self {
        case .excellent: return "Excellent"
        case .good: return "Good"
        case .fair: return "Fair"
        case .needsAttention: return "Needs Attention"
        }
    }

    var emoji: String {
        switch self {
        case .excellent: return "star.fill"
        case .good: return "checkmark.circle.fill"
        case .fair: return "minus.circle.fill"
        case .needsAttention: return "exclamationmark.triangle.fill"
        }
    }

    var colorHex: String {
        switch self {
        case .excellent: return "4CAF50"
        case .good: return "8BC34A"
        case .fair: return "FF9800"
        case .needsAttention: return "F44336"
        }
    }
}

enum WellnessTrainingStatus: String, CaseIterable {
    case detraining, optimal, highRisk, veryHighRisk

    var label: String {
        switch self {
        case .detraining: return "Detraining"
        case .optimal: return "Optimal"
        case .highRisk: return "High Risk"
        case .veryHighRisk: return "Very High Risk"
        }
    }

    var colorHex: String {
        switch self {
        case .detraining: return "FF9800"
        case .optimal: return "4CAF50"
        case .highRisk: return "FF9800"
        case .veryHighRisk: return "F44336"
        }
    }
}

enum RecommendationPriority: Int, Comparable {
    case high = 0, medium = 1, low = 2

    static func < (lhs: RecommendationPriority, rhs: RecommendationPriority) -> Bool {
        lhs.rawValue < rhs.rawValue
    }
}

struct WellnessTrainingLoad {
    var acuteLoad: Int = 0
    var chronicLoad: Int = 0
    var acuteChronicRatio: Float = 1.0
    var status: WellnessTrainingStatus = .optimal
    var weeklyDistanceMeters: Double = 0
    var weeklyDurationMinutes: Int = 0
    var weeklyRuns: Int = 0
}

struct WellnessRecommendation: Identifiable {
    let id = UUID()
    var icon: String
    var title: String
    var description: String
    var priority: RecommendationPriority
}

struct WellnessSleepData: Identifiable {
    let id = UUID()
    var date: Date
    var hours: Double
    var quality: Int // 1-5
}

struct HRVDataPoint: Identifiable {
    let id = UUID()
    var date: Date
    var value: Double // ms
}

struct RestingHRDataPoint: Identifiable {
    let id = UUID()
    var date: Date
    var bpm: Int
}

struct ReadinessBreakdown {
    var sleepScore: Int = 0
    var recoveryScore: Int = 0
    var stressScore: Int = 0
    var trainingLoadScore: Int = 0
}

// MARK: - WellnessViewModel

@Observable
final class WellnessViewModel {
    // Dependencies
    private let wellnessRepository: WellnessRepository?
    private let healthKitService: HealthKitService
    private let smartTrainerService: SmartTrainerService?
    private let gamificationRepository: GamificationRepository?

    // Overall State
    var overallWellnessScore: Int = 50
    var wellnessStatus: WellnessStatus = .fair
    var isLoading = true
    var errorMessage: String?

    // Readiness
    var readinessScore: Int = 0
    var readinessBreakdown = ReadinessBreakdown()

    // Sleep
    var lastNightSleepHours: Double?
    var sleepQuality: Int?
    var weeklyWellnessSleepData: [WellnessSleepData] = []

    // HRV
    var currentHRV: Double?
    var weeklyHRVData: [HRVDataPoint] = []
    var hrvBaseline: Double = 45.0

    // Resting HR
    var restingHR: Int?
    var weeklyRestingHR: [RestingHRDataPoint] = []

    // Stress
    var stressLevel: Int = 3 // 1-5

    // Training Load
    var trainingLoad = WellnessTrainingLoad()

    // Streaks / Gamification
    var currentStreak: Int = 0
    var mindfulnessMinutesThisWeek: Int = 0

    // Recommendations
    var recommendations: [WellnessRecommendation] = []

    // Weekly trend data
    var weeklyReadinessScores: [(date: Date, score: Int)] = []

    init(
        wellnessRepository: WellnessRepository? = nil,
        healthKitService: HealthKitService = HealthKitService(),
        smartTrainerService: SmartTrainerService? = nil,
        gamificationRepository: GamificationRepository? = nil
    ) {
        self.wellnessRepository = wellnessRepository
        self.healthKitService = healthKitService
        self.smartTrainerService = smartTrainerService
        self.gamificationRepository = gamificationRepository
        loadData()
    }

    func loadData() {
        isLoading = true
        Task { @MainActor in
            // Load sleep data
            let sleep = await healthKitService.fetchLastNightSleep()
            self.lastNightSleepHours = sleep?.totalHours
            self.sleepQuality = sleep.map { sleepData in
                switch sleepData.totalHours {
                case 8...: return 5
                case 7...: return 4
                case 6...: return 3
                case 5...: return 2
                default: return 1
                }
            }

            // Load HRV
            let hrv = await healthKitService.fetchLatestHRV()
            self.currentHRV = hrv

            // Load resting HR
            let rhr = await healthKitService.fetchRestingHeartRate()
            self.restingHR = rhr

            // Load readiness
            if let smartTrainer = smartTrainerService {
                let readiness = await smartTrainer.calculateReadiness()
                self.readinessScore = Int(readiness.overall)
                self.readinessBreakdown = ReadinessBreakdown(
                    sleepScore: Int(readiness.sleepScore),
                    recoveryScore: Int(readiness.hrvScore),
                    stressScore: Int(readiness.moodScore),
                    trainingLoadScore: Int(readiness.recentLoadScore)
                )
            }

            // Load streak
            if let gamification = gamificationRepository {
                let userGamification = gamification.getOrCreateUserGamification()
                self.currentStreak = userGamification.currentStreak
            }

            // Load mindfulness minutes
            if let repo = wellnessRepository {
                self.mindfulnessMinutesThisWeek = repo.totalMindfulnessMinutesThisWeek()

                let checkins = repo.fetchRecentCheckins(limit: 7)
                self.weeklyReadinessScores = checkins.compactMap { checkin in
                    if let score = checkin.readinessScore {
                        return (checkin.date, score)
                    }
                    return nil
                }
            }

            calculateOverallWellness()
            generateRecommendations()
            self.isLoading = false
        }
    }

    func refresh() {
        loadData()
    }

    private func calculateOverallWellness() {
        var score = 50
        var factors = 0

        // Streak contribution
        if currentStreak >= 7 {
            score += 15; factors += 1
        } else if currentStreak >= 3 {
            score += 10; factors += 1
        } else if currentStreak >= 1 {
            score += 5; factors += 1
        } else {
            score -= 5; factors += 1
        }

        // Readiness contribution
        if readinessScore > 0 {
            score += (readinessScore - 50) / 5
            factors += 1
        }

        // Training load balance
        if trainingLoad.acuteLoad > 0 {
            switch trainingLoad.status {
            case .optimal: score += 10
            case .detraining: score -= 5
            case .highRisk: score -= 5
            case .veryHighRisk: score -= 15
            }
            factors += 1
        }

        // Sleep contribution
        if let hours = lastNightSleepHours {
            if hours >= 7 {
                score += 10
            } else if hours >= 6 {
                score += 5
            } else {
                score -= 10
            }
            factors += 1
        }

        // Mindfulness bonus
        if mindfulnessMinutesThisWeek > 0 {
            score += min(mindfulnessMinutesThisWeek / 2, 10)
            factors += 1
        }

        let finalScore = max(0, min(100, score))
        overallWellnessScore = finalScore

        if finalScore >= 80 {
            wellnessStatus = .excellent
        } else if finalScore >= 60 {
            wellnessStatus = .good
        } else if finalScore >= 40 {
            wellnessStatus = .fair
        } else {
            wellnessStatus = .needsAttention
        }
    }

    private func generateRecommendations() {
        var recs: [WellnessRecommendation] = []

        // Sleep
        if let hours = lastNightSleepHours, hours < 7 {
            recs.append(WellnessRecommendation(
                icon: "moon.zzz.fill",
                title: "Prioritize Sleep",
                description: "You got \(String(format: "%.1f", hours))h of sleep. Aim for 7-9 hours for optimal recovery.",
                priority: .high
            ))
        }

        // Training load
        switch trainingLoad.status {
        case .detraining:
            recs.append(WellnessRecommendation(
                icon: "chart.line.uptrend.xyaxis",
                title: "Increase Training",
                description: "Your training load is low. Consider adding more workouts this week.",
                priority: .medium
            ))
        case .highRisk, .veryHighRisk:
            recs.append(WellnessRecommendation(
                icon: "exclamationmark.triangle.fill",
                title: "Recovery Needed",
                description: "Your training load is high. Consider a rest day or easy workout.",
                priority: .high
            ))
        case .optimal:
            break
        }

        // Mindfulness
        if mindfulnessMinutesThisWeek < 10 {
            recs.append(WellnessRecommendation(
                icon: "brain.head.profile",
                title: "Add Mindfulness",
                description: "Try a breathing exercise to reduce stress and improve recovery.",
                priority: .low
            ))
        }

        // HRV trend
        if let hrv = currentHRV, hrv < hrvBaseline * 0.8 {
            recs.append(WellnessRecommendation(
                icon: "heart.text.square",
                title: "HRV Below Baseline",
                description: "Your HRV is lower than usual. Consider lighter training today.",
                priority: .high
            ))
        }

        // Streak
        if currentStreak == 0 {
            recs.append(WellnessRecommendation(
                icon: "flame",
                title: "Start Your Streak",
                description: "Complete a workout today to begin building consistency.",
                priority: .medium
            ))
        }

        recommendations = recs.sorted { $0.priority < $1.priority }
    }

    // Computed properties for detail screen
    var hrvTrendDirection: String {
        guard weeklyHRVData.count >= 2 else { return "Stable" }
        let recent = weeklyHRVData.suffix(3).map(\.value)
        let older = weeklyHRVData.prefix(3).map(\.value)
        let recentAvg = recent.reduce(0, +) / Double(recent.count)
        let olderAvg = older.reduce(0, +) / Double(older.count)
        if recentAvg > olderAvg * 1.05 { return "Improving" }
        if recentAvg < olderAvg * 0.95 { return "Declining" }
        return "Stable"
    }

    var sleepAverage: Double {
        guard !weeklyWellnessSleepData.isEmpty else { return 0 }
        return weeklyWellnessSleepData.map(\.hours).reduce(0, +) / Double(weeklyWellnessSleepData.count)
    }

    var acuteChronicRatioFormatted: String {
        String(format: "%.2f", trainingLoad.acuteChronicRatio)
    }
}

