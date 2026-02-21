package com.runtracker.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.RunRepository
import com.runtracker.shared.data.repository.TrainingPlanRepository
import com.runtracker.shared.data.repository.UserRepository
import com.runtracker.shared.training.TrainingPlanGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.runtracker.shared.util.TimeUtils
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val runRepository: RunRepository,
    private val trainingPlanRepository: TrainingPlanRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            userRepository.ensureProfileExists()
        }

        viewModelScope.launch {
            runRepository.getRecentRuns(5).collect { runs ->
                _uiState.update { it.copy(recentRuns = runs) }
            }
        }

        viewModelScope.launch {
            runRepository.getThisWeekRuns().collect { runs ->
                val totalDistance = runs.sumOf { it.distanceMeters }
                val totalDuration = runs.sumOf { it.durationMillis }
                val totalRuns = runs.size

                _uiState.update {
                    it.copy(
                        weeklyDistanceMeters = totalDistance,
                        weeklyDurationMillis = totalDuration,
                        weeklyRunCount = totalRuns
                    )
                }
            }
        }

        viewModelScope.launch {
            trainingPlanRepository.getActivePlan().collect { plan ->
                _uiState.update { it.copy(activePlan = plan) }
                
                if (plan != null) {
                    val nextWorkout = getNextScheduledWorkout(plan)
                    _uiState.update { it.copy(nextWorkout = nextWorkout) }
                }
            }
        }

        viewModelScope.launch {
            userRepository.getProfile().collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
    }

    private fun getNextScheduledWorkout(plan: TrainingPlan): ScheduledWorkout? {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val currentWeek = calculateCurrentWeek(plan.startDate)
        
        return plan.weeklySchedule
            .filter { it.weekNumber == currentWeek && !it.isCompleted }
            .minByOrNull { 
                val diff = it.dayOfWeek - today
                if (diff < 0) diff + 7 else diff
            }
    }

    private fun calculateCurrentWeek(planStartDate: Long): Int {
        return TimeUtils.calculateWeekNumber(planStartDate, System.currentTimeMillis())
    }

    fun getSuggestedWorkout(): ScheduledWorkout? {
        val state = _uiState.value
        return TrainingPlanGenerator.suggestNextWorkout(
            recentRuns = state.recentRuns,
            activePlan = state.activePlan,
            userProfile = state.userProfile ?: UserProfile()
        )
    }
}

data class DashboardUiState(
    val recentRuns: List<Run> = emptyList(),
    val weeklyDistanceMeters: Double = 0.0,
    val weeklyDurationMillis: Long = 0,
    val weeklyRunCount: Int = 0,
    val activePlan: TrainingPlan? = null,
    val nextWorkout: ScheduledWorkout? = null,
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false
) {
    val weeklyDistanceKm: Double get() = weeklyDistanceMeters / 1000.0
    
    val weeklyDurationFormatted: String get() {
        val totalMinutes = weeklyDurationMillis / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }
    
    val weeklyGoalProgress: Float get() {
        val goal = userProfile?.weeklyGoalKm ?: 20.0
        return (weeklyDistanceKm / goal).coerceIn(0.0, 1.0).toFloat()
    }
}
