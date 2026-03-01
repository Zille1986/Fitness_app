package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercisesFlow(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    suspend fun getAllExercises(): List<Exercise>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscleGroupFlow(muscleGroup: MuscleGroup): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE equipment = :equipment ORDER BY name ASC")
    fun getExercisesByEquipmentFlow(equipment: Equipment): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchExercisesFlow(query: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE isCustom = 1 ORDER BY name ASC")
    fun getCustomExercisesFlow(): Flow<List<Exercise>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int
}

@Dao
interface GymWorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: GymWorkout): Long

    @Update
    suspend fun updateWorkout(workout: GymWorkout)

    @Delete
    suspend fun deleteWorkout(workout: GymWorkout)

    @Query("SELECT * FROM gym_workouts WHERE id = :id")
    suspend fun getWorkoutById(id: Long): GymWorkout?

    @Query("SELECT * FROM gym_workouts WHERE id = :id")
    fun getWorkoutByIdFlow(id: Long): Flow<GymWorkout?>

    @Query("SELECT * FROM gym_workouts ORDER BY startTime DESC")
    fun getAllWorkoutsFlow(): Flow<List<GymWorkout>>
    
    @Query("SELECT * FROM gym_workouts ORDER BY startTime DESC")
    suspend fun getAllWorkouts(): List<GymWorkout>

    @Query("SELECT * FROM gym_workouts ORDER BY startTime DESC LIMIT :limit")
    fun getRecentWorkoutsFlow(limit: Int): Flow<List<GymWorkout>>

    @Query("SELECT * FROM gym_workouts WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    fun getWorkoutsInRangeFlow(startTime: Long, endTime: Long): Flow<List<GymWorkout>>

    @Query("SELECT * FROM gym_workouts WHERE isCompleted = 1 ORDER BY startTime DESC")
    fun getCompletedWorkoutsFlow(): Flow<List<GymWorkout>>

    @Query("SELECT * FROM gym_workouts WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveWorkout(): GymWorkout?

    @Query("SELECT * FROM gym_workouts WHERE startTime >= :startTime AND startTime <= :endTime AND isCompleted = 1 ORDER BY startTime DESC")
    suspend fun getCompletedWorkoutsInRange(startTime: Long, endTime: Long): List<GymWorkout>

    @Query("SELECT SUM(totalVolume) FROM gym_workouts WHERE startTime >= :startTime AND startTime <= :endTime")
    suspend fun getTotalVolumeInRange(startTime: Long, endTime: Long): Double?

    @Query("SELECT COUNT(*) FROM gym_workouts WHERE startTime >= :startTime AND startTime <= :endTime AND isCompleted = 1")
    suspend fun getWorkoutCountInRange(startTime: Long, endTime: Long): Int
}

@Dao
interface WorkoutTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: WorkoutTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<WorkoutTemplate>)

    @Update
    suspend fun updateTemplate(template: WorkoutTemplate)

    @Delete
    suspend fun deleteTemplate(template: WorkoutTemplate)

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): WorkoutTemplate?

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    fun getTemplateByIdFlow(id: Long): Flow<WorkoutTemplate?>

    @Query("SELECT * FROM workout_templates ORDER BY CASE WHEN lastUsed IS NULL THEN 1 ELSE 0 END, lastUsed DESC, timesUsed DESC")
    fun getAllTemplatesFlow(): Flow<List<WorkoutTemplate>>

    @Query("SELECT * FROM workout_templates WHERE isDefault = 1")
    fun getDefaultTemplatesFlow(): Flow<List<WorkoutTemplate>>

    @Query("SELECT * FROM workout_templates WHERE isDefault = 0")
    fun getCustomTemplatesFlow(): Flow<List<WorkoutTemplate>>

    @Query("UPDATE workout_templates SET timesUsed = timesUsed + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM workout_templates")
    suspend fun getTemplateCount(): Int
}

@Dao
interface ExerciseHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ExerciseHistory): Long

    @Query("SELECT * FROM exercise_history WHERE exerciseId = :exerciseId ORDER BY date DESC")
    fun getHistoryForExerciseFlow(exerciseId: Long): Flow<List<ExerciseHistory>>

    @Query("SELECT * FROM exercise_history WHERE exerciseId = :exerciseId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentHistoryForExercise(exerciseId: Long, limit: Int): List<ExerciseHistory>

    @Query("SELECT * FROM exercise_history WHERE exerciseId = :exerciseId ORDER BY estimatedOneRepMax DESC LIMIT 1")
    suspend fun getBestOneRepMaxForExercise(exerciseId: Long): ExerciseHistory?

    @Query("SELECT * FROM exercise_history WHERE exerciseId = :exerciseId ORDER BY bestWeight DESC LIMIT 1")
    suspend fun getBestWeightForExercise(exerciseId: Long): ExerciseHistory?

    @Query("SELECT * FROM exercise_history WHERE exerciseId = :exerciseId AND date >= :startTime ORDER BY date DESC")
    suspend fun getHistorySince(exerciseId: Long, startTime: Long): List<ExerciseHistory>

    @Query("SELECT * FROM exercise_history WHERE isPersonalRecord = 1 ORDER BY date DESC")
    fun getPersonalRecordsFlow(): Flow<List<ExerciseHistory>>
    
    @Query("SELECT * FROM exercise_history WHERE isPersonalRecord = 1 ORDER BY date DESC LIMIT 10")
    suspend fun getPersonalRecords(): List<ExerciseHistory>

    @Query("SELECT * FROM exercise_history WHERE isPersonalRecord = 1 AND exerciseId = :exerciseId ORDER BY date DESC")
    fun getPersonalRecordsForExerciseFlow(exerciseId: Long): Flow<List<ExerciseHistory>>

    @Query("SELECT DISTINCT exerciseId FROM exercise_history ORDER BY date DESC")
    suspend fun getExercisesWithHistory(): List<Long>

    @Query("SELECT * FROM exercise_history WHERE workoutId = :workoutId AND exerciseId = :exerciseId LIMIT 1")
    suspend fun getHistoryForWorkoutExercise(workoutId: Long, exerciseId: Long): ExerciseHistory?

    @Update
    suspend fun updateHistory(history: ExerciseHistory)
}
