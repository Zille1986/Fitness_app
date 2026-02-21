package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

@Entity(tableName = "workout_templates")
@TypeConverters(Converters::class)
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val targetMuscleGroups: List<MuscleGroup> = emptyList(),
    val exercises: List<TemplateExercise> = emptyList(),
    val estimatedDurationMinutes: Int = 60,
    val difficulty: Difficulty = Difficulty.INTERMEDIATE,
    val isDefault: Boolean = false,
    val timesUsed: Int = 0,
    val lastUsed: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class TemplateExercise(
    val id: String,
    val exerciseId: Long,
    val exerciseName: String,
    val sets: Int = 3,
    val targetRepsMin: Int = 8,
    val targetRepsMax: Int = 12,
    val restSeconds: Int = 90,
    val notes: String? = null,
    val orderIndex: Int = 0,
    val supersetGroup: Int? = null
) {
    val targetRepsDisplay: String
        get() = if (targetRepsMin == targetRepsMax) {
            "$targetRepsMin"
        } else {
            "$targetRepsMin-$targetRepsMax"
        }
    
    val targetReps: IntRange
        get() = targetRepsMin..targetRepsMax
}

object DefaultTemplates {
    
    fun getPushPullLegs(): List<WorkoutTemplate> = listOf(
        WorkoutTemplate(
            name = "Push Day",
            description = "Chest, shoulders, and triceps",
            targetMuscleGroups = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
            estimatedDurationMinutes = 60,
            isDefault = true
        ),
        WorkoutTemplate(
            name = "Pull Day",
            description = "Back and biceps",
            targetMuscleGroups = listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.LATS),
            estimatedDurationMinutes = 60,
            isDefault = true
        ),
        WorkoutTemplate(
            name = "Leg Day",
            description = "Quads, hamstrings, glutes, and calves",
            targetMuscleGroups = listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
            estimatedDurationMinutes = 70,
            isDefault = true
        )
    )
    
    fun getUpperLower(): List<WorkoutTemplate> = listOf(
        WorkoutTemplate(
            name = "Upper Body",
            description = "Chest, back, shoulders, and arms",
            targetMuscleGroups = listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
            estimatedDurationMinutes = 60,
            isDefault = true
        ),
        WorkoutTemplate(
            name = "Lower Body",
            description = "Quads, hamstrings, glutes, and calves",
            targetMuscleGroups = listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
            estimatedDurationMinutes = 60,
            isDefault = true
        )
    )
    
    fun getFullBody(): WorkoutTemplate = WorkoutTemplate(
        name = "Full Body",
        description = "Complete full body workout",
        targetMuscleGroups = listOf(MuscleGroup.FULL_BODY),
        estimatedDurationMinutes = 75,
        isDefault = true
    )
}
