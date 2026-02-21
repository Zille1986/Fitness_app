package com.runtracker.app.ui.screens.rundetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.strava.StravaService
import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunDetailViewModel @Inject constructor(
    private val runRepository: RunRepository,
    private val stravaService: StravaService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val runId: Long = savedStateHandle.get<Long>("runId") ?: 0L

    private val _uiState = MutableStateFlow(RunDetailUiState())
    val uiState: StateFlow<RunDetailUiState> = _uiState.asStateFlow()

    init {
        loadRun()
    }

    private fun loadRun() {
        viewModelScope.launch {
            runRepository.getRunById(runId).collect { run ->
                _uiState.update { 
                    it.copy(
                        run = run,
                        isLoading = false,
                        isStravaConnected = stravaService.isConnected
                    )
                }
            }
        }
    }

    fun deleteRun() {
        viewModelScope.launch {
            _uiState.value.run?.let { run ->
                runRepository.deleteRun(run)
            }
        }
    }
    
    fun uploadToStrava() {
        val run = _uiState.value.run ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingToStrava = true, stravaError = null) }
            
            val result = stravaService.uploadRun(run)
            result.fold(
                onSuccess = { stravaId ->
                    // Update run with Strava ID
                    val updatedRun = run.copy(stravaId = stravaId.toString())
                    runRepository.updateRun(updatedRun)
                    _uiState.update { 
                        it.copy(
                            isUploadingToStrava = false,
                            stravaUploadSuccess = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isUploadingToStrava = false,
                            stravaError = error.message ?: "Upload failed"
                        )
                    }
                }
            )
        }
    }
    
    fun clearStravaError() {
        _uiState.update { it.copy(stravaError = null) }
    }
    
    fun clearStravaSuccess() {
        _uiState.update { it.copy(stravaUploadSuccess = false) }
    }
}

data class RunDetailUiState(
    val run: Run? = null,
    val isLoading: Boolean = true,
    val isStravaConnected: Boolean = false,
    val isUploadingToStrava: Boolean = false,
    val stravaError: String? = null,
    val stravaUploadSuccess: Boolean = false
)
