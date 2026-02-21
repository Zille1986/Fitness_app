package com.runtracker.app.ui.screens.running

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.repository.RunRepository
import com.runtracker.shared.data.repository.TrainingPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.runtracker.shared.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class RunningDashboardViewModel @Inject constructor(
    private val runRepository: RunRepository,
    private val trainingPlanRepository: TrainingPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunningDashboardUiState())
    val uiState: StateFlow<RunningDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        loadNextRun()
        loadRecentRuns()
        loadWeeklyStats()
    }

    private fun loadNextRun() {
        viewModelScope.launch {
            val nextWorkout = trainingPlanRepository.getNextUpcomingWorkout()
            nextWorkout?.let { (workout, daysAhead) ->
                val scheduledDateText = when (daysAhead) {
                    0 -> "Today"
                    1 -> "Tomorrow"
                    else -> "In $daysAhead days"
                }
                
                _uiState.update {
                    it.copy(
                        nextScheduledRun = ScheduledRunInfo(
                            name = workout.workoutType.name.replace("_", " ").lowercase()
                                .replaceFirstChar { c -> c.uppercase() },
                            description = workout.description,
                            scheduledDate = scheduledDateText,
                            estimatedDuration = workout.targetDurationMinutes?.let { min -> "$min min" } ?: ""
                        )
                    )
                }
            }
        }
    }

    private fun loadRecentRuns() {
        viewModelScope.launch {
            runRepository.getAllRuns().collect { runs ->
                val recentRuns = runs
                    .sortedByDescending { it.startTime }
                    .take(5)
                    .map { run ->
                        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                        val distanceKm = run.distanceMeters / 1000.0
                        val paceMinutes = if (run.distanceMeters > 0) {
                            (run.durationMillis / 1000.0 / 60.0) / (run.distanceMeters / 1000.0)
                        } else 0.0
                        val paceSeconds = ((paceMinutes % 1) * 60).toInt()
                        
                        RecentRunInfo(
                            id = run.id,
                            title = run.notes ?: "Run",
                            date = dateFormat.format(Date(run.startTime)),
                            distance = String.format("%.1f", distanceKm),
                            pace = String.format("%d:%02d", paceMinutes.toInt(), paceSeconds)
                        )
                    }
                
                _uiState.update { it.copy(recentRuns = recentRuns) }
            }
        }
    }

    private fun loadWeeklyStats() {
        viewModelScope.launch {
            val weekStart = TimeUtils.getStartOfWeek()
            val weekEnd = weekStart + TimeUtils.ONE_WEEK_MS

            runRepository.getAllRuns().collect { allRuns ->
                val weeklyRuns = allRuns.filter {
                    it.isCompleted && it.startTime >= weekStart && it.startTime < weekEnd
                }

                // Calculate daily distances for the week (Monday = index 0, Sunday = index 6)
                val dailyDistances = MutableList(7) { 0f }
                weeklyRuns.forEach { run ->
                    val runCalendar = Calendar.getInstance().apply { timeInMillis = run.startTime }
                    val runDayOfWeek = runCalendar.get(Calendar.DAY_OF_WEEK)
                    // Convert to Monday-based index (Monday=0, Tuesday=1, ..., Sunday=6)
                    val dayIndex = if (runDayOfWeek == Calendar.SUNDAY) 6 else runDayOfWeek - Calendar.MONDAY
                    dailyDistances[dayIndex] += (run.distanceMeters / 1000.0).toFloat()
                }

                val totalDistance = dailyDistances.sum()
                
                // Calculate average pace
                val totalDuration = weeklyRuns.sumOf { it.durationMillis }
                val totalDistanceMeters = weeklyRuns.sumOf { it.distanceMeters }
                val avgPace = if (totalDistanceMeters > 0) {
                    val paceMinutes = (totalDuration / 1000.0 / 60.0) / (totalDistanceMeters / 1000.0)
                    val paceSeconds = ((paceMinutes % 1) * 60).toInt()
                    String.format("%d:%02d", paceMinutes.toInt(), paceSeconds)
                } else {
                    "--:--"
                }

                _uiState.update {
                    it.copy(
                        weeklyDistances = dailyDistances,
                        totalWeeklyDistance = totalDistance,
                        averagePace = avgPace
                    )
                }
            }
        }
    }

    fun refresh() {
        loadData()
    }
}

data class RunningDashboardUiState(
    val nextScheduledRun: ScheduledRunInfo? = null,
    val recentRuns: List<RecentRunInfo> = emptyList(),
    val weeklyDistances: List<Float> = List(7) { 0f },
    val totalWeeklyDistance: Float = 0f,
    val averagePace: String = "--:--"
)
