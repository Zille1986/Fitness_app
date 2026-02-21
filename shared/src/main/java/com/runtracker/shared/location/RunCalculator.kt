package com.runtracker.shared.location

import com.runtracker.shared.data.model.RoutePoint
import com.runtracker.shared.data.model.Split
import com.runtracker.shared.location.LocationTracker.Companion.calculateDistance

object RunCalculator {

    fun calculateSplits(points: List<RoutePoint>): List<Split> {
        if (points.size < 2) return emptyList()
        
        val splits = mutableListOf<Split>()
        var currentKm = 1
        var splitStartIndex = 0
        var accumulatedDistance = 0.0
        
        for (i in 1 until points.size) {
            val segmentDistance = calculateDistance(listOf(points[i - 1], points[i]))
            accumulatedDistance += segmentDistance
            
            while (accumulatedDistance >= currentKm * 1000) {
                val splitPoints = points.subList(splitStartIndex, i + 1)
                val splitDuration = points[i].timestamp - points[splitStartIndex].timestamp
                val splitDistance = 1000.0
                val pace = if (splitDistance > 0) splitDuration / splitDistance else 0.0
                
                val elevationChange = splitPoints.mapNotNull { it.altitude }
                    .let { altitudes ->
                        if (altitudes.size >= 2) altitudes.last() - altitudes.first()
                        else 0.0
                    }
                
                val avgHeartRate = splitPoints.mapNotNull { it.heartRate }
                    .takeIf { it.isNotEmpty() }
                    ?.average()
                    ?.toInt()
                
                splits.add(
                    Split(
                        kilometer = currentKm,
                        durationMillis = splitDuration,
                        paceSecondsPerKm = pace,
                        elevationChange = elevationChange,
                        avgHeartRate = avgHeartRate
                    )
                )
                
                splitStartIndex = i
                currentKm++
            }
        }
        
        return splits
    }

    fun calculatePace(distanceMeters: Double, durationMillis: Long): Double {
        if (distanceMeters <= 0) return 0.0
        val distanceKm = distanceMeters / 1000.0
        val durationSeconds = durationMillis / 1000.0
        return durationSeconds / distanceKm
    }

    fun calculateCalories(
        distanceMeters: Double,
        durationMillis: Long,
        weightKg: Double?,
        avgHeartRate: Int?
    ): Int {
        val weight = weightKg ?: 70.0
        val distanceKm = distanceMeters / 1000.0
        
        return if (avgHeartRate != null && avgHeartRate > 0) {
            val durationMinutes = durationMillis / 60000.0
            ((avgHeartRate * 0.6309 + weight * 0.1988 + 
              avgHeartRate * 0.2017 - 55.0969) * durationMinutes / 4.184).toInt()
        } else {
            (distanceKm * weight * 1.036).toInt()
        }
    }

    fun calculateAverageHeartRate(points: List<RoutePoint>): Int? {
        val heartRates = points.mapNotNull { it.heartRate }
        return if (heartRates.isNotEmpty()) heartRates.average().toInt() else null
    }

    fun calculateMaxHeartRate(points: List<RoutePoint>): Int? {
        return points.mapNotNull { it.heartRate }.maxOrNull()
    }

    fun calculateAverageCadence(points: List<RoutePoint>): Int? {
        val cadences = points.mapNotNull { it.cadence }
        return if (cadences.isNotEmpty()) cadences.average().toInt() else null
    }

    fun estimateVO2Max(
        distanceMeters: Double,
        durationMillis: Long,
        avgHeartRate: Int?,
        maxHeartRate: Int
    ): Double? {
        if (avgHeartRate == null || avgHeartRate <= 0) return null
        if (distanceMeters < 1000 || durationMillis < 60000) return null
        
        val speedKmH = (distanceMeters / 1000.0) / (durationMillis / 3600000.0)
        val percentHRMax = avgHeartRate.toDouble() / maxHeartRate
        
        val vo2 = speedKmH * 3.5
        return vo2 / percentHRMax
    }

    fun estimateVO2Max(
        distanceMeters: Double,
        durationMinutes: Double
    ): Double {
        if (distanceMeters <= 0 || durationMinutes <= 0) return 0.0
        // Cooper formula approximation
        return (distanceMeters - 504.9) / 44.73
    }

    fun getRunningZone(currentHeartRate: Int, maxHeartRate: Int): Int {
        val percentage = currentHeartRate.toDouble() / maxHeartRate
        return when {
            percentage < 0.6 -> 1
            percentage < 0.7 -> 2
            percentage < 0.8 -> 3
            percentage < 0.9 -> 4
            else -> 5
        }
    }
}
