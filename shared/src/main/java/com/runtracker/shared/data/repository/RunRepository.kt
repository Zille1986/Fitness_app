package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.RunDao
import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.model.WeeklyStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

class RunRepository(private val runDao: RunDao) {

    fun getAllRuns(): Flow<List<Run>> = runDao.getAllRunsFlow()
    
    suspend fun getAllRunsOnce(): List<Run> = runDao.getAllRuns()

    fun getRecentRuns(limit: Int = 10): Flow<List<Run>> = runDao.getRecentRunsFlow(limit)

    fun getRunById(id: Long): Flow<Run?> = runDao.getRunByIdFlow(id)

    suspend fun getRunByIdOnce(id: Long): Run? = runDao.getRunById(id)

    suspend fun insertRun(run: Run): Long = runDao.insertRun(run)

    suspend fun updateRun(run: Run) = runDao.updateRun(run)

    suspend fun deleteRun(run: Run) = runDao.deleteRun(run)

    fun getRunsInRange(startTime: Long, endTime: Long): Flow<List<Run>> =
        runDao.getRunsInRangeFlow(startTime, endTime)
    
    suspend fun getRunsInRangeOnce(startTime: Long, endTime: Long): List<Run> =
        runDao.getRunsInRange(startTime, endTime)

    suspend fun getWeeklyStats(weekStartDate: Long): WeeklyStats {
        val weekEndDate = weekStartDate + 7 * 24 * 60 * 60 * 1000L
        val runs = runDao.getRunsInRange(weekStartDate, weekEndDate)
        
        val totalDistance = runs.sumOf { it.distanceMeters }
        val totalDuration = runs.sumOf { it.durationMillis }
        val totalElevation = runs.sumOf { it.elevationGainMeters }
        val totalCalories = runs.sumOf { it.caloriesBurned }
        
        val avgPace = if (totalDistance > 0) {
            (totalDuration / 1000.0) / (totalDistance / 1000.0)
        } else 0.0
        
        val longestRun = runs.maxOfOrNull { it.distanceMeters } ?: 0.0
        val fastestPace = runs.filter { it.avgPaceSecondsPerKm > 0 }
            .minOfOrNull { it.avgPaceSecondsPerKm } ?: 0.0

        return WeeklyStats(
            weekStartDate = weekStartDate,
            totalDistanceMeters = totalDistance,
            totalDurationMillis = totalDuration,
            totalRuns = runs.size,
            avgPaceSecondsPerKm = avgPace,
            totalElevationGain = totalElevation,
            totalCalories = totalCalories,
            longestRunMeters = longestRun,
            fastestPaceSecondsPerKm = fastestPace
        )
    }

    suspend fun getWeeklyStatsForPastWeeks(weeks: Int): List<WeeklyStats> {
        val calendar = Calendar.getInstance().apply {
            // First, set time to start of today
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Calculate days since Monday (Monday = 2 in Calendar)
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY

            // Go back to Monday of this week
            add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        }

        return (0 until weeks).map { weekOffset ->
            val weekStart = calendar.timeInMillis - (weekOffset * 7 * 24 * 60 * 60 * 1000L)
            getWeeklyStats(weekStart)
        }
    }

    suspend fun getFastestRunForDistance(distanceKm: Double, toleranceKm: Double = 0.5): Run? {
        val minDistance = (distanceKm - toleranceKm) * 1000
        val maxDistance = (distanceKm + toleranceKm) * 1000
        return runDao.getFastestRunInDistanceRange(minDistance, maxDistance)
    }

    suspend fun getLongestRun(): Run? = runDao.getLongestRun()

    suspend fun getRunByStravaId(stravaId: String): Run? = runDao.getRunByStravaId(stravaId)

    suspend fun getActiveRun(): Run? = runDao.getActiveRun()

    suspend fun getRunByStartTime(startTime: Long): Run? = runDao.getRunByStartTime(startTime)

    fun getThisWeekRuns(): Flow<List<Run>> {
        val calendar = Calendar.getInstance().apply {
            // First, set time to start of today
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Calculate days since Monday (Monday = 2 in Calendar)
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY

            // Go back to Monday of this week
            add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        }
        val weekStart = calendar.timeInMillis
        val weekEnd = weekStart + 7 * 24 * 60 * 60 * 1000L
        return runDao.getRunsInRangeFlow(weekStart, weekEnd).map { runs ->
            runs.filter { it.isCompleted }
        }
    }

    fun getRunsSince(since: Long): Flow<List<Run>> = runDao.getRunsInRangeFlow(since, System.currentTimeMillis())
    
    suspend fun getRunsSinceOnce(since: Long): List<Run> = runDao.getRunsInRange(since, System.currentTimeMillis())
}
