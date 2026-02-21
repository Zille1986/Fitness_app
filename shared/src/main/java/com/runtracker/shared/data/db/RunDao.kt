package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.Run
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: Run): Long

    @Update
    suspend fun updateRun(run: Run)

    @Delete
    suspend fun deleteRun(run: Run)

    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun getRunById(id: Long): Run?

    @Query("SELECT * FROM runs WHERE id = :id")
    fun getRunByIdFlow(id: Long): Flow<Run?>

    @Query("SELECT * FROM runs ORDER BY startTime DESC")
    fun getAllRunsFlow(): Flow<List<Run>>
    
    @Query("SELECT * FROM runs WHERE isCompleted = 1 ORDER BY startTime DESC")
    suspend fun getAllRuns(): List<Run>

    @Query("SELECT * FROM runs ORDER BY startTime DESC LIMIT :limit")
    fun getRecentRunsFlow(limit: Int): Flow<List<Run>>

    @Query("SELECT * FROM runs WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    fun getRunsInRangeFlow(startTime: Long, endTime: Long): Flow<List<Run>>

    @Query("SELECT * FROM runs WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    suspend fun getRunsInRange(startTime: Long, endTime: Long): List<Run>

    @Query("SELECT * FROM runs WHERE isCompleted = 1 ORDER BY startTime DESC")
    fun getCompletedRunsFlow(): Flow<List<Run>>

    @Query("SELECT SUM(distanceMeters) FROM runs WHERE startTime >= :startTime AND startTime <= :endTime")
    suspend fun getTotalDistanceInRange(startTime: Long, endTime: Long): Double?

    @Query("SELECT SUM(durationMillis) FROM runs WHERE startTime >= :startTime AND startTime <= :endTime")
    suspend fun getTotalDurationInRange(startTime: Long, endTime: Long): Long?

    @Query("SELECT COUNT(*) FROM runs WHERE startTime >= :startTime AND startTime <= :endTime")
    suspend fun getRunCountInRange(startTime: Long, endTime: Long): Int

    @Query("SELECT * FROM runs WHERE distanceMeters >= :minDistance AND distanceMeters <= :maxDistance ORDER BY avgPaceSecondsPerKm ASC LIMIT 1")
    suspend fun getFastestRunInDistanceRange(minDistance: Double, maxDistance: Double): Run?

    @Query("SELECT * FROM runs ORDER BY distanceMeters DESC LIMIT 1")
    suspend fun getLongestRun(): Run?

    @Query("SELECT * FROM runs WHERE stravaId = :stravaId LIMIT 1")
    suspend fun getRunByStravaId(stravaId: String): Run?

    @Query("SELECT AVG(avgPaceSecondsPerKm) FROM runs WHERE startTime >= :startTime AND avgPaceSecondsPerKm > 0")
    suspend fun getAveragePaceSince(startTime: Long): Double?

    @Query("SELECT * FROM runs WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveRun(): Run?

    @Query("SELECT * FROM runs WHERE startTime = :startTime LIMIT 1")
    suspend fun getRunByStartTime(startTime: Long): Run?
}
