import Foundation
import SwiftData

@Model
final class CheckInSession {
    @Attribute(.unique) var id: UUID
    var startTime: Date
    var expectedDurationMinutes: Int
    var expectedEndTime: Date
    var activityType: String
    var isActive: Bool
    var checkedIn: Bool
    var checkedInTime: Date?
    var sosTriggered: Bool
    var sosTriggeredTime: Date?
    var lastKnownLatitude: Double?
    var lastKnownLongitude: Double?

    init(
        id: UUID = UUID(),
        startTime: Date,
        expectedDurationMinutes: Int,
        expectedEndTime: Date,
        activityType: String,
        isActive: Bool = true,
        checkedIn: Bool = false,
        checkedInTime: Date? = nil,
        sosTriggered: Bool = false,
        sosTriggeredTime: Date? = nil,
        lastKnownLatitude: Double? = nil,
        lastKnownLongitude: Double? = nil
    ) {
        self.id = id
        self.startTime = startTime
        self.expectedDurationMinutes = expectedDurationMinutes
        self.expectedEndTime = expectedEndTime
        self.activityType = activityType
        self.isActive = isActive
        self.checkedIn = checkedIn
        self.checkedInTime = checkedInTime
        self.sosTriggered = sosTriggered
        self.sosTriggeredTime = sosTriggeredTime
        self.lastKnownLatitude = lastKnownLatitude
        self.lastKnownLongitude = lastKnownLongitude
    }
}
