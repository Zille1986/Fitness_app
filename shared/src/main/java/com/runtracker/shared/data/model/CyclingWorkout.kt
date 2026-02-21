package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

@Entity(tableName = "cycling_workouts")
@TypeConverters(Converters::class)
data class CyclingWorkout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val cyclingType: CyclingType,
    val distanceMeters: Double = 0.0,
    val durationMillis: Long = 0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val avgPowerWatts: Int? = null,
    val maxPowerWatts: Int? = null,
    val normalizedPower: Int? = null,
    val avgCadenceRpm: Int? = null,
    val maxCadenceRpm: Int? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val heartRateZoneTimes: List<HeartRateZoneTime> = emptyList(),
    val caloriesBurned: Int = 0,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val trainingStressScore: Double? = null,
    val intensityFactor: Double? = null,
    val weather: String? = null,
    val temperature: Double? = null,
    val notes: String? = null,
    val routePoints: List<RoutePoint> = emptyList(),
    val splits: List<CyclingSplit> = emptyList(),
    val powerData: List<PowerDataPoint> = emptyList(),
    val source: CyclingSource = CyclingSource.MANUAL,
    val smartTrainerBrand: String? = null,
    val smartTrainerModel: String? = null,
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
    
    val avgSpeedFormatted: String get() = String.format("%.1f km/h", avgSpeedKmh)
    
    val isSmartTrainerWorkout: Boolean get() = cyclingType == CyclingType.SMART_TRAINER
}

enum class CyclingType(val displayName: String) {
    OUTDOOR("Outdoor Ride"),
    SMART_TRAINER("Smart Trainer"),
    STATIONARY_BIKE("Stationary Bike"),
    SPIN_CLASS("Spin Class")
}

enum class CyclingSource {
    MANUAL,
    PHONE_GPS,
    SMART_TRAINER,
    WATCH,
    STRAVA
}

data class CyclingSplit(
    val kilometer: Int,
    val durationMillis: Long,
    val avgSpeedKmh: Double,
    val avgPowerWatts: Int? = null,
    val avgCadenceRpm: Int? = null,
    val elevationChange: Double = 0.0,
    val avgHeartRate: Int? = null
) {
    val durationFormatted: String get() {
        val totalSeconds = durationMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}

data class PowerDataPoint(
    val timestamp: Long,
    val powerWatts: Int,
    val cadenceRpm: Int? = null,
    val heartRate: Int? = null,
    val speedKmh: Double? = null,
    val resistanceLevel: Int? = null
)

@Entity(tableName = "cycling_training_plans")
@TypeConverters(Converters::class)
data class CyclingTrainingPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val goalType: CyclingGoalType,
    val targetDistance: Double? = null,
    val targetTime: Long? = null,
    val ftp: Int? = null,
    val startDate: Long,
    val endDate: Long,
    val weeklySchedule: List<ScheduledCyclingWorkout> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class CyclingGoalType(val displayName: String) {
    FIRST_50K("First 50km"),
    FIRST_100K("First 100km"),
    IMPROVE_FTP("Improve FTP"),
    CENTURY_RIDE("Century Ride (100mi)"),
    GRAN_FONDO("Gran Fondo"),
    TRIATHLON_PREP("Triathlon Prep"),
    GENERAL_FITNESS("General Fitness"),
    WEIGHT_LOSS("Weight Loss"),
    CUSTOM("Custom")
}

data class ScheduledCyclingWorkout(
    val id: String,
    val dayOfWeek: Int,
    val weekNumber: Int,
    val workoutType: CyclingWorkoutType,
    val cyclingType: CyclingType = CyclingType.OUTDOOR,
    val targetDistanceMeters: Double? = null,
    val targetDurationMinutes: Int? = null,
    val targetPowerWatts: Int? = null,
    val targetPowerPercentFtp: Int? = null,
    val intervals: List<CyclingInterval>? = null,
    val description: String,
    val isCompleted: Boolean = false,
    val completedWorkoutId: Long? = null
)

enum class CyclingWorkoutType(val displayName: String) {
    EASY_RIDE("Easy Ride"),
    ENDURANCE_RIDE("Endurance Ride"),
    TEMPO_RIDE("Tempo Ride"),
    SWEET_SPOT("Sweet Spot"),
    THRESHOLD_INTERVALS("Threshold Intervals"),
    VO2_MAX_INTERVALS("VO2 Max Intervals"),
    SPRINT_INTERVALS("Sprint Intervals"),
    HILL_REPEATS("Hill Repeats"),
    RECOVERY_RIDE("Recovery Ride"),
    LONG_RIDE("Long Ride"),
    RACE_SIMULATION("Race Simulation"),
    FTP_TEST("FTP Test"),
    CADENCE_DRILLS("Cadence Drills"),
    STRENGTH_ENDURANCE("Strength Endurance"),
    REST_DAY("Rest Day"),
    CUSTOM("Custom")
}

data class CyclingInterval(
    val type: CyclingIntervalType,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    val targetPowerWatts: Int? = null,
    val targetPowerPercentFtp: Int? = null,
    val targetCadenceRpm: Int? = null,
    val targetHeartRateZone: HeartRateZone? = null,
    val resistanceLevel: Int? = null,
    val repetitions: Int = 1
)

enum class CyclingIntervalType {
    WARMUP,
    WORK,
    RECOVERY,
    COOLDOWN,
    SPRINT,
    CLIMB
}

data class SmartTrainerDevice(
    val id: String,
    val name: String,
    val brand: String,
    val model: String?,
    val address: String,
    val supportsFTMS: Boolean = true,
    val supportsResistanceControl: Boolean = false,
    val supportsSimulationMode: Boolean = false,
    val maxResistanceLevel: Int? = null,
    val lastConnected: Long? = null
)

data class SmartTrainerStatus(
    val isConnected: Boolean = false,
    val currentPowerWatts: Int = 0,
    val currentCadenceRpm: Int = 0,
    val currentSpeedKmh: Double = 0.0,
    val currentHeartRate: Int? = null,
    val currentResistanceLevel: Int = 0,
    val batteryLevel: Int? = null
)
