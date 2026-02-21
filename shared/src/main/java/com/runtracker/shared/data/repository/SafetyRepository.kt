package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.SafetyDao
import com.runtracker.shared.data.model.CheckInSession
import com.runtracker.shared.data.model.EmergencyContact
import com.runtracker.shared.data.model.SafetySettings
import kotlinx.coroutines.flow.Flow

class SafetyRepository(
    private val safetyDao: SafetyDao
) {
    fun getSettings(): Flow<SafetySettings?> = safetyDao.getSettings()
    
    suspend fun getSettingsOnce(): SafetySettings = 
        safetyDao.getSettingsOnce() ?: SafetySettings()
    
    suspend fun saveSettings(settings: SafetySettings) {
        safetyDao.saveSettings(settings.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun addEmergencyContact(contact: EmergencyContact) {
        val settings = getSettingsOnce()
        val updatedContacts = settings.emergencyContacts + contact
        saveSettings(settings.copy(emergencyContacts = updatedContacts))
    }
    
    suspend fun removeEmergencyContact(contactId: String) {
        val settings = getSettingsOnce()
        val updatedContacts = settings.emergencyContacts.filter { it.id != contactId }
        saveSettings(settings.copy(emergencyContacts = updatedContacts))
    }
    
    suspend fun updateEmergencyContact(contact: EmergencyContact) {
        val settings = getSettingsOnce()
        val updatedContacts = settings.emergencyContacts.map { 
            if (it.id == contact.id) contact else it 
        }
        saveSettings(settings.copy(emergencyContacts = updatedContacts))
    }
    
    suspend fun updateSosMessage(message: String) {
        val settings = getSettingsOnce()
        saveSettings(settings.copy(sosMessage = message))
    }
    
    suspend fun updateFakeCallerName(name: String) {
        val settings = getSettingsOnce()
        saveSettings(settings.copy(fakeCallerName = name))
    }
    
    // Check-in Sessions
    suspend fun startCheckInSession(
        expectedMinutes: Int,
        activityType: String
    ): Long {
        val now = System.currentTimeMillis()
        val session = CheckInSession(
            startTime = now,
            expectedDurationMinutes = expectedMinutes,
            expectedEndTime = now + (expectedMinutes * 60 * 1000),
            activityType = activityType
        )
        return safetyDao.insertCheckInSession(session)
    }
    
    suspend fun getActiveCheckInSession(): CheckInSession? = 
        safetyDao.getActiveCheckInSession()
    
    fun getActiveCheckInSessionFlow(): Flow<CheckInSession?> = 
        safetyDao.getActiveCheckInSessionFlow()
    
    suspend fun checkIn(sessionId: Long) {
        safetyDao.markCheckedIn(sessionId)
    }
    
    suspend fun cancelCheckIn(sessionId: Long) {
        safetyDao.deactivateCheckInSession(sessionId)
    }
    
    suspend fun triggerSos(sessionId: Long) {
        safetyDao.markSosTriggered(sessionId)
    }
    
    suspend fun updateSessionLocation(sessionId: Long, lat: Double, lng: Double) {
        safetyDao.updateLastKnownLocation(sessionId, lat, lng)
    }
    
    fun getRecentCheckInSessions(): Flow<List<CheckInSession>> = 
        safetyDao.getRecentCheckInSessions()
}
