import Foundation
import SwiftData

@Model
final class TrainingPlan {
    @Attribute(.unique) var id: UUID
    var name: String
    var planDescription: String
    var goalType: GoalType
    var targetDistance: Double?
    var targetTime: Int64?
    var startDate: Date
    var endDate: Date
    var weeklySchedule: [ScheduledWorkout]
    var isActive: Bool
    var createdAt: Date

    init(
        id: UUID = UUID(),
        name: String,
        planDescription: String,
        goalType: GoalType,
        targetDistance: Double? = nil,
        targetTime: Int64? = nil,
        startDate: Date,
        endDate: Date,
        weeklySchedule: [ScheduledWorkout] = [],
        isActive: Bool = true,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.planDescription = planDescription
        self.goalType = goalType
        self.targetDistance = targetDistance
        self.targetTime = targetTime
        self.startDate = startDate
        self.endDate = endDate
        self.weeklySchedule = weeklySchedule
        self.isActive = isActive
        self.createdAt = createdAt
    }
}

enum GoalType: String, Codable, CaseIterable {
    case first5K = "FIRST_5K"
    case improve5K = "IMPROVE_5K"
    case first10K = "FIRST_10K"
    case improve10K = "IMPROVE_10K"
    case halfMarathon = "HALF_MARATHON"
    case marathon = "MARATHON"
    case generalFitness = "GENERAL_FITNESS"
    case weightLoss = "WEIGHT_LOSS"
    case custom = "CUSTOM"
}

struct ScheduledWorkout: Codable, Hashable, Identifiable {
    var id: String
    var dayOfWeek: Int
    var weekNumber: Int
    var workoutType: WorkoutType
    var targetDistanceMeters: Double?
    var targetDurationMinutes: Int?
    var targetPaceSecondsPerKm: Double?
    var targetPaceMinSecondsPerKm: Double?
    var targetPaceMaxSecondsPerKm: Double?
    var targetHeartRateZone: HeartRateZone?
    var targetHeartRateMin: Int?
    var targetHeartRateMax: Int?
    var intervals: [Interval]?
    var workoutDescription: String
    var isCompleted: Bool
    var completedRunId: UUID?

    init(
        id: String = UUID().uuidString,
        dayOfWeek: Int,
        weekNumber: Int,
        workoutType: WorkoutType,
        targetDistanceMeters: Double? = nil,
        targetDurationMinutes: Int? = nil,
        targetPaceSecondsPerKm: Double? = nil,
        targetPaceMinSecondsPerKm: Double? = nil,
        targetPaceMaxSecondsPerKm: Double? = nil,
        targetHeartRateZone: HeartRateZone? = nil,
        targetHeartRateMin: Int? = nil,
        targetHeartRateMax: Int? = nil,
        intervals: [Interval]? = nil,
        workoutDescription: String,
        isCompleted: Bool = false,
        completedRunId: UUID? = nil
    ) {
        self.id = id
        self.dayOfWeek = dayOfWeek
        self.weekNumber = weekNumber
        self.workoutType = workoutType
        self.targetDistanceMeters = targetDistanceMeters
        self.targetDurationMinutes = targetDurationMinutes
        self.targetPaceSecondsPerKm = targetPaceSecondsPerKm
        self.targetPaceMinSecondsPerKm = targetPaceMinSecondsPerKm
        self.targetPaceMaxSecondsPerKm = targetPaceMaxSecondsPerKm
        self.targetHeartRateZone = targetHeartRateZone
        self.targetHeartRateMin = targetHeartRateMin
        self.targetHeartRateMax = targetHeartRateMax
        self.intervals = intervals
        self.workoutDescription = workoutDescription
        self.isCompleted = isCompleted
        self.completedRunId = completedRunId
    }
}

enum WorkoutType: String, Codable, CaseIterable {
    // Basic runs
    case easyRun = "EASY_RUN"
    case longRun = "LONG_RUN"
    case recoveryRun = "RECOVERY_RUN"

    // Speed work
    case tempoRun = "TEMPO_RUN"
    case intervalTraining = "INTERVAL_TRAINING"
    case fartlek = "FARTLEK"
    case racePace = "RACE_PACE"

    // Strength & hills
    case hillRepeats = "HILL_REPEATS"
    case hillSprints = "HILL_SPRINTS"
    case stairWorkout = "STAIR_WORKOUT"

