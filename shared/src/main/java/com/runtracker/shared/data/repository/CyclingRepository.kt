package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.CyclingWorkoutDao
import com.runtracker.shared.data.db.CyclingTrainingPlanDao
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*
class CyclingRepository(
    private val workoutDao: CyclingWorkoutDao,
    private val planDao: CyclingTrainingPlanDao
) {
    // Workout operations
    fun getAllWorkouts(): Flow<List<CyclingWorkout>> = workoutDao.getAllWorkouts()
    
    suspend fun getWorkoutById(id: Long): CyclingWorkout? = workoutDao.getWorkoutById(id)
    
    fun getWorkoutsInRange(startTime: Long, endTime: Long): Flow<List<CyclingWorkout>> = 
        workoutDao.getWorkoutsInRange(startTime, endTime)
    
    fun getRecentCompletedWorkouts(limit: Int = 10): Flow<List<CyclingWorkout>> = 
        workoutDao.getRecentCompletedWorkouts(limit)
    
    suspend fun getActiveWorkout(): CyclingWorkout? = workoutDao.getActiveWorkout()
    
    suspend fun insertWorkout(workout: CyclingWorkout): Long = workoutDao.insert(workout)
    
    suspend fun updateWorkout(workout: CyclingWorkout) = workoutDao.update(workout)
    
    suspend fun deleteWorkout(workout: CyclingWorkout) = workoutDao.delete(workout)
    
    suspend fun getTotalDistanceSince(startTime: Long): Double = 
        workoutDao.getTotalDistanceSince(startTime) ?: 0.0
    
    suspend fun getWorkoutCountSince(startTime: Long): Int = 
        workoutDao.getWorkoutCountSince(startTime)
    
    suspend fun getAveragePowerSince(startTime: Long): Double = 
        workoutDao.getAveragePowerSince(startTime) ?: 0.0
    
    // Training plan operations
    fun getAllPlans(): Flow<List<CyclingTrainingPlan>> = planDao.getAllPlans()
    
    fun getActivePlan(): Flow<CyclingTrainingPlan?> = planDao.getActivePlan()
    
    suspend fun getPlanById(id: Long): CyclingTrainingPlan? = planDao.getPlanById(id)
    
    suspend fun insertPlan(plan: CyclingTrainingPlan): Long = planDao.insert(plan)
    
    suspend fun updatePlan(plan: CyclingTrainingPlan) = planDao.update(plan)
    
    suspend fun deletePlan(plan: CyclingTrainingPlan) = planDao.delete(plan)
    
    suspend fun activatePlan(id: Long) {
        planDao.deactivateAllPlans()
        planDao.activatePlan(id)
    }
    
    // Get next upcoming workout from active plan
    suspend fun getNextUpcomingWorkout(): Pair<ScheduledCyclingWorkout, Int>? {
        val plan = getActivePlan().first() ?: return null
        
        val calendar = Calendar.getInstance()
        val startDate = Calendar.getInstance().apply { timeInMillis = plan.startDate }
        
        // Find next workout from today onwards
        for (daysAhead in 0..60) {
            val checkDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, daysAhead) }
            val checkDayOfWeek = checkDate.get(Calendar.DAY_OF_WEEK)
            val checkWeek = ((checkDate.timeInMillis - startDate.timeInMillis) / (7 * 24 * 60 * 60 * 1000)).toInt() + 1
            
            val workout = plan.weeklySchedule.find { 
                it.weekNumber == checkWeek && 
                it.dayOfWeek == checkDayOfWeek && 
                !it.isCompleted &&
                it.workoutType != CyclingWorkoutType.REST_DAY
            }
            
            if (workout != null) {
                return Pair(workout, daysAhead)
            }
        }
        
        return null
    }
    
    // Mark workout as completed
    suspend fun markWorkoutCompleted(planId: Long, workoutId: String, completedWorkoutId: Long) {
        val plan = getPlanById(planId) ?: return
        val updatedSchedule = plan.weeklySchedule.map { workout ->
            if (workout.id == workoutId) {
                workout.copy(isCompleted = true, completedWorkoutId = completedWorkoutId)
            } else {
                workout
            }
        }
        updatePlan(plan.copy(weeklySchedule = updatedSchedule))
    }
    
    // Calculate weekly stats
    suspend fun getWeeklyStats(): CyclingWeeklyStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis
        
        val totalDistance = getTotalDistanceSince(weekStart)
        val workoutCount = getWorkoutCountSince(weekStart)
        val avgPower = getAveragePowerSince(weekStart)
        
        return CyclingWeeklyStats(
            totalDistanceMeters = totalDistance,
            workoutCount = workoutCount,
            averagePowerWatts = avgPower,
            weekStartTimestamp = weekStart
        )
    }
}

data class CyclingWeeklyStats(
    val totalDistanceMeters: Double,
    val workoutCount: Int,
    val averagePowerWatts: Double,
    val weekStartTimestamp: Long
) {
    val totalDistanceKm: Double get() = totalDistanceMeters / 1000.0
}
