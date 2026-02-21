package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a personal best record for a specific distance
 */
@Entity(tableName = "personal_bests")
data class PersonalBest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Standard distances in meters
    val distanceMeters: Int,
    
    // Fastest time records
    val fastestTimeMillis: Long? = null,
    val fastestTimeRunId: Long? = null,
    val fastestTimeDate: Long? = null,
    val fastestTimePaceSecondsPerKm: Double? = null,
    
    // Lowest average heart rate records (for same distance)
    val lowestAvgHeartRate: Int? = null,
    val lowestHrRunId: Long? = null,
    val lowestHrDate: Long? = null,
    val lowestHrTimeMillis: Long? = null,
    
    // Workout type for this PB (optional - for workout-specific PBs)
    val workoutType: WorkoutType? = null
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
    
    val fastestTimeFormatted: String get() {
        val millis = fastestTimeMillis ?: return "--:--"
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    val fastestPaceFormatted: String get() {
        val pace = fastestTimePaceSecondsPerKm ?: return "--:--"
        val minutes = (pace / 60).toInt()
        val seconds = (pace % 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }
    
    companion object {
        // Standard distances in meters
        const val DISTANCE_1K = 1000
        const val DISTANCE_5K = 5000
        const val DISTANCE_10K = 10000
        const val DISTANCE_HALF_MARATHON = 21097
        const val DISTANCE_MARATHON = 42195
        
        val STANDARD_DISTANCES = listOf(
            DISTANCE_1K,
            DISTANCE_5K,
            DISTANCE_10K,
            DISTANCE_HALF_MARATHON,
            DISTANCE_MARATHON
        )
        
        fun getDistanceName(distanceMeters: Int): String {
            return when (distanceMeters) {
                DISTANCE_1K -> "1K"
                DISTANCE_5K -> "5K"
                DISTANCE_10K -> "10K"
                DISTANCE_HALF_MARATHON -> "Half Marathon"
                DISTANCE_MARATHON -> "Marathon"
                else -> "${distanceMeters / 1000.0}km"
            }
        }
    }
}

/**
 * Represents the current run progress compared to a personal best
 */
data class CompeteProgress(
    val distanceMeters: Int,
    val personalBestTimeMillis: Long,
    val currentTimeMillis: Long,
    val currentDistanceMeters: Double,
    val pbDistanceAtCurrentTime: Double, // Where PB runner would be at current time
    val timeDifferenceMillis: Long, // Positive = behind PB, Negative = ahead of PB
    val isAheadOfPb: Boolean
) {
    val timeDifferenceFormatted: String get() {
        val absMillis = kotlin.math.abs(timeDifferenceMillis)
        val totalSeconds = absMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val sign = if (isAheadOfPb) "-" else "+"
        return if (minutes > 0) {
            "$sign${minutes}:${String.format("%02d", seconds)}"
        } else {
            "$sign${seconds}s"
        }
    }
    
    // Progress as percentage (0.0 to 1.0)
    val progressPercent: Double get() = (currentDistanceMeters / distanceMeters).coerceIn(0.0, 1.0)
    val pbProgressPercent: Double get() = (pbDistanceAtCurrentTime / distanceMeters).coerceIn(0.0, 1.0)
}
