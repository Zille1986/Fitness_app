import Foundation
import SwiftData

@Model
final class SafetySettings {
    @Attribute(.unique) var id: Int
    var isEnabled: Bool
    var emergencyContacts: [EmergencyContact]
    var sosMessage: String
    var checkInEnabled: Bool
    var defaultCheckInMinutes: Int
    var panicAlarmEnabled: Bool
    var fakeCallEnabled: Bool
    var fakeCallerName: String
    var fakeCallDelay: Int
    var autoShareLocationOnSos: Bool
    var sosCountdownSeconds: Int
    var createdAt: Date
    var updatedAt: Date

    init(
        id: Int = 1,
        isEnabled: Bool = true,
        emergencyContacts: [EmergencyContact] = [],
        sosMessage: String = "I don't feel safe. This is my current location:",
        checkInEnabled: Bool = false,
        defaultCheckInMinutes: Int = 60,
        panicAlarmEnabled: Bool = true,
        fakeCallEnabled: Bool = true,
        fakeCallerName: String = "Mom",
        fakeCallDelay: Int = 5,
        autoShareLocationOnSos: Bool = true,
        sosCountdownSeconds: Int = 5,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.id = id
        self.isEnabled = isEnabled
        self.emergencyContacts = emergencyContacts
        self.sosMessage = sosMessage
        self.checkInEnabled = checkInEnabled
        self.defaultCheckInMinutes = defaultCheckInMinutes
        self.panicAlarmEnabled = panicAlarmEnabled
        self.fakeCallEnabled = fakeCallEnabled
        self.fakeCallerName = fakeCallerName
        self.fakeCallDelay = fakeCallDelay
        self.autoShareLocationOnSos = autoShareLocationOnSos
        self.sosCountdownSeconds = sosCountdownSeconds
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

struct EmergencyContact: Codable, Hashable, Identifiable {
    var id: String
    var name: String
    var phoneNumber: String
    var relationship: String
    var isPrimary: Bool
    var notifyOnSos: Bool
    var notifyOnCheckInMissed: Bool

    init(
        id: String = UUID().uuidString,
        name: String,
        phoneNumber: String,
        relationship: String = "",
        isPrimary: Bool = false,
        notifyOnSos: Bool = true,
        notifyOnCheckInMissed: Bool = true
    ) {
        self.id = id
        self.name = name
        self.phoneNumber = phoneNumber
        self.relationship = relationship
        self.isPrimary = isPrimary
        self.notifyOnSos = notifyOnSos
        self.notifyOnCheckInMissed = notifyOnCheckInMissed
    }
}

enum SafetyAlertType: String, Codable, CaseIterable {
    case sos = "SOS"
    case panicAlarm = "PANIC_ALARM"
    case checkInMissed = "CHECK_IN_MISSED"
    case fakeCall = "FAKE_CALL"
}
