package com.runtracker.wear.presentation.screens

import androidx.compose.ui.graphics.Color

enum class ActivityType {
    RUNNING, SWIMMING, CYCLING
}

enum class ZoneAlert {
    TOO_LOW, IN_ZONE, TOO_HIGH
}

// Swimming workout types with HR zones
enum class SwimWorkoutTypeWatch(
    val displayName: String,
    val description: String,
    val targetHrZone: Int,  // 1-5
    val durationMinutes: Int
) {
    EASY_SWIM("Easy Swim", "Zone 2 • Recovery pace", 2, 30),
    ENDURANCE_SWIM("Endurance Swim", "Zone 3 • Build aerobic base", 3, 45),
    THRESHOLD_SWIM("Threshold Swim", "Zone 4 • Push your limits", 4, 30),
    INTERVAL_SWIM("Interval Sets", "Zone 4-5 • Speed work", 4, 40),
    TECHNIQUE_DRILL("Technique Drills", "Zone 2 • Focus on form", 2, 30),
    OPEN_WATER("Open Water", "Zone 3 • Distance swim", 3, 60),
    SPRINT_SETS("Sprint Sets", "Zone 5 • Max effort", 5, 25),
    RECOVERY_SWIM("Recovery Swim", "Zone 1 • Active recovery", 1, 20)
}

// Cycling workout types with HR zones
enum class CycleWorkoutTypeWatch(
    val displayName: String,
    val description: String,
    val targetHrZone: Int,
    val durationMinutes: Int
) {
    EASY_RIDE("Easy Ride", "Zone 2 • Recovery spin", 2, 45),
    ENDURANCE_RIDE("Endurance Ride", "Zone 3 • Build base", 3, 90),
    TEMPO_RIDE("Tempo Ride", "Zone 3-4 • Sustained effort", 4, 60),
    THRESHOLD_RIDE("Threshold Ride", "Zone 4 • FTP work", 4, 45),
    INTERVAL_RIDE("Intervals", "Zone 4-5 • Power bursts", 5, 50),
    HILL_CLIMB("Hill Climb", "Zone 4-5 • Climbing focus", 4, 60),
    SPRINT_INTERVALS("Sprint Intervals", "Zone 5 • Max power", 5, 30),
    RECOVERY_RIDE("Recovery Ride", "Zone 1 • Easy spin", 1, 30)
}

data class WorkoutTypeInfo(
    val emoji: String,
    val name: String,
    val type: String,
    val description: String,
    val category: String
)

data class SyncedCustomWorkout(
    val id: Long,
    val name: String,
    val description: String,
    val phasesCount: Int,
    val totalDurationSeconds: Int,
    val difficulty: String,
    val phasesJson: String
) {
    val formattedDuration: String
        get() {
            val mins = totalDurationSeconds / 60
            return if (mins >= 60) {
                "${mins / 60}h ${mins % 60}m"
            } else {
                "${mins}m"
            }
        }
}

// Shared color constants to avoid re-creating Color objects
object WearColors {
    val Swimming = Color(0xFF0288D1)
    val Cycling = Color(0xFFFF5722)
    val Music = Color(0xFF9C27B0)
    val Success = Color(0xFF4CAF50)
    val Danger = Color(0xFFE53935)
    val Warning = Color(0xFFFF9800)
    val ZoneBelow = Color(0xFF64B5F6)
    val ZoneIn = Color(0xFF81C784)
    val ZoneAbove = Color(0xFFE57373)
    val IntervalWarmup = Color(0xFF64B5F6)
    val IntervalWork = Color(0xFFE57373)
    val IntervalRecovery = Color(0xFF81C784)
    val IntervalCooldown = Color(0xFF64B5F6)
}

fun formatPace(paceSeconds: Double): String {
    if (paceSeconds <= 0 || paceSeconds.isInfinite() || paceSeconds.isNaN()) return "--:--"
    val minutes = (paceSeconds / 60).toInt()
    val seconds = (paceSeconds % 60).toInt()
    return "$minutes:${String.format("%02d", seconds)}"
}

fun formatWorkoutTypeName(workoutType: com.runtracker.shared.data.model.WorkoutType): String {
    return when (workoutType) {
        com.runtracker.shared.data.model.WorkoutType.EASY_RUN -> "🏃 Easy Run"
        com.runtracker.shared.data.model.WorkoutType.LONG_RUN -> "🏃‍♂️ Long Run"
        com.runtracker.shared.data.model.WorkoutType.TEMPO_RUN -> "⚡ Tempo Run"
        com.runtracker.shared.data.model.WorkoutType.INTERVAL_TRAINING -> "🔥 Intervals"
        com.runtracker.shared.data.model.WorkoutType.HILL_REPEATS -> "⛰️ Hill Repeats"
        com.runtracker.shared.data.model.WorkoutType.FARTLEK -> "🎲 Fartlek"
        com.runtracker.shared.data.model.WorkoutType.RECOVERY_RUN -> "💤 Recovery"
        com.runtracker.shared.data.model.WorkoutType.RACE_PACE -> "🏁 Race Pace"
        else -> "🏃 Run"
    }
}
