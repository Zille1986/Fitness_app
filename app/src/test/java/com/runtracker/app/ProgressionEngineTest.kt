package com.runtracker.app

import com.runtracker.shared.data.model.*
import org.junit.Assert.*
import org.junit.Test

class ProgressionEngineTest {

    @Test
    fun `suggestProgression returns MAINTAIN when no history`() {
        val result = ProgressionEngine.suggestProgression(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            recentHistory = emptyList()
        )
        
        assertEquals(SuggestionType.MAINTAIN, result.suggestionType)
        assertEquals(0f, result.confidence)
    }

    @Test
    fun `suggestProgression returns MAINTAIN when insufficient history`() {
        val history = listOf(
            createHistory(weight = 80.0, reps = 10, oneRepMax = 106.0)
        )
        
        val result = ProgressionEngine.suggestProgression(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            recentHistory = history
        )
        
        assertEquals(SuggestionType.MAINTAIN, result.suggestionType)
        assertEquals(0.5f, result.confidence)
    }

    @Test
    fun `suggestProgression suggests INCREASE_WEIGHT at top of rep range`() {
        val history = listOf(
            createHistory(weight = 80.0, reps = 12, oneRepMax = 116.0),
            createHistory(weight = 80.0, reps = 12, oneRepMax = 116.0),
            createHistory(weight = 80.0, reps = 11, oneRepMax = 112.0)
        )
        
        val result = ProgressionEngine.suggestProgression(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            recentHistory = history,
            targetReps = 8..12
        )
        
        assertEquals(SuggestionType.INCREASE_WEIGHT, result.suggestionType)
        assertEquals(82.5, result.suggestedWeight, 0.1)
        assertEquals(8, result.suggestedReps)
    }

    @Test
    fun `suggestProgression suggests INCREASE_REPS in middle of range`() {
        val history = listOf(
            createHistory(weight = 80.0, reps = 9, oneRepMax = 104.0),
            createHistory(weight = 80.0, reps = 9, oneRepMax = 104.0),
            createHistory(weight = 80.0, reps = 8, oneRepMax = 100.0)
        )
        
        val result = ProgressionEngine.suggestProgression(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            recentHistory = history,
            targetReps = 8..12
        )
        
        assertEquals(SuggestionType.INCREASE_REPS, result.suggestionType)
        assertEquals(80.0, result.suggestedWeight, 0.1)
        assertEquals(10, result.suggestedReps)
    }

    @Test
    fun `suggestProgression suggests DELOAD when performance declining`() {
        val history = listOf(
            createHistory(weight = 80.0, reps = 6, oneRepMax = 95.0),
            createHistory(weight = 80.0, reps = 7, oneRepMax = 98.0),
            createHistory(weight = 80.0, reps = 8, oneRepMax = 100.0)
        )
        
        val result = ProgressionEngine.suggestProgression(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            recentHistory = history,
            targetReps = 8..12
        )
        
        assertEquals(SuggestionType.DELOAD, result.suggestionType)
        assertEquals(72.0, result.suggestedWeight, 1.0) // 90% of current
    }

    @Test
    fun `suggestStartingWeight returns conservative estimate`() {
        val weight = ProgressionEngine.suggestStartingWeight(
            oneRepMax = 100.0,
            targetReps = 10
        )
        
        // 75% of 1RM for 10 reps, then 90% conservative = ~67.5, rounded to 2.5kg increment
        assertTrue(weight >= 60.0 && weight <= 70.0)
    }

    @Test
    fun `suggestStartingWeight returns 0 for null 1RM`() {
        val weight = ProgressionEngine.suggestStartingWeight(
            oneRepMax = null,
            targetReps = 10
        )
        
        assertEquals(0.0, weight, 0.01)
    }

    private fun createHistory(
        weight: Double,
        reps: Int,
        oneRepMax: Double
    ): ExerciseHistory {
        return ExerciseHistory(
            id = 0,
            exerciseId = 1L,
            workoutId = 1L,
            date = System.currentTimeMillis(),
            bestWeight = weight,
            bestReps = reps,
            totalVolume = weight * reps * 3,
            totalSets = 3,
            totalReps = reps * 3,
            estimatedOneRepMax = oneRepMax
        )
    }
}
