package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.PersonalBestDao
import com.runtracker.shared.data.model.PersonalBest
import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.model.WorkoutType
import kotlinx.coroutines.flow.Flow

class PersonalBestRepository(
    private val personalBestDao: PersonalBestDao
) {
    fun getAllPersonalBestsFlow(): Flow<List<PersonalBest>> = personalBestDao.getAllFlow()
    
    suspend fun getAllPersonalBests(): List<PersonalBest> = personalBestDao.getAll()
    
    fun getPersonalBestFlow(distanceMeters: Int): Flow<PersonalBest?> = 
        personalBestDao.getByDistanceFlow(distanceMeters)
    
    suspend fun getPersonalBest(distanceMeters: Int): PersonalBest? = 
        personalBestDao.getByDistance(distanceMeters)
    
    suspend fun getPersonalBestForWorkout(distanceMeters: Int, workoutType: WorkoutType): PersonalBest? =
        personalBestDao.getByDistanceAndWorkoutType(distanceMeters, workoutType)
    
    /**
     * Check if a run sets any new personal bests and update accordingly
     * Returns list of new PBs achieved
     */
    suspend fun checkAndUpdatePersonalBests(run: Run): List<PersonalBestUpdate> {
        val updates = mutableListOf<PersonalBestUpdate>()
        
        // Check each standard distance that the run covers
        for (distance in PersonalBest.STANDARD_DISTANCES) {
            if (run.distanceMeters >= distance) {
                val timeForDistance = calculateTimeForDistance(run, distance)
                if (timeForDistance != null) {
                    val update = checkDistancePB(run, distance, timeForDistance)
                    if (update != null) {
                        updates.add(update)
                    }
                }
            }
        }
        
        return updates
    }
    
    private suspend fun checkDistancePB(run: Run, distanceMeters: Int, timeMillis: Long): PersonalBestUpdate? {
        val existingPB = personalBestDao.getByDistance(distanceMeters)
        val paceSecondsPerKm = (timeMillis / 1000.0) / (distanceMeters / 1000.0)
        
        var newFastestTime = false
        var newLowestHr = false
        
        val updatedPB = if (existingPB == null) {
            // First time for this distance
            newFastestTime = true
            if (run.avgHeartRate != null) newLowestHr = true
            
            PersonalBest(
                distanceMeters = distanceMeters,
                fastestTimeMillis = timeMillis,
                fastestTimeRunId = run.id,
                fastestTimeDate = run.startTime,
                fastestTimePaceSecondsPerKm = paceSecondsPerKm,
                lowestAvgHeartRate = run.avgHeartRate,
                lowestHrRunId = if (run.avgHeartRate != null) run.id else null,
                lowestHrDate = if (run.avgHeartRate != null) run.startTime else null,
                lowestHrTimeMillis = if (run.avgHeartRate != null) timeMillis else null
            )
        } else {
            var updated = existingPB
            
            // Check fastest time
            if (existingPB.fastestTimeMillis == null || timeMillis < existingPB.fastestTimeMillis) {
                newFastestTime = true
                updated = updated.copy(
                    fastestTimeMillis = timeMillis,
                    fastestTimeRunId = run.id,
                    fastestTimeDate = run.startTime,
                    fastestTimePaceSecondsPerKm = paceSecondsPerKm
                )
            }
            
            // Check lowest heart rate (only if HR data available)
            if (run.avgHeartRate != null) {
                if (existingPB.lowestAvgHeartRate == null || run.avgHeartRate < existingPB.lowestAvgHeartRate) {
                    newLowestHr = true
                    updated = updated.copy(
                        lowestAvgHeartRate = run.avgHeartRate,
                        lowestHrRunId = run.id,
                        lowestHrDate = run.startTime,
                        lowestHrTimeMillis = timeMillis
                    )
                }
            }
            
            updated
        }
        
        if (newFastestTime || newLowestHr) {
            if (existingPB == null) {
                personalBestDao.insert(updatedPB)
            } else {
                personalBestDao.update(updatedPB)
            }
            
            return PersonalBestUpdate(
                distanceMeters = distanceMeters,
                distanceName = PersonalBest.getDistanceName(distanceMeters),
                newFastestTime = newFastestTime,
                newLowestHr = newLowestHr,
                timeMillis = timeMillis,
                avgHeartRate = run.avgHeartRate,
                previousFastestTime = existingPB?.fastestTimeMillis,
                previousLowestHr = existingPB?.lowestAvgHeartRate
            )
        }
        
        return null
    }
    
    /**
     * Calculate the time it took to complete a specific distance within a run
     * Uses splits if available, otherwise estimates based on average pace
     */
    private fun calculateTimeForDistance(run: Run, distanceMeters: Int): Long? {
        val distanceKm = distanceMeters / 1000
        
        // If we have splits, use them for more accurate time
        if (run.splits.isNotEmpty() && run.splits.size >= distanceKm) {
            var totalTime = 0L
            for (i in 0 until distanceKm) {
                totalTime += run.splits[i].durationMillis
            }
            return totalTime
        }
        
        // Otherwise estimate based on average pace
        if (run.avgPaceSecondsPerKm > 0) {
            return (run.avgPaceSecondsPerKm * distanceKm * 1000).toLong()
        }
        
        return null
    }
    
    /**
     * Get the pace at each kilometer for a personal best run
     * Used for compete mode comparison
     */
    suspend fun getPersonalBestSplits(distanceMeters: Int, runDao: com.runtracker.shared.data.db.RunDao): List<Long>? {
        val pb = personalBestDao.getByDistance(distanceMeters) ?: return null
        val runId = pb.fastestTimeRunId ?: return null
        val run = runDao.getRunById(runId) ?: return null
        
        return run.splits.map { it.durationMillis }
    }
}

data class PersonalBestUpdate(
    val distanceMeters: Int,
    val distanceName: String,
    val newFastestTime: Boolean,
    val newLowestHr: Boolean,
    val timeMillis: Long,
    val avgHeartRate: Int?,
    val previousFastestTime: Long?,
    val previousLowestHr: Int?
) {
    val timeImprovement: Long? get() = previousFastestTime?.let { it - timeMillis }
    val hrImprovement: Int? get() = previousLowestHr?.let { it - (avgHeartRate ?: 0) }
    
    val timeImprovementFormatted: String? get() {
        val improvement = timeImprovement ?: return null
        val seconds = improvement / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        return if (minutes > 0) "-${minutes}m ${secs}s" else "-${secs}s"
    }
}
