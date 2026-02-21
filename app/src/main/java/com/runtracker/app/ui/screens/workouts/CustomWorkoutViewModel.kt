package com.runtracker.app.ui.screens.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.service.WatchSyncService
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.CustomWorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CustomWorkoutViewModel @Inject constructor(
    private val repository: CustomWorkoutRepository,
    private val watchSyncService: WatchSyncService
) : ViewModel() {

    private val _workouts = MutableStateFlow<List<CustomRunWorkout>>(emptyList())
    val workouts: StateFlow<List<CustomRunWorkout>> = _workouts.asStateFlow()

    private val _selectedCategory = MutableStateFlow<WorkoutCategory?>(null)
    val selectedCategory: StateFlow<WorkoutCategory?> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Workout builder state
    private val _builderState = MutableStateFlow(WorkoutBuilderState())
    val builderState: StateFlow<WorkoutBuilderState> = _builderState.asStateFlow()

    init {
        loadWorkouts()
        seedDefaultWorkoutsIfNeeded()
    }

    private fun loadWorkouts() {
        viewModelScope.launch {
            repository.getAllWorkoutsFlow().collect { workoutList ->
                _workouts.value = workoutList
            }
        }
    }

    private fun seedDefaultWorkoutsIfNeeded() {
        viewModelScope.launch {
            repository.seedDefaultWorkouts()
        }
    }

    fun setCategory(category: WorkoutCategory?) {
        _selectedCategory.value = category
    }

    fun getFilteredWorkouts(): List<CustomRunWorkout> {
        val category = _selectedCategory.value
        return if (category == null) {
            _workouts.value
        } else {
            _workouts.value.filter { it.category == category }
        }
    }

    fun toggleFavorite(workoutId: Long) {
        viewModelScope.launch {
            val workout = repository.getWorkoutById(workoutId)
            workout?.let {
                repository.setWorkoutFavorite(workoutId, !it.isFavorite)
            }
        }
    }

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            repository.deleteWorkoutById(workoutId)
        }
    }

    // Workout Builder Methods
    fun startNewWorkout() {
        _builderState.value = WorkoutBuilderState()
    }

    fun editWorkout(workout: CustomRunWorkout) {
        _builderState.value = WorkoutBuilderState(
            id = workout.id,
            name = workout.name,
            description = workout.description,
            phases = workout.phases.toMutableList(),
            difficulty = workout.difficulty,
            category = workout.category,
            isEditing = true
        )
    }

    fun updateWorkoutName(name: String) {
        _builderState.value = _builderState.value.copy(name = name)
    }

    fun updateWorkoutDescription(description: String) {
        _builderState.value = _builderState.value.copy(description = description)
    }

    fun updateWorkoutDifficulty(difficulty: RunDifficulty) {
        _builderState.value = _builderState.value.copy(difficulty = difficulty)
    }

    fun updateWorkoutCategory(category: WorkoutCategory) {
        _builderState.value = _builderState.value.copy(category = category)
    }

    fun addPhase(phase: WorkoutPhase) {
        val currentPhases = _builderState.value.phases.toMutableList()
        val newPhase = phase.copy(orderIndex = currentPhases.size)
        currentPhases.add(newPhase)
        _builderState.value = _builderState.value.copy(phases = currentPhases)
    }

    fun updatePhase(index: Int, phase: WorkoutPhase) {
        val currentPhases = _builderState.value.phases.toMutableList()
        if (index in currentPhases.indices) {
            currentPhases[index] = phase
            _builderState.value = _builderState.value.copy(phases = currentPhases)
        }
    }

    fun removePhase(index: Int) {
        val currentPhases = _builderState.value.phases.toMutableList()
        if (index in currentPhases.indices) {
            currentPhases.removeAt(index)
            // Re-index remaining phases
            currentPhases.forEachIndexed { i, phase ->
                currentPhases[i] = phase.copy(orderIndex = i)
            }
            _builderState.value = _builderState.value.copy(phases = currentPhases)
        }
    }

    fun movePhaseUp(index: Int) {
        if (index <= 0) return
        val currentPhases = _builderState.value.phases.toMutableList()
        val temp = currentPhases[index]
        currentPhases[index] = currentPhases[index - 1].copy(orderIndex = index)
        currentPhases[index - 1] = temp.copy(orderIndex = index - 1)
        _builderState.value = _builderState.value.copy(phases = currentPhases)
    }

    fun movePhaseDown(index: Int) {
        val currentPhases = _builderState.value.phases.toMutableList()
        if (index >= currentPhases.size - 1) return
        val temp = currentPhases[index]
        currentPhases[index] = currentPhases[index + 1].copy(orderIndex = index)
        currentPhases[index + 1] = temp.copy(orderIndex = index + 1)
        _builderState.value = _builderState.value.copy(phases = currentPhases)
    }

    fun duplicatePhase(index: Int) {
        val currentPhases = _builderState.value.phases.toMutableList()
        if (index in currentPhases.indices) {
            val original = currentPhases[index]
            val duplicate = original.copy(
                id = UUID.randomUUID().toString(),
                orderIndex = currentPhases.size
            )
            currentPhases.add(duplicate)
            _builderState.value = _builderState.value.copy(phases = currentPhases)
        }
    }

    fun saveWorkout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _builderState.value
            if (state.name.isBlank()) return@launch

            val workout = CustomRunWorkout(
                id = if (state.isEditing) state.id else 0,
                name = state.name,
                description = state.description,
                phases = state.phases,
                difficulty = state.difficulty,
                category = state.category,
                estimatedDurationMinutes = state.phases.sumOf { 
                    ((it.durationSeconds ?: 0) * it.repetitions) / 60 
                },
                estimatedDistanceMeters = state.phases.sumOf { 
                    (it.distanceMeters ?: 0.0) * it.repetitions 
                }
            )

            if (state.isEditing) {
                repository.updateWorkout(workout)
            } else {
                repository.saveWorkout(workout)
            }
            
            _builderState.value = WorkoutBuilderState()
            onSuccess()
        }
    }

    fun getWorkoutIntervals(workoutId: Long, onResult: (List<Interval>) -> Unit) {
        viewModelScope.launch {
            val workout = repository.getWorkoutById(workoutId)
            workout?.let {
                val intervals = repository.workoutToIntervals(it)
                repository.recordWorkoutUsage(workoutId)
                onResult(intervals)
            }
        }
    }

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun syncToWatch() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            try {
                val workouts = repository.getAllWorkouts()
                val success = watchSyncService.syncCustomWorkoutsToWatch(workouts)
                _syncStatus.value = if (success) SyncStatus.Success else SyncStatus.Error("Watch not connected")
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
            }
        }
    }

    fun clearSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

data class WorkoutBuilderState(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val phases: List<WorkoutPhase> = emptyList(),
    val difficulty: RunDifficulty = RunDifficulty.MODERATE,
    val category: WorkoutCategory = WorkoutCategory.GENERAL,
    val isEditing: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank() && phases.isNotEmpty()
    
    val totalDurationSeconds: Int
        get() = phases.sumOf { (it.durationSeconds ?: 0) * it.repetitions }
    
    val totalDistanceMeters: Double
        get() = phases.sumOf { (it.distanceMeters ?: 0.0) * it.repetitions }
    
    val formattedDuration: String
        get() {
            val totalSeconds = totalDurationSeconds
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
    
    val formattedDistance: String
        get() {
            val meters = totalDistanceMeters
            return if (meters >= 1000) "%.1f km".format(meters / 1000) else "${meters.toInt()}m"
        }
}
