package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.ScheduledGymWorkout
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledGymWorkoutDao {
    
    @Query("SELECT * FROM scheduled_gym_workouts ORDER BY scheduledDate ASC")
    fun getAllScheduledWorkouts(): Flow<List<ScheduledGymWorkout>>
    
    @Query("SELECT * FROM scheduled_gym_workouts WHERE scheduledDate >= :startDate AND scheduledDate < :endDate ORDER BY scheduledDate ASC")
    fun getScheduledWorkoutsInRange(startDate: Long, endDate: Long): Flow<List<ScheduledGymWorkout>>
    
    @Query("SELECT * FROM scheduled_gym_workouts WHERE scheduledDate = :date LIMIT 1")
    suspend fun getScheduledWorkoutForDate(date: Long): ScheduledGymWorkout?
    
    @Query("SELECT * FROM scheduled_gym_workouts WHERE scheduledDate >= :today AND isCompleted = 0 ORDER BY scheduledDate ASC")
    fun getUpcomingWorkouts(today: Long): Flow<List<ScheduledGymWorkout>>
    
    @Query("SELECT * FROM scheduled_gym_workouts WHERE id = :id")
    suspend fun getById(id: Long): ScheduledGymWorkout?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: ScheduledGymWorkout): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workouts: List<ScheduledGymWorkout>)
    
    @Update
    suspend fun update(workout: ScheduledGymWorkout)
    
    @Delete
    suspend fun delete(workout: ScheduledGymWorkout)
    
    @Query("DELETE FROM scheduled_gym_workouts WHERE scheduledDate = :date")
    suspend fun deleteByDate(date: Long)
    
    @Query("UPDATE scheduled_gym_workouts SET isCompleted = 1, completedWorkoutId = :workoutId WHERE id = :id")
    suspend fun markCompleted(id: Long, workoutId: Long)
}
