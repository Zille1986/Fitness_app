import Foundation
import SwiftData

@Model
final class SwimmingWorkout {
    @Attribute(.unique) var id: UUID
    var startTime: Date
    var endTime: Date?
    var swimType: SwimType
    var poolLength: PoolLength?
    var distanceMeters: Double
    var durationMillis: Int64
    var laps: Int
    var avgPaceSecondsPer100m: Double
    var bestPaceSecondsPer100m: Double
    var avgStrokeRate: Int?
    var avgHeartRate: Int?
    var maxHeartRate: Int?
    var heartRateZoneTimes: [HeartRateZoneTime]
    var caloriesBurned: Int
    var strokeType: StrokeType
    var swolf: Int?
    var waterTemperature: Double?
    var weather: String?
    var notes: String?
    var splits: [SwimSplit]
    var routePoints: [RoutePoint]
    var source: SwimSource
    var isCompleted: Bool

    var distanceKm: Double { distanceMeters / 1000.0 }
    var distanceYards: Double { distanceMeters * 1.09361 }

    var durationFormatted: String {
        let totalSeconds = durationMillis / 1000
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            return String(format: "%d:%02d", minutes, seconds)
        }
    }

    var avgPaceFormatted: String { SwimmingWorkout.formatPace(avgPaceSecondsPer100m) }

    static func formatPace(_ secondsPer100m: Double) -> String {
        if secondsPer100m <= 0 || secondsPer100m.isInfinite || secondsPer100m.isNaN { return "--:--" }
        let minutes = Int(secondsPer100m / 60)
        let seconds = Int(secondsPer100m.truncatingRemainder(dividingBy: 60))
        return String(format: "%d:%02d", minutes, seconds)
    }

    init(
        id: UUID = UUID(),
        startTime: Date,
        endTime: Date? = nil,
        swimType: SwimType,
        poolLength: PoolLength? = nil,
        distanceMeters: Double = 0.0,
        durationMillis: Int64 = 0,
        laps: Int = 0,
        avgPaceSecondsPer100m: Double = 0.0,
        bestPaceSecondsPer100m: Double = 0.0,
        avgStrokeRate: Int? = nil,
        avgHeartRate: Int? = nil,
        maxHeartRate: Int? = nil,
        heartRateZoneTimes: [HeartRateZoneTime] = [],
        caloriesBurned: Int = 0,
        strokeType: StrokeType = .freestyle,
        swolf: Int? = nil,
        waterTemperature: Double? = nil,
        weather: String? = nil,
        notes: String? = nil,
        splits: [SwimSplit] = [],
        routePoints: [RoutePoint] = [],
        source: SwimSource = .manual,
        isCompleted: Bool = false
    ) {
        self.id = id
        self.startTime = startTime
        self.endTime = endTime
        self.swimType = swimType
        self.poolLength = poolLength
        self.distanceMeters = distanceMeters
        self.durationMillis = durationMillis
        self.laps = laps
        self.avgPaceSecondsPer100m = avgPaceSecondsPer100m
        self.bestPaceSecondsPer100m = bestPaceSecondsPer100m
        self.avgStrokeRate = avgStrokeRate
        self.avgHeartRate = avgHeartRate
        self.maxHeartRate = maxHeartRate
        self.heartRateZoneTimes = heartRateZoneTimes
        self.caloriesBurned = caloriesBurned
        self.strokeType = strokeType
        self.swolf = swolf
        self.waterTemperature = waterTemperature
        self.weather = weather
        self.notes = notes
        self.splits = splits
        self.routePoints = routePoints
        self.source = source
        self.isCompleted = isCompleted
    }
}

enum SwimType: String, Codable, CaseIterable, Identifiable {
    var id: String { rawValue }
    case pool = "POOL"
    case ocean = "OCEAN"
    case lake = "LAKE"
    case river = "RIVER"

    var displayName: String {
        switch self {
        case .pool: return "Pool"
        case .ocean: return "Ocean/Sea"
        case .lake: return "Lake"
        case .river: return "River"
        }
    }

    var icon: String {
        switch self {
        case .pool: return "figure.pool.swim"
        case .ocean: return "water.waves"
        case .lake: return "water.waves"
        case .river: return "water.waves"
        }
    }
}

