package com.runtracker.app.ui.screens.mindfulness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.MentalHealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MindfulnessViewModel @Inject constructor(
    private val repository: MentalHealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MindfulnessUiState())
    val uiState: StateFlow<MindfulnessUiState> = _uiState.asStateFlow()

    private val _breathingState = MutableStateFlow<BreathingSessionState?>(null)
    val breathingState: StateFlow<BreathingSessionState?> = _breathingState.asStateFlow()

    private val _guidedSessionState = MutableStateFlow<GuidedSessionState?>(null)
    val guidedSessionState: StateFlow<GuidedSessionState?> = _guidedSessionState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getRecentCompletedSessions(10).collect { sessions ->
                _uiState.update { it.copy(recentSessions = sessions) }
            }
        }
        
        viewModelScope.launch {
            repository.getRecentWellnessCheckins(7).collect { checkins ->
                _uiState.update { it.copy(recentCheckins = checkins) }
            }
        }
        
        viewModelScope.launch {
            val todayCheckin = repository.getTodayCheckin()
            _uiState.update { it.copy(todayCheckin = todayCheckin) }
        }
        
        viewModelScope.launch {
            val minutesThisWeek = repository.getTotalMindfulnessMinutesThisWeek()
            val sessionsThisWeek = repository.getMindfulnessSessionCountThisWeek()
            _uiState.update { 
                it.copy(
                    mindfulnessMinutesThisWeek = minutesThisWeek,
                    sessionsThisWeek = sessionsThisWeek
                ) 
            }
        }
    }

    fun logMood(mood: MoodLevel, energy: EnergyLevel, stress: StressLevel, notes: String = "") {
        viewModelScope.launch {
            repository.logMood(mood, energy, stress, notes)
            loadData()
        }
    }

    fun saveWellnessCheckin(
        sleepHours: Float?,
        sleepQuality: Int?,
        mood: MoodLevel?,
        energy: EnergyLevel?,
        stress: StressLevel?,
        soreness: Int?,
        hydration: Int?,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.saveWellnessCheckin(
                sleepHours = sleepHours,
                sleepQuality = sleepQuality,
                mood = mood,
                energy = energy,
                stress = stress,
                soreness = soreness,
                hydration = hydration,
                notes = notes
            )
            val todayCheckin = repository.getTodayCheckin()
            _uiState.update { it.copy(todayCheckin = todayCheckin) }
        }
    }

    fun startBreathingSession(pattern: BreathingPattern) {
        _breathingState.value = BreathingSessionState(
            pattern = pattern,
            currentCycle = 1,
            phase = BreathingPhase.INHALE,
            secondsRemaining = pattern.inhaleSeconds,
            isActive = true
        )
    }

    fun updateBreathingState(state: BreathingSessionState) {
        _breathingState.value = state
    }

    fun completeBreathingSession(pattern: BreathingPattern, completed: Boolean = true) {
        viewModelScope.launch {
            repository.recordMindfulnessSession(
                type = MindfulnessType.BREATHING_EXERCISE,
                durationSeconds = pattern.totalDuration,
                completed = completed
            )
            _breathingState.value = null
            loadData()
        }
    }

    fun cancelBreathingSession() {
        val state = _breathingState.value
        if (state != null) {
            viewModelScope.launch {
                val completedDuration = (state.currentCycle - 1) * state.pattern.cycleDuration
                if (completedDuration > 30) {
                    repository.recordMindfulnessSession(
                        type = MindfulnessType.BREATHING_EXERCISE,
                        durationSeconds = completedDuration,
                        completed = false
                    )
                }
            }
        }
        _breathingState.value = null
    }

    fun recordMindfulnessSession(type: MindfulnessType, durationSeconds: Int, rating: Int? = null) {
        viewModelScope.launch {
            repository.recordMindfulnessSession(type, durationSeconds, rating = rating)
            loadData()
        }
    }

    fun startGuidedSession(content: MindfulnessContent) {
        val secondsPerStep = content.durationSeconds / content.instructions.size.coerceAtLeast(1)
        _guidedSessionState.value = GuidedSessionState(
            content = content,
            currentStepIndex = 0,
            secondsRemaining = secondsPerStep,
            secondsPerStep = secondsPerStep,
            totalElapsedSeconds = 0,
            isActive = true,
            isPaused = false
        )
    }

    fun updateGuidedSessionState(state: GuidedSessionState) {
        _guidedSessionState.value = state
    }

    fun completeGuidedSession(rating: Int? = null) {
        val state = _guidedSessionState.value
        if (state != null) {
            viewModelScope.launch {
                repository.recordMindfulnessSession(
                    type = state.content.type,
                    durationSeconds = state.totalElapsedSeconds.coerceAtLeast(state.content.durationSeconds),
                    completed = true,
                    rating = rating
                )
                _guidedSessionState.value = null
                loadData()
            }
        }
    }

    fun cancelGuidedSession() {
        val state = _guidedSessionState.value
        if (state != null && state.totalElapsedSeconds > 30) {
            viewModelScope.launch {
                repository.recordMindfulnessSession(
                    type = state.content.type,
                    durationSeconds = state.totalElapsedSeconds,
                    completed = false
                )
            }
        }
        _guidedSessionState.value = null
    }

    fun pauseGuidedSession() {
        _guidedSessionState.update { it?.copy(isPaused = true) }
    }

    fun resumeGuidedSession() {
        _guidedSessionState.update { it?.copy(isPaused = false) }
    }
}

data class MindfulnessUiState(
    val recentSessions: List<MindfulnessSession> = emptyList(),
    val recentCheckins: List<WellnessCheckin> = emptyList(),
    val todayCheckin: WellnessCheckin? = null,
    val mindfulnessMinutesThisWeek: Int = 0,
    val sessionsThisWeek: Int = 0,
    val isLoading: Boolean = false
)

data class BreathingSessionState(
    val pattern: BreathingPattern,
    val currentCycle: Int,
    val phase: BreathingPhase,
    val secondsRemaining: Int,
    val isActive: Boolean
)

enum class BreathingPhase {
    INHALE,
    HOLD_IN,
    EXHALE,
    HOLD_OUT
}

data class GuidedSessionState(
    val content: MindfulnessContent,
    val currentStepIndex: Int,
    val secondsRemaining: Int,
    val secondsPerStep: Int,
    val totalElapsedSeconds: Int,
    val isActive: Boolean,
    val isPaused: Boolean
) {
    val currentInstruction: String
        get() = content.instructions.getOrElse(currentStepIndex) { "" }
    
    val progress: Float
        get() = (currentStepIndex.toFloat() + 1) / content.instructions.size.coerceAtLeast(1)
    
    val isLastStep: Boolean
        get() = currentStepIndex >= content.instructions.size - 1
}
