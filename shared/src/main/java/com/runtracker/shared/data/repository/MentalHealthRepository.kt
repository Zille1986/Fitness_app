package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.MentalHealthDao
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.*

class MentalHealthRepository(
    private val mentalHealthDao: MentalHealthDao
) {
    // Mood Entries
    fun getAllMoodEntries(): Flow<List<MoodEntry>> = mentalHealthDao.getAllMoodEntries()
    
    fun getMoodEntriesSince(since: Long): Flow<List<MoodEntry>> = 
        mentalHealthDao.getMoodEntriesSince(since)
    
    suspend fun getMoodEntriesForRun(runId: Long): List<MoodEntry> = 
        mentalHealthDao.getMoodEntriesForRun(runId)
    
    suspend fun getLatestMoodEntry(): MoodEntry? = mentalHealthDao.getLatestMoodEntry()
    
    suspend fun logMood(
        mood: MoodLevel,
        energy: EnergyLevel,
        stress: StressLevel,
        notes: String = "",
        relatedRunId: Long? = null,
        isPreWorkout: Boolean = true,
        tags: List<String> = emptyList()
    ): Long {
        val entry = MoodEntry(
            mood = mood,
            energy = energy,
            stress = stress,
            notes = notes,
            relatedRunId = relatedRunId,
            isPreWorkout = isPreWorkout,
            tags = tags
        )
        return mentalHealthDao.insertMoodEntry(entry)
    }
    
    suspend fun logPreWorkoutMood(
        mood: MoodLevel,
        energy: EnergyLevel,
        stress: StressLevel,
        runId: Long? = null
    ): Long {
        return logMood(
            mood = mood,
            energy = energy,
            stress = stress,
            relatedRunId = runId,
            isPreWorkout = true,
            tags = listOf("pre-workout")
        )
    }
    
    suspend fun logPostWorkoutMood(
        mood: MoodLevel,
        energy: EnergyLevel,
        stress: StressLevel,
        runId: Long
    ): Long {
        return logMood(
            mood = mood,
            energy = energy,
            stress = stress,
            relatedRunId = runId,
            isPreWorkout = false,
            tags = listOf("post-workout")
        )
    }
    
    // Mindfulness Sessions
    fun getAllMindfulnessSessions(): Flow<List<MindfulnessSession>> = 
        mentalHealthDao.getAllMindfulnessSessions()
    
    fun getRecentCompletedSessions(limit: Int = 10): Flow<List<MindfulnessSession>> = 
        mentalHealthDao.getRecentCompletedSessions(limit)
    
    suspend fun getTotalMindfulnessMinutesThisWeek(): Int {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return (mentalHealthDao.getTotalMindfulnessMinutesSince(weekAgo) ?: 0) / 60
    }
    
    suspend fun getMindfulnessSessionCountThisWeek(): Int {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return mentalHealthDao.getMindfulnessSessionCountSince(weekAgo)
    }
    
    suspend fun recordMindfulnessSession(
        type: MindfulnessType,
        durationSeconds: Int,
        completed: Boolean = true,
        relatedRunId: Long? = null,
        rating: Int? = null
    ): Long {
        val session = MindfulnessSession(
            type = type,
            durationSeconds = durationSeconds,
            completed = completed,
            relatedRunId = relatedRunId,
            rating = rating
        )
        return mentalHealthDao.insertMindfulnessSession(session)
    }
    
    // Wellness Check-ins
    fun getAllWellnessCheckins(): Flow<List<WellnessCheckin>> = 
        mentalHealthDao.getAllWellnessCheckins()
    
    fun getRecentWellnessCheckins(limit: Int = 7): Flow<List<WellnessCheckin>> = 
        mentalHealthDao.getRecentWellnessCheckins(limit)
    
    suspend fun getLatestWellnessCheckin(): WellnessCheckin? = 
        mentalHealthDao.getLatestWellnessCheckin()
    
    suspend fun getTodayCheckin(): WellnessCheckin? {
        val today = getStartOfDay(System.currentTimeMillis())
        return mentalHealthDao.getWellnessCheckinForDate(today)
    }
    
    suspend fun saveWellnessCheckin(
        sleepHours: Float? = null,
        sleepQuality: Int? = null,
        restingHeartRate: Int? = null,
        hrv: Int? = null,
        mood: MoodLevel? = null,
        energy: EnergyLevel? = null,
        stress: StressLevel? = null,
        soreness: Int? = null,
        hydration: Int? = null,
        notes: String = ""
    ): Long {
        val today = getStartOfDay(System.currentTimeMillis())
        val existing = mentalHealthDao.getWellnessCheckinForDate(today)
        
        val readinessScore = calculateReadinessScore(
            sleepHours, sleepQuality, restingHeartRate, hrv, 
            mood, energy, stress, soreness
        )
        
        val checkin = WellnessCheckin(
            id = existing?.id ?: 0,
            date = today,
            sleepHours = sleepHours ?: existing?.sleepHours,
            sleepQuality = sleepQuality ?: existing?.sleepQuality,
            restingHeartRate = restingHeartRate ?: existing?.restingHeartRate,
            hrv = hrv ?: existing?.hrv,
            mood = mood ?: existing?.mood,
            energy = energy ?: existing?.energy,
            stress = stress ?: existing?.stress,
            soreness = soreness ?: existing?.soreness,
            hydration = hydration ?: existing?.hydration,
            notes = notes.ifBlank { existing?.notes ?: "" },
            readinessScore = readinessScore
        )
        
        return mentalHealthDao.insertOrUpdateWellnessCheckin(checkin)
    }
    
    suspend fun getAverageReadinessLastWeek(): Float? {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return mentalHealthDao.getAverageReadinessSince(weekAgo)
    }
    
    // Calculate readiness score (0-100) based on wellness factors
    private fun calculateReadinessScore(
        sleepHours: Float?,
        sleepQuality: Int?,
        restingHeartRate: Int?,
        hrv: Int?,
        mood: MoodLevel?,
        energy: EnergyLevel?,
        stress: StressLevel?,
        soreness: Int?
    ): Int {
        var score = 50 // Base score
        var factors = 0
        
        // Sleep hours (optimal 7-9 hours)
        sleepHours?.let {
            factors++
            score += when {
                it >= 7 && it <= 9 -> 10
                it >= 6 && it < 7 -> 5
                it > 9 && it <= 10 -> 5
                else -> -5
            }
        }
        
        // Sleep quality (1-5)
        sleepQuality?.let {
            factors++
            score += (it - 3) * 5 // -10 to +10
        }
        
        // Mood
        mood?.let {
            factors++
            score += (it.value - 3) * 5 // -10 to +10
        }
        
        // Energy
        energy?.let {
            factors++
            score += (it.value - 3) * 5 // -10 to +10
        }
        
        // Stress (inverted - lower is better)
        stress?.let {
            factors++
            score += (3 - it.value) * 5 // -10 to +10
        }
        
        // Soreness (inverted - lower is better)
        soreness?.let {
            factors++
            score += (3 - it) * 3 // -6 to +6
        }
        
        // HRV bonus (higher is generally better for readiness)
        hrv?.let {
            factors++
            score += when {
                it > 60 -> 10
                it > 40 -> 5
                it > 20 -> 0
                else -> -5
            }
        }
        
        return score.coerceIn(0, 100)
    }
    
    // Helpers
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    // Get mood improvement from pre to post workout
    suspend fun getMoodImprovementForRun(runId: Long): MoodImprovement? {
        val entries = getMoodEntriesForRun(runId)
        val preWorkout = entries.find { it.isPreWorkout }
        val postWorkout = entries.find { !it.isPreWorkout }
        
        if (preWorkout == null || postWorkout == null) return null
        
        return MoodImprovement(
            moodChange = postWorkout.mood.value - preWorkout.mood.value,
            energyChange = postWorkout.energy.value - preWorkout.energy.value,
            stressChange = preWorkout.stress.value - postWorkout.stress.value // Inverted - decrease is good
        )
    }
}

data class MoodImprovement(
    val moodChange: Int,
    val energyChange: Int,
    val stressChange: Int
) {
    val overallImprovement: Int
        get() = moodChange + energyChange + stressChange
    
    val isPositive: Boolean
        get() = overallImprovement > 0
}
