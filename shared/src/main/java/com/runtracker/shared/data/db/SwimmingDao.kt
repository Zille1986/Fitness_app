package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.SwimmingWorkout
import com.runtracker.shared.data.model.SwimmingTrainingPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface SwimmingWorkoutDao {
    
    @Query("SELECT * FROM swimming_workouts ORDER BY startTime DESC")
    fun getAllWorkouts(): Flow<List<SwimmingWorkout>>
    
    @Query("SELECT * FROM swimming_workouts WHERE id = :id")
    suspend fun getWorkoutById(id: Long): SwimmingWorkout?
    
    @Query("SELECT * FROM swimming_workouts WHERE startTime >= :startTime AND startTime < :endTime ORDER BY startTime DESC")
    fun getWorkoutsInRange(startTime: Long, endTime: Long): Flow<List<SwimmingWorkout>>
    
    @Query("SELECT * FROM swimming_workouts WHERE isCompleted = 1 ORDER BY startTime DESC LIMIT :limit")
    fun getRecentCompletedWorkouts(limit: Int): Flow<List<SwimmingWorkout>>
    
    @Query("SELECT * FROM swimming_workouts WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveWorkout(): SwimmingWorkout?
    
    @Query("SELECT SUM(distanceMeters) FROM swimming_workouts WHERE startTime >= :startTime AND isCompleted = 1")
    suspend fun getTotalDistanceSince(startTime: Long): Double?
    
    @Query("SELECT COUNT(*) FROM swimming_workouts WHERE startTime >= :startTime AND isCompleted = 1")
    suspend fun getWorkoutCountSince(startTime: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: SwimmingWorkout): Long
    
    @Update
    suspend fun update(workout: SwimmingWorkout)
    
    @Delete
    suspend fun delete(workout: SwimmingWorkout)
    
    @Query("DELETE FROM swimming_workouts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SwimmingTrainingPlanDao {
    
    @Query("SELECT * FROM swimming_training_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<SwimmingTrainingPlan>>
    
    @Query("SELECT * FROM swimming_training_plans WHERE isActive = 1 LIMIT 1")
    fun getActivePlan(): Flow<SwimmingTrainingPlan?>
    
    @Query("SELECT * FROM swimming_training_plans WHERE id = :id")
    suspend fun getPlanById(id: Long): SwimmingTrainingPlan?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: SwimmingTrainingPlan): Long
    
    @Update
    suspend fun update(plan: SwimmingTrainingPlan)
    
    @Delete
    suspend fun delete(plan: SwimmingTrainingPlan)
    
    @Query("UPDATE swimming_training_plans SET isActive = 0")
    suspend fun deactivateAllPlans()
    
    @Query("UPDATE swimming_training_plans SET isActive = 1 WHERE id = :id")
    suspend fun activatePlan(id: Long)
}
