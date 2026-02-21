package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "My Workout Plan",
    val durationMinutes: Int = 45,
    val scheduledWorkouts: List<WeeklyWorkoutDay> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

data class WeeklyWorkoutDay(
    val dayOfWeek: Int, // 1=Monday, 2=Tuesday, etc.
    val workoutType: WeeklyWorkoutType,
    val templateId: Long? = null,
    val templateName: String? = null
)

enum class WeeklyWorkoutType {
    GYM,
    RUNNING,
    REST;
    
    val displayName: String
        get() = when (this) {
            GYM -> "Gym"
            RUNNING -> "Run"
            REST -> "Rest"
        }
}
