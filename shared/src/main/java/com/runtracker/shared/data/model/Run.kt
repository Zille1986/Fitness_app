package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

@Entity(tableName = "runs")
@TypeConverters(Converters::class)
data class Run(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val distanceMeters: Double = 0.0,
    val durationMillis: Long = 0,
    val avgPaceSecondsPerKm: Double = 0.0,
    val maxPaceSecondsPerKm: Double = 0.0,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val caloriesBurned: Int = 0,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val avgCadence: Int? = null,
    val weather: String? = null,
    val temperature: Double? = null,
    val notes: String? = null,
    val routePoints: List<RoutePoint> = emptyList(),
    val splits: List<Split> = emptyList(),
    val source: RunSource = RunSource.PHONE,
    val stravaId: String? = null,
    val isCompleted: Boolean = false
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
    val distanceMiles: Double get() = distanceMeters / 1609.344
    
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
    
    val avgPaceFormatted: String get() = formatPace(avgPaceSecondsPerKm)
    
    companion object {
        fun formatPace(secondsPerKm: Double): String {
            if (secondsPerKm <= 0 || secondsPerKm.isInfinite() || secondsPerKm.isNaN()) return "--:--"
            val minutes = (secondsPerKm / 60).toInt()
            val seconds = (secondsPerKm % 60).toInt()
            return String.format("%d:%02d", minutes, seconds)
        }
    }
}

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val timestamp: Long,
    val accuracy: Float? = null,
    val heartRate: Int? = null,
    val cadence: Int? = null,
    val speed: Float? = null
)

data class Split(
    val kilometer: Int,
    val durationMillis: Long,
    val paceSecondsPerKm: Double,
    val elevationChange: Double = 0.0,
    val avgHeartRate: Int? = null
) {
    val paceFormatted: String get() = Run.formatPace(paceSecondsPerKm)
    
    val durationFormatted: String get() {
        val totalSeconds = durationMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}

enum class RunSource {
    PHONE,
    WATCH,
    STRAVA,
    MANUAL
}
