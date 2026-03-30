import Foundation
import SwiftData

@Model
final class MoodEntry {
    @Attribute(.unique) var id: UUID
    var timestamp: Date
    var mood: MoodLevel
    var energy: EnergyLevel
    var stress: StressLevel
    var notes: String
    var relatedRunId: UUID?
    var isPreWorkout: Bool
    var tags: [String]

    init(
        id: UUID = UUID(),
        timestamp: Date = Date(),
        mood: MoodLevel,
        energy: EnergyLevel,
        stress: StressLevel,
        notes: String = "",
        relatedRunId: UUID? = nil,
        isPreWorkout: Bool = true,
        tags: [String] = []
    ) {
        self.id = id
        self.timestamp = timestamp
        self.mood = mood
        self.energy = energy
        self.stress = stress
        self.notes = notes
        self.relatedRunId = relatedRunId
        self.isPreWorkout = isPreWorkout
        self.tags = tags
    }
}

enum MoodLevel: Int, Codable, CaseIterable {
    case veryLow = 1
    case low = 2
    case neutral = 3
    case good = 4
    case great = 5

    var emoji: String {
        switch self {
        case .veryLow: return "\u{1F622}"
        case .low: return "\u{1F614}"
        case .neutral: return "\u{1F610}"
        case .good: return "\u{1F642}"
        case .great: return "\u{1F604}"
        }
    }

    var label: String {
        switch self {
        case .veryLow: return "Very Low"
        case .low: return "Low"
        case .neutral: return "Neutral"
        case .good: return "Good"
        case .great: return "Great"
        }
    }
}

enum EnergyLevel: Int, Codable, CaseIterable {
    case exhausted = 1
    case tired = 2
    case moderate = 3
    case energized = 4
    case pumped = 5

    var emoji: String {
        switch self {
        case .exhausted: return "\u{1F634}"
        case .tired: return "\u{1F971}"
        case .moderate: return "\u{1F60A}"
        case .energized: return "\u{1F4AA}"
        case .pumped: return "\u{1F525}"
        }
    }

    var label: String {
        switch self {
        case .exhausted: return "Exhausted"
        case .tired: return "Tired"
        case .moderate: return "Moderate"
        case .energized: return "Energized"
        case .pumped: return "Pumped"
        }
    }
}

enum StressLevel: Int, Codable, CaseIterable {
    case veryHigh = 5
    case high = 4
    case moderate = 3
    case low = 2
    case veryLow = 1

    var emoji: String {
        switch self {
        case .veryHigh: return "\u{1F630}"
        case .high: return "\u{1F61F}"
        case .moderate: return "\u{1F610}"
        case .low: return "\u{1F60C}"
        case .veryLow: return "\u{1F60A}"
        }
    }

    var label: String {
        switch self {
        case .veryHigh: return "Very High"
        case .high: return "High"
        case .moderate: return "Moderate"
        case .low: return "Low"
        case .veryLow: return "Very Low"
        }
    }
}

@Model
final class MindfulnessSession {
    @Attribute(.unique) var id: UUID
    var timestamp: Date
    var type: MindfulnessType
    var durationSeconds: Int
    var completed: Bool
    var relatedRunId: UUID?
    var rating: Int?

    init(
        id: UUID = UUID(),
        timestamp: Date = Date(),
        type: MindfulnessType,
        durationSeconds: Int,
        completed: Bool = true,
        relatedRunId: UUID? = nil,
        rating: Int? = nil
    ) {
        self.id = id
        self.timestamp = timestamp
        self.type = type
        self.durationSeconds = durationSeconds
        self.completed = completed
        self.relatedRunId = relatedRunId
        self.rating = rating
    }
}

enum MindfulnessType: String, Codable, CaseIterable {
    case preRunFocus = "PRE_RUN_FOCUS"
    case postRunGratitude = "POST_RUN_GRATITUDE"
    case breathingExercise = "BREATHING_EXERCISE"
    case bodyScan = "BODY_SCAN"
    case visualization = "VISUALIZATION"
    case stressRelief = "STRESS_RELIEF"
    case sleepPreparation = "SLEEP_PREPARATION"
    case morningEnergizer = "MORNING_ENERGIZER"

    var title: String {
        switch self {
        case .preRunFocus: return "Pre-Run Focus"
        case .postRunGratitude: return "Post-Run Gratitude"
        case .breathingExercise: return "Breathing Exercise"
        case .bodyScan: return "Body Scan"
        case .visualization: return "Visualization"
        case .stressRelief: return "Stress Relief"
        case .sleepPreparation: return "Sleep Preparation"
        case .morningEnergizer: return "Morning Energizer"
        }
    }

    var typeDescription: String {
        switch self {
        case .preRunFocus: return "Clear your mind and set intentions for your run"
        case .postRunGratitude: return "Reflect on your accomplishment and express gratitude"
        case .breathingExercise: return "Calm your nervous system with guided breathing"
        case .bodyScan: return "Check in with your body from head to toe"
        case .visualization: return "Visualize your perfect run or race"
        case .stressRelief: return "Release tension and find calm"
        case .sleepPreparation: return "Wind down for restful recovery sleep"
        case .morningEnergizer: return "Start your day with positive energy"
        }
    }

    var defaultDuration: Int {
        switch self {
        case .preRunFocus: return 180
        case .postRunGratitude: return 180
        case .breathingExercise: return 120
        case .bodyScan: return 300
        case .visualization: return 240
        case .stressRelief: return 300
        case .sleepPreparation: return 600
        case .morningEnergizer: return 180
        }
    }

