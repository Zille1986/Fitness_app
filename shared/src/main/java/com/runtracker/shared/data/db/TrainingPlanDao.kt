package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.TrainingPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: TrainingPlan): Long

    @Update
    suspend fun updatePlan(plan: TrainingPlan)

    @Delete
    suspend fun deletePlan(plan: TrainingPlan)

    @Query("SELECT * FROM training_plans WHERE id = :id")
    suspend fun getPlanById(id: Long): TrainingPlan?

    @Query("SELECT * FROM training_plans WHERE id = :id")
    fun getPlanByIdFlow(id: Long): Flow<TrainingPlan?>

    @Query("SELECT * FROM training_plans ORDER BY createdAt DESC")
    fun getAllPlansFlow(): Flow<List<TrainingPlan>>

    @Query("SELECT * FROM training_plans WHERE isActive = 1 LIMIT 1")
    fun getActivePlanFlow(): Flow<TrainingPlan?>

    @Query("SELECT * FROM training_plans WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlan(): TrainingPlan?

    @Query("UPDATE training_plans SET isActive = 0")
    suspend fun deactivateAllPlans()
}
