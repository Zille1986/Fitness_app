package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.HIITSession
import kotlinx.coroutines.flow.Flow

@Dao
interface HIITDao {

    @Query("SELECT * FROM hiit_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<HIITSession>>

    @Query("SELECT * FROM hiit_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): HIITSession?

    @Query("SELECT * FROM hiit_sessions WHERE isCompleted = 1 ORDER BY date DESC LIMIT :limit")
    fun getRecentCompletedSessions(limit: Int): Flow<List<HIITSession>>

    @Query("SELECT * FROM hiit_sessions WHERE date >= :startTime AND isCompleted = 1 ORDER BY date DESC")
    fun getSessionsSince(startTime: Long): Flow<List<HIITSession>>

    @Query("SELECT COUNT(*) FROM hiit_sessions WHERE date >= :startTime AND isCompleted = 1")
    suspend fun getSessionCountSince(startTime: Long): Int

    @Query("SELECT SUM(caloriesEstimate) FROM hiit_sessions WHERE date >= :startTime AND isCompleted = 1")
    suspend fun getTotalCaloriesSince(startTime: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: HIITSession): Long

    @Update
    suspend fun update(session: HIITSession)

    @Delete
    suspend fun delete(session: HIITSession)

    @Query("DELETE FROM hiit_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
