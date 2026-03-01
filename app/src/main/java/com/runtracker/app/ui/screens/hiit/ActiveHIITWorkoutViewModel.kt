package com.runtracker.app.ui.screens.hiit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.audio.HIITAudioCueManager
import com.runtracker.shared.data.model.HIITExerciseLibrary
import com.runtracker.shared.data.model.HIITSession
import com.runtracker.shared.data.model.HIITWorkoutTemplate
import com.runtracker.shared.data.repository.HIITRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HIITPhase {
    WARMUP, WORK, REST, COOLDOWN, COMPLETE, PAUSED
}

data class ActiveHIITWorkoutUiState(
    val template: HIITWorkoutTemplate? = null,
    val phase: HIITPhase = HIITPhase.WARMUP,
    val currentExerciseIndex: Int = 0,
    val currentRound: Int = 1,
    val remainingSeconds: Int = 0,
    val phaseDurationSeconds: Int = 0,
    val totalElapsedMs: Long = 0,
    val isPaused: Boolean = false,
    val isComplete: Boolean = false,
    val currentExerciseName: String = "Get Ready!",
    val currentExerciseDescription: String = "",
    val nextExerciseName: String? = null,
    val caloriesEstimate: Int = 0,
    val savedSessionId: Long? = null
) {
    val phaseProgress: Float
        get() = if (phaseDurationSeconds > 0) {
            1f - (remainingSeconds.toFloat() / phaseDurationSeconds.toFloat())
        } else 0f

    val roundProgress: String
        get() = "Round ${currentRound}/${template?.rounds ?: 0}"

    val exerciseProgress: String
        get() = "${currentExerciseIndex + 1}/${template?.exercises?.size ?: 0}"
}

