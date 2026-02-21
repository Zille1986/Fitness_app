package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.CustomRunWorkout
import com.runtracker.shared.data.model.CustomTrainingPlan
import com.runtracker.shared.data.model.WorkoutCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRunWorkoutDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: CustomRunWorkout): Long
    
    @Update
    suspend fun update(workout: CustomRunWorkout)
    
    @Delete
    suspend fun delete(workout: CustomRunWorkout)
    
    @Query("SELECT * FROM custom_run_workouts WHERE id = :id")
    suspend fun getById(id: Long): CustomRunWorkout?
    
    @Query("SELECT * FROM custom_run_workouts WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<CustomRunWorkout?>
    
    @Query("SELECT * FROM custom_run_workouts ORDER BY lastUsed DESC, createdAt DESC")
    fun getAllFlow(): Flow<List<CustomRunWorkout>>
    
    @Query("SELECT * FROM custom_run_workouts ORDER BY lastUsed DESC, createdAt DESC")
    suspend fun getAll(): List<CustomRunWorkout>
    
    @Query("SELECT * FROM custom_run_workouts WHERE category = :category ORDER BY timesUsed DESC")
    fun getByCategory(category: WorkoutCategory): Flow<List<CustomRunWorkout>>
    
    @Query("SELECT * FROM custom_run_workouts WHERE isFavorite = 1 ORDER BY lastUsed DESC")
    fun getFavorites(): Flow<List<CustomRunWorkout>>
    
    @Query("SELECT * FROM custom_run_workouts ORDER BY timesUsed DESC LIMIT :limit")
    fun getMostUsed(limit: Int = 10): Flow<List<CustomRunWorkout>>
    
    @Query("UPDATE custom_run_workouts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    
    @Query("UPDATE custom_run_workouts SET timesUsed = timesUsed + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM custom_run_workouts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CustomTrainingPlanDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: CustomTrainingPlan): Long
    
    @Update
    suspend fun update(plan: CustomTrainingPlan)
    
    @Delete
    suspend fun delete(plan: CustomTrainingPlan)
    
    @Query("SELECT * FROM custom_training_plans WHERE id = :id")
    suspend fun getById(id: Long): CustomTrainingPlan?
    
    @Query("SELECT * FROM custom_training_plans WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<CustomTrainingPlan?>
    
    @Query("SELECT * FROM custom_training_plans ORDER BY lastModified DESC")
    fun getAllFlow(): Flow<List<CustomTrainingPlan>>
    
    @Query("SELECT * FROM custom_training_plans ORDER BY lastModified DESC")
    suspend fun getAll(): List<CustomTrainingPlan>
    
    @Query("SELECT * FROM custom_training_plans WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlan(): CustomTrainingPlan?
    
    @Query("SELECT * FROM custom_training_plans WHERE isActive = 1 LIMIT 1")
    fun getActivePlanFlow(): Flow<CustomTrainingPlan?>
    
    @Query("SELECT * FROM custom_training_plans WHERE isFavorite = 1 ORDER BY lastModified DESC")
    fun getFavorites(): Flow<List<CustomTrainingPlan>>
    
    @Query("UPDATE custom_training_plans SET isActive = 0")
    suspend fun deactivateAllPlans()
    
    @Query("UPDATE custom_training_plans SET isActive = 1 WHERE id = :id")
    suspend fun activatePlan(id: Long)
    
    @Query("UPDATE custom_training_plans SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    
    @Query("DELETE FROM custom_training_plans WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Transaction
    suspend fun setActivePlan(id: Long) {
        deactivateAllPlans()
        activatePlan(id)
    }
}
