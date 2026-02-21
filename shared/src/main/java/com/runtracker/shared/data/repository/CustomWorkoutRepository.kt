package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.CustomRunWorkoutDao
import com.runtracker.shared.data.db.CustomTrainingPlanDao
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.flow.Flow

class CustomWorkoutRepository(
    private val customRunWorkoutDao: CustomRunWorkoutDao,
    private val customTrainingPlanDao: CustomTrainingPlanDao
) {
    // Custom Run Workouts
    fun getAllWorkoutsFlow(): Flow<List<CustomRunWorkout>> = customRunWorkoutDao.getAllFlow()
    
    suspend fun getAllWorkouts(): List<CustomRunWorkout> = customRunWorkoutDao.getAll()
    
    suspend fun getWorkoutById(id: Long): CustomRunWorkout? = customRunWorkoutDao.getById(id)
    
    fun getWorkoutByIdFlow(id: Long): Flow<CustomRunWorkout?> = customRunWorkoutDao.getByIdFlow(id)
    
    fun getWorkoutsByCategory(category: WorkoutCategory): Flow<List<CustomRunWorkout>> = 
        customRunWorkoutDao.getByCategory(category)
    
    fun getFavoriteWorkouts(): Flow<List<CustomRunWorkout>> = customRunWorkoutDao.getFavorites()
    
    fun getMostUsedWorkouts(limit: Int = 10): Flow<List<CustomRunWorkout>> = 
        customRunWorkoutDao.getMostUsed(limit)
    
    suspend fun saveWorkout(workout: CustomRunWorkout): Long = customRunWorkoutDao.insert(workout)
    
    suspend fun updateWorkout(workout: CustomRunWorkout) = customRunWorkoutDao.update(workout)
    
    suspend fun deleteWorkout(workout: CustomRunWorkout) = customRunWorkoutDao.delete(workout)
    
    suspend fun deleteWorkoutById(id: Long) = customRunWorkoutDao.deleteById(id)
    
    suspend fun setWorkoutFavorite(id: Long, isFavorite: Boolean) = 
        customRunWorkoutDao.setFavorite(id, isFavorite)
    
    suspend fun recordWorkoutUsage(id: Long) = customRunWorkoutDao.incrementUsage(id)
    
    // Custom Training Plans
    fun getAllPlansFlow(): Flow<List<CustomTrainingPlan>> = customTrainingPlanDao.getAllFlow()
    
    suspend fun getAllPlans(): List<CustomTrainingPlan> = customTrainingPlanDao.getAll()
    
    suspend fun getPlanById(id: Long): CustomTrainingPlan? = customTrainingPlanDao.getById(id)
    
    fun getPlanByIdFlow(id: Long): Flow<CustomTrainingPlan?> = customTrainingPlanDao.getByIdFlow(id)
    
    suspend fun getActivePlan(): CustomTrainingPlan? = customTrainingPlanDao.getActivePlan()
    
    fun getActivePlanFlow(): Flow<CustomTrainingPlan?> = customTrainingPlanDao.getActivePlanFlow()
    
    fun getFavoritePlans(): Flow<List<CustomTrainingPlan>> = customTrainingPlanDao.getFavorites()
    
    suspend fun savePlan(plan: CustomTrainingPlan): Long = customTrainingPlanDao.insert(plan)
    
    suspend fun updatePlan(plan: CustomTrainingPlan) = customTrainingPlanDao.update(plan)
    
    suspend fun deletePlan(plan: CustomTrainingPlan) = customTrainingPlanDao.delete(plan)
    
    suspend fun deletePlanById(id: Long) = customTrainingPlanDao.deleteById(id)
    
    suspend fun setActivePlan(id: Long) = customTrainingPlanDao.setActivePlan(id)
    
    suspend fun setPlanFavorite(id: Long, isFavorite: Boolean) = 
        customTrainingPlanDao.setFavorite(id, isFavorite)
    
    // Helper to convert CustomRunWorkout to intervals for tracking
    fun workoutToIntervals(workout: CustomRunWorkout): List<Interval> {
        return workout.phases.flatMap { phase ->
            (1..phase.repetitions).map {
                Interval(
                    type = when (phase.type) {
                        PhaseType.WARMUP -> IntervalType.WARMUP
                        PhaseType.COOLDOWN -> IntervalType.COOLDOWN
                        PhaseType.RECOVERY, PhaseType.REST, PhaseType.FLOAT -> IntervalType.RECOVERY
                        else -> IntervalType.WORK
                    },
                    durationSeconds = phase.durationSeconds,
                    distanceMeters = phase.distanceMeters,
                    targetPaceMinSecondsPerKm = phase.targetPaceMin,
                    targetPaceMaxSecondsPerKm = phase.targetPaceMax,
                    targetHeartRateMin = phase.targetHeartRateMin,
                    targetHeartRateMax = phase.targetHeartRateMax,
                    targetHeartRateZone = phase.targetHeartRateZone,
                    repetitions = 1
                )
            }
        }
    }
    
    // Seed default workout templates
    suspend fun seedDefaultWorkouts() {
        val existing = getAllWorkouts()
        if (existing.isEmpty()) {
            RunWorkoutTemplates.getAllTemplates().forEach { template ->
                saveWorkout(template)
            }
        }
    }
}