    var icon: String {
        switch self {
        case .preRunFocus: return "figure.run"
        case .postRunGratitude: return "heart.fill"
        case .breathingExercise: return "wind"
        case .bodyScan: return "person.fill"
        case .visualization: return "eye.fill"
        case .stressRelief: return "leaf.fill"
        case .sleepPreparation: return "moon.fill"
        case .morningEnergizer: return "sun.max.fill"
        }
    }
}

struct BreathingPattern: Codable, Hashable, Identifiable {
    var id: String { name }
    var name: String
    var patternDescription: String
    var inhaleSeconds: Int
    var holdAfterInhale: Int
    var exhaleSeconds: Int
    var holdAfterExhale: Int
    var cycles: Int
    var benefits: [String]

    var cycleDuration: Int {
        inhaleSeconds + holdAfterInhale + exhaleSeconds + holdAfterExhale
    }

    var totalDuration: Int {
        cycleDuration * cycles
    }
}

enum BreathingPatterns {
    static let boxBreathing = BreathingPattern(
        name: "Box Breathing",
        patternDescription: "Equal parts inhale, hold, exhale, hold. Used by Navy SEALs for stress relief.",
        inhaleSeconds: 4, holdAfterInhale: 4, exhaleSeconds: 4, holdAfterExhale: 4, cycles: 6,
        benefits: ["Reduces stress", "Improves focus", "Calms nervous system"]
    )

    static let relaxingBreath = BreathingPattern(
        name: "4-7-8 Relaxing Breath",
        patternDescription: "Dr. Andrew Weil's technique for relaxation and sleep.",
        inhaleSeconds: 4, holdAfterInhale: 7, exhaleSeconds: 8, holdAfterExhale: 0, cycles: 4,
        benefits: ["Promotes sleep", "Reduces anxiety", "Lowers heart rate"]
    )

    static let energizingBreath = BreathingPattern(
        name: "Energizing Breath",
        patternDescription: "Quick, powerful breaths to increase alertness before a run.",
        inhaleSeconds: 2, holdAfterInhale: 0, exhaleSeconds: 2, holdAfterExhale: 0, cycles: 15,
        benefits: ["Increases energy", "Improves alertness", "Warms up lungs"]
    )

    static let calmingBreath = BreathingPattern(
        name: "Extended Exhale",
        patternDescription: "Longer exhale activates parasympathetic nervous system.",
        inhaleSeconds: 4, holdAfterInhale: 0, exhaleSeconds: 8, holdAfterExhale: 2, cycles: 6,
        benefits: ["Activates rest response", "Slows heart rate", "Reduces cortisol"]
    )

    static let recoveryBreath = BreathingPattern(
        name: "Post-Run Recovery",
        patternDescription: "Gentle breathing to aid recovery after intense exercise.",
        inhaleSeconds: 5, holdAfterInhale: 2, exhaleSeconds: 5, holdAfterExhale: 2, cycles: 8,
        benefits: ["Speeds recovery", "Reduces lactic acid", "Restores calm"]
    )

    static func getAll() -> [BreathingPattern] {
        [boxBreathing, relaxingBreath, energizingBreath, calmingBreath, recoveryBreath]
    }

    static func getPreRunPatterns() -> [BreathingPattern] { [energizingBreath, boxBreathing] }
    static func getPostRunPatterns() -> [BreathingPattern] { [recoveryBreath, calmingBreath] }
    static func getSleepPatterns() -> [BreathingPattern] { [relaxingBreath, calmingBreath] }
}

@Model
final class WellnessCheckin {
    @Attribute(.unique) var id: UUID
    var date: Date
    var sleepHours: Float?
    var sleepQuality: Int?
    var restingHeartRate: Int?
    var hrv: Int?
    var mood: MoodLevel?
    var energy: EnergyLevel?
    var stress: StressLevel?
    var soreness: Int?
    var hydration: Int?
    var notes: String
    var readinessScore: Int?

    init(
        id: UUID = UUID(),
        date: Date,
        sleepHours: Float? = nil,
        sleepQuality: Int? = nil,
        restingHeartRate: Int? = nil,
        hrv: Int? = nil,
        mood: MoodLevel? = nil,
        energy: EnergyLevel? = nil,
        stress: StressLevel? = nil,
        soreness: Int? = nil,
        hydration: Int? = nil,
        notes: String = "",
        readinessScore: Int? = nil
    ) {
        self.id = id
        self.date = date
        self.sleepHours = sleepHours
        self.sleepQuality = sleepQuality
        self.restingHeartRate = restingHeartRate
        self.hrv = hrv
        self.mood = mood
        self.energy = energy
        self.stress = stress
        self.soreness = soreness
        self.hydration = hydration
        self.notes = notes
        self.readinessScore = readinessScore
    }
}

struct MindfulnessContent: Codable, Hashable, Identifiable {
    var id: String
    var title: String
    var contentDescription: String
    var type: MindfulnessType
    var durationSeconds: Int
    var audioUrl: String?
    var instructions: [String]
    var tags: [String]

    init(
        id: String,
        title: String,
        contentDescription: String,
        type: MindfulnessType,
        durationSeconds: Int,
        audioUrl: String? = nil,
        instructions: [String],
        tags: [String] = []
    ) {
        self.id = id
        self.title = title
        self.contentDescription = contentDescription
        self.type = type
        self.durationSeconds = durationSeconds
        self.audioUrl = audioUrl
        self.instructions = instructions
        self.tags = tags
    }
}
