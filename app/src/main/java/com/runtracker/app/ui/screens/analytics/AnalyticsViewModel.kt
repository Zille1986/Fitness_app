package com.runtracker.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.model.WeeklyStats
import com.runtracker.shared.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.runtracker.shared.util.TimeUtils
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val runRepository: RunRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            val weeklyStats = runRepository.getWeeklyStatsForPastWeeks(12)
            _uiState.update { it.copy(weeklyStats = weeklyStats) }
        }

        viewModelScope.launch {
            runRepository.getAllRuns().collect { runs ->
                val paceProgression = calculatePaceProgression(runs)
                val distanceProgression = calculateDistanceProgression(runs)
                val personalRecords = calculatePersonalRecords(runs)
                
                _uiState.update { 
                    it.copy(
                        allRuns = runs,
                        paceProgression = paceProgression,
                        distanceProgression = distanceProgression,
                        personalRecords = personalRecords,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun calculatePaceProgression(runs: List<Run>): List<PaceDataPoint> {
        return runs
            .filter { it.avgPaceSecondsPerKm > 0 && it.distanceMeters > 1000 }
            .sortedBy { it.startTime }
            .takeLast(20)
            .map { run ->
                PaceDataPoint(
                    timestamp = run.startTime,
                    paceSecondsPerKm = run.avgPaceSecondsPerKm,
                    distanceKm = run.distanceKm
                )
            }
    }

    private fun calculateDistanceProgression(runs: List<Run>): List<DistanceDataPoint> {
        return runs
            .groupBy { run ->
                TimeUtils.getStartOfWeek(run.startTime)
            }
            .map { (weekStart, weekRuns) ->
                DistanceDataPoint(
                    weekStart = weekStart,
                    totalDistanceKm = weekRuns.sumOf { it.distanceKm },
                    runCount = weekRuns.size
                )
            }
            .sortedBy { it.weekStart }
            .takeLast(12)
    }

    private fun calculatePersonalRecords(runs: List<Run>): PersonalRecordsData {
        val completedRuns = runs.filter { it.isCompleted && it.distanceMeters > 0 }
        
        return PersonalRecordsData(
            fastest1K = findFastestForDistance(completedRuns, 1.0, 0.2),
            fastest5K = findFastestForDistance(completedRuns, 5.0, 0.5),
            fastest10K = findFastestForDistance(completedRuns, 10.0, 1.0),
            fastestHalfMarathon = findFastestForDistance(completedRuns, 21.1, 1.0),
            longestRun = completedRuns.maxByOrNull { it.distanceMeters },
            mostElevation = completedRuns.maxByOrNull { it.elevationGainMeters }
        )
    }

    private fun findFastestForDistance(runs: List<Run>, targetKm: Double, toleranceKm: Double): Run? {
        return runs
            .filter { 
                val distKm = it.distanceKm
                distKm >= targetKm - toleranceKm && distKm <= targetKm + toleranceKm
            }
            .minByOrNull { it.avgPaceSecondsPerKm }
    }
}

data class AnalyticsUiState(
    val allRuns: List<Run> = emptyList(),
    val weeklyStats: List<WeeklyStats> = emptyList(),
    val paceProgression: List<PaceDataPoint> = emptyList(),
    val distanceProgression: List<DistanceDataPoint> = emptyList(),
    val personalRecords: PersonalRecordsData = PersonalRecordsData(),
    val isLoading: Boolean = true
)

data class PaceDataPoint(
    val timestamp: Long,
    val paceSecondsPerKm: Double,
    val distanceKm: Double
)

data class DistanceDataPoint(
    val weekStart: Long,
    val totalDistanceKm: Double,
    val runCount: Int
)

data class PersonalRecordsData(
    val fastest1K: Run? = null,
    val fastest5K: Run? = null,
    val fastest10K: Run? = null,
    val fastestHalfMarathon: Run? = null,
    val longestRun: Run? = null,
    val mostElevation: Run? = null
)
