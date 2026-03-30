import Foundation
import SwiftData

@Model
final class CyclingWorkout {
    @Attribute(.unique) var id: UUID
    var startTime: Date
    var endTime: Date?
    var cyclingType: CyclingType
    var distanceMeters: Double
    var durationMillis: Int64
    var avgSpeedKmh: Double
    var maxSpeedKmh: Double
    var avgPowerWatts: Int?
    var maxPowerWatts: Int?
    var normalizedPower: Int?
    var avgCadenceRpm: Int?
    var maxCadenceRpm: Int?
    var avgHeartRate: Int?
    var maxHeartRate: Int?
    var heartRateZoneTimes: [HeartRateZoneTime]
    var caloriesBurned: Int
    var elevationGainMeters: Double
    var elevationLossMeters: Double
    var trainingStressScore: Double?
    var intensityFactor: Double?
    var weather: String?
    var temperature: Double?
    var notes: String?
    var routePoints: [RoutePoint]
    var splits: [CyclingSplit]
    var powerData: [PowerDataPoint]
    var source: CyclingSource
    var smartTrainerBrand: String?
    var smartTrainerModel: String?
    var isCompleted: Bool

    var distanceKm: Double { distanceMeters / 1000.0 }
    var distanceMiles: Double { distanceMeters / 1609.344 }

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

    var avgSpeedFormatted: String { String(format: "%.1f km/h", avgSpeedKmh) }
    var isSmartTrainerWorkout: Bool { cyclingType == .smartTrainer }

    init(
        id: UUID = UUID(),
        startTime: Date,
        endTime: Date? = nil,
        cyclingType: CyclingType,
        distanceMeters: Double = 0.0,
        durationMillis: Int64 = 0,
        avgSpeedKmh: Double = 0.0,
        maxSpeedKmh: Double = 0.0,
        avgPowerWatts: Int? = nil,
        maxPowerWatts: Int? = nil,
        normalizedPower: Int? = nil,
        avgCadenceRpm: Int? = nil,
        maxCadenceRpm: Int? = nil,
        avgHeartRate: Int? = nil,
        maxHeartRate: Int? = nil,
        heartRateZoneTimes: [HeartRateZoneTime] = [],
        caloriesBurned: Int = 0,
        elevationGainMeters: Double = 0.0,
        elevationLossMeters: Double = 0.0,
        trainingStressScore: Double? = nil,
        intensityFactor: Double? = nil,
        weather: String? = nil,
        temperature: Double? = nil,
        notes: String? = nil,
        routePoints: [RoutePoint] = [],
        splits: [CyclingSplit] = [],
        powerData: [PowerDataPoint] = [],
        source: CyclingSource = .manual,
        smartTrainerBrand: String? = nil,
        smartTrainerModel: String? = nil,
        isCompleted: Bool = false
    ) {
        self.id = id
        self.startTime = startTime
        self.endTime = endTime
        self.cyclingType = cyclingType
        self.distanceMeters = distanceMeters
        self.durationMillis = durationMillis
        self.avgSpeedKmh = avgSpeedKmh
        self.maxSpeedKmh = maxSpeedKmh
        self.avgPowerWatts = avgPowerWatts
        self.maxPowerWatts = maxPowerWatts
        self.normalizedPower = normalizedPower
        self.avgCadenceRpm = avgCadenceRpm
        self.maxCadenceRpm = maxCadenceRpm
        self.avgHeartRate = avgHeartRate
        self.maxHeartRate = maxHeartRate
        self.heartRateZoneTimes = heartRateZoneTimes
        self.caloriesBurned = caloriesBurned
        self.elevationGainMeters = elevationGainMeters
        self.elevationLossMeters = elevationLossMeters
        self.trainingStressScore = trainingStressScore
        self.intensityFactor = intensityFactor
        self.weather = weather
        self.temperature = temperature
        self.notes = notes
        self.routePoints = routePoints
        self.splits = splits
        self.powerData = powerData
        self.source = source
        self.smartTrainerBrand = smartTrainerBrand
        self.smartTrainerModel = smartTrainerModel
        self.isCompleted = isCompleted
    }
}

