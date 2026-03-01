package com.runtracker.app.ui.screens.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.GymRepository
import com.runtracker.shared.data.repository.GymWeeklyStats
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GymDashboardViewModel"

@HiltViewModel
class GymDashboardViewModel @Inject constructor(
    private val gymRepository: GymRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GymDashboardUiState())
    val uiState: StateFlow<GymDashboardUiState> = _uiState.asStateFlow()

    init {
        initializeData()
        loadDashboardData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            gymRepository.initializeDefaultExercises()
            gymRepository.initializeDefaultTemplates()
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            gymRepository.getRecentWorkouts(5)
                .catch { e -> Log.e(TAG, "Error loading recent workouts", e) }
                .collect { workouts ->
                    _uiState.update { it.copy(recentWorkouts = workouts) }
                }
        }

        viewModelScope.launch {
            gymRepository.getAllTemplates()
                .catch { e -> Log.e(TAG, "Error loading templates", e) }
                .collect { templates ->
                    _uiState.update { it.copy(templates = templates) }
                }
        }

        viewModelScope.launch {
            val weeklyStats = gymRepository.getWeeklyGymStats()
            val muscleVolume = gymRepository.getWeeklyMuscleGroupVolume()
            _uiState.update { it.copy(
                weeklyStats = weeklyStats,
                muscleGroupVolume = muscleVolume,
                isLoading = false
            ) }
        }

        viewModelScope.launch {
            val activeWorkout = gymRepository.getActiveWorkout()
            _uiState.update { it.copy(activeWorkout = activeWorkout) }
        }
    }

    fun startWorkoutFromTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            val exercises = template.exercises.map { templateExercise ->
                WorkoutExercise(
                    id = java.util.UUID.randomUUID().toString(),
                    exerciseId = templateExercise.exerciseId,
                    exerciseName = templateExercise.exerciseName,
                    sets = (1..templateExercise.sets).map { setNum ->
                        WorkoutSet(
                            id = java.util.UUID.randomUUID().toString(),
                            setNumber = setNum,
                            targetReps = templateExercise.targetReps.first
                        )
                    },
                    restSeconds = templateExercise.restSeconds,
                    orderIndex = templateExercise.orderIndex
                )
            }

            val workout = GymWorkout(
                name = template.name,
                startTime = System.currentTimeMillis(),
                exercises = exercises,
                templateId = template.id
            )

            val workoutId = gymRepository.insertWorkout(workout)
            gymRepository.incrementTemplateUsage(template.id)
            
            _uiState.update { it.copy(activeWorkout = workout.copy(id = workoutId)) }
        }
    }

    fun startEmptyWorkout() {
        viewModelScope.launch {
            val workout = GymWorkout(
                name = "Quick Workout",
                startTime = System.currentTimeMillis()
            )
            val workoutId = gymRepository.insertWorkout(workout)
            _uiState.update { it.copy(activeWorkout = workout.copy(id = workoutId)) }
        }
    }
    
    fun clearActiveWorkout() {
        _uiState.update { it.copy(activeWorkout = null) }
    }
}

data class GymDashboardUiState(
    val recentWorkouts: List<GymWorkout> = emptyList(),
    val templates: List<WorkoutTemplate> = emptyList(),
    val weeklyStats: GymWeeklyStats? = null,
    val muscleGroupVolume: Map<MuscleGroup, Int> = emptyMap(),
    val activeWorkout: GymWorkout? = null,
    val isLoading: Boolean = true
)
