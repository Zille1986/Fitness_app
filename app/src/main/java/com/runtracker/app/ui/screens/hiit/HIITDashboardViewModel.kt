package com.runtracker.app.ui.screens.hiit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.HIITExerciseLibrary
import com.runtracker.shared.data.model.HIITSession
import com.runtracker.shared.data.model.HIITWorkoutTemplate
import com.runtracker.shared.data.repository.HIITRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HIITDashboardUiState(
    val templates: List<HIITWorkoutTemplate> = HIITExerciseLibrary.allTemplates,
    val recentSessions: List<HIITSession> = emptyList(),
    val weeklySessionCount: Int = 0,
    val weeklyCalories: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class HIITDashboardViewModel @Inject constructor(
    private val hiitRepository: HIITRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HIITDashboardUiState())
    val uiState: StateFlow<HIITDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            hiitRepository.getRecentCompletedSessions(5).collect { sessions ->
                val stats = hiitRepository.getWeeklyStats()
                _uiState.update {
                    it.copy(
                        recentSessions = sessions,
                        weeklySessionCount = stats.sessionCount,
                        weeklyCalories = stats.totalCalories,
                        isLoading = false
                    )
                }
            }
        }
    }
}
