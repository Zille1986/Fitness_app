package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val muscleGroup: MuscleGroup,
    val secondaryMuscleGroups: List<MuscleGroup> = emptyList(),
    val equipment: Equipment,
    val exerciseType: ExerciseType,
    val difficulty: Difficulty = Difficulty.INTERMEDIATE,
    val instructions: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val isCustom: Boolean = false,
    val imageUrl: String? = null,
    val videoFileName: String? = null  // Asset filename without extension, e.g. "barbell-bench-press"
)

enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    ABS,
    OBLIQUES,
    LOWER_BACK,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
    HIP_FLEXORS,
    TRAPS,
    LATS,
    FULL_BODY
}

enum class Equipment {
    BARBELL,
    DUMBBELL,
    KETTLEBELL,
    CABLE,
    MACHINE,
    BODYWEIGHT,
    RESISTANCE_BAND,
    EZ_BAR,
    SMITH_MACHINE,
    PULL_UP_BAR,
    DIP_BARS,
    BENCH,
    MEDICINE_BALL,
    FOAM_ROLLER,
    OTHER,
    NONE
}

enum class ExerciseType {
    COMPOUND,
    ISOLATION,
    CARDIO,
    STRETCHING,
    PLYOMETRIC
}

enum class Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

val MuscleGroup.displayName: String
    get() = name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

val Equipment.displayName: String
    get() = name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
