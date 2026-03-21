package com.runtracker.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.repository.NutritionRepository
import com.runtracker.shared.data.repository.UserRepository
import com.runtracker.shared.data.repository.TrainingPlanRepository
import com.runtracker.shared.data.repository.RunRepository
import com.runtracker.shared.data.repository.GymRepository
import com.runtracker.app.health.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.runtracker.shared.util.TimeUtils
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val userRepository: UserRepository,
    private val trainingPlanRepository: TrainingPlanRepository,
    private val runRepository: RunRepository,
    private val gymRepository: GymRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        loadNutritionData()
        loadTodayWorkouts()
        loadWeeklyStats()
        loadReadinessScore()
        loadStreak()
    }
    
    private fun loadStreak() {
        viewModelScope.launch {
            // Calculate streak from runs and gym workouts
            val runs = runRepository.getAllRunsOnce()
            val gymWorkouts = gymRepository.getAllWorkoutsOnce()
            
            // Get all workout dates
            val workoutDates = mutableSetOf<String>()
            runs.forEach { run ->
                val cal = Calendar.getInstance().apply { timeInMillis = run.startTime }
                workoutDates.add("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}")
            }
            gymWorkouts.forEach { workout ->
                val cal = Calendar.getInstance().apply { timeInMillis = workout.startTime }
                workoutDates.add("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}")
            }
            
            // Calculate current streak
            var currentStreak = 0
            var longestStreak = 0
            val today = Calendar.getInstance()
            val checkDate = Calendar.getInstance()
            var skippedToday = false

            // Check backwards from today
            for (i in 0..365) {
                checkDate.timeInMillis = today.timeInMillis
                checkDate.add(Calendar.DAY_OF_YEAR, -i)
                val dateKey = "${checkDate.get(Calendar.YEAR)}-${checkDate.get(Calendar.DAY_OF_YEAR)}"

                if (workoutDates.contains(dateKey)) {
                    currentStreak++
                } else if (i == 0) {
                    // Today not worked out yet, keep checking yesterday
                    skippedToday = true
                    continue
                } else {
                    // Gap found, streak ends
                    break
                }
            }

            // Calculate longest streak across all history
            var tempStreak = 0
            val sortedDates = workoutDates.sortedDescending()
            for (j in 0..365) {
                checkDate.timeInMillis = today.timeInMillis
                checkDate.add(Calendar.DAY_OF_YEAR, -j)
                val dateKey = "${checkDate.get(Calendar.YEAR)}-${checkDate.get(Calendar.DAY_OF_YEAR)}"
                if (workoutDates.contains(dateKey)) {
                    tempStreak++
                    longestStreak = maxOf(longestStreak, tempStreak)
                } else {
                    tempStreak = 0
                }
            }
            
            val message = when {
                currentStreak >= 30 -> "🔥 Incredible! Keep the fire burning!"
                currentStreak >= 14 -> "💪 Two weeks strong!"
                currentStreak >= 7 -> "⭐ One week streak!"
                currentStreak >= 3 -> "🌟 Great momentum!"
                currentStreak > 0 -> "✨ Keep it going!"
                else -> "Start your streak today!"
            }
            
            _uiState.update { 
                it.copy(
                    currentStreak = currentStreak,
                    longestStreak = longestStreak,
                    streakMessage = message
                )
            }
        }
    }

    private fun loadTodayWorkouts() {
        viewModelScope.launch {
            val todayWorkouts = mutableListOf<TodayWorkout>()
            
            // Load from training plan
            val todayWorkout = trainingPlanRepository.getTodaysWorkout()
            todayWorkout?.let { workout ->
                todayWorkouts.add(
                    TodayWorkout(
                        id = workout.hashCode().toLong(),
                        name = workout.workoutType.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        description = workout.description,
                        type = "running",
                        completed = workout.isCompleted
                    )
                )
            }
            
            _uiState.update { it.copy(todayWorkouts = todayWorkouts) }
        }
    }

    private fun loadNutritionData() {
        viewModelScope.launch {
            // Load today's nutrition data (includes targets and consumed)
            nutritionRepository.getTodayNutrition().collect { nutrition ->
                if (nutrition != null && (nutrition.consumedCalories > 0 || nutrition.consumedProteinGrams > 0)) {
                    _uiState.update {
                        it.copy(
                            caloriesConsumed = nutrition.consumedCalories,
                            caloriesGoal = nutrition.targetCalories,
                            proteinConsumed = nutrition.consumedProteinGrams,
                            proteinGoal = nutrition.targetProteinGrams,
                            hasNutritionData = true
                        )
                    }
                } else {
                    _uiState.update { it.copy(hasNutritionData = false) }
                }
            }
        }
        
        viewModelScope.launch {
            // Load user profile for name
            userRepository.getProfile().collect { profile ->
                profile?.let { p ->
                    _uiState.update {
                        it.copy(userName = p.name.ifEmpty { "Athlete" })
                    }
                }
            }
        }
    }

    private fun loadWeeklyStats() {
        viewModelScope.launch {
            val weekStart = TimeUtils.getStartOfWeek()

            // Get runs this week
            runRepository.getAllRuns().collect { allRuns ->
                val weeklyRuns = allRuns.filter { it.startTime >= weekStart }
                val totalDistance = weeklyRuns.sumOf { it.distanceMeters }.toFloat()

                _uiState.update {
                    it.copy(
                        weeklyStats = it.weeklyStats.copy(
                            runCount = weeklyRuns.size,
                            totalRunDistance = totalDistance
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            val weekStart = TimeUtils.getStartOfWeek()

            // Get gym workouts this week
            gymRepository.getAllWorkouts().collect { allWorkouts ->
                val weeklyGymWorkouts = allWorkouts.filter { it.startTime >= weekStart }
                val totalSets = weeklyGymWorkouts.sumOf { workout ->
                    workout.exercises.sumOf { it.sets.size }
                }

                _uiState.update {
                    it.copy(
                        weeklyStats = it.weeklyStats.copy(
                            gymCount = weeklyGymWorkouts.size,
                            totalSets = totalSets
                        )
                    )
                }
            }
        }
    }

    private fun loadReadinessScore() {
        viewModelScope.launch {
            val factors = mutableListOf<ReadinessFactor>()

            // Sleep — from Health Connect (Pixel Watch, Samsung, etc.)
            val sleepData = try { healthConnectManager.getLastNightSleep() } catch (_: Exception) { null }
            val sleepStatus = sleepData?.quality ?: "No data"
            val sleepDisplay = if (sleepData != null) "${sleepStatus} (${sleepData.formatted})" else "No data"
            factors.add(ReadinessFactor("Sleep", sleepDisplay))

            // Recovery factor based on recent workouts
            val recentWorkouts = _uiState.value.weeklyStats.runCount + _uiState.value.weeklyStats.gymCount
            val recoveryStatus = when {
                recentWorkouts <= 2 -> "Good"
                recentWorkouts <= 4 -> "Moderate"
                else -> "Low"
            }
            factors.add(ReadinessFactor("Recovery", recoveryStatus))

            // Nutrition factor — only score if user has logged something
            val hasNutrition = _uiState.value.hasNutritionData
            val nutritionStatus = if (!hasNutrition) {
                "No data"
            } else if (_uiState.value.caloriesConsumed >= _uiState.value.caloriesGoal * 0.8) {
                "Good"
            } else {
                "Moderate"
            }
            factors.add(ReadinessFactor("Nutrition", nutritionStatus))

            // Calculate overall score — only from factors that have real data
            // Use raw quality values for scoring (not the display strings)
            val rawScores = listOf(sleepStatus, recoveryStatus, nutritionStatus)
            val scoredValues = rawScores.filter { it != "No data" }
            val score = if (scoredValues.isEmpty()) {
                50 // neutral default when no data
            } else {
                var total = 0
                scoredValues.forEach { status ->
                    total += when (status) {
                        "Good" -> 33
                        "Moderate" -> 22
                        else -> 11
                    }
                }
                // Scale to 99 based on available factors
                (total.toDouble() / scoredValues.size * 3).toInt().coerceIn(0, 99)
            }

            val recommendation = when {
                scoredValues.isEmpty() -> "Log your meals and workouts for personalised readiness insights."
                score >= 80 -> "You're ready for a high-intensity workout today!"
                score >= 60 -> "Consider a moderate workout. Listen to your body."
                else -> "Focus on recovery today. Light activity or rest recommended."
            }

            _uiState.update {
                it.copy(
                    readinessScore = score,
                    readinessFactors = factors,
                    aiRecommendation = recommendation
                )
            }
        }
    }

    fun refresh() {
        loadData()
    }
}

data class HomeUiState(
    val userName: String = "",
    val todayWorkouts: List<TodayWorkout> = emptyList(),
    val weeklyStats: WeeklyStats = WeeklyStats(),
    val readinessScore: Int = 50,
    val readinessFactors: List<ReadinessFactor> = emptyList(),
    val aiRecommendation: String = "Loading...",
    val caloriesConsumed: Int = 0,
    val caloriesGoal: Int = 2000,
    val proteinConsumed: Int = 0,
    val proteinGoal: Int = 150,
    val hasNutritionData: Boolean = false,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val streakMessage: String = ""
)
