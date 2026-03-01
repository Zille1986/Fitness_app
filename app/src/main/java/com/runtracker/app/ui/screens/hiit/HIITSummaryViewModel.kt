package com.runtracker.app.ui.screens.hiit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.HIITSession
import com.runtracker.shared.data.repository.HIITRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HIITSummaryUiState(
    val session: HIITSession? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HIITSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val hiitRepository: HIITRepository
) : ViewModel() {

    private val sessionId: Long = savedStateHandle["sessionId"] ?: 0L

    private val _uiState = MutableStateFlow(HIITSummaryUiState())
    val uiState: StateFlow<HIITSummaryUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            val session = hiitRepository.getSessionById(sessionId)
            _uiState.update {
                it.copy(session = session, isLoading = false)
            }
        }
    }
}
