package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a scheduled gym workout for a specific date
 */
@Entity(tableName = "scheduled_gym_workouts")
data class ScheduledGymWorkout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: Long,
    val templateName: String,
    val scheduledDate: Long, // Start of day timestamp
    val isCompleted: Boolean = false,
    val completedWorkoutId: Long? = null, // Links to GymWorkout when completed
    val createdAt: Long = System.currentTimeMillis()
)
