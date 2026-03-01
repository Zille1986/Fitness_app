package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

@Entity(tableName = "gym_workouts")
@TypeConverters(Converters::class)
data class GymWorkout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val startTime: Long,
    val endTime: Long? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
    val notes: String? = null,
    val templateId: Long? = null,
    val isCompleted: Boolean = false,
    val totalVolume: Double = 0.0,
    val totalSets: Int = 0,
    val totalReps: Int = 0
) {
    val durationMillis: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime
    
    val durationFormatted: String
        get() {
            val totalMinutes = durationMillis / 60000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
}

data class WorkoutExercise(
    val id: String,
    val exerciseId: Long,
    val exerciseName: String,
    val sets: List<WorkoutSet> = emptyList(),
    val notes: String? = null,
    val restSeconds: Int = 90,
    val orderIndex: Int = 0,
    val videoFileName: String? = null  // Asset filename for demo video
) {
    val completedSets: Int get() = sets.count { it.isCompleted }
    val totalVolume: Double get() = sets.filter { it.isCompleted }.sumOf { it.weight * it.reps }
    val totalReps: Int get() = sets.filter { it.isCompleted }.sumOf { it.reps }
}

data class WorkoutSet(
    val id: String,
    val setNumber: Int,
    val setType: SetType = SetType.WORKING,
    val weight: Double = 0.0,
    val reps: Int = 0,
    val targetWeight: Double? = null,
    val targetReps: Int? = null,
    val rpe: Int? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notes: String? = null
) {
    val volume: Double get() = weight * reps
    
    val isPersonalRecord: Boolean get() = false // Calculated separately
}

enum class SetType {
    WARMUP,
    WORKING,
    DROP_SET,
    FAILURE,
    AMRAP
}

val SetType.displayName: String
    get() = when (this) {
        SetType.WARMUP -> "Warm-up"
        SetType.WORKING -> "Working"
        SetType.DROP_SET -> "Drop Set"
        SetType.FAILURE -> "To Failure"
        SetType.AMRAP -> "AMRAP"
    }
