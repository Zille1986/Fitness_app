package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.TrainingPlanDao
import com.runtracker.shared.data.model.ScheduledWorkout
import com.runtracker.shared.data.model.TrainingPlan
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TrainingPlanRepository(private val trainingPlanDao: TrainingPlanDao) {

    fun getAllPlans(): Flow<List<TrainingPlan>> = trainingPlanDao.getAllPlansFlow()

    fun getActivePlan(): Flow<TrainingPlan?> = trainingPlanDao.getActivePlanFlow()

    suspend fun getActivePlanOnce(): TrainingPlan? = trainingPlanDao.getActivePlan()

    suspend fun getPlanById(id: Long): TrainingPlan? = trainingPlanDao.getPlanById(id)

    fun getPlanByIdFlow(id: Long): Flow<TrainingPlan?> = trainingPlanDao.getPlanByIdFlow(id)

    suspend fun insertPlan(plan: TrainingPlan): Long = trainingPlanDao.insertPlan(plan)

    suspend fun updatePlan(plan: TrainingPlan) = trainingPlanDao.updatePlan(plan)

    suspend fun deletePlan(plan: TrainingPlan) = trainingPlanDao.deletePlan(plan)

    suspend fun setActivePlan(plan: TrainingPlan) {
        trainingPlanDao.deactivateAllPlans()
        trainingPlanDao.updatePlan(plan.copy(isActive = true))
    }

    suspend fun deactivateAllPlans() = trainingPlanDao.deactivateAllPlans()

    suspend fun getTodaysWorkout(): ScheduledWorkout? {
        val activePlan = getActivePlanOnce() ?: return null
        
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // Calculate current week of the plan
        val planStartDate = activePlan.startDate
        val daysSinceStart = ((System.currentTimeMillis() - planStartDate) / (24 * 60 * 60 * 1000)).toInt()
        val currentWeek = (daysSinceStart / 7) + 1
        
        // Find workout for today
        return activePlan.weeklySchedule.find { workout ->
            workout.weekNumber == currentWeek && workout.dayOfWeek == dayOfWeek
        }
    }

    /**
     * Get the next upcoming workout (today or future)
     * Returns a pair of (ScheduledWorkout, daysFromNow)
     */
    suspend fun getNextUpcomingWorkout(): Pair<ScheduledWorkout, Int>? {
        val activePlan = getActivePlanOnce() ?: return null
        
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_WEEK)
        val planStartDate = activePlan.startDate
        val now = System.currentTimeMillis()
        
        // Check if plan has ended
        if (now > activePlan.endDate) return null
        
        // Calculate current week of the plan
        val daysSinceStart = ((now - planStartDate) / (24 * 60 * 60 * 1000)).toInt()
        val currentWeek = (daysSinceStart / 7) + 1
        
        // Calculate total weeks in plan
        val totalDays = ((activePlan.endDate - planStartDate) / (24 * 60 * 60 * 1000)).toInt()
        val totalWeeks = (totalDays / 7) + 1
        
        // Search for next workout starting from today
        for (daysAhead in 0..60) { // Look up to 60 days ahead
            val targetCalendar = Calendar.getInstance()
            targetCalendar.add(Calendar.DAY_OF_MONTH, daysAhead)
            val targetDayOfWeek = targetCalendar.get(Calendar.DAY_OF_WEEK)
            
            val targetDaysSinceStart = daysSinceStart + daysAhead
            val targetWeek = (targetDaysSinceStart / 7) + 1
            
            // Don't look beyond plan end
            if (targetWeek > totalWeeks) break
            
            val workout = activePlan.weeklySchedule.find { w ->
                w.weekNumber == targetWeek && w.dayOfWeek == targetDayOfWeek && !w.isCompleted
            }
            
            if (workout != null) {
                return Pair(workout, daysAhead)
            }
        }
        
        return null
    }
}
