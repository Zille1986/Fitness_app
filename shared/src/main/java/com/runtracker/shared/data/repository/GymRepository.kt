package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.*
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.*

class GymRepository(
    private val exerciseDao: ExerciseDao,
    private val gymWorkoutDao: GymWorkoutDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val exerciseHistoryDao: ExerciseHistoryDao
) {
    // Exercises
    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercisesFlow()
    
    fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>> = 
        exerciseDao.getExercisesByMuscleGroupFlow(muscleGroup)
    
    fun getExercisesByEquipment(equipment: Equipment): Flow<List<Exercise>> = 
        exerciseDao.getExercisesByEquipmentFlow(equipment)
    
    fun searchExercises(query: String): Flow<List<Exercise>> = 
        exerciseDao.searchExercisesFlow(query)
    
    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getExerciseById(id)
    
    suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.insertExercise(exercise)
    
    suspend fun insertExercises(exercises: List<Exercise>) = exerciseDao.insertExercises(exercises)
    
    suspend fun initializeDefaultExercises() {
        if (exerciseDao.getExerciseCount() == 0) {
            exerciseDao.insertExercises(ExerciseLibrary.getDefaultExercises())
        }
    }

    // Workouts
    fun getAllWorkouts(): Flow<List<GymWorkout>> = gymWorkoutDao.getAllWorkoutsFlow()
    
    suspend fun getAllWorkoutsOnce(): List<GymWorkout> = gymWorkoutDao.getAllWorkouts()
    
    fun getRecentWorkouts(limit: Int = 10): Flow<List<GymWorkout>> = 
        gymWorkoutDao.getRecentWorkoutsFlow(limit)
    
    fun getWorkoutById(id: Long): Flow<GymWorkout?> = gymWorkoutDao.getWorkoutByIdFlow(id)
    
    suspend fun getWorkoutByIdOnce(id: Long): GymWorkout? = gymWorkoutDao.getWorkoutById(id)
    
    suspend fun insertWorkout(workout: GymWorkout): Long = gymWorkoutDao.insertWorkout(workout)
    
    suspend fun updateWorkout(workout: GymWorkout) = gymWorkoutDao.updateWorkout(workout)
    
    suspend fun deleteWorkout(workout: GymWorkout) = gymWorkoutDao.deleteWorkout(workout)
    
    suspend fun getActiveWorkout(): GymWorkout? = gymWorkoutDao.getActiveWorkout()
    
    fun getWorkoutsInRange(startTime: Long, endTime: Long): Flow<List<GymWorkout>> =
        gymWorkoutDao.getWorkoutsInRangeFlow(startTime, endTime)

    // Templates
    fun getAllTemplates(): Flow<List<WorkoutTemplate>> = workoutTemplateDao.getAllTemplatesFlow()
    
    fun getDefaultTemplates(): Flow<List<WorkoutTemplate>> = workoutTemplateDao.getDefaultTemplatesFlow()
    
    fun getCustomTemplates(): Flow<List<WorkoutTemplate>> = workoutTemplateDao.getCustomTemplatesFlow()
    
    suspend fun getTemplateById(id: Long): WorkoutTemplate? = workoutTemplateDao.getTemplateById(id)
    
    fun getTemplateByIdFlow(id: Long): Flow<WorkoutTemplate?> = workoutTemplateDao.getTemplateByIdFlow(id)
    
    suspend fun insertTemplate(template: WorkoutTemplate): Long = workoutTemplateDao.insertTemplate(template)
    
    suspend fun updateTemplate(template: WorkoutTemplate) = workoutTemplateDao.updateTemplate(template)
    
    suspend fun deleteTemplate(template: WorkoutTemplate) = workoutTemplateDao.deleteTemplate(template)
    
    suspend fun incrementTemplateUsage(templateId: Long) = workoutTemplateDao.incrementUsage(templateId)
    
    suspend fun initializeDefaultTemplates() {
        if (workoutTemplateDao.getTemplateCount() == 0) {
            val templates = DefaultTemplates.getPushPullLegs() + 
                           DefaultTemplates.getUpperLower() + 
                           listOf(DefaultTemplates.getFullBody())
            workoutTemplateDao.insertTemplates(templates)
        }
    }

    // Exercise History
    fun getHistoryForExercise(exerciseId: Long): Flow<List<ExerciseHistory>> = 
        exerciseHistoryDao.getHistoryForExerciseFlow(exerciseId)
    
    suspend fun getRecentHistoryForExercise(exerciseId: Long, limit: Int = 10): List<ExerciseHistory> =
        exerciseHistoryDao.getRecentHistoryForExercise(exerciseId, limit)
    
    suspend fun getBestOneRepMax(exerciseId: Long): ExerciseHistory? =
        exerciseHistoryDao.getBestOneRepMaxForExercise(exerciseId)
    
    suspend fun getBestWeight(exerciseId: Long): ExerciseHistory? =
        exerciseHistoryDao.getBestWeightForExercise(exerciseId)
    
    fun getPersonalRecords(): Flow<List<ExerciseHistory>> = exerciseHistoryDao.getPersonalRecordsFlow()
    
    suspend fun getPersonalRecordsSnapshot(): List<ExerciseHistory> = exerciseHistoryDao.getPersonalRecords()
    
    suspend fun recordExerciseHistory(
        exerciseId: Long,
        workoutId: Long,
        sets: List<WorkoutSet>
    ): PBResult? {
        val completedSets = sets.filter { it.isCompleted }
        if (completedSets.isEmpty()) return null
        
        val bestSet = completedSets.maxByOrNull { it.weight * it.reps }!!
        val totalVolume = completedSets.sumOf { it.weight * it.reps }
        val totalReps = completedSets.sumOf { it.reps }
        val estimatedOneRepMax = OneRepMaxCalculator.calculate(bestSet.weight, bestSet.reps)
        
        val previousBest = exerciseHistoryDao.getBestOneRepMaxForExercise(exerciseId)
        val isPersonalRecord = previousBest == null || estimatedOneRepMax > previousBest.estimatedOneRepMax
        
        val history = ExerciseHistory(
            exerciseId = exerciseId,
            workoutId = workoutId,
            date = System.currentTimeMillis(),
            bestWeight = bestSet.weight,
            bestReps = bestSet.reps,
            totalVolume = totalVolume,
            totalSets = completedSets.size,
            totalReps = totalReps,
            estimatedOneRepMax = estimatedOneRepMax,
            isPersonalRecord = isPersonalRecord
        )
        
        exerciseHistoryDao.insertHistory(history)
        
        return if (isPersonalRecord) {
            PBResult(
                exerciseId = exerciseId,
                weight = bestSet.weight,
                reps = bestSet.reps,
                estimatedOneRepMax = estimatedOneRepMax
            )
        } else null
    }
    
    data class PBResult(
        val exerciseId: Long,
        val weight: Double,
        val reps: Int,
        val estimatedOneRepMax: Double
    )

    // Progression suggestions
    suspend fun getProgressionSuggestion(
        exerciseId: Long,
        exerciseName: String,
        targetReps: IntRange = 8..12
    ): ProgressionSuggestion {
        val recentHistory = exerciseHistoryDao.getRecentHistoryForExercise(exerciseId, 5)
        return ProgressionEngine.suggestProgression(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            recentHistory = recentHistory,
            targetReps = targetReps
        )
    }

    // Weekly stats
    suspend fun getWeeklyGymStats(): GymWeeklyStats {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekStart = calendar.timeInMillis
        val weekEnd = weekStart + 7 * 24 * 60 * 60 * 1000L
        
        val totalVolume = gymWorkoutDao.getTotalVolumeInRange(weekStart, weekEnd) ?: 0.0
        val workoutCount = gymWorkoutDao.getWorkoutCountInRange(weekStart, weekEnd)
        
        return GymWeeklyStats(
            weekStartDate = weekStart,
            totalVolume = totalVolume,
            workoutCount = workoutCount
        )
    }
}

data class GymWeeklyStats(
    val weekStartDate: Long,
    val totalVolume: Double,
    val workoutCount: Int
) {
    val totalVolumeFormatted: String
        get() = if (totalVolume >= 1000) {
            String.format("%.1fk kg", totalVolume / 1000)
        } else {
            String.format("%.0f kg", totalVolume)
        }
}
