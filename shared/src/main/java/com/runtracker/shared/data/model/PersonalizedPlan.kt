package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

/**
 * A personalized 3-month workout plan generated from body scan analysis
 */
@Entity(tableName = "personalized_plans")
@TypeConverters(Converters::class)
data class PersonalizedPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bodyScanId: Long, // Reference to the body scan this plan is based on
    val name: String,
    val description: String,
    val fitnessGoal: FitnessGoal,
    val startDate: Long,
    val endDate: Long, // 3 months from start
    val userPreferences: PlanPreferences,
    val weeklySchedule: List<PlannedDay>, // Template for each day of the week
    val allWorkouts: List<PlannedWorkout>, // All workouts for the 3-month period
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val completedWorkouts: Int = 0,
    val totalWorkouts: Int = 0
) {
    val progressPercent: Float
        get() = if (totalWorkouts > 0) (completedWorkouts.toFloat() / totalWorkouts * 100) else 0f
    
    val isExpired: Boolean
        get() = System.currentTimeMillis() > endDate
    
    val daysRemaining: Int
        get() = ((endDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
}

/**
 * User preferences for plan generation
 */
data class PlanPreferences(
    val workoutDaysPerWeek: Int, // 2-7
    val preferredDays: List<Int>, // 1=Sunday, 2=Monday, etc. (legacy, use runningDays/gymDays)
    val availableTimePerDay: Map<Int, Int>, // Day of week -> minutes available
    val includeRunning: Boolean = true,
    val includeGym: Boolean = true,
    val runningToGymRatio: Float = 0.5f, // 0.0 = all gym, 1.0 = all running
    val preferMorningWorkouts: Boolean = true,
    val includeRestDays: Boolean = true,
    val fitnessLevel: FitnessLevel = FitnessLevel.INTERMEDIATE,
    val runningDays: List<Int> = emptyList(), // Specific days for running (1=Sunday, etc.)
    val gymDays: List<Int> = emptyList() // Specific days for gym workouts
)

/**
 * Template for a day in the weekly schedule
 */
data class PlannedDay(
    val dayOfWeek: Int, // 1=Sunday, 2=Monday, etc.
    val dayName: String,
    val workoutType: PlannedWorkoutType,
    val focus: String, // e.g., "Upper Body", "Cardio", "Rest"
    val estimatedDuration: Int, // minutes
    val isRestDay: Boolean = false
)

enum class PlannedWorkoutType {
    RUNNING,
    GYM_STRENGTH,
    GYM_HYPERTROPHY,
    HIIT,
    CARDIO,
    FLEXIBILITY,
    ACTIVE_RECOVERY,
    REST
}

/**
 * A specific workout scheduled for a specific date
 */
data class PlannedWorkout(
    val id: String,
    val date: Long,
    val dayOfWeek: Int,
    val weekNumber: Int, // 1-12 for 3 months
    val workoutType: PlannedWorkoutType,
    val title: String,
    val description: String,
    val estimatedDuration: Int, // minutes
    val exercises: List<PlannedExercise>,
    val targetZones: List<BodyZone>, // Body zones this workout targets
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val linkedRunId: Long? = null, // If this was a running workout
    val linkedGymWorkoutId: Long? = null // If this was a gym workout
)

/**
 * An exercise within a planned workout
 */
data class PlannedExercise(
    val id: String,
    val name: String,
    val exerciseType: PlannedExerciseType,
    val sets: Int? = null,
    val reps: String? = null, // e.g., "8-12" or "10"
    val duration: Int? = null, // minutes for cardio
    val distance: Double? = null, // meters for running
    val targetPace: String? = null, // e.g., "5:30/km"
    val restSeconds: Int? = null,
    val notes: String? = null,
    val targetZone: BodyZone? = null
)

enum class PlannedExerciseType {
    // Gym exercises
    COMPOUND_STRENGTH,
    ISOLATION_STRENGTH,
    BODYWEIGHT,
    MACHINE,
    CABLE,
    FREE_WEIGHT,
    
    // Cardio
    RUNNING,
    INTERVAL_RUN,
    TEMPO_RUN,
    EASY_RUN,
    LONG_RUN,
    HIIT_CARDIO,
    
    // Flexibility
    STRETCH,
    MOBILITY,
    YOGA,
    
    // Other
    WARMUP,
    COOLDOWN
}

/**
 * Recommendation for plan generation based on body scan
 */
data class PlanRecommendation(
    val recommendedDaysPerWeek: Int,
    val recommendedSplit: TrainingSplit,
    val runningDays: Int,
    val gymDays: Int,
    val focusAreas: List<BodyZone>,
    val estimatedTimePerSession: Int, // minutes
    val rationale: String
)

/**
 * Progress tracking for the plan
 */
data class PlanProgress(
    val planId: Long,
    val currentWeek: Int,
    val completedWorkouts: Int,
    val totalWorkouts: Int,
    val adherencePercent: Float,
    val streakDays: Int,
    val missedWorkouts: Int,
    val upcomingWorkouts: List<PlannedWorkout>,
    val recentCompletedWorkouts: List<PlannedWorkout>
)
