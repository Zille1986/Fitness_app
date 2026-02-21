package com.runtracker.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.Gender
import com.runtracker.shared.data.model.Units
import com.runtracker.shared.data.model.UserProfile
import com.runtracker.shared.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userRepository.getProfile().collect { profile ->
                profile?.let {
                    _uiState.update { state ->
                        state.copy(
                            profile = it,
                            name = it.name,
                            age = it.age?.toString() ?: "",
                            weight = it.weight?.toString() ?: "",
                            height = it.height?.toString() ?: "",
                            gender = it.gender,
                            restingHeartRate = it.restingHeartRate?.toString() ?: "",
                            maxHeartRate = it.maxHeartRate?.toString() ?: "",
                            weeklyGoalKm = it.weeklyGoalKm.toString(),
                            preferredUnits = it.preferredUnits,
                            isStravaConnected = it.stravaAccessToken != null,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateAge(age: String) {
        _uiState.update { it.copy(age = age) }
    }

    fun updateWeight(weight: String) {
        _uiState.update { it.copy(weight = weight) }
    }

    fun updateHeight(height: String) {
        _uiState.update { it.copy(height = height) }
    }

    fun updateGender(gender: Gender?) {
        _uiState.update { it.copy(gender = gender) }
    }

    fun updateRestingHeartRate(hr: String) {
        _uiState.update { it.copy(restingHeartRate = hr) }
    }

    fun updateMaxHeartRate(hr: String) {
        _uiState.update { it.copy(maxHeartRate = hr) }
    }

    fun updateWeeklyGoal(goal: String) {
        _uiState.update { it.copy(weeklyGoalKm = goal) }
    }

    fun updatePreferredUnits(units: Units) {
        _uiState.update { it.copy(preferredUnits = units) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value
            val profile = state.profile?.copy(
                name = state.name,
                age = state.age.toIntOrNull(),
                weight = state.weight.toDoubleOrNull(),
                height = state.height.toDoubleOrNull(),
                gender = state.gender,
                restingHeartRate = state.restingHeartRate.toIntOrNull(),
                maxHeartRate = state.maxHeartRate.toIntOrNull(),
                weeklyGoalKm = state.weeklyGoalKm.toDoubleOrNull() ?: 20.0,
                preferredUnits = state.preferredUnits
            ) ?: UserProfile(
                name = state.name,
                age = state.age.toIntOrNull(),
                weight = state.weight.toDoubleOrNull(),
                height = state.height.toDoubleOrNull(),
                gender = state.gender,
                restingHeartRate = state.restingHeartRate.toIntOrNull(),
                maxHeartRate = state.maxHeartRate.toIntOrNull(),
                weeklyGoalKm = state.weeklyGoalKm.toDoubleOrNull() ?: 20.0,
                preferredUnits = state.preferredUnits
            )
            
            userRepository.saveProfile(profile)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun disconnectStrava() {
        viewModelScope.launch {
            userRepository.clearStravaConnection()
        }
    }
}

data class ProfileUiState(
    val profile: UserProfile? = null,
    val name: String = "",
    val age: String = "",
    val weight: String = "",
    val height: String = "",
    val gender: Gender? = null,
    val restingHeartRate: String = "",
    val maxHeartRate: String = "",
    val weeklyGoalKm: String = "20",
    val preferredUnits: Units = Units.METRIC,
    val isStravaConnected: Boolean = false,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)