    // Specialized
    case progressionRun = "PROGRESSION_RUN"
    case negativeSplit = "NEGATIVE_SPLIT"
    case thresholdRun = "THRESHOLD_RUN"
    case vo2MaxIntervals = "VO2_MAX_INTERVALS"
    case yasso800s = "YASSO_800S"
    case mileRepeats = "MILE_REPEATS"
    case ladderWorkout = "LADDER_WORKOUT"
    case pyramidWorkout = "PYRAMID_WORKOUT"

    // Race simulation
    case raceSimulation = "RACE_SIMULATION"
    case timeTrial = "TIME_TRIAL"
    case parkrunPrep = "PARKRUN_PREP"

    // Recovery & maintenance
    case shakeOutRun = "SHAKE_OUT_RUN"
    case baseBuilding = "BASE_BUILDING"
    case aerobicMaintenance = "AEROBIC_MAINTENANCE"

    // Cross-training
    case crossTraining = "CROSS_TRAINING"
    case cycling = "CYCLING"
    case swimming = "SWIMMING"
    case strengthTraining = "STRENGTH_TRAINING"
    case yogaStretching = "YOGA_STRETCHING"

    // Rest
    case restDay = "REST_DAY"
    case activeRecovery = "ACTIVE_RECOVERY"

    // Custom
    case custom = "CUSTOM"
}

struct Interval: Codable, Hashable {
    var type: IntervalType
    var durationSeconds: Int?
    var distanceMeters: Double?
    var targetPaceSecondsPerKm: Double?
    var targetPaceMinSecondsPerKm: Double?
    var targetPaceMaxSecondsPerKm: Double?
    var targetHeartRateZone: HeartRateZone?
    var targetHeartRateMin: Int?
    var targetHeartRateMax: Int?
    var repetitions: Int

    init(
        type: IntervalType,
        durationSeconds: Int? = nil,
        distanceMeters: Double? = nil,
        targetPaceSecondsPerKm: Double? = nil,
        targetPaceMinSecondsPerKm: Double? = nil,
        targetPaceMaxSecondsPerKm: Double? = nil,
        targetHeartRateZone: HeartRateZone? = nil,
        targetHeartRateMin: Int? = nil,
        targetHeartRateMax: Int? = nil,
        repetitions: Int = 1
    ) {
        self.type = type
        self.durationSeconds = durationSeconds
        self.distanceMeters = distanceMeters
        self.targetPaceSecondsPerKm = targetPaceSecondsPerKm
        self.targetPaceMinSecondsPerKm = targetPaceMinSecondsPerKm
        self.targetPaceMaxSecondsPerKm = targetPaceMaxSecondsPerKm
        self.targetHeartRateZone = targetHeartRateZone
        self.targetHeartRateMin = targetHeartRateMin
        self.targetHeartRateMax = targetHeartRateMax
        self.repetitions = repetitions
    }
}

enum IntervalType: String, Codable, CaseIterable {
    case warmup = "WARMUP"
    case work = "WORK"
    case recovery = "RECOVERY"
    case cooldown = "COOLDOWN"
}

enum HeartRateZone: String, Codable, CaseIterable {
    case zone1 = "ZONE_1"
    case zone2 = "ZONE_2"
    case zone3 = "ZONE_3"
    case zone4 = "ZONE_4"
    case zone5 = "ZONE_5"

    var displayName: String {
        switch self {
        case .zone1: return "Recovery"
        case .zone2: return "Aerobic Base"
        case .zone3: return "Aerobic Capacity"
        case .zone4: return "Threshold"
        case .zone5: return "VO2 Max"
        }
    }

    var minPercent: Int {
        switch self {
        case .zone1: return 50
        case .zone2: return 60
        case .zone3: return 70
        case .zone4: return 80
        case .zone5: return 90
        }
    }

    var maxPercent: Int {
        switch self {
        case .zone1: return 60
        case .zone2: return 70
        case .zone3: return 80
        case .zone4: return 90
        case .zone5: return 100
        }
    }
}

struct HeartRateZoneTime: Codable, Hashable {
    var zone: HeartRateZone
    var durationMillis: Int64
    var percentOfTotal: Double

    var durationFormatted: String {
        let totalSeconds = durationMillis / 1000
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    init(zone: HeartRateZone, durationMillis: Int64, percentOfTotal: Double = 0.0) {
        self.zone = zone
        self.durationMillis = durationMillis
        self.percentOfTotal = percentOfTotal
    }
}
