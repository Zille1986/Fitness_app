package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MentalHealthDao {
    
    // Mood Entries
    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    fun getAllMoodEntries(): Flow<List<MoodEntry>>
    
    @Query("SELECT * FROM mood_entries WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getMoodEntriesSince(since: Long): Flow<List<MoodEntry>>
    
    @Query("SELECT * FROM mood_entries WHERE relatedRunId = :runId")
    suspend fun getMoodEntriesForRun(runId: Long): List<MoodEntry>
    
    @Query("SELECT * FROM mood_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    suspend fun getMoodEntriesForDay(startOfDay: Long, endOfDay: Long): List<MoodEntry>
    
    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMoodEntry(): MoodEntry?
    
    @Insert
    suspend fun insertMoodEntry(entry: MoodEntry): Long
    
    @Update
    suspend fun updateMoodEntry(entry: MoodEntry)
    
    @Delete
    suspend fun deleteMoodEntry(entry: MoodEntry)
    
    // Mindfulness Sessions
    @Query("SELECT * FROM mindfulness_sessions ORDER BY timestamp DESC")
    fun getAllMindfulnessSessions(): Flow<List<MindfulnessSession>>
    
    @Query("SELECT * FROM mindfulness_sessions WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getMindfulnessSessionsSince(since: Long): Flow<List<MindfulnessSession>>
    
    @Query("SELECT * FROM mindfulness_sessions WHERE completed = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCompletedSessions(limit: Int = 10): Flow<List<MindfulnessSession>>
    
    @Query("SELECT SUM(durationSeconds) FROM mindfulness_sessions WHERE completed = 1 AND timestamp >= :since")
    suspend fun getTotalMindfulnessMinutesSince(since: Long): Int?
    
    @Query("SELECT COUNT(*) FROM mindfulness_sessions WHERE completed = 1 AND timestamp >= :since")
    suspend fun getMindfulnessSessionCountSince(since: Long): Int
    
    @Insert
    suspend fun insertMindfulnessSession(session: MindfulnessSession): Long
    
    @Update
    suspend fun updateMindfulnessSession(session: MindfulnessSession)
    
    // Wellness Check-ins
    @Query("SELECT * FROM wellness_checkins ORDER BY date DESC")
    fun getAllWellnessCheckins(): Flow<List<WellnessCheckin>>
    
    @Query("SELECT * FROM wellness_checkins WHERE date = :date")
    suspend fun getWellnessCheckinForDate(date: Long): WellnessCheckin?
    
    @Query("SELECT * FROM wellness_checkins ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWellnessCheckin(): WellnessCheckin?
    
    @Query("SELECT * FROM wellness_checkins ORDER BY date DESC LIMIT :limit")
    fun getRecentWellnessCheckins(limit: Int = 7): Flow<List<WellnessCheckin>>
    
    @Query("SELECT AVG(readinessScore) FROM wellness_checkins WHERE date >= :since AND readinessScore IS NOT NULL")
    suspend fun getAverageReadinessSince(since: Long): Float?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWellnessCheckin(checkin: WellnessCheckin): Long
    
    @Delete
    suspend fun deleteWellnessCheckin(checkin: WellnessCheckin)
}
