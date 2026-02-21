package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

/**
 * A user-created custom running workout template
 */
@Entity(tableName = "custom_run_workouts")
@TypeConverters(Converters::class)
data class CustomRunWorkout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val phases: List<WorkoutPhase> = emptyList(),
    val estimatedDurationMinutes: Int = 0,
    val estimatedDistanceMeters: Double = 0.0,
    val difficulty: RunDifficulty = RunDifficulty.MODERATE,
    val category: WorkoutCategory = WorkoutCategory.GENERAL,
    val isFavorite: Boolean = false,
    val timesUsed: Int = 0,
    val lastUsed: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalDurationSeconds: Int
        get() = phases.sumOf { phase -> 
            (phase.durationSeconds ?: 0) * phase.repetitions 
        }
    
    val formattedDuration: String
        get() {
            val totalSeconds = totalDurationSeconds
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
}

/**
 * A single phase within a custom workout (warmup, interval, recovery, cooldown)
 */
data class WorkoutPhase(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: PhaseType,
    val name: String = "",
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    val targetPaceMin: Double? = null,      // seconds per km
    val targetPaceMax: Double? = null,      // seconds per km
    val targetHeartRateMin: Int? = null,
    val targetHeartRateMax: Int? = null,
    val targetHeartRateZone: HeartRateZone? = null,
    val effort: EffortLevel = EffortLevel.MODERATE,
    val repetitions: Int = 1,
    val orderIndex: Int = 0,
    val notes: String? = null
) {
    val durationFormatted: String
        get() {
            val seconds = durationSeconds ?: return "--"
            val mins = seconds / 60
            val secs = seconds % 60
            return if (mins > 0) {
                if (secs > 0) "${mins}m ${secs}s" else "${mins}m"
            } else {
                "${secs}s"
            }
        }
    
    val distanceFormatted: String
        get() {
            val meters = distanceMeters ?: return "--"
            return if (meters >= 1000) {
                "%.1f km".format(meters / 1000)
            } else {
                "${meters.toInt()}m"
            }
        }
    
    val paceRangeFormatted: String
        get() {
            val min = targetPaceMin ?: return "--"
            val max = targetPaceMax
            val minFormatted = formatPace(min)
            return if (max != null && max != min) {
                "$minFormatted - ${formatPace(max)}"
            } else {
                minFormatted
            }
        }
    
    private fun formatPace(secondsPerKm: Double): String {
        val mins = (secondsPerKm / 60).toInt()
        val secs = (secondsPerKm % 60).toInt()
        return "$mins:${secs.toString().padStart(2, '0')}"
    }
}

enum class PhaseType {
    WARMUP,
    WORK,
    RECOVERY,
    REST,
    COOLDOWN,
    STRIDE,         // Short acceleration
    FLOAT,          // Easy running between hard efforts
    SURGE,          // Brief speed increase
    TEMPO,          // Sustained threshold effort
    SPRINT          // All-out effort
}

enum class EffortLevel {
    VERY_EASY,      // Can hold a conversation easily
    EASY,           // Conversational pace
    MODERATE,       // Can speak in sentences
    HARD,           // Can only speak a few words
    VERY_HARD,      // Cannot speak
    ALL_OUT         // Maximum effort
}

enum class RunDifficulty {
    EASY,
    MODERATE,
    HARD,
    VERY_HARD
}

enum class WorkoutCategory {
    GENERAL,
    SPEED,
    ENDURANCE,
    STRENGTH,
    RECOVERY,
    RACE_PREP,
    CUSTOM
}

/**
 * A user-created custom training plan
 */
@Entity(tableName = "custom_training_plans")
@TypeConverters(Converters::class)
data class CustomTrainingPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val goalType: GoalType = GoalType.CUSTOM,
    val targetRaceDistance: Double? = null,     // meters
    val targetRaceTime: Long? = null,           // milliseconds
    val targetRaceDate: Long? = null,
    val durationWeeks: Int = 12,
    val startDate: Long? = null,
    val weeks: List<PlanWeek> = emptyList(),
    val currentWeek: Int = 1,
    val isActive: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
) {
    val totalWorkouts: Int
        get() = weeks.sumOf { it.workouts.size }
    
    val completedWorkouts: Int
        get() = weeks.sumOf { week -> week.workouts.count { it.isCompleted } }
    
    val progressPercent: Float
        get() = if (totalWorkouts > 0) completedWorkouts.toFloat() / totalWorkouts else 0f
}

