package com.runtracker.shared.data.model

data class ProgressionSuggestion(
    val exerciseId: Long,
    val exerciseName: String,
    val suggestionType: SuggestionType,
    val currentWeight: Double,
    val currentReps: Int,
    val suggestedWeight: Double,
    val suggestedReps: Int,
    val confidence: Float,
    val reasoning: String
)

enum class SuggestionType {
    INCREASE_WEIGHT,
    INCREASE_REPS,
    MAINTAIN,
    DELOAD,
    TRY_NEW_VARIATION
}

object ProgressionEngine {
    
    private const val MIN_SESSIONS_FOR_PROGRESSION = 2
    private const val WEIGHT_INCREMENT_KG = 2.5
    private const val WEIGHT_INCREMENT_LB = 5.0
    private const val TARGET_RPE = 8
    
    fun suggestProgression(
        exerciseId: Long,
        exerciseName: String,
        recentHistory: List<ExerciseHistory>,
        targetReps: IntRange = 8..12,
        useMetric: Boolean = true
    ): ProgressionSuggestion {
        if (recentHistory.isEmpty()) {
            return ProgressionSuggestion(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                suggestionType = SuggestionType.MAINTAIN,
                currentWeight = 0.0,
                currentReps = targetReps.first,
                suggestedWeight = 0.0,
                suggestedReps = targetReps.first,
                confidence = 0f,
                reasoning = "No history available. Start with a comfortable weight."
            )
        }
        
        val lastSession = recentHistory.first()
        val increment = if (useMetric) WEIGHT_INCREMENT_KG else WEIGHT_INCREMENT_LB
        
        // Check if we have enough data
        if (recentHistory.size < MIN_SESSIONS_FOR_PROGRESSION) {
            return ProgressionSuggestion(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                suggestionType = SuggestionType.MAINTAIN,
                currentWeight = lastSession.bestWeight,
                currentReps = lastSession.bestReps,
                suggestedWeight = lastSession.bestWeight,
                suggestedReps = lastSession.bestReps,
                confidence = 0.5f,
                reasoning = "Need more sessions to make a confident suggestion. Keep current weight."
            )
        }
        
        // Analyze recent performance
        val avgReps = recentHistory.take(3).map { it.bestReps }.average()
        val avgWeight = recentHistory.take(3).map { it.bestWeight }.average()
        val isImproving = recentHistory.size >= 2 && 
            recentHistory[0].estimatedOneRepMax > recentHistory[1].estimatedOneRepMax
        
        // Check for deload need (declining performance over 3+ sessions)
        if (recentHistory.size >= 3) {
            val declining = recentHistory[0].estimatedOneRepMax < recentHistory[1].estimatedOneRepMax &&
                           recentHistory[1].estimatedOneRepMax < recentHistory[2].estimatedOneRepMax
            if (declining) {
                return ProgressionSuggestion(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    suggestionType = SuggestionType.DELOAD,
                    currentWeight = lastSession.bestWeight,
                    currentReps = lastSession.bestReps,
                    suggestedWeight = lastSession.bestWeight * 0.9,
                    suggestedReps = targetReps.last,
                    confidence = 0.8f,
                    reasoning = "Performance declining. Consider a deload week at 90% weight."
                )
            }
        }
        
        // If hitting top of rep range consistently, increase weight
        if (avgReps >= targetReps.last - 1) {
            return ProgressionSuggestion(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                suggestionType = SuggestionType.INCREASE_WEIGHT,
                currentWeight = lastSession.bestWeight,
                currentReps = lastSession.bestReps,
                suggestedWeight = lastSession.bestWeight + increment,
                suggestedReps = targetReps.first,
                confidence = 0.85f,
                reasoning = "Consistently hitting ${targetReps.last} reps. Time to increase weight by ${increment}kg."
            )
        }
        
        // If in middle of rep range, increase reps
        if (avgReps >= targetReps.first && avgReps < targetReps.last - 1) {
            return ProgressionSuggestion(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                suggestionType = SuggestionType.INCREASE_REPS,
                currentWeight = lastSession.bestWeight,
                currentReps = lastSession.bestReps,
                suggestedWeight = lastSession.bestWeight,
                suggestedReps = lastSession.bestReps + 1,
                confidence = 0.75f,
                reasoning = "Good progress! Try to add 1 more rep this session."
            )
        }
        
        // If below rep range, maintain
        return ProgressionSuggestion(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            suggestionType = SuggestionType.MAINTAIN,
            currentWeight = lastSession.bestWeight,
            currentReps = lastSession.bestReps,
            suggestedWeight = lastSession.bestWeight,
            suggestedReps = targetReps.first,
            confidence = 0.7f,
            reasoning = "Focus on hitting ${targetReps.first} reps before progressing."
        )
    }
    
    fun suggestStartingWeight(
        oneRepMax: Double?,
        targetReps: Int,
        useMetric: Boolean = true
    ): Double {
        if (oneRepMax == null || oneRepMax <= 0) return 0.0
        
        val percentage = OneRepMaxCalculator.getPercentageOfMax(targetReps)
        val rawWeight = oneRepMax * percentage * 0.9 // Start conservative
        
        // Round to nearest increment
        val increment = if (useMetric) WEIGHT_INCREMENT_KG else WEIGHT_INCREMENT_LB
        return (rawWeight / increment).toInt() * increment
    }
}
