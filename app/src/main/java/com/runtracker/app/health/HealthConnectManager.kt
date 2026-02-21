package com.runtracker.app.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
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
