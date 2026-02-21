package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.PersonalizedPlanDao
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.service.PlanGeneratorService
import kotlinx.coroutines.flow.Flow

class PersonalizedPlanRepository(
    private val personalizedPlanDao: PersonalizedPlanDao,
    private val planGeneratorService: PlanGeneratorService,
    private val bodyAnalysisRepository: BodyAnalysisRepository
) {
    
    fun getActivePlan(): Flow<PersonalizedPlan?> = personalizedPlanDao.getActivePlan()
    
    suspend fun getActivePlanOnce(): PersonalizedPlan? = personalizedPlanDao.getActivePlanOnce()
    
    fun getAllPlans(): Flow<List<PersonalizedPlan>> = personalizedPlanDao.getAllPlans()
    
    suspend fun getPlanById(id: Long): PersonalizedPlan? = personalizedPlanDao.getPlanById(id)
    
    /**
     * Generate a recommendation based on the latest body scan
     */
    suspend fun getRecommendation(): PlanRecommendation? {
        val latestScan = bodyAnalysisRepository.getLatestScan() ?: return null
        return planGeneratorService.generateRecommendation(latestScan)
    }
    
    /**
     * Generate and save a new personalized plan
     */
    suspend fun generateAndSavePlan(preferences: PlanPreferences): PersonalizedPlan? {
        val latestScan = bodyAnalysisRepository.getLatestScan() ?: return null
        return generateAndSavePlanWithScan(latestScan, preferences)
    }
    
    /**
     * Generate and save a new personalized plan with a provided body scan
     */
    suspend fun generateAndSavePlanWithScan(bodyScan: BodyScan, preferences: PlanPreferences): PersonalizedPlan? {
        // Deactivate any existing active plans
        personalizedPlanDao.deactivateAllPlans()
        
        // Generate new plan
        val plan = planGeneratorService.generatePlan(bodyScan, preferences)
        
        // Save to database
        val planId = personalizedPlanDao.insertPlan(plan)
        
        return personalizedPlanDao.getPlanById(planId)
    }
    
    /**
     * Get today's workout from the active plan
     */
    suspend fun getTodaysWorkout(): PlannedWorkout? {
        val plan = getActivePlanOnce() ?: return null
        val today = System.currentTimeMillis()
        val startOfDay = getStartOfDay(today)
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000
        
        // Get today's day of week (1=Sunday, 2=Monday, etc.)
        val calendar = java.util.Calendar.getInstance()
        val todayDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        
        // First try to find by exact date match
        val byDate = plan.allWorkouts.find { workout ->
            workout.date >= startOfDay && workout.date < endOfDay && !workout.isCompleted
        }
        if (byDate != null) return byDate
        
        // Then try to find by day of week - get the first uncompleted workout for today's day
        val byDayOfWeek = plan.allWorkouts
            .filter { workout -> 
                workout.dayOfWeek == todayDayOfWeek && !workout.isCompleted
            }
            .minByOrNull { it.date }
        
        if (byDayOfWeek != null) return byDayOfWeek
        
        // Finally, just get the next upcoming uncompleted workout
        return plan.allWorkouts
            .filter { !it.isCompleted && it.workoutType != PlannedWorkoutType.REST }
            .minByOrNull { it.date }
    }
    
    /**
     * Get next running workout from the plan
     */
    suspend fun getNextRunningWorkout(): PlannedWorkout? {
        val plan = getActivePlanOnce() ?: return null
        return plan.allWorkouts
            .filter { !it.isCompleted && it.workoutType == PlannedWorkoutType.RUNNING }
            .minByOrNull { it.date }
    }
    
    /**
     * Get next gym workout from the plan
     */
    suspend fun getNextGymWorkout(): PlannedWorkout? {
        val plan = getActivePlanOnce() ?: return null
        return plan.allWorkouts
            .filter { !it.isCompleted && 
                (it.workoutType == PlannedWorkoutType.GYM_STRENGTH || 
                 it.workoutType == PlannedWorkoutType.GYM_HYPERTROPHY) }
            .minByOrNull { it.date }
    }
    
    /**
     * Get upcoming workouts for the next N days
     */
    suspend fun getUpcomingWorkouts(days: Int = 7): List<PlannedWorkout> {
        val plan = getActivePlanOnce() ?: return emptyList()
        val today = getStartOfDay(System.currentTimeMillis())
        val endDate = today + days * 24 * 60 * 60 * 1000
        
        return plan.allWorkouts.filter { workout ->
            workout.date >= today && workout.date < endDate && !workout.isCompleted
        }.sortedBy { it.date }
    }
    
    /**
     * Mark a workout as completed
     */
    suspend fun markWorkoutCompleted(
        workoutId: String,
        linkedRunId: Long? = null,
        linkedGymWorkoutId: Long? = null
    ) {
        val plan = getActivePlanOnce() ?: return
        
        val updatedWorkouts = plan.allWorkouts.map { workout ->
            if (workout.id == workoutId) {
                workout.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis(),
                    linkedRunId = linkedRunId,
                    linkedGymWorkoutId = linkedGymWorkoutId
                )
            } else workout
        }
        
        val completedCount = updatedWorkouts.count { it.isCompleted }
        
        personalizedPlanDao.updatePlan(plan.copy(
            allWorkouts = updatedWorkouts,
            completedWorkouts = completedCount
        ))
    }
    
    /**
     * Get plan progress
     */
    suspend fun getPlanProgress(): PlanProgress? {
        val plan = getActivePlanOnce() ?: return null
        
        val today = getStartOfDay(System.currentTimeMillis())
        val planStartDay = getStartOfDay(plan.startDate)
        val daysSinceStart = ((today - planStartDay) / (24 * 60 * 60 * 1000)).toInt()
        val currentWeek = (daysSinceStart / 7) + 1
        
        val completedWorkouts = plan.allWorkouts.count { it.isCompleted }
        val expectedWorkouts = plan.allWorkouts.count { it.date < today && !it.workoutType.isRestDay() }
        val adherence = if (expectedWorkouts > 0) {
            (completedWorkouts.toFloat() / expectedWorkouts * 100).coerceIn(0f, 100f)
        } else 100f
        
        val missedWorkouts = expectedWorkouts - completedWorkouts
        
        val upcomingWorkouts = plan.allWorkouts
            .filter { it.date >= today && !it.isCompleted }
            .sortedBy { it.date }
            .take(7)
        
        val recentCompleted = plan.allWorkouts
            .filter { it.isCompleted }
            .sortedByDescending { it.completedAt }
            .take(5)
        
        // Calculate streak
        var streak = 0
        val sortedWorkouts = plan.allWorkouts
            .filter { !it.workoutType.isRestDay() }
            .sortedByDescending { it.date }
        
        for (workout in sortedWorkouts) {
            if (workout.date > today) continue
            if (workout.isCompleted) streak++
            else break
        }
        
        return PlanProgress(
            planId = plan.id,
            currentWeek = currentWeek.coerceIn(1, 12),
            completedWorkouts = completedWorkouts,
            totalWorkouts = plan.totalWorkouts,
            adherencePercent = adherence,
            streakDays = streak,
            missedWorkouts = missedWorkouts.coerceAtLeast(0),
            upcomingWorkouts = upcomingWorkouts,
            recentCompletedWorkouts = recentCompleted
        )
    }
    
    /**
     * Check if the current plan is expired and needs a new body scan
     */
    suspend fun isPlanExpiredOrMissing(): Boolean {
        val plan = getActivePlanOnce()
        return plan == null || plan.isExpired
    }
    
    /**
     * Deactivate the current plan
     */
    suspend fun deactivateCurrentPlan() {
        personalizedPlanDao.deactivateAllPlans()
    }
    
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun PlannedWorkoutType.isRestDay(): Boolean {
        return this == PlannedWorkoutType.REST || this == PlannedWorkoutType.ACTIVE_RECOVERY
    }
}
