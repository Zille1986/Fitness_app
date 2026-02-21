package com.runtracker.app.ui.screens.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.ai.TrainingPlanAI
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.RunRepository
import com.runtracker.shared.data.repository.TrainingPlanRepository
import com.runtracker.shared.data.repository.UserRepository
import com.runtracker.shared.training.TrainingPlanGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainingPlansViewModel @Inject constructor(
    private val trainingPlanRepository: TrainingPlanRepository,
    private val runRepository: RunRepository,
    private val userRepository: UserRepository,
    private val trainingPlanAI: TrainingPlanAI
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingPlansUiState())
    val uiState: StateFlow<TrainingPlansUiState> = _uiState.asStateFlow()

    init {
        loadPlans()
    }

    private fun loadPlans() {
        viewModelScope.launch {
            trainingPlanRepository.getAllPlans().collect { plans ->
                _uiState.update { it.copy(plans = plans, isLoading = false) }
            }
        }

        viewModelScope.launch {
            trainingPlanRepository.getActivePlan().collect { plan ->
                _uiState.update { it.copy(activePlan = plan) }
            }
        }
    }

    fun createPlan(goalType: GoalType, weeksAvailable: Int = 8, daysPerWeek: Int = 4) {
        viewModelScope.launch {
            val weeklyStats = runRepository.getWeeklyStatsForPastWeeks(4)
            val avgWeeklyKm = weeklyStats.map { it.totalDistanceKm }.average()
            
            val fitnessLevel = determineFitnessLevel(avgWeeklyKm)

            val plan = TrainingPlanGenerator.generatePlan(
                goalType = goalType,
                currentFitnessLevel = fitnessLevel,
                weeklyAvailableDays = daysPerWeek,
                currentWeeklyDistanceKm = avgWeeklyKm
            )

            trainingPlanRepository.deactivateAllPlans()
            trainingPlanRepository.insertPlan(plan)
        }
    }
    
    fun createAdvancedPlan(
        goalType: GoalType,
        selectedDays: List<Int>,
        currentWeeklyKm: Int,
        currentLongRunKm: Int
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            
            // Get user's running history for personalized zones
            val previousRuns = runRepository.getAllRunsOnce()
            val userProfile = userRepository.getProfile().first()
            val userAge = userProfile?.age
            
            // Try AI-generated plan first
            val aiPlan = trainingPlanAI.generateTrainingPlan(
                goalType = goalType,
                selectedDays = selectedDays,
                currentWeeklyKm = currentWeeklyKm.toDouble(),
                currentLongRunKm = currentLongRunKm.toDouble()
            )
            
            val plan = if (aiPlan != null) {
                aiPlan
            } else {
                // Fallback to rule-based generator with personalized zones
                val fitnessLevel = determineFitnessLevel(currentWeeklyKm.toDouble())
                TrainingPlanGenerator.generateAdvancedPlan(
                    goalType = goalType,
                    currentFitnessLevel = fitnessLevel,
                    selectedDays = selectedDays,
                    currentWeeklyDistanceKm = currentWeeklyKm.toDouble(),
                    currentLongRunKm = currentLongRunKm.toDouble(),
                    previousRuns = previousRuns,
                    userAge = userAge
                )
            }

            trainingPlanRepository.deactivateAllPlans()
            trainingPlanRepository.insertPlan(plan)
            _uiState.update { it.copy(isGenerating = false) }
        }
    }
    
    private fun determineFitnessLevel(avgWeeklyKm: Double): FitnessLevel {
        return when {
            avgWeeklyKm < 10 -> FitnessLevel.BEGINNER
            avgWeeklyKm < 25 -> FitnessLevel.NOVICE
            avgWeeklyKm < 40 -> FitnessLevel.INTERMEDIATE
            avgWeeklyKm < 60 -> FitnessLevel.ADVANCED
            else -> FitnessLevel.ELITE
        }
    }

    fun setActivePlan(plan: TrainingPlan) {
        viewModelScope.launch {
            trainingPlanRepository.setActivePlan(plan)
        }
    }

    fun deletePlan(plan: TrainingPlan) {
        viewModelScope.launch {
            trainingPlanRepository.deletePlan(plan)
        }
    }
}

data class TrainingPlansUiState(
    val plans: List<TrainingPlan> = emptyList(),
    val activePlan: TrainingPlan? = null,
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false
)
