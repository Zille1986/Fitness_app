import Foundation

struct StravaOAuthToken: Codable, Hashable {
    var accessToken: String
    var refreshToken: String
    var expiresAt: Date
    var athleteId: String
    var tokenType: String

    var isExpired: Bool { Date() >= expiresAt }

    init(
        accessToken: String,
        refreshToken: String,
        expiresAt: Date,
        athleteId: String,
        tokenType: String = "Bearer"
    ) {
        self.accessToken = accessToken
        self.refreshToken = refreshToken
        self.expiresAt = expiresAt
        self.athleteId = athleteId
        self.tokenType = tokenType
    }
}

struct StravaAthlete: Codable, Hashable {
    var id: Int
    var username: String?
    var firstName: String?
    var lastName: String?
    var profileImageUrl: String?
    var city: String?
    var state: String?
    var country: String?
    var sex: String?
    var weight: Double?

    var fullName: String {
        [firstName, lastName].compactMap { $0 }.joined(separator: " ")
    }

    enum CodingKeys: String, CodingKey {
        case id
        case username
        case firstName = "firstname"
        case lastName = "lastname"
        case profileImageUrl = "profile"
        case city
        case state
        case country
        case sex
        case weight
    }
}

struct StravaActivity: Codable, Hashable, Identifiable {
    var id: Int
    var name: String
    var type: String
    var sportType: String?
    var startDate: Date
    var startDateLocal: Date
    var elapsedTime: Int
    var movingTime: Int
    var distance: Double
    var totalElevationGain: Double
    var averageSpeed: Double
    var maxSpeed: Double
    var averageHeartrate: Double?
    var maxHeartrate: Double?
    var averageCadence: Double?
    var averageWatts: Double?
    var maxWatts: Int?
    var kilojoules: Double?
    var calories: Double?
    var map: StravaMap?
    var hasHeartrate: Bool?
    var deviceWatts: Bool?
    var externalId: String?

    var distanceKm: Double { distance / 1000.0 }
    var avgPaceSecondsPerKm: Double {
        guard distance > 0 else { return 0 }
        return Double(movingTime) / (distance / 1000.0)
    }

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case type
        case sportType = "sport_type"
        case startDate = "start_date"
        case startDateLocal = "start_date_local"
        case elapsedTime = "elapsed_time"
        case movingTime = "moving_time"
        case distance
        case totalElevationGain = "total_elevation_gain"
        case averageSpeed = "average_speed"
        case maxSpeed = "max_speed"
        case averageHeartrate = "average_heartrate"
        case maxHeartrate = "max_heartrate"
        case averageCadence = "average_cadence"
        case averageWatts = "average_watts"
        case maxWatts = "max_watts"
        case kilojoules
        case calories
        case map
        case hasHeartrate = "has_heartrate"
        case deviceWatts = "device_watts"
        case externalId = "external_id"
    }
}

struct StravaMap: Codable, Hashable {
    var id: String
    var summaryPolyline: String?
    var polyline: String?

    enum CodingKeys: String, CodingKey {
        case id
        case summaryPolyline = "summary_polyline"
        case polyline
    }
}

struct StravaDetailedActivity: Codable, Hashable {
    var id: Int
    var name: String
    var type: String
    var sportType: String?
    var startDate: Date
    var startDateLocal: Date
    var elapsedTime: Int
    var movingTime: Int
    var distance: Double
    var totalElevationGain: Double
    var averageSpeed: Double
    var maxSpeed: Double
    var averageHeartrate: Double?
    var maxHeartrate: Double?
    var averageCadence: Double?
    var averageWatts: Double?
    var maxWatts: Int?
    var weightedAverageWatts: Int?
    var kilojoules: Double?
    var calories: Double?
    var map: StravaMap?
    var hasHeartrate: Bool?
    var deviceWatts: Bool?
    var description: String?
    var splits: [StravaSplit]?
    var laps: [StravaLap]?

    enum CodingKeys: String, CodingKey {
        case id, name, type
        case sportType = "sport_type"
        case startDate = "start_date"
        case startDateLocal = "start_date_local"
        case elapsedTime = "elapsed_time"
        case movingTime = "moving_time"
        case distance
        case totalElevationGain = "total_elevation_gain"
        case averageSpeed = "average_speed"
        case maxSpeed = "max_speed"
        case averageHeartrate = "average_heartrate"
        case maxHeartrate = "max_heartrate"
        case averageCadence = "average_cadence"
        case averageWatts = "average_watts"
        case maxWatts = "max_watts"
        case weightedAverageWatts = "weighted_average_watts"
        case kilojoules, calories, map
        case hasHeartrate = "has_heartrate"
        case deviceWatts = "device_watts"
        case description, splits, laps
    }
}

struct StravaSplit: Codable, Hashable {
    var distance: Double
    var elapsedTime: Int
    var movingTime: Int
    var elevationDifference: Double
    var averageSpeed: Double
    var averageHeartrate: Double?
    var paceZone: Int?
    var split: Int

    enum CodingKeys: String, CodingKey {
        case distance
        case elapsedTime = "elapsed_time"
        case movingTime = "moving_time"
        case elevationDifference = "elevation_difference"
        case averageSpeed = "average_speed"
        case averageHeartrate = "average_heartrate"
        case paceZone = "pace_zone"
        case split
    }
}

struct StravaLap: Codable, Hashable {
    var id: Int
    var name: String
    var distance: Double
    var elapsedTime: Int
    var movingTime: Int
    var startIndex: Int
    var endIndex: Int
    var averageSpeed: Double
    var maxSpeed: Double
    var averageHeartrate: Double?
    var maxHeartrate: Double?
    var averageCadence: Double?
    var lapIndex: Int

    enum CodingKeys: String, CodingKey {
        case id, name, distance
        case elapsedTime = "elapsed_time"
        case movingTime = "moving_time"
        case startIndex = "start_index"
        case endIndex = "end_index"
        case averageSpeed = "average_speed"
        case maxSpeed = "max_speed"
        case averageHeartrate = "average_heartrate"
        case maxHeartrate = "max_heartrate"
        case averageCadence = "average_cadence"
        case lapIndex = "lap_index"
    }
}

struct StravaTokenResponse: Codable {
    var accessToken: String
    var refreshToken: String
    var expiresAt: Int
    var expiresIn: Int?
    var tokenType: String?
    var athlete: StravaAthlete?

    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case refreshToken = "refresh_token"
        case expiresAt = "expires_at"
        case expiresIn = "expires_in"
        case tokenType = "token_type"
        case athlete
    }
}

enum StravaActivityType: String, Codable, CaseIterable {
    case run = "Run"
    case ride = "Ride"
    case swim = "Swim"
    case walk = "Walk"
    case hike = "Hike"
    case virtualRide = "VirtualRide"
    case virtualRun = "VirtualRun"
    case workout = "Workout"
    case weightTraining = "WeightTraining"
    case yoga = "Yoga"
    case crossfit = "Crossfit"
    case elliptical = "Elliptical"
    case stairStepper = "StairStepper"
    case other = "Other"
}

struct StravaImportResult: Codable {
    var importedCount: Int
    var skippedCount: Int
    var failedCount: Int
    var importedActivityIds: [Int]
    var errors: [String]

    init(
        importedCount: Int = 0,
        skippedCount: Int = 0,
        failedCount: Int = 0,
        importedActivityIds: [Int] = [],
        errors: [String] = []
    ) {
        self.importedCount = importedCount
        self.skippedCount = skippedCount
        self.failedCount = failedCount
        self.importedActivityIds = importedActivityIds
        self.errors = errors
    }
}
