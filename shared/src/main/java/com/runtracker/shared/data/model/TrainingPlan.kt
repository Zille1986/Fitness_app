package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

@Entity(tableName = "training_plans")
@TypeConverters(Converters::class)
data class TrainingPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val goalType: GoalType,
    val targetDistance: Double? = null,
    val targetTime: Long? = null,
    val startDate: Long,
    val endDate: Long,
    val weeklySchedule: List<ScheduledWorkout> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class GoalType {
    FIRST_5K,
    IMPROVE_5K,
    FIRST_10K,
    IMPROVE_10K,
    HALF_MARATHON,
    MARATHON,
    GENERAL_FITNESS,
    WEIGHT_LOSS,
    CUSTOM
}

data class ScheduledWorkout(
    val id: String,
    val dayOfWeek: Int,
    val weekNumber: Int,
    val workoutType: WorkoutType,
    val targetDistanceMeters: Double? = null,
    val targetDurationMinutes: Int? = null,
    val targetPaceSecondsPerKm: Double? = null,
    val targetPaceMinSecondsPerKm: Double? = null,
    val targetPaceMaxSecondsPerKm: Double? = null,
    val targetHeartRateZone: HeartRateZone? = null,
    val targetHeartRateMin: Int? = null,
    val targetHeartRateMax: Int? = null,
    val intervals: List<Interval>? = null,
    val description: String,
    val isCompleted: Boolean = false,
    val completedRunId: Long? = null
)

enum class WorkoutType {
    // Basic runs
    EASY_RUN,
    LONG_RUN,
    RECOVERY_RUN,
    
    // Speed work
    TEMPO_RUN,
    INTERVAL_TRAINING,
    FARTLEK,
    RACE_PACE,
    
    // Strength & hills
    HILL_REPEATS,
    HILL_SPRINTS,
    STAIR_WORKOUT,
    
    // Specialized
    PROGRESSION_RUN,      // Start easy, finish fast
    NEGATIVE_SPLIT,       // Second half faster than first
    THRESHOLD_RUN,        // Sustained effort at lactate threshold
    VO2_MAX_INTERVALS,    // Short, very hard intervals
    YASSO_800S,           // Marathon predictor workout
    MILE_REPEATS,         // Classic speed workout
    LADDER_WORKOUT,       // Increasing/decreasing intervals
    PYRAMID_WORKOUT,      // Build up then back down
    
    // Race simulation
    RACE_SIMULATION,      // Practice race conditions
    TIME_TRIAL,           // All-out effort for distance
    PARKRUN_PREP,         // 5K race preparation
    
    // Recovery & maintenance
    SHAKE_OUT_RUN,        // Short, easy pre/post race
    BASE_BUILDING,        // Aerobic foundation
    AEROBIC_MAINTENANCE,  // Maintain fitness
    
    // Cross-training
    CROSS_TRAINING,
    CYCLING,
    SWIMMING,
    STRENGTH_TRAINING,
    YOGA_STRETCHING,
    
    // Rest
    REST_DAY,
    ACTIVE_RECOVERY,      // Very light activity
    
    // Custom
    CUSTOM                // User-designed workout
}

data class Interval(
    val type: IntervalType,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    val targetPaceSecondsPerKm: Double? = null,
    val targetPaceMinSecondsPerKm: Double? = null,
    val targetPaceMaxSecondsPerKm: Double? = null,
    val targetHeartRateZone: HeartRateZone? = null,
    val targetHeartRateMin: Int? = null,
    val targetHeartRateMax: Int? = null,
    val repetitions: Int = 1
)

enum class IntervalType {
    WARMUP,
    WORK,
    RECOVERY,
    COOLDOWN
}

enum class HeartRateZone(val displayName: String, val minPercent: Int, val maxPercent: Int) {
    ZONE_1("Recovery", 50, 60),       // 50-60% max HR - Recovery
    ZONE_2("Aerobic Base", 60, 70),   // 60-70% max HR - Aerobic base
    ZONE_3("Aerobic Capacity", 70, 80), // 70-80% max HR - Aerobic capacity
    ZONE_4("Threshold", 80, 90),      // 80-90% max HR - Threshold
    ZONE_5("VO2 Max", 90, 100)        // 90-100% max HR - VO2 Max
}

data class HeartRateZoneTime(
    val zone: HeartRateZone,
    val durationMillis: Long,
    val percentOfTotal: Double = 0.0
) {
    val durationFormatted: String get() {
        val totalSeconds = durationMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
