import Foundation
import HealthKit
import SwiftUI

// MARK: - Workout Activity Types

enum WatchActivityType: String, CaseIterable, Identifiable, Codable {
    case outdoorRun = "Outdoor Run"
    case indoorRun = "Indoor Run"
    case gym = "Gym"
    case poolSwim = "Pool Swim"
    case openWaterSwim = "Open Water Swim"
    case outdoorCycle = "Outdoor Cycle"
    case indoorCycle = "Indoor Cycle"
    case hiit = "HIIT"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .outdoorRun: return "figure.run"
        case .indoorRun: return "figure.run"
        case .gym: return "dumbbell.fill"
        case .poolSwim: return "figure.pool.swim"
        case .openWaterSwim: return "figure.open.water.swim"
        case .outdoorCycle: return "bicycle"
        case .indoorCycle: return "bicycle"
        case .hiit: return "flame.fill"
        }
    }

    var hkActivityType: HKWorkoutActivityType {
        switch self {
        case .outdoorRun, .indoorRun: return .running
        case .gym: return .traditionalStrengthTraining
        case .poolSwim, .openWaterSwim: return .swimming
        case .outdoorCycle, .indoorCycle: return .cycling
        case .hiit: return .highIntensityIntervalTraining
        }
    }

    var hkLocationType: HKWorkoutSessionLocationType {
        switch self {
        case .outdoorRun, .openWaterSwim, .outdoorCycle: return .outdoor
        case .indoorRun, .poolSwim, .gym, .indoorCycle, .hiit: return .indoor
        }
    }

    var needsGPS: Bool {
        switch self {
        case .outdoorRun, .openWaterSwim, .outdoorCycle: return true
        default: return false
        }
    }

    var isSwimming: Bool {
        self == .poolSwim || self == .openWaterSwim
    }

    var isRunning: Bool {
        self == .outdoorRun || self == .indoorRun
    }

    var isCycling: Bool {
        self == .outdoorCycle || self == .indoorCycle
    }

    var distanceType: HKQuantityTypeIdentifier {
        switch self {
        case .outdoorRun, .indoorRun: return .distanceWalkingRunning
        case .poolSwim, .openWaterSwim: return .distanceSwimming
        case .outdoorCycle, .indoorCycle: return .distanceCycling
        case .gym, .hiit: return .distanceWalkingRunning
        }
    }
}

// MARK: - Pool Length

enum PoolLength: Int, CaseIterable, Identifiable, Codable {
    case m15 = 15
    case m20 = 20
    case m25 = 25
    case m33 = 33
    case m50 = 50

    var id: Int { rawValue }
    var label: String { "\(rawValue)m" }
    var quantity: HKQuantity { HKQuantity(unit: .meter(), doubleValue: Double(rawValue)) }
}

// MARK: - Heart Rate Zone

enum HRZone: Int, CaseIterable, Identifiable, Codable {
    case zone1 = 1, zone2, zone3, zone4, zone5

    var id: Int { rawValue }

    var name: String {
        switch self {
        case .zone1: return "Recovery"
        case .zone2: return "Fat Burn"
        case .zone3: return "Aerobic"
        case .zone4: return "Threshold"
        case .zone5: return "VO2 Max"
        }
    }

    var color: SwiftUI.Color {
        switch self {
        case .zone1: return .gray
        case .zone2: return .blue
        case .zone3: return .green
        case .zone4: return .orange
        case .zone5: return .red
        }
    }

    func range(maxHR: Int) -> ClosedRange<Int> {
        let m = Double(maxHR)
        switch self {
        case .zone1: return Int(m * 0.50)...Int(m * 0.60)
        case .zone2: return Int(m * 0.60)...Int(m * 0.70)
        case .zone3: return Int(m * 0.70)...Int(m * 0.80)
        case .zone4: return Int(m * 0.80)...Int(m * 0.90)
        case .zone5: return Int(m * 0.90)...maxHR
        }
    }
}

// MARK: - Zone Alert

enum ZoneAlert: String, Codable {
    case inZone, tooLow, tooHigh
}

// MARK: - Lap Data

struct LapData: Identifiable, Codable {
    let id: Int
    let startTime: TimeInterval
    let endTime: TimeInterval
    let distanceMeters: Double
    var paceSecondsPerKm: Double {
        let duration = endTime - startTime
        let km = distanceMeters / 1000.0
        guard km > 0 else { return 0 }
        return duration / km
    }
}

// MARK: - Gym Exercise

