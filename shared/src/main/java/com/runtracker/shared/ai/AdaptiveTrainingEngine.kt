package com.runtracker.shared.ai

import com.runtracker.shared.data.model.*

/**
 * Agentic AI Engine for adaptive training recommendations.
 * Analyzes user's physiological state and adjusts workouts dynamically.
 */
class AdaptiveTrainingEngine {

    /**
     * Analyze user's current state and recommend workout adjustments
     */
    fun analyzeAndRecommend(
        scheduledWorkout: ScheduledWorkout?,
        wellnessCheckin: WellnessCheckin?,
        recentRuns: List<Run>,
        gamification: UserGamification?,
        currentHrv: Int? = null,
        restingHeartRate: Int? = null
    ): TrainingRecommendation {
        
        val readinessScore = calculateReadinessScore(
            wellnessCheckin = wellnessCheckin,
            recentRuns = recentRuns,
            currentHrv = currentHrv,
            restingHeartRate = restingHeartRate
        )
        
        val fatigueLevel = calculateFatigueLevel(recentRuns)
        val trainingLoad = calculateTrainingLoad(recentRuns)
        
        // Determine if workout should be modified
        val adjustment = determineWorkoutAdjustment(
            readinessScore = readinessScore,
            fatigueLevel = fatigueLevel,
            trainingLoad = trainingLoad,
            scheduledWorkout = scheduledWorkout
        )
        
        return TrainingRecommendation(
            readinessScore = readinessScore,
            fatigueLevel = fatigueLevel,
            trainingLoadStatus = trainingLoad.status,
            adjustment = adjustment,
            originalWorkout = scheduledWorkout,
            suggestedWorkout = adjustment.suggestedWorkout,
            reasoning = generateReasoning(readinessScore, fatigueLevel, trainingLoad, adjustment),
            insights = generateInsights(wellnessCheckin, recentRuns, gamification)
        )
    }

    private fun calculateReadinessScore(
        wellnessCheckin: WellnessCheckin?,
        recentRuns: List<Run>,
        currentHrv: Int?,
        restingHeartRate: Int?
    ): Int {
        var score = 70 // Base readiness
        
        // Sleep impact
        wellnessCheckin?.sleepHours?.let { hours ->
            score += when {
                hours >= 8 -> 10
                hours >= 7 -> 5
                hours >= 6 -> 0
                hours >= 5 -> -10
                else -> -20
            }
        }
        
        // Sleep quality impact
        wellnessCheckin?.sleepQuality?.let { quality ->
            score += (quality - 3) * 5 // -10 to +10
        }
        
        // Mood impact
        wellnessCheckin?.mood?.let { mood ->
            score += (mood.value - 3) * 3 // -6 to +6
        }
        
        // Energy impact
        wellnessCheckin?.energy?.let { energy ->
            score += (energy.value - 3) * 4 // -8 to +8
        }
        
        // Stress impact (inverted)
        wellnessCheckin?.stress?.let { stress ->
            score += (3 - stress.value) * 4 // -8 to +8
        }
        
        // Soreness impact
        wellnessCheckin?.soreness?.let { soreness ->
            score += (3 - soreness) * 3 // -6 to +6
        }
        
        // HRV impact (higher is better)
        currentHrv?.let { hrv ->
            score += when {
                hrv > 70 -> 10
                hrv > 50 -> 5
                hrv > 30 -> 0
                else -> -10
            }
        }
        
        // Recent training load impact
        val recentLoad = recentRuns.filter { 
            it.startTime > System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000) 
        }
        if (recentLoad.size >= 3) {
            score -= 10 // High recent volume
        }
        
