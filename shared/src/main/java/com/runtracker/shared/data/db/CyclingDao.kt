package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.CyclingWorkout
import com.runtracker.shared.data.model.CyclingTrainingPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface CyclingWorkoutDao {
    
    @Query("SELECT * FROM cycling_workouts ORDER BY startTime DESC")
    fun getAllWorkouts(): Flow<List<CyclingWorkout>>
    
    @Query("SELECT * FROM cycling_workouts WHERE id = :id")
    suspend fun getWorkoutById(id: Long): CyclingWorkout?
    
    @Query("SELECT * FROM cycling_workouts WHERE startTime >= :startTime AND startTime < :endTime ORDER BY startTime DESC")
    fun getWorkoutsInRange(startTime: Long, endTime: Long): Flow<List<CyclingWorkout>>
    
    @Query("SELECT * FROM cycling_workouts WHERE isCompleted = 1 ORDER BY startTime DESC LIMIT :limit")
    fun getRecentCompletedWorkouts(limit: Int): Flow<List<CyclingWorkout>>
    
    @Query("SELECT * FROM cycling_workouts WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveWorkout(): CyclingWorkout?
    
    @Query("SELECT SUM(distanceMeters) FROM cycling_workouts WHERE startTime >= :startTime AND isCompleted = 1")
    suspend fun getTotalDistanceSince(startTime: Long): Double?
    
    @Query("SELECT COUNT(*) FROM cycling_workouts WHERE startTime >= :startTime AND isCompleted = 1")
    suspend fun getWorkoutCountSince(startTime: Long): Int
    
    @Query("SELECT AVG(avgPowerWatts) FROM cycling_workouts WHERE avgPowerWatts IS NOT NULL AND startTime >= :startTime AND isCompleted = 1")
    suspend fun getAveragePowerSince(startTime: Long): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: CyclingWorkout): Long
    
    @Update
    suspend fun update(workout: CyclingWorkout)
    
    @Delete
    suspend fun delete(workout: CyclingWorkout)
    
    @Query("DELETE FROM cycling_workouts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CyclingTrainingPlanDao {
    
    @Query("SELECT * FROM cycling_training_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<CyclingTrainingPlan>>
    
    @Query("SELECT * FROM cycling_training_plans WHERE isActive = 1 LIMIT 1")
    fun getActivePlan(): Flow<CyclingTrainingPlan?>
    
    @Query("SELECT * FROM cycling_training_plans WHERE id = :id")
    suspend fun getPlanById(id: Long): CyclingTrainingPlan?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: CyclingTrainingPlan): Long
    
    @Update
    suspend fun update(plan: CyclingTrainingPlan)
    
    @Delete
    suspend fun delete(plan: CyclingTrainingPlan)
    
    @Query("UPDATE cycling_training_plans SET isActive = 0")
    suspend fun deactivateAllPlans()
    
    @Query("UPDATE cycling_training_plans SET isActive = 1 WHERE id = :id")
    suspend fun activatePlan(id: Long)
}
