package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

@Entity(tableName = "swimming_workouts")
@TypeConverters(Converters::class)
data class SwimmingWorkout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val swimType: SwimType,
    val poolLength: PoolLength? = null,
    val distanceMeters: Double = 0.0,
    val durationMillis: Long = 0,
    val laps: Int = 0,
    val avgPaceSecondsPer100m: Double = 0.0,
    val bestPaceSecondsPer100m: Double = 0.0,
    val avgStrokeRate: Int? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val heartRateZoneTimes: List<HeartRateZoneTime> = emptyList(),
    val caloriesBurned: Int = 0,
    val strokeType: StrokeType = StrokeType.FREESTYLE,
    val swolf: Int? = null,
    val waterTemperature: Double? = null,
    val weather: String? = null,
    val notes: String? = null,
    val splits: List<SwimSplit> = emptyList(),
    val routePoints: List<RoutePoint> = emptyList(),
    val source: SwimSource = SwimSource.MANUAL,
    val isCompleted: Boolean = false
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
    val distanceYards: Double get() = distanceMeters * 1.09361
    
    val durationFormatted: String get() {
        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    val avgPaceFormatted: String get() = formatPace(avgPaceSecondsPer100m)
    
    companion object {
        fun formatPace(secondsPer100m: Double): String {
            if (secondsPer100m <= 0 || secondsPer100m.isInfinite() || secondsPer100m.isNaN()) return "--:--"
            val minutes = (secondsPer100m / 60).toInt()
            val seconds = (secondsPer100m % 60).toInt()
            return String.format("%d:%02d", minutes, seconds)
        }
    }
}

enum class SwimType(val displayName: String) {
    POOL("Pool"),
    OCEAN("Ocean/Sea"),
    LAKE("Lake"),
    RIVER("River")
}

enum class PoolLength(val meters: Int, val displayName: String) {
    SHORT_COURSE_METERS(25, "25m"),
    LONG_COURSE_METERS(50, "50m"),
    SHORT_COURSE_YARDS(23, "25yd"),
    CUSTOM(0, "Custom")
}

enum class StrokeType(val displayName: String) {
    FREESTYLE("Freestyle"),
    BACKSTROKE("Backstroke"),
    BREASTSTROKE("Breaststroke"),
    BUTTERFLY("Butterfly"),
    INDIVIDUAL_MEDLEY("Individual Medley"),
    MIXED("Mixed")
}

enum class SwimSource {
    MANUAL,
    WATCH,
    PHONE
}

data class SwimSplit(
    val lapNumber: Int,
    val distanceMeters: Double,
    val durationMillis: Long,
    val paceSecondsPer100m: Double,
    val strokeCount: Int? = null,
    val strokeType: StrokeType = StrokeType.FREESTYLE,
    val avgHeartRate: Int? = null
) {
    val paceFormatted: String get() = SwimmingWorkout.formatPace(paceSecondsPer100m)
    
    val durationFormatted: String get() {
        val totalSeconds = durationMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}

@Entity(tableName = "swimming_training_plans")
@TypeConverters(Converters::class)
data class SwimmingTrainingPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val goalType: SwimGoalType,
    val targetDistance: Double? = null,
    val targetTime: Long? = null,
    val startDate: Long,
    val endDate: Long,
    val weeklySchedule: List<ScheduledSwimWorkout> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SwimGoalType(val displayName: String) {
    FIRST_500M("First 500m"),
    FIRST_1K("First 1km"),
    FIRST_MILE("First Mile"),
    IMPROVE_SPEED("Improve Speed"),
    OPEN_WATER_PREP("Open Water Prep"),
    TRIATHLON_PREP("Triathlon Prep"),
    GENERAL_FITNESS("General Fitness"),
    TECHNIQUE_FOCUS("Technique Focus"),
    CUSTOM("Custom")
}

data class ScheduledSwimWorkout(
    val id: String,
    val dayOfWeek: Int,
    val weekNumber: Int,
    val workoutType: SwimWorkoutType,
    val swimType: SwimType = SwimType.POOL,
    val targetDistanceMeters: Double? = null,
    val targetDurationMinutes: Int? = null,
    val targetPaceSecondsPer100m: Double? = null,
    val targetHeartRateZone: HeartRateZone? = null,
    val targetHeartRateMin: Int? = null,
    val targetHeartRateMax: Int? = null,
    val sets: List<SwimSet>? = null,
    val description: String,
    val isCompleted: Boolean = false,
    val completedWorkoutId: Long? = null
)

enum class SwimWorkoutType(val displayName: String) {
    EASY_SWIM("Easy Swim"),
    ENDURANCE_SWIM("Endurance Swim"),
    TECHNIQUE_DRILLS("Technique Drills"),
    INTERVAL_TRAINING("Interval Training"),
    SPRINT_SETS("Sprint Sets"),
    KICK_SETS("Kick Sets"),
    PULL_SETS("Pull Sets"),
    OPEN_WATER_PRACTICE("Open Water Practice"),
    TIME_TRIAL("Time Trial"),
    RECOVERY_SWIM("Recovery Swim"),
    WARM_UP("Warm Up"),
    COOL_DOWN("Cool Down"),
    REST_DAY("Rest Day"),
    CUSTOM("Custom")
}

data class SwimSet(
    val repetitions: Int,
    val distanceMeters: Double,
    val strokeType: StrokeType = StrokeType.FREESTYLE,
    val targetPaceSecondsPer100m: Double? = null,
    val restSeconds: Int = 30,
    val description: String? = null
)
