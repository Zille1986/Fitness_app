package com.runtracker.app.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.ai.AdaptiveTrainingEngine
import com.runtracker.shared.ai.TrainingRecommendation
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.GamificationRepository
import com.runtracker.shared.data.repository.MentalHealthRepository
import com.runtracker.shared.data.repository.RunRepository
import com.runtracker.shared.data.repository.TrainingPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdaptiveTrainingViewModel @Inject constructor(
    private val runRepository: RunRepository,
    private val trainingPlanRepository: TrainingPlanRepository,
    private val gamificationRepository: GamificationRepository,
    private val mentalHealthRepository: MentalHealthRepository
) : ViewModel() {

    private val adaptiveEngine = AdaptiveTrainingEngine()

    private val _uiState = MutableStateFlow(AdaptiveTrainingUiState())
    val uiState: StateFlow<AdaptiveTrainingUiState> = _uiState.asStateFlow()

    private val _suggestedWorkout = MutableStateFlow<ScheduledWorkout?>(null)
    val suggestedWorkout: StateFlow<ScheduledWorkout?> = _suggestedWorkout.asStateFlow()

    init {
        analyzeAndRecommend()
    }

    private fun analyzeAndRecommend() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get today's scheduled workout (if any)
                val scheduledWorkout = trainingPlanRepository.getTodaysWorkout()
                
                // Get wellness check-in
                val wellnessCheckin = mentalHealthRepository.getTodayCheckin()
                
                // Get recent runs (last 14 days)
                val twoWeeksAgo = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000)
                val recentRuns = runRepository.getRunsSinceOnce(twoWeeksAgo)
                
                // Get gamification data
                val gamification = gamificationRepository.getOrCreateUserGamification()
                
                // Run the AI analysis
                val recommendation = adaptiveEngine.analyzeAndRecommend(
                    scheduledWorkout = scheduledWorkout,
                    wellnessCheckin = wellnessCheckin,
                    recentRuns = recentRuns,
                    gamification = gamification
                )
                
                _uiState.update { 
                    it.copy(
                        recommendation = recommendation,
                        scheduledWorkout = scheduledWorkout,
                        isLoading = false
                    ) 
                }
                
                _suggestedWorkout.value = recommendation.suggestedWorkout
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    ) 
                }
            }
        }
    }

    fun refresh() {
        analyzeAndRecommend()
    }

    fun acceptSuggestedWorkout() {
        _suggestedWorkout.value?.let { workout ->
            // Store the suggested workout for the running screen to use
            viewModelScope.launch {
                // Could save to a temporary storage or pass via navigation
            }
        }
    }
}

data class AdaptiveTrainingUiState(
    val recommendation: TrainingRecommendation? = null,
    val scheduledWorkout: ScheduledWorkout? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