@HiltViewModel
class ActiveHIITWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val hiitRepository: HIITRepository,
    private val audioCueManager: HIITAudioCueManager
) : ViewModel() {

    private val templateId: String = savedStateHandle["templateId"] ?: ""

    private val _uiState = MutableStateFlow(ActiveHIITWorkoutUiState())
    val uiState: StateFlow<ActiveHIITWorkoutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var startTimeMs: Long = 0
    private var pausedAccumulatedMs: Long = 0
    private var pauseStartMs: Long = 0
    private var prePausePhase: HIITPhase = HIITPhase.WARMUP

    init {
        val template = HIITExerciseLibrary.getTemplateById(templateId)
        if (template != null) {
            _uiState.update {
                it.copy(
                    template = template,
                    phase = HIITPhase.WARMUP,
                    remainingSeconds = template.warmupSec,
                    phaseDurationSeconds = template.warmupSec,
                    currentExerciseName = "Warm Up",
                    currentExerciseDescription = "Get your body ready",
                    nextExerciseName = template.exercises.firstOrNull()?.exercise?.name
                )
            }
            audioCueManager.playPhaseTone()
            startTimer()
        }
    }

    private fun startTimer() {
        startTimeMs = System.currentTimeMillis()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (state.isPaused || state.isComplete) continue

                val newRemaining = state.remainingSeconds - 1
                val elapsed = System.currentTimeMillis() - startTimeMs - pausedAccumulatedMs

                // Audio cue
                audioCueManager.onTick(
                    remainingSeconds = newRemaining,
                    isWorkPhase = state.phase == HIITPhase.WORK
                )

                // Estimate calories: ~10 cal/min during work, ~4 during rest
                val calPerSec = when (state.phase) {
                    HIITPhase.WORK -> 10.0 / 60.0
                    HIITPhase.REST -> 4.0 / 60.0
                    else -> 3.0 / 60.0
                }

                if (newRemaining <= 0) {
                    advancePhase()
                } else {
                    _uiState.update {
                        it.copy(
                            remainingSeconds = newRemaining,
                            totalElapsedMs = elapsed,
                            caloriesEstimate = (elapsed / 1000.0 * calPerSec).toInt()
                                .coerceAtLeast(it.caloriesEstimate)
                        )
                    }
                }
            }
        }
    }

    private fun advancePhase() {
        val state = _uiState.value
        val template = state.template ?: return

        when (state.phase) {
            HIITPhase.WARMUP -> {
                // Start first exercise
                val firstExercise = template.exercises[0]
                _uiState.update {
                    it.copy(
                        phase = HIITPhase.WORK,
                        currentExerciseIndex = 0,
                        remainingSeconds = firstExercise.durationOverrideSec ?: template.workDurationSec,
                        phaseDurationSeconds = firstExercise.durationOverrideSec ?: template.workDurationSec,
                        currentExerciseName = firstExercise.exercise.name,
                        currentExerciseDescription = firstExercise.exercise.description,
                        nextExerciseName = if (template.exercises.size > 1) template.exercises[1].exercise.name else null
                    )
                }
            }
            HIITPhase.WORK -> {
                val nextExIndex = state.currentExerciseIndex + 1
                if (nextExIndex < template.exercises.size) {
                    // Rest between exercises
                    _uiState.update {
                        it.copy(
                            phase = HIITPhase.REST,
                            remainingSeconds = template.restDurationSec,
                            phaseDurationSeconds = template.restDurationSec,
                            currentExerciseName = "Rest",
                            currentExerciseDescription = "Catch your breath",
                            nextExerciseName = template.exercises[nextExIndex].exercise.name
                        )
                    }
                } else if (state.currentRound < template.rounds) {
                    // Rest before next round
                    _uiState.update {
                        it.copy(
                            phase = HIITPhase.REST,
                            remainingSeconds = template.restDurationSec,
                            phaseDurationSeconds = template.restDurationSec,
                            currentExerciseName = "Rest",
                            currentExerciseDescription = "Round ${state.currentRound} complete!",
                            nextExerciseName = template.exercises[0].exercise.name
                        )
                    }
                } else {
                    // All rounds done — cooldown
                    _uiState.update {
                        it.copy(
                            phase = HIITPhase.COOLDOWN,
                            remainingSeconds = template.cooldownSec,
                            phaseDurationSeconds = template.cooldownSec,
                            currentExerciseName = "Cool Down",
                            currentExerciseDescription = "Stretch and breathe",
                            nextExerciseName = null
                        )
                    }
                    audioCueManager.playPhaseTone()
                }
            }
            HIITPhase.REST -> {
                val nextExIndex = state.currentExerciseIndex + 1
                val (newIndex, newRound) = if (nextExIndex < template.exercises.size) {
                    nextExIndex to state.currentRound
                } else {
                    0 to (state.currentRound + 1)
                }
                val exercise = template.exercises[newIndex]
                val afterNext = if (newIndex + 1 < template.exercises.size) {
                    template.exercises[newIndex + 1].exercise.name
                } else null

                _uiState.update {
                    it.copy(
                        phase = HIITPhase.WORK,
                        currentExerciseIndex = newIndex,
                        currentRound = newRound,
                        remainingSeconds = exercise.durationOverrideSec ?: template.workDurationSec,
                        phaseDurationSeconds = exercise.durationOverrideSec ?: template.workDurationSec,
                        currentExerciseName = exercise.exercise.name,
                        currentExerciseDescription = exercise.exercise.description,
                        nextExerciseName = afterNext
                    )
                }
            }
            HIITPhase.COOLDOWN -> {
                // Workout complete!
                audioCueManager.playCompleteTone()
                timerJob?.cancel()
                val elapsed = System.currentTimeMillis() - startTimeMs - pausedAccumulatedMs
                _uiState.update {
                    it.copy(
                        phase = HIITPhase.COMPLETE,
                        isComplete = true,
                        remainingSeconds = 0,
                        totalElapsedMs = elapsed
                    )
                }
                saveSession()
            }
            else -> {}
        }
    }

    fun togglePause() {
        val state = _uiState.value
        if (state.isComplete) return

        if (state.isPaused) {
            // Resume
            pausedAccumulatedMs += System.currentTimeMillis() - pauseStartMs
            _uiState.update {
                it.copy(isPaused = false, phase = prePausePhase)
            }
        } else {
            // Pause
            pauseStartMs = System.currentTimeMillis()
            prePausePhase = state.phase
            _uiState.update {
                it.copy(isPaused = true, phase = HIITPhase.PAUSED)
            }
        }
    }

    fun stopWorkout() {
        timerJob?.cancel()
        val elapsed = System.currentTimeMillis() - startTimeMs - pausedAccumulatedMs
        _uiState.update {
            it.copy(
                phase = HIITPhase.COMPLETE,
                isComplete = true,
                totalElapsedMs = elapsed
            )
        }
        saveSession()
    }

    private fun saveSession() {
        viewModelScope.launch {
            val state = _uiState.value
            val template = state.template ?: return@launch
            val session = HIITSession(
                templateId = template.id,
                templateName = template.name,
                totalDurationMs = state.totalElapsedMs,
                exerciseCount = template.exercises.size,
                roundsCompleted = state.currentRound,
                totalRounds = template.rounds,
                caloriesEstimate = state.caloriesEstimate,
                isCompleted = state.currentRound >= template.rounds,
                source = "phone"
            )
            val id = hiitRepository.insertSession(session)
            _uiState.update { it.copy(savedSessionId = id) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        audioCueManager.release()
    }
}
