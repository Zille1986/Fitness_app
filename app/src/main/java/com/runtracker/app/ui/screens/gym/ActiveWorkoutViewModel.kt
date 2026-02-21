package com.runtracker.app.ui.screens.gym

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.assistant.AssistantEventBus
import com.runtracker.app.assistant.AssistantTrigger
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.GymRepository
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject

private const val TAG = "ActiveWorkoutViewModel"

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val gymRepository: GymRepository,
    private val assistantEventBus: AssistantEventBus,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val templateId: Long = savedStateHandle.get<Long>("workoutId") ?: 0L
    private var actualWorkoutId: Long = 0L

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()
    private val workoutMutex = Mutex()

    private var timerJob: Job? = null
    private var restTimerJob: Job? = null

    init {
        createWorkoutFromTemplate()
        startTimer()
    }

    private fun createWorkoutFromTemplate() {
        viewModelScope.launch {
            // First try to get the template
            val template = gymRepository.getTemplateById(templateId)
            
            if (template != null) {
                // Create a new workout from the template
                val exercises = template.exercises.map { templateExercise ->
                    WorkoutExercise(
                        id = UUID.randomUUID().toString(),
                        exerciseId = templateExercise.exerciseId,
                        exerciseName = templateExercise.exerciseName,
                        sets = (1..templateExercise.sets).map { setNum ->
                            WorkoutSet(
                                id = UUID.randomUUID().toString(),
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

                actualWorkoutId = gymRepository.insertWorkout(workout)
                gymRepository.incrementTemplateUsage(template.id)
                
                // Now load the created workout
                loadWorkout(actualWorkoutId)
            } else {
                // Maybe it's already a workout ID, try loading directly
                loadWorkout(templateId)
            }
        }
    }

    private fun loadWorkout(workoutId: Long) {
        viewModelScope.launch {
            gymRepository.getWorkoutById(workoutId)
                .catch { e -> Log.e(TAG, "Error loading workout", e) }
                .collect { workout ->
                    Log.d(TAG, "Loaded workout: ${workout?.name}, exercises: ${workout?.exercises?.map { "${it.exerciseName}(id=${it.exerciseId})" }}")
                    _uiState.update { state ->
                        state.copy(
                            workout = workout,
                            isLoading = false
                        )
                    }
                    // Load PBs and last workout data for each exercise
                    workout?.exercises?.forEach { exercise ->
                        loadExerciseHistory(exercise.exerciseId)
                    }
                }
        }
    }
    
    private fun loadExerciseHistory(exerciseId: Long) {
        viewModelScope.launch {
            android.util.Log.d("ActiveWorkoutVM", "Loading history for exerciseId: $exerciseId")
            val bestOneRepMax = gymRepository.getBestOneRepMax(exerciseId)
            val bestWeight = gymRepository.getBestWeight(exerciseId)
            val recentHistory = gymRepository.getRecentHistoryForExercise(exerciseId, 1)
            val lastWorkout = recentHistory.firstOrNull()
            
            android.util.Log.d("ActiveWorkoutVM", "exerciseId=$exerciseId: bestOneRepMax=$bestOneRepMax, bestWeight=$bestWeight, lastWorkout=$lastWorkout")
            
            _uiState.update { state ->
                val updatedPBs = state.exercisePBs.toMutableMap()
                val updatedLastWorkouts = state.exerciseLastWorkouts.toMutableMap()
                
                if (bestOneRepMax != null || bestWeight != null) {
                    updatedPBs[exerciseId] = ExercisePB(
                        exerciseId = exerciseId,
                        bestWeight = bestWeight?.bestWeight ?: 0.0,
                        bestWeightReps = bestWeight?.bestReps ?: 0,
                        bestOneRepMax = bestOneRepMax?.estimatedOneRepMax ?: 0.0,
                        bestOneRepMaxDate = bestOneRepMax?.date ?: 0L
                    )
                    android.util.Log.d("ActiveWorkoutVM", "Added PB for exerciseId=$exerciseId: ${updatedPBs[exerciseId]}")
                } else {
                    android.util.Log.d("ActiveWorkoutVM", "No PB data found for exerciseId=$exerciseId")
                }
                
                if (lastWorkout != null) {
                    updatedLastWorkouts[exerciseId] = lastWorkout
                }
                
                state.copy(
                    exercisePBs = updatedPBs,
                    exerciseLastWorkouts = updatedLastWorkouts
                )
            }
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                workoutMutex.withLock {
                    _uiState.update { state ->
                        state.workout?.let { workout ->
                            val duration = System.currentTimeMillis() - workout.startTime
                            state.copy(elapsedMillis = duration)
                        } ?: state
                    }
                }
            }
        }
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            val currentWorkout = _uiState.value.workout ?: return@launch
            
            val workoutExercise = WorkoutExercise(
                id = UUID.randomUUID().toString(),
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                sets = listOf(
                    WorkoutSet(
                        id = UUID.randomUUID().toString(),
                        setNumber = 1
                    )
                ),
                orderIndex = currentWorkout.exercises.size
            )

            val updatedExercises = currentWorkout.exercises + workoutExercise
            val updatedWorkout = currentWorkout.copy(exercises = updatedExercises)
            
            gymRepository.updateWorkout(updatedWorkout)
            
            // Load PB and history for the new exercise
            loadExerciseHistory(exercise.id)
        }
    }
    
    fun addExerciseById(exerciseId: Long) {
        viewModelScope.launch {
            val exercise = gymRepository.getExerciseById(exerciseId) ?: return@launch
            val currentWorkout = _uiState.value.workout ?: return@launch
            
            val workoutExercise = WorkoutExercise(
                id = UUID.randomUUID().toString(),
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                sets = listOf(
                    WorkoutSet(
                        id = UUID.randomUUID().toString(),
                        setNumber = 1
                    )
                ),
                orderIndex = currentWorkout.exercises.size
            )

            val updatedExercises = currentWorkout.exercises + workoutExercise
            val updatedWorkout = currentWorkout.copy(exercises = updatedExercises)
            
            gymRepository.updateWorkout(updatedWorkout)
            
            // Load PB and history for the new exercise
            loadExerciseHistory(exerciseId)
        }
    }

    fun addSet(exerciseIndex: Int) {
        viewModelScope.launch {
            val currentWorkout = _uiState.value.workout ?: return@launch
            val exercise = currentWorkout.exercises.getOrNull(exerciseIndex) ?: return@launch
            
            val lastSet = exercise.sets.lastOrNull()
            val newSet = WorkoutSet(
                id = UUID.randomUUID().toString(),
                setNumber = exercise.sets.size + 1,
                weight = lastSet?.weight ?: 0.0,
                targetReps = lastSet?.targetReps ?: 10
            )

            val updatedSets = exercise.sets + newSet
            val updatedExercise = exercise.copy(sets = updatedSets)
            val updatedExercises = currentWorkout.exercises.toMutableList().apply {
                set(exerciseIndex, updatedExercise)
            }
            
            gymRepository.updateWorkout(currentWorkout.copy(exercises = updatedExercises))
        }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        viewModelScope.launch {
            val currentWorkout = _uiState.value.workout ?: return@launch
            val exercise = currentWorkout.exercises.getOrNull(exerciseIndex) ?: return@launch
            
            if (exercise.sets.size <= 1) return@launch
            
            val updatedSets = exercise.sets.toMutableList().apply {
                removeAt(setIndex)
            }.mapIndexed { index, set ->
                set.copy(setNumber = index + 1)
            }
            
            val updatedExercise = exercise.copy(sets = updatedSets)
            val updatedExercises = currentWorkout.exercises.toMutableList().apply {
                set(exerciseIndex, updatedExercise)
            }
            
            gymRepository.updateWorkout(currentWorkout.copy(exercises = updatedExercises))
        }
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, weight: Double?, reps: Int?) {
        viewModelScope.launch {
            val currentWorkout = _uiState.value.workout ?: return@launch
            val exercise = currentWorkout.exercises.getOrNull(exerciseIndex) ?: return@launch
            val set = exercise.sets.getOrNull(setIndex) ?: return@launch

            val updatedSet = set.copy(
                weight = weight ?: set.weight,
                reps = reps ?: set.reps
            )

            val updatedSets = exercise.sets.toMutableList().apply {
                set(setIndex, updatedSet)
            }
            
            val updatedExercise = exercise.copy(sets = updatedSets)
            val updatedExercises = currentWorkout.exercises.toMutableList().apply {
                set(exerciseIndex, updatedExercise)
            }
            
            gymRepository.updateWorkout(currentWorkout.copy(exercises = updatedExercises))
        }
    }

    fun completeSet(exerciseIndex: Int, setIndex: Int) {
        viewModelScope.launch {
            workoutMutex.withLock {
                val currentWorkout = _uiState.value.workout ?: return@launch
                val exercise = currentWorkout.exercises.getOrNull(exerciseIndex) ?: return@launch
                val set = exercise.sets.getOrNull(setIndex) ?: return@launch

                val updatedSet = set.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis()
                )

                val updatedSets = exercise.sets.toMutableList().apply {
                    set(setIndex, updatedSet)
                }

                val updatedExercise = exercise.copy(sets = updatedSets)
                val updatedExercises = currentWorkout.exercises.toMutableList().apply {
                    set(exerciseIndex, updatedExercise)
                }

                gymRepository.updateWorkout(currentWorkout.copy(exercises = updatedExercises))

                // Start rest timer
                startRestTimer(exercise.restSeconds)
            }
        }
    }

    fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _uiState.update { it.copy(restTimeRemaining = seconds, isRestTimerActive = true) }
        
        restTimerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update { it.copy(restTimeRemaining = remaining) }
            }
            _uiState.update { it.copy(isRestTimerActive = false, restTimeRemaining = 0) }
        }
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        _uiState.update { it.copy(isRestTimerActive = false, restTimeRemaining = 0) }
    }

    fun removeExercise(exerciseIndex: Int) {
        viewModelScope.launch {
            val currentWorkout = _uiState.value.workout ?: return@launch
            
            val updatedExercises = currentWorkout.exercises.toMutableList().apply {
                removeAt(exerciseIndex)
            }.mapIndexed { index, exercise ->
                exercise.copy(orderIndex = index)
            }
            
            gymRepository.updateWorkout(currentWorkout.copy(exercises = updatedExercises))
        }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            val currentWorkout = _uiState.value.workout
            
            if (currentWorkout == null) {
                // No workout loaded, just navigate back
                _uiState.update { it.copy(isFinished = true) }
                return@launch
            }
            
            try {
                val totalVolume = currentWorkout.exercises.sumOf { it.totalVolume }
                val totalSets = currentWorkout.exercises.sumOf { it.completedSets }
                val totalReps = currentWorkout.exercises.sumOf { it.totalReps }

                val completedWorkout = currentWorkout.copy(
                    endTime = System.currentTimeMillis(),
                    isCompleted = true,
                    totalVolume = totalVolume,
                    totalSets = totalSets,
                    totalReps = totalReps
                )

                gymRepository.updateWorkout(completedWorkout)

                // Record history for each exercise and check for PBs
                val pbResults = mutableListOf<Pair<String, GymRepository.PBResult>>()
                currentWorkout.exercises.forEach { exercise ->
                    if (exercise.sets.any { it.isCompleted }) {
                        val pbResult = gymRepository.recordExerciseHistory(
                            exerciseId = exercise.exerciseId,
                            workoutId = currentWorkout.id,
                            sets = exercise.sets
                        )
                        if (pbResult != null) {
                            pbResults.add(exercise.exerciseName to pbResult)
                        }
                    }
                }
                
                // Trigger assistant for PBs
                pbResults.forEach { (exerciseName, pb) ->
                    assistantEventBus.tryEmit(
                        AssistantTrigger.NewPB(
                            exerciseName = exerciseName,
                            weight = pb.weight,
                            reps = pb.reps
                        )
                    )
                }
                
                // Trigger workout complete event
                assistantEventBus.tryEmit(
                    AssistantTrigger.WorkoutComplete(
                        duration = completedWorkout.durationFormatted,
                        volume = totalVolume
                    )
                )
            } finally {
                _uiState.update { it.copy(isFinished = true) }
            }
        }
    }

    fun cancelWorkout() {
        viewModelScope.launch {
            _uiState.value.workout?.let { workout ->
                gymRepository.deleteWorkout(workout)
            }
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    suspend fun getProgressionSuggestion(exerciseId: Long, exerciseName: String): ProgressionSuggestion {
        return gymRepository.getProgressionSuggestion(exerciseId, exerciseName)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        restTimerJob?.cancel()
    }
}

data class ExercisePB(
    val exerciseId: Long,
    val bestWeight: Double,
    val bestWeightReps: Int,
    val bestOneRepMax: Double,
    val bestOneRepMaxDate: Long
) {
    val bestWeightFormatted: String
        get() = "${bestWeight}kg × $bestWeightReps"
    
    val bestOneRepMaxFormatted: String
        get() = String.format("%.1f kg", bestOneRepMax)
}

data class ActiveWorkoutUiState(
    val workout: GymWorkout? = null,
    val elapsedMillis: Long = 0,
    val isRestTimerActive: Boolean = false,
    val restTimeRemaining: Int = 0,
    val isLoading: Boolean = true,
    val isFinished: Boolean = false,
    val exercisePBs: Map<Long, ExercisePB> = emptyMap(),
    val exerciseLastWorkouts: Map<Long, ExerciseHistory> = emptyMap()
) {
    val elapsedFormatted: String
        get() {
            val totalSeconds = elapsedMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    val restTimeFormatted: String
        get() {
            val minutes = restTimeRemaining / 60
            val seconds = restTimeRemaining % 60
            return String.format("%d:%02d", minutes, seconds)
        }
}
