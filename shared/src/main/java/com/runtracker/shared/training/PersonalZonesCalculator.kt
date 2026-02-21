package com.runtracker.shared.training

import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.model.HeartRateZone
import kotlin.math.roundToInt

data class PersonalPaceZones(
    val recoveryPaceMin: Double,  // seconds per km
    val recoveryPaceMax: Double,
    val easyPaceMin: Double,
    val easyPaceMax: Double,
    val tempoPaceMin: Double,
    val tempoPaceMax: Double,
    val thresholdPaceMin: Double,
    val thresholdPaceMax: Double,
    val intervalPaceMin: Double,
    val intervalPaceMax: Double,
    val sprintPaceMin: Double,
    val sprintPaceMax: Double
)

data class PersonalHeartRateZones(
    val maxHr: Int,
    val restingHr: Int,
    val zone1Min: Int,  // Recovery 50-60%
    val zone1Max: Int,
    val zone2Min: Int,  // Aerobic 60-70%
    val zone2Max: Int,
    val zone3Min: Int,  // Tempo 70-80%
    val zone3Max: Int,
    val zone4Min: Int,  // Threshold 80-90%
    val zone4Max: Int,
    val zone5Min: Int,  // VO2 Max 90-100%
    val zone5Max: Int
) {
    fun getZoneRange(zone: HeartRateZone): Pair<Int, Int> {
        return when (zone) {
            HeartRateZone.ZONE_1 -> Pair(zone1Min, zone1Max)
            HeartRateZone.ZONE_2 -> Pair(zone2Min, zone2Max)
            HeartRateZone.ZONE_3 -> Pair(zone3Min, zone3Max)
            HeartRateZone.ZONE_4 -> Pair(zone4Min, zone4Max)
            HeartRateZone.ZONE_5 -> Pair(zone5Min, zone5Max)
        }
    }
}

object PersonalZonesCalculator {
    
    fun calculatePaceZones(runs: List<Run>): PersonalPaceZones {
        if (runs.isEmpty()) {
            return getDefaultPaceZones()
        }
        
        // Filter to completed runs with valid pace data
        val validRuns = runs.filter { 
            it.isCompleted && 
            it.avgPaceSecondsPerKm > 0 && 
            it.avgPaceSecondsPerKm < 1200 && // Less than 20 min/km
            it.distanceMeters > 500 // At least 500m
        }
        
        if (validRuns.isEmpty()) {
            return getDefaultPaceZones()
        }
        
        // Calculate different pace metrics
        val allPaces = validRuns.map { it.avgPaceSecondsPerKm }
        val avgPace = allPaces.average()
        
        // Find fastest pace from runs > 3km (likely tempo/race efforts)
        val longerRuns = validRuns.filter { it.distanceMeters > 3000 }
        val fastestPace = if (longerRuns.isNotEmpty()) {
            longerRuns.minOf { it.avgPaceSecondsPerKm }
        } else {
            allPaces.minOrNull() ?: avgPace
        }
        
        // Find easy pace from longer runs > 5km
        val longRuns = validRuns.filter { it.distanceMeters > 5000 }
        val easyPace = if (longRuns.isNotEmpty()) {
            longRuns.map { it.avgPaceSecondsPerKm }.average()
        } else {
            avgPace
        }
        
        // Calculate zones based on the user's actual performance
        // Using Jack Daniels' VDOT-like approach
        val thresholdPace = fastestPace * 1.05  // ~5% slower than fastest
        val tempoPace = fastestPace * 1.10      // ~10% slower
        val easyPaceCalc = fastestPace * 1.25   // ~25% slower
        val recoveryPace = fastestPace * 1.40   // ~40% slower
        val intervalPace = fastestPace * 0.95   // ~5% faster than threshold
        val sprintPace = fastestPace * 0.85     // ~15% faster
        
        return PersonalPaceZones(
            recoveryPaceMin = recoveryPace,
            recoveryPaceMax = recoveryPace * 1.15,
            easyPaceMin = easyPaceCalc * 0.95,
            easyPaceMax = easyPaceCalc * 1.10,
            tempoPaceMin = tempoPace * 0.95,
            tempoPaceMax = tempoPace * 1.05,
            thresholdPaceMin = thresholdPace * 0.97,
            thresholdPaceMax = thresholdPace * 1.03,
            intervalPaceMin = intervalPace * 0.95,
            intervalPaceMax = intervalPace * 1.05,
            sprintPaceMin = sprintPace * 0.90,
            sprintPaceMax = sprintPace * 1.05
        )
    }
    
    fun calculateHeartRateZones(
        runs: List<Run>,
        age: Int?,
        restingHr: Int? = null
    ): PersonalHeartRateZones {
        // Try to find max HR from actual run data
        val measuredMaxHr = runs
            .filter { it.maxHeartRate != null && it.maxHeartRate!! > 100 }
            .maxOfOrNull { it.maxHeartRate!! }
        
        // Estimate max HR if not measured (220 - age formula, or Tanaka formula)
        val estimatedMaxHr = age?.let { 
            // Tanaka formula: 208 - (0.7 × age) - more accurate for active people
            (208 - (0.7 * it)).roundToInt()
        } ?: 185  // Default if no age
        
        val maxHr = measuredMaxHr ?: estimatedMaxHr
        val restHr = restingHr ?: 60  // Default resting HR
        
        // Calculate zones using Heart Rate Reserve (Karvonen) method
        // Zone % is applied to (maxHr - restHr) + restHr
        val hrReserve = maxHr - restHr
        
        return PersonalHeartRateZones(
            maxHr = maxHr,
            restingHr = restHr,
            zone1Min = (restHr + hrReserve * 0.50).roundToInt(),
            zone1Max = (restHr + hrReserve * 0.60).roundToInt(),
            zone2Min = (restHr + hrReserve * 0.60).roundToInt(),
            zone2Max = (restHr + hrReserve * 0.70).roundToInt(),
            zone3Min = (restHr + hrReserve * 0.70).roundToInt(),
            zone3Max = (restHr + hrReserve * 0.80).roundToInt(),
            zone4Min = (restHr + hrReserve * 0.80).roundToInt(),
            zone4Max = (restHr + hrReserve * 0.90).roundToInt(),
            zone5Min = (restHr + hrReserve * 0.90).roundToInt(),
            zone5Max = maxHr
        )
    }
    
    private fun getDefaultPaceZones(): PersonalPaceZones {
        // Default zones for a ~5:30/km easy pace runner
        return PersonalPaceZones(
            recoveryPaceMin = 390.0,  // 6:30/km
            recoveryPaceMax = 450.0,  // 7:30/km
            easyPaceMin = 330.0,      // 5:30/km
            easyPaceMax = 390.0,      // 6:30/km
            tempoPaceMin = 280.0,     // 4:40/km
            tempoPaceMax = 310.0,     // 5:10/km
            thresholdPaceMin = 260.0, // 4:20/km
            thresholdPaceMax = 285.0, // 4:45/km
            intervalPaceMin = 230.0,  // 3:50/km
            intervalPaceMax = 260.0,  // 4:20/km
            sprintPaceMin = 200.0,    // 3:20/km
            sprintPaceMax = 230.0     // 3:50/km
        )
    }
}
