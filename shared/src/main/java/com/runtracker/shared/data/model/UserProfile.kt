package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

@Entity(tableName = "user_profile")
@TypeConverters(Converters::class)
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "",
    val age: Int? = null,
    val weight: Double? = null,
    val height: Double? = null,
    val gender: Gender? = null,
    val restingHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val weeklyGoalKm: Double = 20.0,
    val preferredUnits: Units = Units.METRIC,
    val stravaAccessToken: String? = null,
    val stravaRefreshToken: String? = null,
    val stravaTokenExpiry: Long? = null,
    val stravaAthleteId: String? = null,
    val personalRecords: PersonalRecords = PersonalRecords(),
    val isOnboardingComplete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val estimatedMaxHeartRate: Int get() = maxHeartRate ?: (220 - (age ?: 30))
    
    fun getHeartRateZones(): List<HeartRateZoneRange> {
        val max = estimatedMaxHeartRate
        return listOf(
            HeartRateZoneRange(HeartRateZone.ZONE_1, (max * 0.50).toInt(), (max * 0.60).toInt()),
            HeartRateZoneRange(HeartRateZone.ZONE_2, (max * 0.60).toInt(), (max * 0.70).toInt()),
            HeartRateZoneRange(HeartRateZone.ZONE_3, (max * 0.70).toInt(), (max * 0.80).toInt()),
            HeartRateZoneRange(HeartRateZone.ZONE_4, (max * 0.80).toInt(), (max * 0.90).toInt()),
            HeartRateZoneRange(HeartRateZone.ZONE_5, (max * 0.90).toInt(), max)
        )
    }
}

enum class Gender {
    MALE,
    FEMALE,
    OTHER
}

enum class Units {
    METRIC,
    IMPERIAL
}

data class HeartRateZoneRange(
    val zone: HeartRateZone,
    val minBpm: Int,
    val maxBpm: Int
)

data class PersonalRecords(
    val fastest1K: Long? = null,
    val fastest5K: Long? = null,
    val fastest10K: Long? = null,
    val fastestHalfMarathon: Long? = null,
    val fastestMarathon: Long? = null,
    val longestRun: Double? = null,
    val highestElevationGain: Double? = null
)
