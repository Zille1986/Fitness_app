import Foundation
import SwiftData

@Model
final class Run {
    @Attribute(.unique) var id: UUID
    var startTime: Date
    var endTime: Date?
    var distanceMeters: Double
    var durationMillis: Int64
    var avgPaceSecondsPerKm: Double
    var maxPaceSecondsPerKm: Double
    var avgHeartRate: Int?
    var maxHeartRate: Int?
    var caloriesBurned: Int
    var elevationGainMeters: Double
    var elevationLossMeters: Double
    var avgCadence: Int?
    var weather: String?
    var temperature: Double?
    var notes: String?
    var routePoints: [RoutePoint]
    var splits: [Split]
    var source: RunSource
    var stravaId: String?
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

    var avgPaceFormatted: String { Run.formatPace(avgPaceSecondsPerKm) }

    static func formatPace(_ secondsPerKm: Double) -> String {
        if secondsPerKm <= 0 || secondsPerKm.isInfinite || secondsPerKm.isNaN { return "--:--" }
        let minutes = Int(secondsPerKm / 60)
        let seconds = Int(secondsPerKm.truncatingRemainder(dividingBy: 60))
        return String(format: "%d:%02d", minutes, seconds)
    }

    init(
        id: UUID = UUID(),
        startTime: Date,
        endTime: Date? = nil,
        distanceMeters: Double = 0.0,
        durationMillis: Int64 = 0,
        avgPaceSecondsPerKm: Double = 0.0,
        maxPaceSecondsPerKm: Double = 0.0,
        avgHeartRate: Int? = nil,
        maxHeartRate: Int? = nil,
        caloriesBurned: Int = 0,
        elevationGainMeters: Double = 0.0,
        elevationLossMeters: Double = 0.0,
        avgCadence: Int? = nil,
        weather: String? = nil,
        temperature: Double? = nil,
        notes: String? = nil,
        routePoints: [RoutePoint] = [],
        splits: [Split] = [],
        source: RunSource = .phone,
        stravaId: String? = nil,
        isCompleted: Bool = false
    ) {
        self.id = id
        self.startTime = startTime
        self.endTime = endTime
        self.distanceMeters = distanceMeters
        self.durationMillis = durationMillis
        self.avgPaceSecondsPerKm = avgPaceSecondsPerKm
        self.maxPaceSecondsPerKm = maxPaceSecondsPerKm
        self.avgHeartRate = avgHeartRate
        self.maxHeartRate = maxHeartRate
        self.caloriesBurned = caloriesBurned
        self.elevationGainMeters = elevationGainMeters
        self.elevationLossMeters = elevationLossMeters
        self.avgCadence = avgCadence
        self.weather = weather
        self.temperature = temperature
        self.notes = notes
        self.routePoints = routePoints
        self.splits = splits
        self.source = source
        self.stravaId = stravaId
        self.isCompleted = isCompleted
    }
}

struct RoutePoint: Codable, Hashable, Identifiable {
    var id: UUID
    var latitude: Double
    var longitude: Double
    var altitude: Double?
    var timestamp: Date
    var accuracy: Double?
    var heartRate: Int?
    var cadence: Int?
    var speed: Double?

    init(
        latitude: Double,
        longitude: Double,
        altitude: Double? = nil,
        timestamp: Date = .now,
        accuracy: Double? = nil,
        speed: Double? = nil,
        heartRate: Int? = nil,
        cadence: Int? = nil
    ) {
        self.id = UUID()
        self.latitude = latitude
        self.longitude = longitude
        self.altitude = altitude
        self.timestamp = timestamp
        self.accuracy = accuracy
        self.speed = speed
        self.heartRate = heartRate
        self.cadence = cadence
    }
}

struct Split: Codable, Hashable {
    var kilometer: Int
    var durationMillis: Int64
    var paceSecondsPerKm: Double
    var elevationChange: Double
    var avgHeartRate: Int?

    var paceFormatted: String { Run.formatPace(paceSecondsPerKm) }

    var durationFormatted: String {
        let totalSeconds = durationMillis / 1000
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    init(
        kilometer: Int,
        durationMillis: Int64,
        paceSecondsPerKm: Double,
        elevationChange: Double = 0.0,
        avgHeartRate: Int? = nil
    ) {
        self.kilometer = kilometer
        self.durationMillis = durationMillis
        self.paceSecondsPerKm = paceSecondsPerKm
        self.elevationChange = elevationChange
        self.avgHeartRate = avgHeartRate
    }
}

enum RunSource: String, Codable, CaseIterable {
    case phone = "PHONE"
    case watch = "WATCH"
    case strava = "STRAVA"
    case manual = "MANUAL"
}
