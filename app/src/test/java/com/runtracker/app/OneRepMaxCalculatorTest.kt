package com.runtracker.app

import com.runtracker.shared.data.model.OneRepMaxCalculator
import org.junit.Assert.*
import org.junit.Test

class OneRepMaxCalculatorTest {

    @Test
    fun `calculate returns weight when reps is 1`() {
        val result = OneRepMaxCalculator.calculate(100.0, 1)
        assertEquals(100.0, result, 0.01)
    }

    @Test
    fun `calculate returns 0 for invalid inputs`() {
        assertEquals(0.0, OneRepMaxCalculator.calculate(0.0, 5), 0.01)
        assertEquals(0.0, OneRepMaxCalculator.calculate(100.0, 0), 0.01)
        assertEquals(0.0, OneRepMaxCalculator.calculate(-50.0, 5), 0.01)
    }

    @Test
    fun `calculate uses Brzycki formula correctly`() {
        // Brzycki: weight * (36 / (37 - reps))
        // 100kg x 5 reps = 100 * (36 / 32) = 112.5
        val result = OneRepMaxCalculator.calculate(100.0, 5)
        assertEquals(112.5, result, 0.1)
    }

    @Test
    fun `calculate handles various rep ranges`() {
        val weight = 100.0
        
        val oneRep = OneRepMaxCalculator.calculate(weight, 1)
        val fiveReps = OneRepMaxCalculator.calculate(weight, 5)
        val tenReps = OneRepMaxCalculator.calculate(weight, 10)
        
        // Higher reps should estimate higher 1RM
        assertTrue(fiveReps > oneRep)
        assertTrue(tenReps > fiveReps)
    }

    @Test
    fun `estimateWeight returns correct percentage of 1RM`() {
        val oneRepMax = 100.0
        
        val weight1Rep = OneRepMaxCalculator.estimateWeight(oneRepMax, 1)
        val weight5Reps = OneRepMaxCalculator.estimateWeight(oneRepMax, 5)
        val weight10Reps = OneRepMaxCalculator.estimateWeight(oneRepMax, 10)
        
        assertEquals(100.0, weight1Rep, 0.1)
        assertTrue(weight5Reps < weight1Rep)
        assertTrue(weight10Reps < weight5Reps)
    }

    @Test
    fun `estimateWeight is inverse of calculate`() {
        val originalWeight = 80.0
        val reps = 8
        
        val estimated1RM = OneRepMaxCalculator.calculate(originalWeight, reps)
        val backToWeight = OneRepMaxCalculator.estimateWeight(estimated1RM, reps)
        
        assertEquals(originalWeight, backToWeight, 1.0)
    }

    @Test
    fun `getPercentageOfMax returns expected percentages`() {
        assertEquals(1.00, OneRepMaxCalculator.getPercentageOfMax(1), 0.01)
        assertEquals(0.89, OneRepMaxCalculator.getPercentageOfMax(5), 0.01)
        assertEquals(0.75, OneRepMaxCalculator.getPercentageOfMax(10), 0.01)
        assertEquals(0.65, OneRepMaxCalculator.getPercentageOfMax(15), 0.01)
    }
}
