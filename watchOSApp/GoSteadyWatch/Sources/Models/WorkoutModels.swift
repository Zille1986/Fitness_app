import Foundation
import HealthKit

// MARK: - Activity Types

enum ActivityType: String, CaseIterable, Identifiable {
    case running = "Running"
    case swimming = "Swimming"
    case cycling = "Cycling"
    case gym = "Gym"
    case hiit = "HIIT"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .running: return "figure.run"
        case .swimming: return "figure.pool.swim"
        case .cycling: return "bicycle"
        case .gym: return "dumbbell.fill"
        case .hiit: return "flame.fill"
        }
    }

    var hkType: HKWorkoutActivityType {
        switch self {
        case .running: return .running
        case .swimming: return .swimming
        case .cycling: return .cycling
        case .gym: return .traditionalStrengthTraining
        case .hiit: return .highIntensityIntervalTraining
        }
    }

    var locationType: HKWorkoutSessionLocationType {
        switch self {
        case .running, .cycling: return .outdoor
        case .swimming: return .unknown // set per swim type
        case .gym, .hiit: return .indoor
        }
    }
}

// MARK: - Swim Types

enum SwimType: String, CaseIterable, Identifiable {
    case pool = "Pool"
    case openWater = "Open Water"

    var id: String { rawValue }

    var hkLocationType: HKWorkoutSessionLocationType {
        switch self {
        case .pool: return .indoor
        case .openWater: return .outdoor
        }
    }

    var hkSwimLocationType: HKWorkoutSwimmingLocationType {
        switch self {
        case .pool: return .pool
        case .openWater: return .openWater
        }
    }
}

enum PoolLength: Int, CaseIterable, Identifiable {
    case m15 = 15
    case m20 = 20
    case m25 = 25
    case m33 = 33
    case m50 = 50

    var id: Int { rawValue }
    var label: String { "\(rawValue)m" }
    var quantity: HKQuantity { HKQuantity(unit: .meter(), doubleValue: Double(rawValue)) }
}

// MARK: - Cycling Types

enum CyclingType: String, CaseIterable, Identifiable {
    case outdoor = "Outdoor"
    case indoor = "Indoor"
    case smartTrainer = "Smart Trainer"

    var id: String { rawValue }
}

// MARK: - Heart Rate Zones

enum HRZone: Int, CaseIterable {
    case zone1 = 1, zone2, zone3, zone4, zone5

    var name: String {
        switch self {
        case .zone1: return "Recovery"
        case .zone2: return "Aerobic"
        case .zone3: return "Tempo"
        case .zone4: return "Threshold"
        case .zone5: return "VO2 Max"
        }
    }

    func range(maxHR: Int) -> ClosedRange<Int> {
        let max = Double(maxHR)
        switch self {
        case .zone1: return Int(max * 0.50)...Int(max * 0.60)
        case .zone2: return Int(max * 0.60)...Int(max * 0.70)
        case .zone3: return Int(max * 0.70)...Int(max * 0.80)
        case .zone4: return Int(max * 0.80)...Int(max * 0.90)
        case .zone5: return Int(max * 0.90)...maxHR
        }
    }
}

// MARK: - Zone Alert

enum ZoneAlert {
    case inZone, tooLow, tooHigh
}

// MARK: - Tracking State

struct TrackingState {
    var isTracking = false
    var isPaused = false
    var activityType: ActivityType = .running
    var elapsedSeconds: TimeInterval = 0
    var distanceMeters: Double = 0
    var heartRate: Int = 0
    var currentPaceSecondsPerKm: Double = 0
    var calories: Double = 0
    var laps: Int = 0

    // Zones
    var targetHRMin: Int?
    var targetHRMax: Int?
    var hrAlert: ZoneAlert = .inZone

    // Swimming
    var swimType: SwimType = .pool
    var poolLength: PoolLength = .m25
    var strokeCount: Int = 0

    var distanceKm: Double { distanceMeters / 1000.0 }

    var paceFormatted: String {
        guard currentPaceSecondsPerKm > 0, currentPaceSecondsPerKm < 3600 else { return "--:--" }
        let m = Int(currentPaceSecondsPerKm) / 60
        let s = Int(currentPaceSecondsPerKm) % 60
        return String(format: "%d:%02d", m, s)
    }

    var durationFormatted: String {
        let h = Int(elapsedSeconds) / 3600
        let m = (Int(elapsedSeconds) % 3600) / 60
        let s = Int(elapsedSeconds) % 60
        return h > 0 ? String(format: "%d:%02d:%02d", h, m, s) : String(format: "%02d:%02d", m, s)
    }
}
