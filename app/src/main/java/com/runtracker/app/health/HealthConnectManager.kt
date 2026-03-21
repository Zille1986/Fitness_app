package com.runtracker.app.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _connectionState = MutableStateFlow<HealthConnectState>(HealthConnectState.NotChecked)
    val connectionState: StateFlow<HealthConnectState> = _connectionState.asStateFlow()

    private var healthConnectClient: HealthConnectClient? = null

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    suspend fun checkAvailability() {
        val status = HealthConnectClient.getSdkStatus(context)
        _connectionState.value = when (status) {
            HealthConnectClient.SDK_AVAILABLE -> {
                healthConnectClient = HealthConnectClient.getOrCreate(context)
                HealthConnectState.Available
            }
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectState.NotSupported
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectState.NotInstalled
            else -> HealthConnectState.NotSupported
        }
    }

    fun getHealthConnectSettingsIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return permissions.all { it in granted }
    }

    fun createPermissionRequestContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun getTodaySteps(): Int {
        val client = healthConnectClient ?: return 0
        
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            
            // Deduplicate by taking only one record per data source to avoid double counting
            // Group by metadata.dataOrigin and take the records from the primary source
            val recordsByOrigin = response.records.groupBy { it.metadata.dataOrigin.packageName }
            
            // If multiple sources, prefer the one with the most reasonable total
            // (some apps may report cumulative vs incremental)
            if (recordsByOrigin.size > 1) {
                // Take the source with the highest single-record count (likely the primary tracker)
                // or the source that looks most like a step counter app
                val primarySource = recordsByOrigin.entries
                    .maxByOrNull { entry -> entry.value.maxOfOrNull { it.count } ?: 0 }
                primarySource?.value?.sumOf { it.count.toInt() } ?: 0
            } else {
                response.records.sumOf { it.count.toInt() }
            }
        } catch (e: Exception) {
            android.util.Log.e("HealthConnectManager", "Error getting steps", e)
            0
        }
    }

    suspend fun getStepsForDate(date: LocalDate): Int {
        val client = healthConnectClient ?: return 0
        
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )
            response.records.sumOf { it.count.toInt() }
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getWeeklySteps(): Map<LocalDate, Int> {
        val client = healthConnectClient ?: return emptyMap()
        
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)
        val startOfWeek = weekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfWeek, now)
                )
            )
            
            response.records.groupBy { record ->
                record.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
            }.mapValues { (_, records) ->
                records.sumOf { it.count.toInt() }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Reads last night's sleep data from Health Connect.
     * Returns total sleep duration in hours, or null if no data.
     */
    suspend fun getLastNightSleep(): SleepData? {
        val client = healthConnectClient ?: return null

        // Look for sleep sessions ending today (last night's sleep)
        val today = LocalDate.now()
        val yesterdayEvening = today.minusDays(1).atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(yesterdayEvening, now)
                )
            )

            if (response.records.isEmpty()) return null

            // Sum total sleep across all sessions (some watches split sleep into segments)
            var totalSleepMillis = 0L
            var deepSleepMillis = 0L
            var remSleepMillis = 0L
            var lightSleepMillis = 0L

            for (session in response.records) {
                val sessionDuration = java.time.Duration.between(session.startTime, session.endTime).toMillis()
                totalSleepMillis += sessionDuration

                for (stage in session.stages) {
                    val stageDuration = java.time.Duration.between(stage.startTime, stage.endTime).toMillis()
                    when (stage.stage) {
                        SleepSessionRecord.STAGE_TYPE_DEEP -> deepSleepMillis += stageDuration
                        SleepSessionRecord.STAGE_TYPE_REM -> remSleepMillis += stageDuration
                        SleepSessionRecord.STAGE_TYPE_LIGHT -> lightSleepMillis += stageDuration
                        else -> {} // AWAKE, OUT_OF_BED, etc.
                    }
                }
            }

            SleepData(
                totalHours = totalSleepMillis / 3600000.0,
                deepHours = deepSleepMillis / 3600000.0,
                remHours = remSleepMillis / 3600000.0,
                lightHours = lightSleepMillis / 3600000.0
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTodayExerciseSessions(): List<ExerciseSessionData> {
        val client = healthConnectClient ?: return emptyList()
        
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            
            response.records.map { session ->
                ExerciseSessionData(
                    startTime = session.startTime,
                    endTime = session.endTime,
                    exerciseType = session.exerciseType,
                    title = session.title ?: "Workout"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getHeartRateForTimeRange(start: Instant, end: Instant): List<Int> {
        val client = healthConnectClient ?: return emptyList()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            
            response.records.flatMap { record ->
                record.samples.map { it.beatsPerMinute.toInt() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    /**
     * Reads swimming exercise sessions from Health Connect for the last 7 days.
     * Returns distance (meters) and duration for each session.
     */
    suspend fun getRecentSwimSessions(): List<SwimSessionData> {
        val client = healthConnectClient ?: return emptyList()

        val now = Instant.now()
        val weekAgo = now.minusSeconds(7 * 24 * 3600)

        return try {
            val sessions = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(weekAgo, now)
                )
            )

            val swimSessions = sessions.records.filter { session ->
                session.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL ||
                session.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
            }

            swimSessions.map { session ->
                // Read distance for this session's time range
                val distanceRecords = client.readRecords(
                    ReadRecordsRequest(
                        recordType = DistanceRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
                    )
                )
                val totalDistanceMeters = distanceRecords.records.sumOf {
                    it.distance.inMeters
                }

                SwimSessionData(
                    startTime = session.startTime,
                    endTime = session.endTime,
                    distanceMeters = totalDistanceMeters,
                    durationMillis = java.time.Duration.between(session.startTime, session.endTime).toMillis(),
                    isOpenWater = session.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
                    title = session.title ?: "Swim"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class SwimSessionData(
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Double,
    val durationMillis: Long,
    val isOpenWater: Boolean,
    val title: String
)

data class SleepData(
    val totalHours: Double,
    val deepHours: Double = 0.0,
    val remHours: Double = 0.0,
    val lightHours: Double = 0.0
) {
    val quality: String get() = when {
        totalHours >= 7.0 -> "Good"
        totalHours >= 5.5 -> "Moderate"
        else -> "Low"
    }

    val formatted: String get() {
        val h = totalHours.toInt()
        val m = ((totalHours - h) * 60).toInt()
        return "${h}h ${m}m"
    }
}

sealed class HealthConnectState {
    object NotChecked : HealthConnectState()
    object Available : HealthConnectState()
    object NotInstalled : HealthConnectState()
    object NotSupported : HealthConnectState()
}

data class ExerciseSessionData(
    val startTime: Instant,
    val endTime: Instant,
    val exerciseType: Int,
    val title: String
)