enum PoolLength: String, Codable, CaseIterable, Identifiable {
    var id: String { rawValue }
    case shortCourseMeters = "SHORT_COURSE_METERS"
    case longCourseMeters = "LONG_COURSE_METERS"
    case shortCourseYards = "SHORT_COURSE_YARDS"
    case custom = "CUSTOM"

    var meters: Int {
        switch self {
        case .shortCourseMeters: return 25
        case .longCourseMeters: return 50
        case .shortCourseYards: return 23
        case .custom: return 0
        }
    }

    var displayName: String {
        switch self {
        case .shortCourseMeters: return "25m"
        case .longCourseMeters: return "50m"
        case .shortCourseYards: return "25yd"
        case .custom: return "Custom"
        }
    }
}

enum StrokeType: String, Codable, CaseIterable {
    case freestyle = "FREESTYLE"
    case backstroke = "BACKSTROKE"
    case breaststroke = "BREASTSTROKE"
    case butterfly = "BUTTERFLY"
    case individualMedley = "INDIVIDUAL_MEDLEY"
    case mixed = "MIXED"

    var displayName: String {
        switch self {
        case .freestyle: return "Freestyle"
        case .backstroke: return "Backstroke"
        case .breaststroke: return "Breaststroke"
        case .butterfly: return "Butterfly"
        case .individualMedley: return "Individual Medley"
        case .mixed: return "Mixed"
        }
    }
}

enum SwimSource: String, Codable, CaseIterable {
    case manual = "MANUAL"
    case watch = "WATCH"
    case phone = "PHONE"
}

struct SwimSplit: Codable, Hashable, Identifiable {
    var id: Int { lapNumber }
    var lapNumber: Int
    var distanceMeters: Double
    var durationMillis: Int64
    var paceSecondsPer100m: Double
    var strokeCount: Int?
    var strokeType: StrokeType
    var avgHeartRate: Int?

    var paceFormatted: String { SwimmingWorkout.formatPace(paceSecondsPer100m) }

    var durationFormatted: String {
        let totalSeconds = durationMillis / 1000
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    init(
        lapNumber: Int,
        distanceMeters: Double,
        durationMillis: Int64,
        paceSecondsPer100m: Double,
        strokeCount: Int? = nil,
        strokeType: StrokeType = .freestyle,
        avgHeartRate: Int? = nil
    ) {
        self.lapNumber = lapNumber
        self.distanceMeters = distanceMeters
        self.durationMillis = durationMillis
        self.paceSecondsPer100m = paceSecondsPer100m
        self.strokeCount = strokeCount
        self.strokeType = strokeType
        self.avgHeartRate = avgHeartRate
    }
}

@Model
final class SwimmingTrainingPlan {
    @Attribute(.unique) var id: UUID
    var name: String
    var planDescription: String
    var goalType: SwimGoalType
    var targetDistance: Double?
    var targetTime: Int64?
    var startDate: Date
    var endDate: Date
    var weeklySchedule: [ScheduledSwimWorkout]
    var isActive: Bool
    var createdAt: Date