enum CyclingType: String, Codable, CaseIterable, Identifiable {
    case outdoor = "OUTDOOR"
    case smartTrainer = "SMART_TRAINER"
    case stationaryBike = "STATIONARY_BIKE"
    case spinClass = "SPIN_CLASS"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .outdoor: return "Outdoor Ride"
        case .smartTrainer: return "Smart Trainer"
        case .stationaryBike: return "Stationary Bike"
        case .spinClass: return "Spin Class"
        }
    }

    var icon: String {
        switch self {
        case .outdoor: return "bicycle"
        case .smartTrainer: return "figure.indoor.cycle"
        case .stationaryBike: return "figure.indoor.cycle"
        case .spinClass: return "figure.indoor.cycle"
        }
    }
}

enum CyclingSource: String, Codable, CaseIterable {
    case manual = "MANUAL"
    case phoneGps = "PHONE_GPS"
    case smartTrainer = "SMART_TRAINER"
    case watch = "WATCH"
    case strava = "STRAVA"
}

struct CyclingSplit: Codable, Hashable {
    var kilometer: Int
    var durationMillis: Int64
    var avgSpeedKmh: Double
    var avgPowerWatts: Int?
    var avgCadenceRpm: Int?
    var elevationChange: Double
    var avgHeartRate: Int?

    var durationFormatted: String {
        let totalSeconds = durationMillis / 1000
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    init(
        kilometer: Int,
        durationMillis: Int64,
        avgSpeedKmh: Double,
        avgPowerWatts: Int? = nil,
        avgCadenceRpm: Int? = nil,
        elevationChange: Double = 0.0,
        avgHeartRate: Int? = nil
    ) {
        self.kilometer = kilometer
        self.durationMillis = durationMillis
        self.avgSpeedKmh = avgSpeedKmh
        self.avgPowerWatts = avgPowerWatts
        self.avgCadenceRpm = avgCadenceRpm
        self.elevationChange = elevationChange
        self.avgHeartRate = avgHeartRate
    }
}

struct PowerDataPoint: Codable, Hashable {
    var timestamp: Date
    var powerWatts: Int
    var cadenceRpm: Int?
    var heartRate: Int?
    var speedKmh: Double?
    var resistanceLevel: Int?
}

@Model
final class CyclingTrainingPlan {
    @Attribute(.unique) var id: UUID
    var name: String
    var planDescription: String
    var goalType: CyclingGoalType
    var targetDistance: Double?
    var targetTime: Int64?
    var ftp: Int?
    var startDate: Date
    var endDate: Date
    var weeklySchedule: [ScheduledCyclingWorkout]
    var isActive: Bool
    var createdAt: Date

    init(
        id: UUID = UUID(),
        name: String,
        planDescription: String,
        goalType: CyclingGoalType,
        targetDistance: Double? = nil,
        targetTime: Int64? = nil,
        ftp: Int? = nil,
        startDate: Date,
        endDate: Date,
        weeklySchedule: [ScheduledCyclingWorkout] = [],
        isActive: Bool = true,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.planDescription = planDescription
        self.goalType = goalType
        self.targetDistance = targetDistance
        self.targetTime = targetTime
        self.ftp = ftp
        self.startDate = startDate
        self.endDate = endDate
        self.weeklySchedule = weeklySchedule
        self.isActive = isActive
        self.createdAt = createdAt
    }
}

enum CyclingGoalType: String, Codable, CaseIterable {
    case first50K = "FIRST_50K"
    case first100K = "FIRST_100K"
    case improveFTP = "IMPROVE_FTP"
    case centuryRide = "CENTURY_RIDE"
    case granFondo = "GRAN_FONDO"
    case triathlonPrep = "TRIATHLON_PREP"
    case generalFitness = "GENERAL_FITNESS"
    case weightLoss = "WEIGHT_LOSS"
    case custom = "CUSTOM"