        return score.coerceIn(0, 100)
    }

    private fun calculateFatigueLevel(recentRuns: List<Run>): FatigueLevel {
        val last7Days = recentRuns.filter { 
            it.startTime > System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) 
        }
        val last3Days = recentRuns.filter { 
            it.startTime > System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000) 
        }
        
        val weeklyVolume = last7Days.sumOf { it.distanceMeters }
        val recentVolume = last3Days.sumOf { it.distanceMeters }
        val recentIntensity = last3Days.count { run ->
            run.avgPaceSecondsPerKm > 0 && run.avgPaceSecondsPerKm < 330 // Faster than 5:30/km
        }
        
        return when {
            recentIntensity >= 2 && recentVolume > 15000 -> FatigueLevel.VERY_HIGH
            last3Days.size >= 3 || recentVolume > 20000 -> FatigueLevel.HIGH
            last3Days.size >= 2 || weeklyVolume > 30000 -> FatigueLevel.MODERATE
            last7Days.isEmpty() -> FatigueLevel.VERY_LOW
            else -> FatigueLevel.LOW
        }
    }

    private fun calculateTrainingLoad(recentRuns: List<Run>): TrainingLoadAnalysis {
        val last7Days = recentRuns.filter { 
            it.startTime > System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) 
        }
        
        // Calculate acute load (last 7 days)
        val acuteLoad = last7Days.sumOf { run ->
            val durationHours = run.durationMillis / 3600000.0
            val intensityFactor = when {
                run.avgPaceSecondsPerKm > 0 && run.avgPaceSecondsPerKm < 300 -> 1.5
                run.avgPaceSecondsPerKm > 0 && run.avgPaceSecondsPerKm < 360 -> 1.2
                else -> 1.0
            }
            (durationHours * intensityFactor * 100).toInt()
        }
        
        // Simplified chronic load (would normally be 4-week average)
        val chronicLoad = (acuteLoad * 0.8).toInt()
        
        val acwr = if (chronicLoad > 0) acuteLoad.toFloat() / chronicLoad else 1f
        
        val status = when {
            acwr < 0.8 -> TrainingLoadStatus.UNDERTRAINING
            acwr in 0.8..1.3 -> TrainingLoadStatus.OPTIMAL
            acwr in 1.3..1.5 -> TrainingLoadStatus.OVERREACHING
            else -> TrainingLoadStatus.OVERTRAINING
        }
        
        return TrainingLoadAnalysis(
            acuteLoad = acuteLoad,
            chronicLoad = chronicLoad,
            acuteChronicRatio = acwr,
            status = status
        )
    }

    private fun determineWorkoutAdjustment(
        readinessScore: Int,
        fatigueLevel: FatigueLevel,
        trainingLoad: TrainingLoadAnalysis,
        scheduledWorkout: ScheduledWorkout?
    ): WorkoutAdjustment {
        
        // No scheduled workout - suggest based on state
        if (scheduledWorkout == null) {
            return when {
                readinessScore < 40 || fatigueLevel == FatigueLevel.VERY_HIGH -> {
                    WorkoutAdjustment(
                        type = AdjustmentType.REST_DAY,
                        reason = "Your body needs recovery",
                        suggestedWorkout = null
                    )
                }
                readinessScore < 60 || fatigueLevel == FatigueLevel.HIGH -> {
                    WorkoutAdjustment(
                        type = AdjustmentType.REDUCE_INTENSITY,
                        reason = "Light activity recommended",
                        suggestedWorkout = createRecoveryWorkout()
                    )
                }
                trainingLoad.status == TrainingLoadStatus.UNDERTRAINING -> {
                    WorkoutAdjustment(
                        type = AdjustmentType.INCREASE_VOLUME,
                        reason = "You're ready for more training",
                        suggestedWorkout = createModerateWorkout()
                    )
                }
                else -> {
                    WorkoutAdjustment(
                        type = AdjustmentType.PROCEED_AS_PLANNED,
                        reason = "You're in good shape to train",
                        suggestedWorkout = createStandardWorkout()
                    )
                }
            }
        }
        
        // Adjust scheduled workout based on state
        val workoutIntensity = getWorkoutIntensity(scheduledWorkout.workoutType)
        
        return when {
            // Very low readiness - convert to rest or very easy
            readinessScore < 40 -> {
                if (workoutIntensity == WorkoutIntensity.HIGH) {
                    WorkoutAdjustment(
                        type = AdjustmentType.SWAP_WORKOUT,
                        reason = "Your readiness is low. Swapping intense workout for recovery.",
                        suggestedWorkout = createRecoveryWorkout()
                    )
                } else {
                    WorkoutAdjustment(
                        type = AdjustmentType.REST_DAY,
                        reason = "Consider taking a rest day for optimal recovery.",
                        suggestedWorkout = null
                    )
                }
            }
            
            // Low readiness - reduce intensity
            readinessScore < 60 && workoutIntensity == WorkoutIntensity.HIGH -> {
                WorkoutAdjustment(
                    type = AdjustmentType.REDUCE_INTENSITY,
                    reason = "Reducing intensity due to moderate fatigue.",
                    suggestedWorkout = convertToEasierWorkout(scheduledWorkout)
                )
            }
            
            // High fatigue - reduce volume
            fatigueLevel == FatigueLevel.VERY_HIGH || fatigueLevel == FatigueLevel.HIGH -> {
                WorkoutAdjustment(
                    type = AdjustmentType.REDUCE_VOLUME,
                    reason = "Reducing volume due to accumulated fatigue.",
                    suggestedWorkout = reduceWorkoutVolume(scheduledWorkout)
                )
            }
            
            // Overtraining risk
            trainingLoad.status == TrainingLoadStatus.OVERTRAINING -> {
                WorkoutAdjustment(
                    type = AdjustmentType.SWAP_WORKOUT,
                    reason = "Training load is very high. Switching to recovery.",
                    suggestedWorkout = createRecoveryWorkout()
                )
            }
            
            // High readiness - can push harder
            readinessScore > 85 && fatigueLevel == FatigueLevel.LOW -> {
                WorkoutAdjustment(
                    type = AdjustmentType.INCREASE_INTENSITY,
                    reason = "You're feeling great! Consider pushing a bit harder.",
                    suggestedWorkout = scheduledWorkout // Keep original, just encourage
                )
            }
            
            // Normal - proceed as planned
            else -> {
                WorkoutAdjustment(
                    type = AdjustmentType.PROCEED_AS_PLANNED,
                    reason = "You're ready for today's workout.",
                    suggestedWorkout = scheduledWorkout
                )
            }
        }
    }

    private fun getWorkoutIntensity(workoutType: WorkoutType): WorkoutIntensity {
        return when (workoutType) {
            WorkoutType.EASY_RUN, WorkoutType.RECOVERY_RUN, WorkoutType.LONG_RUN -> WorkoutIntensity.LOW
            WorkoutType.TEMPO_RUN, WorkoutType.PROGRESSION_RUN, WorkoutType.THRESHOLD_RUN -> WorkoutIntensity.MEDIUM
            WorkoutType.INTERVAL_TRAINING, WorkoutType.HILL_REPEATS, WorkoutType.VO2_MAX_INTERVALS,
            WorkoutType.YASSO_800S, WorkoutType.TIME_TRIAL, WorkoutType.RACE_PACE -> WorkoutIntensity.HIGH
            else -> WorkoutIntensity.MEDIUM
        }
    }

    private fun createRecoveryWorkout(): ScheduledWorkout {
        return ScheduledWorkout(
            id = "ai_recovery_${System.currentTimeMillis()}",
            dayOfWeek = 0,
            weekNumber = 0,
            workoutType = WorkoutType.RECOVERY_RUN,
            targetDurationMinutes = 20,
            description = "Easy recovery run - keep effort very light",
            intervals = listOf(
                Interval(
                    type = IntervalType.WARMUP,
                    durationSeconds = 300
                ),
                Interval(
                    type = IntervalType.RECOVERY,
                    durationSeconds = 900
                )
            )
        )
    }

    private fun createModerateWorkout(): ScheduledWorkout {
        return ScheduledWorkout(
            id = "ai_moderate_${System.currentTimeMillis()}",
            dayOfWeek = 0,
            weekNumber = 0,
            workoutType = WorkoutType.EASY_RUN,
            targetDurationMinutes = 35,
            description = "Easy aerobic run",
            intervals = listOf(
                Interval(
                    type = IntervalType.WARMUP,
                    durationSeconds = 300
                ),
                Interval(
                    type = IntervalType.WORK,
                    durationSeconds = 1500
                ),
                Interval(
                    type = IntervalType.COOLDOWN,
                    durationSeconds = 300
                )
            )
        )
    }

    private fun createStandardWorkout(): ScheduledWorkout {
        return ScheduledWorkout(
            id = "ai_standard_${System.currentTimeMillis()}",
            dayOfWeek = 0,
            weekNumber = 0,
            workoutType = WorkoutType.EASY_RUN,
            targetDurationMinutes = 45,
            description = "Standard easy run",
            intervals = null
        )
    }

    private fun convertToEasierWorkout(original: ScheduledWorkout): ScheduledWorkout {
        return original.copy(
            workoutType = WorkoutType.EASY_RUN,
            description = "Modified: ${original.description} → Easy run due to fatigue",
            intervals = listOf(
                Interval(
                    type = IntervalType.WARMUP,
                    durationSeconds = 300
                ),
                Interval(
                    type = IntervalType.WORK,
                    durationSeconds = (original.targetDurationMinutes ?: 30) * 60 - 600
                ),
                Interval(
                    type = IntervalType.COOLDOWN,
                    durationSeconds = 300
                )
            )
        )
    }

    private fun reduceWorkoutVolume(original: ScheduledWorkout): ScheduledWorkout {
        val reducedDuration = ((original.targetDurationMinutes ?: 30) * 0.7).toInt()
        return original.copy(
            targetDurationMinutes = reducedDuration,
            description = "Reduced volume: ${original.description}",
            intervals = original.intervals?.map { interval ->
                interval.copy(durationSeconds = interval.durationSeconds?.let { (it * 0.7).toInt() })
            }
        )
    }

    private fun generateReasoning(
        readinessScore: Int,
        fatigueLevel: FatigueLevel,
        trainingLoad: TrainingLoadAnalysis,
        adjustment: WorkoutAdjustment
    ): List<String> {
        val reasons = mutableListOf<String>()
        
        reasons.add("Readiness Score: $readinessScore/100")
        reasons.add("Fatigue Level: ${fatigueLevel.label}")
        reasons.add("Training Load: ${trainingLoad.status.label} (ACWR: %.2f)".format(trainingLoad.acuteChronicRatio))
        reasons.add("Recommendation: ${adjustment.reason}")
        
        return reasons
    }

    private fun generateInsights(
        wellnessCheckin: WellnessCheckin?,
        recentRuns: List<Run>,
        gamification: UserGamification?
    ): List<TrainingInsight> {
        val insights = mutableListOf<TrainingInsight>()
        
        // Sleep insight
        wellnessCheckin?.sleepHours?.let { hours ->
            if (hours < 7) {
                insights.add(TrainingInsight(
                    icon = "😴",
                    title = "Sleep Deficit",
                    message = "You got ${hours}h of sleep. Performance may be impacted by 10-15%.",
                    priority = InsightPriority.HIGH
                ))
            }
        }
        
        // Streak insight
        gamification?.let { g ->
            if (g.currentStreak >= 7) {
                insights.add(TrainingInsight(
                    icon = "🔥",
                    title = "Great Consistency!",
                    message = "${g.currentStreak} day streak! Consider a recovery day soon.",
                    priority = InsightPriority.MEDIUM
                ))
            }
        }
        
        // Volume trend
        val thisWeek = recentRuns.filter { 
            it.startTime > System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) 
        }
        val weeklyKm = thisWeek.sumOf { it.distanceMeters } / 1000
        if (weeklyKm > 50) {
            insights.add(TrainingInsight(
                icon = "📈",
                title = "High Volume Week",
                message = "You've run ${weeklyKm.toInt()}km this week. Monitor for fatigue.",
                priority = InsightPriority.MEDIUM
            ))
        }
        
        return insights
    }
}

