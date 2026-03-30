import Foundation
import SwiftData

@Model
final class CustomRunWorkout {
    @Attribute(.unique) var id: UUID
    var name: String
    var workoutDescription: String
    var phases: [WorkoutPhase]
    var estimatedDurationMinutes: Int
    var estimatedDistanceMeters: Double
    var difficulty: RunDifficulty
    var category: WorkoutCategory
    var isFavorite: Bool
    var timesUsed: Int
    var lastUsed: Date?
    var createdAt: Date

    var totalDurationSeconds: Int {
        phases.reduce(0) { $0 + ($1.durationSeconds ?? 0) * $1.repetitions }
    }

    var formattedDuration: String {
        let totalSeconds = totalDurationSeconds
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        return hours > 0 ? "\(hours)h \(minutes)m" : "\(minutes)m"
    }

    init(
        id: UUID = UUID(),
        name: String,
        workoutDescription: String = "",
        phases: [WorkoutPhase] = [],
        estimatedDurationMinutes: Int = 0,
        estimatedDistanceMeters: Double = 0.0,
        difficulty: RunDifficulty = .moderate,
        category: WorkoutCategory = .general,
        isFavorite: Bool = false,
        timesUsed: Int = 0,
        lastUsed: Date? = nil,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.workoutDescription = workoutDescription
        self.phases = phases
        self.estimatedDurationMinutes = estimatedDurationMinutes
        self.estimatedDistanceMeters = estimatedDistanceMeters
        self.difficulty = difficulty
        self.category = category
        self.isFavorite = isFavorite
        self.timesUsed = timesUsed
        self.lastUsed = lastUsed
        self.createdAt = createdAt
    }
}

struct WorkoutPhase: Codable, Hashable, Identifiable {
    var id: String
    var type: PhaseType
    var name: String
    var durationSeconds: Int?
    var distanceMeters: Double?
    var targetPaceMin: Double?
    var targetPaceMax: Double?
    var targetHeartRateMin: Int?
    var targetHeartRateMax: Int?
    var targetHeartRateZone: HeartRateZone?
    var effort: EffortLevel
    var repetitions: Int
    var orderIndex: Int
    var notes: String?

    var durationFormatted: String {
        guard let seconds = durationSeconds else { return "--" }
        let mins = seconds / 60
        let secs = seconds % 60
        if mins > 0 {
            return secs > 0 ? "\(mins)m \(secs)s" : "\(mins)m"
        } else {
            return "\(secs)s"
        }
    }

    var distanceFormatted: String {
        guard let meters = distanceMeters else { return "--" }
        if meters >= 1000 {
            return String(format: "%.1f km", meters / 1000)
        } else {
            return "\(Int(meters))m"
        }
    }

    var paceRangeFormatted: String {
        guard let minPace = targetPaceMin else { return "--" }
        let minFormatted = formatPace(minPace)
        if let maxPace = targetPaceMax, maxPace != minPace {
            return "\(minFormatted) - \(formatPace(maxPace))"
        }
        return minFormatted
    }

    private func formatPace(_ secondsPerKm: Double) -> String {
        let mins = Int(secondsPerKm / 60)
        let secs = Int(secondsPerKm.truncatingRemainder(dividingBy: 60))
        return "\(mins):\(String(format: "%02d", secs))"
    }

    init(
        id: String = UUID().uuidString,
        type: PhaseType,
        name: String = "",
        durationSeconds: Int? = nil,
        distanceMeters: Double? = nil,
        targetPaceMin: Double? = nil,
        targetPaceMax: Double? = nil,
        targetHeartRateMin: Int? = nil,
        targetHeartRateMax: Int? = nil,
        targetHeartRateZone: HeartRateZone? = nil,
        effort: EffortLevel = .moderate,
        repetitions: Int = 1,
        orderIndex: Int = 0,
        notes: String? = nil
    ) {
        self.id = id
        self.type = type
        self.name = name
        self.durationSeconds = durationSeconds
        self.distanceMeters = distanceMeters
        self.targetPaceMin = targetPaceMin
        self.targetPaceMax = targetPaceMax
        self.targetHeartRateMin = targetHeartRateMin
        self.targetHeartRateMax = targetHeartRateMax
        self.targetHeartRateZone = targetHeartRateZone
        self.effort = effort
        self.repetitions = repetitions
        self.orderIndex = orderIndex
        self.notes = notes
    }
}

enum PhaseType: String, Codable, CaseIterable {
    case warmup = "WARMUP"
    case work = "WORK"
    case recovery = "RECOVERY"
    case rest = "REST"
    case cooldown = "COOLDOWN"
    case stride = "STRIDE"
    case float = "FLOAT"
    case surge = "SURGE"
    case tempo = "TEMPO"
    case sprint = "SPRINT"
}

enum EffortLevel: String, Codable, CaseIterable {
    case veryEasy = "VERY_EASY"
    case easy = "EASY"
    case moderate = "MODERATE"
    case hard = "HARD"
    case veryHard = "VERY_HARD"
    case allOut = "ALL_OUT"
}

