package com.runtracker.app

import com.runtracker.shared.data.model.RoutePoint
import com.runtracker.shared.location.RunCalculator
import org.junit.Assert.*
import org.junit.Test

class RunCalculatorTest {

    @Test
    fun `calculatePace returns correct pace for 5km in 25 minutes`() {
        val distanceMeters = 5000.0
        val durationMillis = 25 * 60 * 1000L // 25 minutes
        
        val pace = RunCalculator.calculatePace(distanceMeters, durationMillis)
        
        // 25 min / 5 km = 5 min/km = 300 sec/km
        assertEquals(300.0, pace, 1.0)
    }

    @Test
    fun `calculatePace returns 0 for zero distance`() {
        val pace = RunCalculator.calculatePace(0.0, 1000L)
        assertEquals(0.0, pace, 0.01)
    }

    @Test
    fun `calculatePace returns 0 for zero duration`() {
        val pace = RunCalculator.calculatePace(1000.0, 0L)
        assertEquals(0.0, pace, 0.01)
    }

    @Test
    fun `calculateCalories returns reasonable value for 5km run`() {
        val calories = RunCalculator.calculateCalories(
            distanceMeters = 5000.0,
            durationMillis = 30 * 60 * 1000L,
            weightKg = 70.0,
            avgHeartRate = null
        )
        
        // Rough estimate: ~60-80 cal per km for 70kg person
        assertTrue(calories in 250..500)
    }

    @Test
    fun `calculateCalories increases with weight`() {
        val duration = 30 * 60 * 1000L
        val distance = 5000.0
        
        val caloriesLight = RunCalculator.calculateCalories(distance, duration, 60.0, null)
        val caloriesHeavy = RunCalculator.calculateCalories(distance, duration, 90.0, null)
        
        assertTrue(caloriesHeavy > caloriesLight)
    }

    @Test
    fun `calculateSplits returns correct number of splits`() {
        val routePoints = createRoutePointsForDistance(5500.0) // 5.5km
        
        val splits = RunCalculator.calculateSplits(routePoints)
        
        assertEquals(5, splits.size) // 5 complete kilometers
    }

    @Test
    fun `calculateSplits returns empty for short distance`() {
        val routePoints = createRoutePointsForDistance(500.0) // 500m
        
        val splits = RunCalculator.calculateSplits(routePoints)
        
        assertTrue(splits.isEmpty())
    }

    @Test
    fun `calculateAverageHeartRate returns correct average`() {
        val routePoints = listOf(
            createRoutePoint(heartRate = 140),
            createRoutePoint(heartRate = 150),
            createRoutePoint(heartRate = 160),
            createRoutePoint(heartRate = null)
        )
        
        val avgHr = RunCalculator.calculateAverageHeartRate(routePoints)
        
        assertEquals(150, avgHr)
    }

    @Test
    fun `calculateAverageHeartRate returns null when no HR data`() {
        val routePoints = listOf(
            createRoutePoint(heartRate = null),
            createRoutePoint(heartRate = null)
        )
        
        val avgHr = RunCalculator.calculateAverageHeartRate(routePoints)
        
        assertNull(avgHr)
    }

    @Test
    fun `calculateMaxHeartRate returns highest value`() {
        val routePoints = listOf(
            createRoutePoint(heartRate = 140),
            createRoutePoint(heartRate = 185),
            createRoutePoint(heartRate = 160)
        )
        
        val maxHr = RunCalculator.calculateMaxHeartRate(routePoints)
        
        assertEquals(185, maxHr)
    }

    @Test
    fun `estimateVO2Max returns reasonable value`() {
        // Cooper test: 12 min, 3000m = good fitness
        val vo2max = RunCalculator.estimateVO2Max(
            distanceMeters = 3000.0,
            durationMinutes = 12.0
        )
        
        // Should be around 50-55 ml/kg/min for this performance
        assertTrue(vo2max in 45.0..60.0)
    }

    private fun createRoutePointsForDistance(totalMeters: Double): List<RoutePoint> {
        val points = mutableListOf<RoutePoint>()
        val numPoints = (totalMeters / 100).toInt() // One point per 100m
        var currentLat = 0.0
        
        for (i in 0..numPoints) {
            points.add(
                RoutePoint(
                    latitude = currentLat,
                    longitude = 0.0,
                    altitude = 0.0,
                    timestamp = i * 30000L, // 30 sec per 100m = 5 min/km
                    heartRate = 150
                )
            )
            currentLat += 0.0009 // ~100m per 0.0009 degrees
        }
        
        return points
    }

    private fun createRoutePoint(heartRate: Int?): RoutePoint {
        return RoutePoint(
            latitude = 0.0,
            longitude = 0.0,
            altitude = 0.0,
            timestamp = System.currentTimeMillis(),
            heartRate = heartRate
        )
    }
}
