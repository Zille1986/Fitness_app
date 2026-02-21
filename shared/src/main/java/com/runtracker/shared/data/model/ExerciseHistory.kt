package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "exercise_history",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class ExerciseHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    val workoutId: Long,
    val date: Long,
    val bestWeight: Double,
    val bestReps: Int,
    val totalVolume: Double,
    val totalSets: Int,
    val totalReps: Int,
    val estimatedOneRepMax: Double,
    val isPersonalRecord: Boolean = false
)

data class ExerciseStats(
    val exerciseId: Long,
    val exerciseName: String,
    val currentOneRepMax: Double,
    val previousOneRepMax: Double?,
    val currentMaxWeight: Double,
    val previousMaxWeight: Double?,
    val totalVolumeLast30Days: Double,
    val totalSetsLast30Days: Int,
    val lastPerformed: Long?,
    val timesPerformed: Int,
    val progressTrend: ProgressTrend
)

enum class ProgressTrend {
    IMPROVING,
    MAINTAINING,
    DECLINING,
    NEW
}

data class PersonalRecord(
    val exerciseId: Long,
    val exerciseName: String,
    val recordType: RecordType,
    val value: Double,
    val reps: Int? = null,
    val date: Long,
    val previousValue: Double?
)

enum class RecordType {
    ONE_REP_MAX,
    MAX_WEIGHT,
    MAX_REPS,
    MAX_VOLUME
}

object OneRepMaxCalculator {
    fun calculate(weight: Double, reps: Int): Double {
        if (reps <= 0 || weight <= 0) return 0.0
        if (reps == 1) return weight
        
        // Brzycki formula
        return weight * (36.0 / (37.0 - reps))
    }
    
    fun estimateWeight(oneRepMax: Double, targetReps: Int): Double {
        if (targetReps <= 0 || oneRepMax <= 0) return 0.0
        if (targetReps == 1) return oneRepMax
        
        // Inverse Brzycki
        return oneRepMax * (37.0 - targetReps) / 36.0
    }
    
    fun getPercentageOfMax(reps: Int): Double {
        return when (reps) {
            1 -> 1.00
            2 -> 0.97
            3 -> 0.94
            4 -> 0.92
            5 -> 0.89
            6 -> 0.86
            7 -> 0.83
            8 -> 0.81
            9 -> 0.78
            10 -> 0.75
            11 -> 0.73
            12 -> 0.71
            else -> if (reps > 12) 0.65 else 1.0
        }
    }
}