// Data classes for the AI engine
data class TrainingRecommendation(
    val readinessScore: Int,
    val fatigueLevel: FatigueLevel,
    val trainingLoadStatus: TrainingLoadStatus,
    val adjustment: WorkoutAdjustment,
    val originalWorkout: ScheduledWorkout?,
    val suggestedWorkout: ScheduledWorkout?,
    val reasoning: List<String>,
    val insights: List<TrainingInsight>
)

data class WorkoutAdjustment(
    val type: AdjustmentType,
    val reason: String,
    val suggestedWorkout: ScheduledWorkout?
)

data class TrainingLoadAnalysis(
    val acuteLoad: Int,
    val chronicLoad: Int,
    val acuteChronicRatio: Float,
    val status: TrainingLoadStatus
)

data class TrainingInsight(
    val icon: String,
    val title: String,
    val message: String,
    val priority: InsightPriority
)

enum class FatigueLevel(val label: String) {
    VERY_LOW("Very Low"),
    LOW("Low"),
    MODERATE("Moderate"),
    HIGH("High"),
    VERY_HIGH("Very High")
}

enum class TrainingLoadStatus(val label: String) {
    UNDERTRAINING("Undertraining"),
    OPTIMAL("Optimal"),
    OVERREACHING("Overreaching"),
    OVERTRAINING("Overtraining")
}

enum class AdjustmentType {
    PROCEED_AS_PLANNED,
    REDUCE_INTENSITY,
    REDUCE_VOLUME,
    INCREASE_INTENSITY,
    INCREASE_VOLUME,
    SWAP_WORKOUT,
    REST_DAY
}

enum class WorkoutIntensity {
    LOW, MEDIUM, HIGH
}

enum class InsightPriority {
    LOW, MEDIUM, HIGH
}
