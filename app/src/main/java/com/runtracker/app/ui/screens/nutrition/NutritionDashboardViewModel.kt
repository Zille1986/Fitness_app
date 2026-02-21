package com.runtracker.app.ui.screens.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.health.HealthConnectManager
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.NutritionRepository
import com.runtracker.shared.data.repository.RunRepository
import com.runtracker.shared.data.repository.GymRepository
import com.runtracker.shared.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class NutritionDashboardViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val userRepository: UserRepository,
    private val runRepository: RunRepository,
    private val gymRepository: GymRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionDashboardUiState())
    val uiState: StateFlow<NutritionDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
        loadStepsFromHealthConnect()
        loadTodayRunCalories()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load user profile
            userRepository.getProfile().collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
                loadNutritionData(profile)
            }
        }

        viewModelScope.launch {
            nutritionRepository.getGoals().collect { goals ->
                _uiState.update { it.copy(goals = goals ?: NutritionGoals()) }
            }
        }
    }
    
    private fun loadStepsFromHealthConnect() {
        viewModelScope.launch {
            try {
                healthConnectManager.checkAvailability()
                val hasPermissions = healthConnectManager.hasAllPermissions()
                android.util.Log.d("NutritionDashboardVM", "Health Connect permissions: $hasPermissions")
                
                if (hasPermissions) {
                    val steps = healthConnectManager.getTodaySteps()
                    android.util.Log.d("NutritionDashboardVM", "Steps from Health Connect: $steps")
                    if (steps > 0) {
                        updateStepCount(steps)
                    }
                } else {
                    android.util.Log.w("NutritionDashboardVM", "Health Connect permissions not granted")
                }
            } catch (e: Exception) {
                android.util.Log.e("NutritionDashboardVM", "Failed to load steps from Health Connect", e)
            }
        }
    }
    
    fun refreshSteps() {
        loadStepsFromHealthConnect()
    }
    
    private fun loadTodayRunCalories() {
        viewModelScope.launch {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val tomorrow = today + 24 * 60 * 60 * 1000
            
            runRepository.getRunsInRange(today, tomorrow).collect { runs ->
                val totalRunCalories = runs.sumOf { it.caloriesBurned }
                if (totalRunCalories > 0) {
                    // Set workout calories (not add) to avoid duplicates
                    nutritionRepository.setWorkoutCalories(totalRunCalories)
                    android.util.Log.d("NutritionDashboardVM", "Set workout calories to $totalRunCalories from runs")
                }
            }
        }
    }

    private fun loadNutritionData(profile: UserProfile?) {
        viewModelScope.launch {
            val goals = nutritionRepository.getGoalsOnce() ?: NutritionGoals()
            val todayNutrition = nutritionRepository.getOrCreateTodayNutrition(profile, goals)
            
            // Determine day type based on today's activities
            val dayType = determineTodayType()
            
            // Generate suggestions
            val currentMealType = getCurrentMealType()
            val suggestions = MealSuggestionEngine.generateSuggestions(
                dailyNutrition = todayNutrition,
                goals = goals,
                dayType = dayType,
                currentMealType = currentMealType
            )
            
            // Generate tips
            val tips = MealSuggestionEngine.generateDailyTips(
                dailyNutrition = todayNutrition,
                dayType = dayType,
                stepCount = todayNutrition.stepCount,
                stepGoal = goals.dailyStepGoal
            )

            _uiState.update {
                it.copy(
                    todayNutrition = todayNutrition,
                    dayType = dayType,
                    currentMealType = currentMealType,
                    mealSuggestions = suggestions,
                    tips = tips,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            nutritionRepository.getTodayNutrition().collect { nutrition ->
                nutrition?.let { n ->
                    _uiState.update { it.copy(todayNutrition = n) }
                }
            }
        }
    }

    private suspend fun determineTodayType(): DayType {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val tomorrow = today + 24 * 60 * 60 * 1000

        // Check for runs today
        val hasRun = runRepository.getRunsInRange(today, tomorrow).firstOrNull()?.isNotEmpty() == true
        
        // Check for gym workouts today
        val hasGymWorkout = gymRepository.getWorkoutsInRange(today, tomorrow).firstOrNull()?.isNotEmpty() == true
        
        val stepCount = _uiState.value.todayNutrition?.stepCount ?: 0
        val stepGoal = _uiState.value.goals?.dailyStepGoal ?: 10000

        return MealSuggestionEngine.determineDayType(hasRun, hasGymWorkout, stepCount, stepGoal)
    }

    private fun getCurrentMealType(): MealType {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 10 -> MealType.BREAKFAST
            hour < 12 -> MealType.MORNING_SNACK
            hour < 14 -> MealType.LUNCH
            hour < 17 -> MealType.AFTERNOON_SNACK
            hour < 20 -> MealType.DINNER
            else -> MealType.EVENING_SNACK
        }
    }

    fun addWater(ml: Int) {
        viewModelScope.launch {
            nutritionRepository.addWater(ml)
        }
    }

    fun updateStepCount(steps: Int) {
        viewModelScope.launch {
            val weight = _uiState.value.userProfile?.weight ?: 70.0
            nutritionRepository.updateStepCount(steps, weight)
        }
    }

    fun logMealFromSuggestion(suggestion: MealSuggestion) {
        viewModelScope.launch {
            val meal = MealEntry(
                id = UUID.randomUUID().toString(),
                name = suggestion.name,
                mealType = suggestion.mealType,
                calories = suggestion.calories,
                proteinGrams = suggestion.proteinGrams,
                carbsGrams = suggestion.carbsGrams,
                fatGrams = suggestion.fatGrams,
                fiberGrams = suggestion.fiberGrams
            )
            nutritionRepository.addMeal(meal)
            refreshSuggestions()
        }
    }

    fun logQuickMeal(name: String, calories: Int, protein: Int, carbs: Int, fat: Int) {
        viewModelScope.launch {
            val meal = MealEntry(
                id = UUID.randomUUID().toString(),
                name = name,
                mealType = getCurrentMealType(),
                calories = calories,
                proteinGrams = protein.toDouble(),
                carbsGrams = carbs.toDouble(),
                fatGrams = fat.toDouble()
            )
            nutritionRepository.addMeal(meal)
            refreshSuggestions()
        }
    }

    fun removeMeal(mealId: String) {
        viewModelScope.launch {
            nutritionRepository.removeMeal(mealId)
        }
    }

    private fun refreshSuggestions() {
        viewModelScope.launch {
            val nutrition = _uiState.value.todayNutrition ?: return@launch
            val goals = _uiState.value.goals ?: NutritionGoals()
            val dayType = _uiState.value.dayType
            val mealType = getCurrentMealType()

            val suggestions = MealSuggestionEngine.generateSuggestions(
                dailyNutrition = nutrition,
                goals = goals,
                dayType = dayType,
                currentMealType = mealType
            )

            val tips = MealSuggestionEngine.generateDailyTips(
                dailyNutrition = nutrition,
                dayType = dayType,
                stepCount = nutrition.stepCount,
                stepGoal = goals.dailyStepGoal
            )

            _uiState.update {
                it.copy(
                    mealSuggestions = suggestions,
                    tips = tips,
                    currentMealType = mealType
                )
            }
        }
    }
}

data class NutritionDashboardUiState(
    val todayNutrition: DailyNutrition? = null,
    val goals: NutritionGoals? = null,
    val userProfile: UserProfile? = null,
    val dayType: DayType = DayType.REST_DAY,
    val currentMealType: MealType = MealType.BREAKFAST,
    val mealSuggestions: List<MealSuggestion> = emptyList(),
    val tips: List<String> = emptyList(),
    val isLoading: Boolean = true
)
