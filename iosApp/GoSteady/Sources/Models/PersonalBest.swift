import Foundation
import SwiftData

@Model
final class PersonalBest {
    @Attribute(.unique) var id: UUID
    var distanceMeters: Int
    var fastestTimeMillis: Int64?
    var fastestTimeRunId: UUID?
    var fastestTimeDate: Date?
    var fastestTimePaceSecondsPerKm: Double?
    var lowestAvgHeartRate: Int?
    var lowestHrRunId: UUID?
    var lowestHrDate: Date?
    var lowestHrTimeMillis: Int64?
    var workoutType: WorkoutType?

    // Gym exercise personal bests
    var exerciseId: UUID?
    var exerciseName: String?
    var bestWeight: Double
    var bestReps: Int
    var estimatedOneRepMax: Double
    var achievedDate: Date

    var distanceKm: Double { Double(distanceMeters) / 1000.0 }

    var fastestTimeFormatted: String {
        guard let millis = fastestTimeMillis else { return "--:--" }
        let totalSeconds = millis / 1000
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            return String(format: "%d:%02d", minutes, seconds)
        }
    }

    var fastestPaceFormatted: String {
        guard let pace = fastestTimePaceSecondsPerKm else { return "--:--" }
        let minutes = Int(pace / 60)
        let seconds = Int(pace.truncatingRemainder(dividingBy: 60))
        return String(format: "%d:%02d", minutes, seconds)
    }

    static let distance1K = 1000
    static let distance5K = 5000
    static let distance10K = 10000
    static let distanceHalfMarathon = 21097
    static let distanceMarathon = 42195

    static let standardDistances = [distance1K, distance5K, distance10K, distanceHalfMarathon, distanceMarathon]

    static func getDistanceName(_ distanceMeters: Int) -> String {
        switch distanceMeters {
        case distance1K: return "1K"
        case distance5K: return "5K"
        case distance10K: return "10K"
        case distanceHalfMarathon: return "Half Marathon"
        case distanceMarathon: return "Marathon"
        default: return "\(Double(distanceMeters) / 1000.0)km"
        }
    }

    init(
        id: UUID = UUID(),
        distanceMeters: Int = 0,
        fastestTimeMillis: Int64? = nil,
        fastestTimeRunId: UUID? = nil,
        fastestTimeDate: Date? = nil,
        fastestTimePaceSecondsPerKm: Double? = nil,
        lowestAvgHeartRate: Int? = nil,
        lowestHrRunId: UUID? = nil,
        lowestHrDate: Date? = nil,
        lowestHrTimeMillis: Int64? = nil,
        workoutType: WorkoutType? = nil,
        exerciseId: UUID? = nil,
        exerciseName: String? = nil,
        bestWeight: Double = 0,
        bestReps: Int = 0,
        estimatedOneRepMax: Double = 0,
        achievedDate: Date = Date()
    ) {
        self.id = id
        self.distanceMeters = distanceMeters
        self.fastestTimeMillis = fastestTimeMillis
        self.fastestTimeRunId = fastestTimeRunId
        self.fastestTimeDate = fastestTimeDate
        self.fastestTimePaceSecondsPerKm = fastestTimePaceSecondsPerKm
        self.lowestAvgHeartRate = lowestAvgHeartRate
        self.lowestHrRunId = lowestHrRunId
        self.lowestHrDate = lowestHrDate
        self.lowestHrTimeMillis = lowestHrTimeMillis
        self.workoutType = workoutType
        self.exerciseId = exerciseId
        self.exerciseName = exerciseName
        self.bestWeight = bestWeight
        self.bestReps = bestReps
        self.estimatedOneRepMax = estimatedOneRepMax
        self.achievedDate = achievedDate
    }
}

struct CompeteProgress: Codable, Hashable {
    var distanceMeters: Int
    var personalBestTimeMillis: Int64
    var currentTimeMillis: Int64
    var currentDistanceMeters: Double
    var pbDistanceAtCurrentTime: Double
    var timeDifferenceMillis: Int64
    var isAheadOfPb: Bool

    var timeDifferenceFormatted: String {
        let absMillis = abs(timeDifferenceMillis)
        let totalSeconds = absMillis / 1000
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        let sign = isAheadOfPb ? "-" : "+"
        if minutes > 0 {
            return "\(sign)\(minutes):\(String(format: "%02d", seconds))"
        } else {
            return "\(sign)\(seconds)s"
        }
    }

    var progressPercent: Double {
        min(max(currentDistanceMeters / Double(distanceMeters), 0.0), 1.0)
    }

    var pbProgressPercent: Double {
        min(max(pbDistanceAtCurrentTime / Double(distanceMeters), 0.0), 1.0)
    }
}