    var displayName: String {
        switch self {
        case .first50K: return "First 50km"
        case .first100K: return "First 100km"
        case .improveFTP: return "Improve FTP"
        case .centuryRide: return "Century Ride (100mi)"
        case .granFondo: return "Gran Fondo"
        case .triathlonPrep: return "Triathlon Prep"
        case .generalFitness: return "General Fitness"
        case .weightLoss: return "Weight Loss"
        case .custom: return "Custom"
        }
    }
}

struct ScheduledCyclingWorkout: Codable, Hashable, Identifiable {
    var id: String
    var dayOfWeek: Int
    var weekNumber: Int
    var workoutType: CyclingWorkoutType
    var cyclingType: CyclingType
    var targetDistanceMeters: Double?
    var targetDurationMinutes: Int?
    var targetPowerWatts: Int?
    var targetPowerPercentFtp: Int?
    var intervals: [CyclingInterval]?
    var workoutDescription: String
    var isCompleted: Bool
    var completedWorkoutId: UUID?

    init(
        id: String = UUID().uuidString,
        dayOfWeek: Int,
        weekNumber: Int,
        workoutType: CyclingWorkoutType,
        cyclingType: CyclingType = .outdoor,
        targetDistanceMeters: Double? = nil,
        targetDurationMinutes: Int? = nil,
        targetPowerWatts: Int? = nil,
        targetPowerPercentFtp: Int? = nil,
        intervals: [CyclingInterval]? = nil,
        workoutDescription: String,
        isCompleted: Bool = false,
        completedWorkoutId: UUID? = nil
    ) {
        self.id = id
        self.dayOfWeek = dayOfWeek
        self.weekNumber = weekNumber
        self.workoutType = workoutType
        self.cyclingType = cyclingType
        self.targetDistanceMeters = targetDistanceMeters
        self.targetDurationMinutes = targetDurationMinutes
        self.targetPowerWatts = targetPowerWatts
        self.targetPowerPercentFtp = targetPowerPercentFtp
        self.intervals = intervals
        self.workoutDescription = workoutDescription
        self.isCompleted = isCompleted
        self.completedWorkoutId = completedWorkoutId
    }
}

enum CyclingWorkoutType: String, Codable, CaseIterable {
    case easyRide = "EASY_RIDE"
    case enduranceRide = "ENDURANCE_RIDE"
    case tempoRide = "TEMPO_RIDE"
    case sweetSpot = "SWEET_SPOT"
    case thresholdIntervals = "THRESHOLD_INTERVALS"
    case vo2MaxIntervals = "VO2_MAX_INTERVALS"
    case sprintIntervals = "SPRINT_INTERVALS"
    case hillRepeats = "HILL_REPEATS"
    case recoveryRide = "RECOVERY_RIDE"
    case longRide = "LONG_RIDE"
    case raceSimulation = "RACE_SIMULATION"
    case ftpTest = "FTP_TEST"
    case cadenceDrills = "CADENCE_DRILLS"
    case strengthEndurance = "STRENGTH_ENDURANCE"
    case restDay = "REST_DAY"
    case custom = "CUSTOM"

