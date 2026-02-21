package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.PersonalizedPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalizedPlanDao {
    
    @Query("SELECT * FROM personalized_plans WHERE isActive = 1 LIMIT 1")
    fun getActivePlan(): Flow<PersonalizedPlan?>
    
    @Query("SELECT * FROM personalized_plans WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlanOnce(): PersonalizedPlan?
    
    @Query("SELECT * FROM personalized_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<PersonalizedPlan>>
    
    @Query("SELECT * FROM personalized_plans WHERE id = :id")
    suspend fun getPlanById(id: Long): PersonalizedPlan?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PersonalizedPlan): Long
    
    @Update
    suspend fun updatePlan(plan: PersonalizedPlan)
    
    @Query("UPDATE personalized_plans SET isActive = 0")
    suspend fun deactivateAllPlans()
    
    @Delete
    suspend fun deletePlan(plan: PersonalizedPlan)
    
    @Query("DELETE FROM personalized_plans WHERE id = :id")
    suspend fun deletePlanById(id: Long)
}