struct GymExercise: Identifiable, Codable {
    let id: UUID
    var name: String
    var targetSets: Int
    var targetReps: Int
    var weight: Double
    var completedSets: Int
    var unit: String // "kg" or "lb"

    init(name: String, targetSets: Int = 3, targetReps: Int = 10, weight: Double = 0, unit: String = "kg") {
        self.id = UUID()
        self.name = name
        self.targetSets = targetSets
        self.targetReps = targetReps
        self.weight = weight
        self.completedSets = 0
        self.unit = unit
    }
}

// MARK: - HIIT Template

struct HIITTemplate: Identifiable, Codable {
    let id: String
    let name: String
    let rounds: Int
    let workSeconds: Int
    let restSeconds: Int
    let exercises: [String]
    let warmupSeconds: Int
    let cooldownSeconds: Int

    var totalDurationSeconds: Int {
        warmupSeconds + (rounds * exercises.count * (workSeconds + restSeconds)) + cooldownSeconds
    }

    var formattedDuration: String {
        let mins = totalDurationSeconds / 60
        return "\(mins) min"
    }

    static let defaults: [HIITTemplate] = [
        HIITTemplate(id: "tabata", name: "Tabata", rounds: 8, workSeconds: 20, restSeconds: 10,
                     exercises: ["Burpees", "Mountain Climbers", "Jump Squats", "High Knees"],
                     warmupSeconds: 60, cooldownSeconds: 60),
        HIITTemplate(id: "emom15", name: "EMOM 15", rounds: 5, workSeconds: 40, restSeconds: 20,
                     exercises: ["Push Ups", "Kettlebell Swings", "Box Jumps"],
                     warmupSeconds: 120, cooldownSeconds: 120),
        HIITTemplate(id: "amrap20", name: "AMRAP 20", rounds: 4, workSeconds: 45, restSeconds: 15,
                     exercises: ["Thrusters", "Pull Ups", "Rowing", "Lunges", "Plank Hold"],
                     warmupSeconds: 120, cooldownSeconds: 120),
        HIITTemplate(id: "quickburn", name: "Quick Burn", rounds: 3, workSeconds: 30, restSeconds: 15,
                     exercises: ["Burpees", "Squat Jumps", "Push Ups"],
                     warmupSeconds: 60, cooldownSeconds: 60),
        HIITTemplate(id: "fullbody", name: "Full Body Blast", rounds: 4, workSeconds: 40, restSeconds: 20,
                     exercises: ["Deadlifts", "Bench Press", "Rows", "Squats", "Overhead Press"],
                     warmupSeconds: 180, cooldownSeconds: 180)
    ]
}

// MARK: - HIIT Phase

enum HIITPhase: String, Codable {
    case warmup = "WARM UP"
    case work = "WORK"
    case rest = "REST"
    case cooldown = "COOL DOWN"
    case complete = "DONE"
}

// MARK: - Workout Summary

struct WorkoutSummaryData {
    var activityType: WatchActivityType = .outdoorRun
    var duration: TimeInterval = 0
    var distanceMeters: Double = 0
    var calories: Double = 0
    var avgHeartRate: Int = 0
    var maxHeartRate: Int = 0
    var laps: [LapData] = []
    var heartRateSamples: [(time: TimeInterval, bpm: Int)] = []
    var strokeCount: Int = 0
    var elevationGain: Double = 0
}

// MARK: - Helpers

func formatPace(_ secondsPerKm: Double) -> String {
    guard secondsPerKm > 0, secondsPerKm < 3600, !secondsPerKm.isNaN, !secondsPerKm.isInfinite else { return "--:--" }
    let m = Int(secondsPerKm) / 60
    let s = Int(secondsPerKm) % 60
    return String(format: "%d:%02d", m, s)
}

func formatDuration(_ seconds: TimeInterval) -> String {
    let h = Int(seconds) / 3600
    let m = (Int(seconds) % 3600) / 60
    let s = Int(seconds) % 60
    if h > 0 {
        return String(format: "%d:%02d:%02d", h, m, s)
    }
    return String(format: "%02d:%02d", m, s)
}

func formatDurationLong(_ seconds: TimeInterval) -> String {
    let h = Int(seconds) / 3600
    let m = (Int(seconds) % 3600) / 60
    let s = Int(seconds) % 60
    if h > 0 {
        return String(format: "%dh %02dm %02ds", h, m, s)
    }
    return String(format: "%dm %02ds", m, s)
}