    var displayName: String {
        switch self {
        case .easyRide: return "Easy Ride"
        case .enduranceRide: return "Endurance Ride"
        case .tempoRide: return "Tempo Ride"
        case .sweetSpot: return "Sweet Spot"
        case .thresholdIntervals: return "Threshold Intervals"
        case .vo2MaxIntervals: return "VO2 Max Intervals"
        case .sprintIntervals: return "Sprint Intervals"
        case .hillRepeats: return "Hill Repeats"
        case .recoveryRide: return "Recovery Ride"
        case .longRide: return "Long Ride"
        case .raceSimulation: return "Race Simulation"
        case .ftpTest: return "FTP Test"
        case .cadenceDrills: return "Cadence Drills"
        case .strengthEndurance: return "Strength Endurance"
        case .restDay: return "Rest Day"
        case .custom: return "Custom"
        }
    }
}

struct CyclingInterval: Codable, Hashable {
    var type: CyclingIntervalType
    var durationSeconds: Int?
    var distanceMeters: Double?
    var targetPowerWatts: Int?
    var targetPowerPercentFtp: Int?
    var targetCadenceRpm: Int?
    var targetHeartRateZone: HeartRateZone?
    var resistanceLevel: Int?
    var repetitions: Int

    init(
        type: CyclingIntervalType,
        durationSeconds: Int? = nil,
        distanceMeters: Double? = nil,
        targetPowerWatts: Int? = nil,
        targetPowerPercentFtp: Int? = nil,
        targetCadenceRpm: Int? = nil,
        targetHeartRateZone: HeartRateZone? = nil,
        resistanceLevel: Int? = nil,
        repetitions: Int = 1
    ) {
        self.type = type
        self.durationSeconds = durationSeconds
        self.distanceMeters = distanceMeters
        self.targetPowerWatts = targetPowerWatts
        self.targetPowerPercentFtp = targetPowerPercentFtp
        self.targetCadenceRpm = targetCadenceRpm
        self.targetHeartRateZone = targetHeartRateZone
        self.resistanceLevel = resistanceLevel
        self.repetitions = repetitions
    }
}

enum CyclingIntervalType: String, Codable, CaseIterable {
    case warmup = "WARMUP"
    case work = "WORK"
    case recovery = "RECOVERY"
    case cooldown = "COOLDOWN"
    case sprint = "SPRINT"
    case climb = "CLIMB"
}

struct SmartTrainerDevice: Codable, Hashable, Identifiable {
    var id: String
    var name: String
    var brand: String
    var model: String?
    var address: String
    var supportsFTMS: Bool
    var supportsResistanceControl: Bool
    var supportsSimulationMode: Bool
    var maxResistanceLevel: Int?
    var lastConnected: Date?

    init(
        id: String,
        name: String,
        brand: String,
        model: String? = nil,
        address: String,
        supportsFTMS: Bool = true,
        supportsResistanceControl: Bool = false,
        supportsSimulationMode: Bool = false,
        maxResistanceLevel: Int? = nil,
        lastConnected: Date? = nil
    ) {
        self.id = id
        self.name = name
        self.brand = brand
        self.model = model
        self.address = address
        self.supportsFTMS = supportsFTMS
        self.supportsResistanceControl = supportsResistanceControl
        self.supportsSimulationMode = supportsSimulationMode
        self.maxResistanceLevel = maxResistanceLevel
        self.lastConnected = lastConnected
    }
}

struct SmartTrainerStatus: Codable, Hashable {
    var isConnected: Bool
    var currentPowerWatts: Int
    var currentCadenceRpm: Int
    var currentSpeedKmh: Double
    var currentHeartRate: Int?
    var currentResistanceLevel: Int
    var batteryLevel: Int?

    init(
        isConnected: Bool = false,
        currentPowerWatts: Int = 0,
        currentCadenceRpm: Int = 0,
        currentSpeedKmh: Double = 0.0,
        currentHeartRate: Int? = nil,
        currentResistanceLevel: Int = 0,
        batteryLevel: Int? = nil
    ) {
        self.isConnected = isConnected
        self.currentPowerWatts = currentPowerWatts
        self.currentCadenceRpm = currentCadenceRpm
        self.currentSpeedKmh = currentSpeedKmh
        self.currentHeartRate = currentHeartRate
        self.currentResistanceLevel = currentResistanceLevel
        self.batteryLevel = batteryLevel
    }
}
