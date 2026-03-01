package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.HIITDao
import com.runtracker.shared.data.model.HIITSession
import kotlinx.coroutines.flow.Flow
import java.util.*

class HIITRepository(
    private val hiitDao: HIITDao
) {
    fun getAllSessions(): Flow<List<HIITSession>> = hiitDao.getAllSessions()

    suspend fun getSessionById(id: Long): HIITSession? = hiitDao.getSessionById(id)

    fun getRecentCompletedSessions(limit: Int = 10): Flow<List<HIITSession>> =
        hiitDao.getRecentCompletedSessions(limit)

    fun getSessionsSince(startTime: Long): Flow<List<HIITSession>> =
        hiitDao.getSessionsSince(startTime)

    suspend fun insertSession(session: HIITSession): Long = hiitDao.insert(session)

    suspend fun updateSession(session: HIITSession) = hiitDao.update(session)

    suspend fun deleteSession(session: HIITSession) = hiitDao.delete(session)

    suspend fun getSessionCountSince(startTime: Long): Int =
        hiitDao.getSessionCountSince(startTime)

    suspend fun getTotalCaloriesSince(startTime: Long): Int =
        hiitDao.getTotalCaloriesSince(startTime) ?: 0

    suspend fun getWeeklyStats(): HIITWeeklyStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis

        return HIITWeeklyStats(
            sessionCount = getSessionCountSince(weekStart),
            totalCalories = getTotalCaloriesSince(weekStart),
            weekStartTimestamp = weekStart
        )
    }
}

data class HIITWeeklyStats(
    val sessionCount: Int,
    val totalCalories: Int,
    val weekStartTimestamp: Long
)
