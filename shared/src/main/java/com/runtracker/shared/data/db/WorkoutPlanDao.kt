package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {
    
    @Query("SELECT * FROM workout_plans WHERE isActive = 1 LIMIT 1")
    fun getActivePlan(): Flow<WorkoutPlan?>
    
    @Query("SELECT * FROM workout_plans WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlanOnce(): WorkoutPlan?
    
    @Query("SELECT * FROM workout_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<WorkoutPlan>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlan): Long
    
    @Update
    suspend fun updatePlan(plan: WorkoutPlan)
    
    @Query("UPDATE workout_plans SET isActive = 0")
    suspend fun deactivateAllPlans()
    
    @Delete
    suspend fun deletePlan(plan: WorkoutPlan)
}
