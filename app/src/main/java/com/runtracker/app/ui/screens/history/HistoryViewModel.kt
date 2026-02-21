package com.runtracker.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val runRepository: RunRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadRuns()
    }

    private fun loadRuns() {
        viewModelScope.launch {
            runRepository.getAllRuns().collect { runs ->
                val groupedRuns = groupRunsByMonth(runs)
                _uiState.update { 
                    it.copy(
                        runs = runs,
                        groupedRuns = groupedRuns,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun groupRunsByMonth(runs: List<Run>): Map<String, List<Run>> {
        val calendar = Calendar.getInstance()
        val monthFormat = java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        
        return runs.groupBy { run ->
            calendar.timeInMillis = run.startTime
            monthFormat.format(calendar.time)
        }
    }

    fun deleteRun(run: Run) {
        viewModelScope.launch {
            runRepository.deleteRun(run)
        }
    }

    fun setFilter(filter: HistoryFilter) {
        _uiState.update { it.copy(filter = filter) }
    }
}

data class HistoryUiState(
    val runs: List<Run> = emptyList(),
    val groupedRuns: Map<String, List<Run>> = emptyMap(),
    val filter: HistoryFilter = HistoryFilter.ALL,
    val isLoading: Boolean = true
)

enum class HistoryFilter {
    ALL,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR
}
