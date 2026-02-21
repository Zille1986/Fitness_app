package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

@Entity(tableName = "safety_settings")
@TypeConverters(Converters::class)
data class SafetySettings(
    @PrimaryKey
    val id: Long = 1, // Singleton - only one settings record
    val isEnabled: Boolean = true,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val sosMessage: String = "I don't feel safe. This is my current location:",
    val checkInEnabled: Boolean = false,
    val defaultCheckInMinutes: Int = 60,
    val panicAlarmEnabled: Boolean = true,
    val fakeCallEnabled: Boolean = true,
    val fakeCallerName: String = "Mom",
    val fakeCallDelay: Int = 5, // seconds delay before fake call
    val autoShareLocationOnSos: Boolean = true,
    val sosCountdownSeconds: Int = 5, // countdown before SOS is sent (can cancel)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class EmergencyContact(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val phoneNumber: String,
    val relationship: String = "", // e.g., "Partner", "Parent", "Friend"
    val isPrimary: Boolean = false, // Primary contact gets called, others get SMS
    val notifyOnSos: Boolean = true,
    val notifyOnCheckInMissed: Boolean = true
)

@Entity(tableName = "check_in_sessions")
data class CheckInSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val expectedDurationMinutes: Int,
    val expectedEndTime: Long,
    val activityType: String, // RUNNING, SWIMMING, CYCLING
    val isActive: Boolean = true,
    val checkedIn: Boolean = false,
    val checkedInTime: Long? = null,
    val sosTriggered: Boolean = false,
    val sosTriggeredTime: Long? = null,
    val lastKnownLatitude: Double? = null,
    val lastKnownLongitude: Double? = null
)

enum class SafetyAlertType {
    SOS,
    PANIC_ALARM,
    CHECK_IN_MISSED,
    FAKE_CALL
}
