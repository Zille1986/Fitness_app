package com.runtracker.shared.data.model

data class WeeklyStats(
    val weekStartDate: Long,
    val totalDistanceMeters: Double,
    val totalDurationMillis: Long,
    val totalRuns: Int,
    val avgPaceSecondsPerKm: Double,
    val totalElevationGain: Double,
    val totalCalories: Int,
    val longestRunMeters: Double,
    val fastestPaceSecondsPerKm: Double
) {
    val totalDistanceKm: Double get() = totalDistanceMeters / 1000.0
    
    val avgPaceFormatted: String get() = Run.formatPace(avgPaceSecondsPerKm)
    
    val totalDurationFormatted: String get() {
        val totalSeconds = totalDurationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return String.format("%dh %dm", hours, minutes)
    }
}

data class MonthlyStats(
    val month: Int,
    val year: Int,
    val totalDistanceMeters: Double,
    val totalDurationMillis: Long,
    val totalRuns: Int,
    val avgPaceSecondsPerKm: Double,
    val totalElevationGain: Double,
    val totalCalories: Int,
    val weeklyBreakdown: List<WeeklyStats>
)

data class RunningTrend(
    val periodStart: Long,
    val periodEnd: Long,
    val avgWeeklyDistance: Double,
    val avgWeeklyRuns: Double,
    val paceImprovement: Double,
    val consistencyScore: Double,
    val fitnessLevel: FitnessLevel,
    val recommendations: List<String>
)

enum class FitnessLevel(val displayName: String, val multiplier: Float) {
    BEGINNER("Beginner", 0.7f),
    NOVICE("Novice", 0.85f),
    INTERMEDIATE("Intermediate", 1.0f),
    ADVANCED("Advanced", 1.3f),
    ELITE("Elite", 1.5f)
}

data class RunComparison(
    val currentRun: Run,
    val previousSimilarRun: Run?,
    val paceChange: Double?,
    val heartRateChange: Int?,
    val isImprovement: Boolean
)
