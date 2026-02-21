package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.CheckInSession
import com.runtracker.shared.data.model.SafetySettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SafetyDao {
    
    // Safety Settings
    @Query("SELECT * FROM safety_settings WHERE id = 1")
    fun getSettings(): Flow<SafetySettings?>
    
    @Query("SELECT * FROM safety_settings WHERE id = 1")
    suspend fun getSettingsOnce(): SafetySettings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SafetySettings)
    
    @Query("UPDATE safety_settings SET updatedAt = :timestamp WHERE id = 1")
    suspend fun touchSettings(timestamp: Long = System.currentTimeMillis())
    
    // Check-in Sessions
    @Insert
    suspend fun insertCheckInSession(session: CheckInSession): Long
    
    @Update
    suspend fun updateCheckInSession(session: CheckInSession)
    
    @Query("SELECT * FROM check_in_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveCheckInSession(): CheckInSession?
    
    @Query("SELECT * FROM check_in_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    fun getActiveCheckInSessionFlow(): Flow<CheckInSession?>
    
    @Query("UPDATE check_in_sessions SET isActive = 0 WHERE id = :sessionId")
    suspend fun deactivateCheckInSession(sessionId: Long)
    
    @Query("UPDATE check_in_sessions SET checkedIn = 1, checkedInTime = :time, isActive = 0 WHERE id = :sessionId")
    suspend fun markCheckedIn(sessionId: Long, time: Long = System.currentTimeMillis())
    
    @Query("UPDATE check_in_sessions SET sosTriggered = 1, sosTriggeredTime = :time WHERE id = :sessionId")
    suspend fun markSosTriggered(sessionId: Long, time: Long = System.currentTimeMillis())
    
    @Query("UPDATE check_in_sessions SET lastKnownLatitude = :lat, lastKnownLongitude = :lng WHERE id = :sessionId")
    suspend fun updateLastKnownLocation(sessionId: Long, lat: Double, lng: Double)
    
    @Query("SELECT * FROM check_in_sessions ORDER BY startTime DESC LIMIT 20")
    fun getRecentCheckInSessions(): Flow<List<CheckInSession>>
}