enum RunDifficulty: String, Codable, CaseIterable {
    case easy = "EASY"
    case moderate = "MODERATE"
    case hard = "HARD"
    case veryHard = "VERY_HARD"
}

enum WorkoutCategory: String, Codable, CaseIterable {
    case general = "GENERAL"
    case speed = "SPEED"
    case endurance = "ENDURANCE"
    case strength = "STRENGTH"
    case recovery = "RECOVERY"
    case racePrep = "RACE_PREP"
    case custom = "CUSTOM"
}

@Model
final class CustomTrainingPlan {
    @Attribute(.unique) var id: UUID
    var name: String
    var planDescription: String
    var goalType: GoalType
    var targetRaceDistance: Double?
    var targetRaceTime: Int64?
    var targetRaceDate: Date?
    var durationWeeks: Int
    var startDate: Date?
    var weeks: [PlanWeek]
    var currentWeek: Int
    var isActive: Bool
    var isFavorite: Bool
    var createdAt: Date
    var lastModified: Date

    var totalWorkouts: Int {
        weeks.reduce(0) { $0 + $1.workouts.count }
    }

    var completedWorkouts: Int {
        weeks.reduce(0) { sum, week in sum + week.workouts.filter(\.isCompleted).count }
    }

    var progressPercent: Float {
        totalWorkouts > 0 ? Float(completedWorkouts) / Float(totalWorkouts) : 0
    }

    init(
        id: UUID = UUID(),
        name: String,
        planDescription: String = "",
        goalType: GoalType = .custom,
        targetRaceDistance: Double? = nil,
        targetRaceTime: Int64? = nil,
        targetRaceDate: Date? = nil,
        durationWeeks: Int = 12,
        startDate: Date? = nil,
        weeks: [PlanWeek] = [],
        currentWeek: Int = 1,
        isActive: Bool = false,
        isFavorite: Bool = false,
        createdAt: Date = Date(),
        lastModified: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.planDescription = planDescription
        self.goalType = goalType
        self.targetRaceDistance = targetRaceDistance
        self.targetRaceTime = targetRaceTime
        self.targetRaceDate = targetRaceDate
        self.durationWeeks = durationWeeks
        self.startDate = startDate
        self.weeks = weeks
        self.currentWeek = currentWeek
        self.isActive = isActive
        self.isFavorite = isFavorite
        self.createdAt = createdAt
        self.lastModified = lastModified
    }
}

struct PlanWeek: Codable, Hashable {
    var weekNumber: Int
    var name: String
    var weekDescription: String
    var weekType: WeekType
    var workouts: [PlanWorkout]
    var totalDistanceMeters: Double
    var totalDurationMinutes: Int

    init(
        weekNumber: Int,
        name: String? = nil,
        weekDescription: String = "",
        weekType: WeekType = .build,
        workouts: [PlanWorkout] = [],
        totalDistanceMeters: Double = 0.0,
        totalDurationMinutes: Int = 0
    ) {
        self.weekNumber = weekNumber
        self.name = name ?? "Week \(weekNumber)"
        self.weekDescription = weekDescription
        self.weekType = weekType
        self.workouts = workouts
        self.totalDistanceMeters = totalDistanceMeters
        self.totalDurationMinutes = totalDurationMinutes
    }
}

struct PlanWorkout: Codable, Hashable, Identifiable {
    var id: String
    var dayOfWeek: Int
    var workoutType: WorkoutType
    var customWorkoutId: UUID?
    var name: String
    var workoutDescription: String
    var targetDistanceMeters: Double?
    var targetDurationMinutes: Int?
    var targetPaceMin: Double?
    var targetPaceMax: Double?
    var intervals: [Interval]?
    var isCompleted: Bool
    var completedRunId: UUID?
    var completedDate: Date?
    var notes: String?

    init(
        id: String = UUID().uuidString,
        dayOfWeek: Int,
        workoutType: WorkoutType,
        customWorkoutId: UUID? = nil,
        name: String = "",
        workoutDescription: String = "",
        targetDistanceMeters: Double? = nil,
        targetDurationMinutes: Int? = nil,
        targetPaceMin: Double? = nil,
        targetPaceMax: Double? = nil,
        intervals: [Interval]? = nil,
        isCompleted: Bool = false,
        completedRunId: UUID? = nil,
        completedDate: Date? = nil,
        notes: String? = nil
    ) {
        self.id = id
        self.dayOfWeek = dayOfWeek
        self.workoutType = workoutType
        self.customWorkoutId = customWorkoutId
        self.name = name
        self.workoutDescription = workoutDescription
        self.targetDistanceMeters = targetDistanceMeters
        self.targetDurationMinutes = targetDurationMinutes
        self.targetPaceMin = targetPaceMin
        self.targetPaceMax = targetPaceMax
        self.intervals = intervals
        self.isCompleted = isCompleted
        self.completedRunId = completedRunId
        self.completedDate = completedDate
        self.notes = notes
    }
}

enum WeekType: String, Codable, CaseIterable {
    case base = "BASE"
    case build = "BUILD"
    case peak = "PEAK"
    case taper = "TAPER"
    case recovery = "RECOVERY"
    case race = "RACE"
}
