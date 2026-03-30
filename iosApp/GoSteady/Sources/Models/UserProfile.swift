import Foundation
import SwiftData

@Model
final class UserProfile {
    @Attribute(.unique) var id: Int
    var name: String
    var age: Int?
    var weight: Double?
    var height: Double?
    var gender: Gender?
    var restingHeartRate: Int?
    var maxHeartRate: Int?
    var weeklyGoalKm: Double
    var preferredUnits: Units
    var stravaAccessToken: String?
    var stravaRefreshToken: String?
    var stravaTokenExpiry: Date?
    var stravaAthleteId: String?
    var personalRecords: PersonalRecords
    var isOnboardingComplete: Bool
    var createdAt: Date
    var updatedAt: Date

    var estimatedMaxHeartRate: Int {
        maxHeartRate ?? (220 - (age ?? 30))
    }

    func getHeartRateZones() -> [HeartRateZoneRange] {
        let max = estimatedMaxHeartRate
        return [
            HeartRateZoneRange(zone: .zone1, minBpm: Int(Double(max) * 0.50), maxBpm: Int(Double(max) * 0.60)),
            HeartRateZoneRange(zone: .zone2, minBpm: Int(Double(max) * 0.60), maxBpm: Int(Double(max) * 0.70)),
            HeartRateZoneRange(zone: .zone3, minBpm: Int(Double(max) * 0.70), maxBpm: Int(Double(max) * 0.80)),
            HeartRateZoneRange(zone: .zone4, minBpm: Int(Double(max) * 0.80), maxBpm: Int(Double(max) * 0.90)),
            HeartRateZoneRange(zone: .zone5, minBpm: Int(Double(max) * 0.90), maxBpm: max)
        ]
    }

    init(
        id: Int = 1,
        name: String = "",
        age: Int? = nil,
        weight: Double? = nil,
        height: Double? = nil,
        gender: Gender? = nil,
        restingHeartRate: Int? = nil,
        maxHeartRate: Int? = nil,
        weeklyGoalKm: Double = 20.0,
        preferredUnits: Units = .metric,
        stravaAccessToken: String? = nil,
        stravaRefreshToken: String? = nil,
        stravaTokenExpiry: Date? = nil,
        stravaAthleteId: String? = nil,
        personalRecords: PersonalRecords = PersonalRecords(),
        isOnboardingComplete: Bool = false,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.age = age
        self.weight = weight
        self.height = height
        self.gender = gender
        self.restingHeartRate = restingHeartRate
        self.maxHeartRate = maxHeartRate
        self.weeklyGoalKm = weeklyGoalKm
        self.preferredUnits = preferredUnits
        self.stravaAccessToken = stravaAccessToken
        self.stravaRefreshToken = stravaRefreshToken
        self.stravaTokenExpiry = stravaTokenExpiry
        self.stravaAthleteId = stravaAthleteId
        self.personalRecords = personalRecords
        self.isOnboardingComplete = isOnboardingComplete
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

enum Gender: String, Codable, CaseIterable {
    case male = "MALE"
    case female = "FEMALE"
    case other = "OTHER"
}

enum Units: String, Codable, CaseIterable {
    case metric = "METRIC"
    case imperial = "IMPERIAL"
}

enum DemoVideoModel: String, Codable, CaseIterable {
    case male = "MALE"
    case female = "FEMALE"

    var subfolder: String { rawValue.lowercased() }
}

struct HeartRateZoneRange: Codable, Hashable {
    var zone: HeartRateZone
    var minBpm: Int
    var maxBpm: Int
}

struct PersonalRecords: Codable, Hashable {
    var fastest1K: Int64?
    var fastest5K: Int64?
    var fastest10K: Int64?
    var fastestHalfMarathon: Int64?
    var fastestMarathon: Int64?
    var longestRun: Double?
    var highestElevationGain: Double?

    init(
        fastest1K: Int64? = nil,
        fastest5K: Int64? = nil,
        fastest10K: Int64? = nil,
        fastestHalfMarathon: Int64? = nil,
        fastestMarathon: Int64? = nil,
        longestRun: Double? = nil,
        highestElevationGain: Double? = nil
    ) {
        self.fastest1K = fastest1K
        self.fastest5K = fastest5K
        self.fastest10K = fastest10K
        self.fastestHalfMarathon = fastestHalfMarathon
        self.fastestMarathon = fastestMarathon
        self.longestRun = longestRun
        self.highestElevationGain = highestElevationGain
    }
}

enum FitnessLevel: String, Codable, CaseIterable {
    case beginner = "BEGINNER"
    case novice = "NOVICE"
    case intermediate = "INTERMEDIATE"
    case advanced = "ADVANCED"
    case elite = "ELITE"

    var displayName: String {
        switch self {
        case .beginner: return "Beginner"
        case .novice: return "Novice"
        case .intermediate: return "Intermediate"
        case .advanced: return "Advanced"
        case .elite: return "Elite"
        }
    }

    var multiplier: Float {
        switch self {
        case .beginner: return 0.7
        case .novice: return 0.85
        case .intermediate: return 1.0
        case .advanced: return 1.3
        case .elite: return 1.5
        }
    }
}