/**
 * A week within a custom training plan
 */
data class PlanWeek(
    val weekNumber: Int,
    val name: String = "Week $weekNumber",
    val description: String = "",
    val weekType: WeekType = WeekType.BUILD,
    val workouts: List<PlanWorkout> = emptyList(),
    val totalDistanceMeters: Double = 0.0,
    val totalDurationMinutes: Int = 0
)

/**
 * A scheduled workout within a plan week
 */
data class PlanWorkout(
    val id: String = java.util.UUID.randomUUID().toString(),
    val dayOfWeek: Int,                         // 1 = Monday, 7 = Sunday
    val workoutType: WorkoutType,
    val customWorkoutId: Long? = null,          // Reference to CustomRunWorkout if custom
    val name: String = "",
    val description: String = "",
    val targetDistanceMeters: Double? = null,
    val targetDurationMinutes: Int? = null,
    val targetPaceMin: Double? = null,
    val targetPaceMax: Double? = null,
    val intervals: List<Interval>? = null,
    val isCompleted: Boolean = false,
    val completedRunId: Long? = null,
    val completedDate: Long? = null,
    val notes: String? = null
)

enum class WeekType {
    BASE,           // Foundation building
    BUILD,          // Increasing intensity/volume
    PEAK,           // Highest training load
    TAPER,          // Reducing load before race
    RECOVERY,       // Easy week for adaptation
    RACE            // Race week
}

/**
 * Pre-built workout templates for common workouts
 */
object RunWorkoutTemplates {
    