    init(
        id: UUID = UUID(),
        name: String,
        planDescription: String,
        goalType: SwimGoalType,
        targetDistance: Double? = nil,
        targetTime: Int64? = nil,
        startDate: Date,
        endDate: Date,
        weeklySchedule: [ScheduledSwimWorkout] = [],
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

enum SwimGoalType: String, Codable, CaseIterable {
    case first500m = "FIRST_500M"
    case first1K = "FIRST_1K"
    case firstMile = "FIRST_MILE"
    case improveSpeed = "IMPROVE_SPEED"
    case openWaterPrep = "OPEN_WATER_PREP"
    case triathlonPrep = "TRIATHLON_PREP"
    case generalFitness = "GENERAL_FITNESS"
    case techniqueFocus = "TECHNIQUE_FOCUS"
    case custom = "CUSTOM"

    var displayName: String {
        switch self {
        case .first500m: return "First 500m"
        case .first1K: return "First 1km"
        case .firstMile: return "First Mile"
        case .improveSpeed: return "Improve Speed"
        case .openWaterPrep: return "Open Water Prep"
        case .triathlonPrep: return "Triathlon Prep"
        case .generalFitness: return "General Fitness"
        case .techniqueFocus: return "Technique Focus"
        case .custom: return "Custom"
        }
    }
}

struct ScheduledSwimWorkout: Codable, Hashable, Identifiable {
    var id: String
    var dayOfWeek: Int
    var weekNumber: Int
    var workoutType: SwimWorkoutType
    var swimType: SwimType
    var targetDistanceMeters: Double?
    var targetDurationMinutes: Int?
    var targetPaceSecondsPer100m: Double?
    var targetHeartRateZone: HeartRateZone?
    var targetHeartRateMin: Int?
    var targetHeartRateMax: Int?
    var sets: [SwimSet]?
    var workoutDescription: String
    var isCompleted: Bool
    var completedWorkoutId: UUID?

    init(
        id: String = UUID().uuidString,
        dayOfWeek: Int,
        weekNumber: Int,
        workoutType: SwimWorkoutType,
        swimType: SwimType = .pool,
        targetDistanceMeters: Double? = nil,
        targetDurationMinutes: Int? = nil,
        targetPaceSecondsPer100m: Double? = nil,
        targetHeartRateZone: HeartRateZone? = nil,
        targetHeartRateMin: Int? = nil,
        targetHeartRateMax: Int? = nil,
        sets: [SwimSet]? = nil,
        workoutDescription: String,
        isCompleted: Bool = false,
        completedWorkoutId: UUID? = nil
    ) {
        self.id = id
        self.dayOfWeek = dayOfWeek
        self.weekNumber = weekNumber
        self.workoutType = workoutType
        self.swimType = swimType
        self.targetDistanceMeters = targetDistanceMeters
        self.targetDurationMinutes = targetDurationMinutes
        self.targetPaceSecondsPer100m = targetPaceSecondsPer100m
        self.targetHeartRateZone = targetHeartRateZone
        self.targetHeartRateMin = targetHeartRateMin
        self.targetHeartRateMax = targetHeartRateMax
        self.sets = sets
        self.workoutDescription = workoutDescription
        self.isCompleted = isCompleted
        self.completedWorkoutId = completedWorkoutId
    }
}

enum SwimWorkoutType: String, Codable, CaseIterable {
    case easySwim = "EASY_SWIM"
    case enduranceSwim = "ENDURANCE_SWIM"
    case techniqueDrills = "TECHNIQUE_DRILLS"
    case intervalTraining = "INTERVAL_TRAINING"
    case sprintSets = "SPRINT_SETS"
    case kickSets = "KICK_SETS"
    case pullSets = "PULL_SETS"
    case openWaterPractice = "OPEN_WATER_PRACTICE"
    case timeTrial = "TIME_TRIAL"
    case recoverySwim = "RECOVERY_SWIM"
    case warmUp = "WARM_UP"
    case coolDown = "COOL_DOWN"
    case restDay = "REST_DAY"
    case custom = "CUSTOM"

    var displayName: String {
        switch self {
        case .easySwim: return "Easy Swim"
        case .enduranceSwim: return "Endurance Swim"
        case .techniqueDrills: return "Technique Drills"
        case .intervalTraining: return "Interval Training"
        case .sprintSets: return "Sprint Sets"
        case .kickSets: return "Kick Sets"
        case .pullSets: return "Pull Sets"
        case .openWaterPractice: return "Open Water Practice"
        case .timeTrial: return "Time Trial"
        case .recoverySwim: return "Recovery Swim"
        case .warmUp: return "Warm Up"
        case .coolDown: return "Cool Down"
        case .restDay: return "Rest Day"
        case .custom: return "Custom"
        }
    }
}

struct SwimSet: Codable, Hashable {
    var repetitions: Int
    var distanceMeters: Double
    var strokeType: StrokeType
    var targetPaceSecondsPer100m: Double?
    var restSeconds: Int
    var setDescription: String?

    init(
        repetitions: Int,
        distanceMeters: Double,
        strokeType: StrokeType = .freestyle,
        targetPaceSecondsPer100m: Double? = nil,
        restSeconds: Int = 30,
        setDescription: String? = nil
    ) {
        self.repetitions = repetitions
        self.distanceMeters = distanceMeters
        self.strokeType = strokeType
        self.targetPaceSecondsPer100m = targetPaceSecondsPer100m
        self.restSeconds = restSeconds
        self.setDescription = setDescription
    }
}
