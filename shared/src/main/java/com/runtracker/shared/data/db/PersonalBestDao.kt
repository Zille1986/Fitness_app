package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.PersonalBest
import com.runtracker.shared.data.model.WorkoutType
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalBestDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(personalBest: PersonalBest): Long
    
    @Update
    suspend fun update(personalBest: PersonalBest)
    
    @Delete
    suspend fun delete(personalBest: PersonalBest)
    
    @Query("SELECT * FROM personal_bests WHERE distanceMeters = :distanceMeters AND workoutType IS NULL LIMIT 1")
    suspend fun getByDistance(distanceMeters: Int): PersonalBest?
    
    @Query("SELECT * FROM personal_bests WHERE distanceMeters = :distanceMeters AND workoutType IS NULL LIMIT 1")
    fun getByDistanceFlow(distanceMeters: Int): Flow<PersonalBest?>
    
    @Query("SELECT * FROM personal_bests WHERE distanceMeters = :distanceMeters AND workoutType = :workoutType LIMIT 1")
    suspend fun getByDistanceAndWorkoutType(distanceMeters: Int, workoutType: WorkoutType): PersonalBest?
    
    @Query("SELECT * FROM personal_bests WHERE workoutType IS NULL ORDER BY distanceMeters ASC")
    fun getAllFlow(): Flow<List<PersonalBest>>
    
    @Query("SELECT * FROM personal_bests WHERE workoutType IS NULL ORDER BY distanceMeters ASC")
    suspend fun getAll(): List<PersonalBest>
    
    @Query("SELECT * FROM personal_bests WHERE workoutType = :workoutType ORDER BY distanceMeters ASC")
    fun getByWorkoutTypeFlow(workoutType: WorkoutType): Flow<List<PersonalBest>>
    
    @Query("DELETE FROM personal_bests WHERE distanceMeters = :distanceMeters AND workoutType IS NULL")
    suspend fun deleteByDistance(distanceMeters: Int)
    
    @Query("DELETE FROM personal_bests")
    suspend fun deleteAll()
}