    fun getSpeedWorkouts(): List<CustomRunWorkout> = listOf(
        CustomRunWorkout(
            name = "Classic 400m Repeats",
            description = "8x400m with 200m recovery jog",
            category = WorkoutCategory.SPEED,
            difficulty = RunDifficulty.HARD,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 600, effort = EffortLevel.EASY, name = "Easy jog"),
                WorkoutPhase(type = PhaseType.STRIDE, durationSeconds = 20, effort = EffortLevel.HARD, repetitions = 4, name = "Strides"),
                WorkoutPhase(type = PhaseType.WORK, distanceMeters = 400.0, effort = EffortLevel.VERY_HARD, repetitions = 8, name = "400m repeat"),
                WorkoutPhase(type = PhaseType.RECOVERY, distanceMeters = 200.0, effort = EffortLevel.EASY, repetitions = 8, name = "Recovery jog"),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 600, effort = EffortLevel.VERY_EASY, name = "Cool down")
            )
        ),
        CustomRunWorkout(
            name = "800m Repeats",
            description = "6x800m at 5K pace with 400m recovery",
            category = WorkoutCategory.SPEED,
            difficulty = RunDifficulty.HARD,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 600, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.WORK, distanceMeters = 800.0, effort = EffortLevel.VERY_HARD, repetitions = 6),
                WorkoutPhase(type = PhaseType.RECOVERY, distanceMeters = 400.0, effort = EffortLevel.EASY, repetitions = 6),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 600, effort = EffortLevel.VERY_EASY)
            )
        ),
        CustomRunWorkout(
            name = "Mile Repeats",
            description = "4x1 mile at threshold pace with 3 min rest",
            category = WorkoutCategory.SPEED,
            difficulty = RunDifficulty.VERY_HARD,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 600, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.WORK, distanceMeters = 1609.0, effort = EffortLevel.HARD, repetitions = 4),
                WorkoutPhase(type = PhaseType.REST, durationSeconds = 180, effort = EffortLevel.VERY_EASY, repetitions = 4),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 600, effort = EffortLevel.VERY_EASY)
            )
        ),
        CustomRunWorkout(
            name = "Yasso 800s",
            description = "Marathon predictor: 10x800m at goal marathon time",
            category = WorkoutCategory.SPEED,
            difficulty = RunDifficulty.VERY_HARD,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 600, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.WORK, distanceMeters = 800.0, effort = EffortLevel.HARD, repetitions = 10),
                WorkoutPhase(type = PhaseType.RECOVERY, distanceMeters = 400.0, effort = EffortLevel.EASY, repetitions = 10),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 600, effort = EffortLevel.VERY_EASY)
            )
        )
    )
    
    fun getLadderWorkouts(): List<CustomRunWorkout> = listOf(
        CustomRunWorkout(
            name = "Classic Ladder",
            description = "200-400-600-800-600-400-200m pyramid",
            category = WorkoutCategory.SPEED,
            difficulty = RunDifficulty.HARD,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 600, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.WORK, distanceMeters = 200.0, effort = EffortLevel.VERY_HARD),
                WorkoutPhase(type = PhaseType.RECOVERY, durationSeconds = 60, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.WORK, distanceMeters = 400.0, effort = EffortLevel.VERY_HARD),
                WorkoutPhase(type = PhaseType.RECOVERY, durationSeconds = 90, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.WORK, distanceMeters = 600.0, effort = EffortLevel.VERY_HARD),
                WorkoutPhase(type = PhaseType.RECOVERY, durationSeconds = 120, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.WORK, distanceMeters = 800.0, effort = EffortLevel.VERY_HARD),
                WorkoutPhase(type = PhaseType.RECOVERY, durationSeconds = 120, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.WORK, distanceMeters = 600.0, effort = EffortLevel.VERY_HARD),
                WorkoutPhase(type = PhaseType.RECOVERY, durationSeconds = 90, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.WORK, distanceMeters = 400.0, effort = EffortLevel.VERY_HARD),
                WorkoutPhase(type = PhaseType.RECOVERY, durationSeconds = 60, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.WORK, distanceMeters = 200.0, effort = EffortLevel.VERY_HARD),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 600, effort = EffortLevel.VERY_EASY)
            )
        )
    )
    
    fun getTempoWorkouts(): List<CustomRunWorkout> = listOf(
        CustomRunWorkout(
            name = "Classic Tempo",
            description = "20 min at threshold pace",
            category = WorkoutCategory.ENDURANCE,
            difficulty = RunDifficulty.MODERATE,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 600, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.TEMPO, durationSeconds = 1200, effort = EffortLevel.HARD),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 600, effort = EffortLevel.VERY_EASY)
            )
        ),
        CustomRunWorkout(
            name = "Cruise Intervals",
            description = "4x5 min at threshold with 1 min recovery",
            category = WorkoutCategory.ENDURANCE,
            difficulty = RunDifficulty.HARD,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 600, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.TEMPO, durationSeconds = 300, effort = EffortLevel.HARD, repetitions = 4),
                WorkoutPhase(type = PhaseType.RECOVERY, durationSeconds = 60, effort = EffortLevel.EASY, repetitions = 4),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 600, effort = EffortLevel.VERY_EASY)
            )
        ),
        CustomRunWorkout(
            name = "Progression Run",
            description = "Start easy, finish at tempo pace",
            category = WorkoutCategory.ENDURANCE,
            difficulty = RunDifficulty.MODERATE,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 300, effort = EffortLevel.VERY_EASY, name = "Very easy start"),
                WorkoutPhase(type = PhaseType.WORK, durationSeconds = 600, effort = EffortLevel.EASY, name = "Easy pace"),
                WorkoutPhase(type = PhaseType.WORK, durationSeconds = 600, effort = EffortLevel.MODERATE, name = "Moderate pace"),
                WorkoutPhase(type = PhaseType.TEMPO, durationSeconds = 600, effort = EffortLevel.HARD, name = "Tempo finish"),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 300, effort = EffortLevel.VERY_EASY)
            )
        )
    )
    
    fun getHillWorkouts(): List<CustomRunWorkout> = listOf(
        CustomRunWorkout(
            name = "Hill Repeats",
            description = "8x60s uphill with jog down recovery",
            category = WorkoutCategory.STRENGTH,
            difficulty = RunDifficulty.HARD,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 600, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.WORK, durationSeconds = 60, effort = EffortLevel.VERY_HARD, repetitions = 8, name = "Hill sprint"),
                WorkoutPhase(type = PhaseType.RECOVERY, durationSeconds = 90, effort = EffortLevel.VERY_EASY, repetitions = 8, name = "Jog down"),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 600, effort = EffortLevel.VERY_EASY)
            )
        ),
        CustomRunWorkout(
            name = "Hill Sprints",
            description = "10x20s all-out hill sprints",
            category = WorkoutCategory.STRENGTH,
            difficulty = RunDifficulty.VERY_HARD,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 600, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.SPRINT, durationSeconds = 20, effort = EffortLevel.ALL_OUT, repetitions = 10, name = "Sprint"),
                WorkoutPhase(type = PhaseType.RECOVERY, durationSeconds = 120, effort = EffortLevel.VERY_EASY, repetitions = 10, name = "Full recovery"),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 600, effort = EffortLevel.VERY_EASY)
            )
        )
    )
    
    fun getFartlekWorkouts(): List<CustomRunWorkout> = listOf(
        CustomRunWorkout(
            name = "Classic Fartlek",
            description = "Unstructured speed play with varied efforts",
            category = WorkoutCategory.SPEED,
            difficulty = RunDifficulty.MODERATE,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 600, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.SURGE, durationSeconds = 60, effort = EffortLevel.HARD, repetitions = 6),
                WorkoutPhase(type = PhaseType.FLOAT, durationSeconds = 120, effort = EffortLevel.EASY, repetitions = 6),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 600, effort = EffortLevel.VERY_EASY)
            )
        ),
        CustomRunWorkout(
            name = "Mona Fartlek",
            description = "2x(90s hard, 90s easy, 60s hard, 60s easy, 30s hard, 30s easy)",
            category = WorkoutCategory.SPEED,
            difficulty = RunDifficulty.HARD,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 600, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.WORK, durationSeconds = 90, effort = EffortLevel.HARD, repetitions = 2),
                WorkoutPhase(type = PhaseType.FLOAT, durationSeconds = 90, effort = EffortLevel.EASY, repetitions = 2),
                WorkoutPhase(type = PhaseType.WORK, durationSeconds = 60, effort = EffortLevel.HARD, repetitions = 2),
                WorkoutPhase(type = PhaseType.FLOAT, durationSeconds = 60, effort = EffortLevel.EASY, repetitions = 2),
                WorkoutPhase(type = PhaseType.WORK, durationSeconds = 30, effort = EffortLevel.VERY_HARD, repetitions = 2),
                WorkoutPhase(type = PhaseType.FLOAT, durationSeconds = 30, effort = EffortLevel.EASY, repetitions = 2),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 600, effort = EffortLevel.VERY_EASY)
            )
        )
    )
    
    fun getRecoveryWorkouts(): List<CustomRunWorkout> = listOf(
        CustomRunWorkout(
            name = "Recovery Run",
            description = "Easy 30 min recovery jog",
            category = WorkoutCategory.RECOVERY,
            difficulty = RunDifficulty.EASY,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WARMUP, durationSeconds = 300, effort = EffortLevel.VERY_EASY),
                WorkoutPhase(type = PhaseType.WORK, durationSeconds = 1500, effort = EffortLevel.EASY),
                WorkoutPhase(type = PhaseType.COOLDOWN, durationSeconds = 300, effort = EffortLevel.VERY_EASY)
            )
        ),
        CustomRunWorkout(
            name = "Shake Out Run",
            description = "Short easy run before or after race",
            category = WorkoutCategory.RECOVERY,
            difficulty = RunDifficulty.EASY,
            phases = listOf(
                WorkoutPhase(type = PhaseType.WORK, durationSeconds = 900, effort = EffortLevel.VERY_EASY),
                WorkoutPhase(type = PhaseType.STRIDE, durationSeconds = 15, effort = EffortLevel.MODERATE, repetitions = 4),
                WorkoutPhase(type = PhaseType.WORK, durationSeconds = 300, effort = EffortLevel.VERY_EASY)
            )
        )
    )
    
    fun getAllTemplates(): List<CustomRunWorkout> = 
        getSpeedWorkouts() + getLadderWorkouts() + getTempoWorkouts() + 
        getHillWorkouts() + getFartlekWorkouts() + getRecoveryWorkouts()
}
