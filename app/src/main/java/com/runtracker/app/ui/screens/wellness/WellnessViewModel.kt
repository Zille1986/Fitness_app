package com.runtracker.app.ui.screens.wellness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.GamificationRepository
import com.runtracker.shared.data.repository.MentalHealthRepository
import com.runtracker.shared.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.runtracker.shared.util.TimeUtils
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WellnessViewModel @Inject constructor(
    private val runRepository: RunRepository,
    private val gamificationRepository: GamificationRepository,
    private val mentalHealthRepository: MentalHealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WellnessUiState())
    val uiState: StateFlow<WellnessUiState> = _uiState.asStateFlow()

    init {
        loadWellnessData()
    }

    private fun loadWellnessData() {
        viewModelScope.launch {
            // Load gamification data
            gamificationRepository.getUserGamification().collect { gamification ->
                _uiState.update { it.copy(gamification = gamification) }
                calculateOverallWellness()
            }
        }
        
        viewModelScope.launch {
            // Load today's rings
            val rings = gamificationRepository.getTodayRings()
            _uiState.update { it.copy(todayRings = rings) }
            calculateOverallWellness()
        }
        
        viewModelScope.launch {
            // Load recent wellness check-ins
            mentalHealthRepository.getRecentWellnessCheckins(7).collect { checkins ->
                _uiState.update { it.copy(recentCheckins = checkins) }
                calculateOverallWellness()
            }
        }
        
        viewModelScope.launch {
            // Load today's check-in
            val todayCheckin = mentalHealthRepository.getTodayCheckin()
            _uiState.update { it.copy(todayCheckin = todayCheckin) }
            calculateOverallWellness()
        }
        
        viewModelScope.launch {
            // Load recent runs for training load
            val weekAgo = System.currentTimeMillis() - TimeUtils.ONE_WEEK_MS
            runRepository.getRunsSince(weekAgo).collect { runs ->
                _uiState.update { it.copy(recentRuns = runs) }
                calculateTrainingLoad(runs)
                calculateOverallWellness()
            }
        }
        
        viewModelScope.launch {
            // Load mindfulness stats
            val mindfulnessMinutes = mentalHealthRepository.getTotalMindfulnessMinutesThisWeek()
            _uiState.update { it.copy(mindfulnessMinutesThisWeek = mindfulnessMinutes) }
        }
    }

    private fun calculateTrainingLoad(runs: List<Run>) {
        if (runs.isEmpty()) {
            _uiState.update { it.copy(trainingLoad = TrainingLoad()) }
            return
        }
        
        val now = System.currentTimeMillis()
        val weekAgo = now - TimeUtils.ONE_WEEK_MS
        val twoWeeksAgo = now - (2 * TimeUtils.ONE_WEEK_MS)
        
        // Acute load (last 7 days)
        val acuteRuns = runs.filter { it.startTime >= weekAgo }
        val acuteDistance = acuteRuns.sumOf { it.distanceMeters }
        val acuteDuration = acuteRuns.sumOf { it.durationMillis }
        
        // Calculate training stress score (simplified)
        val acuteLoad = acuteRuns.sumOf { run ->
            val durationHours = run.durationMillis / 3600000.0
            val intensityFactor = when {
                run.avgPaceSecondsPerKm > 0 && run.avgPaceSecondsPerKm < 300 -> 1.5 // Fast pace
                run.avgPaceSecondsPerKm > 0 && run.avgPaceSecondsPerKm < 360 -> 1.2 // Moderate pace
                else -> 1.0 // Easy pace
            }
            (durationHours * intensityFactor * 100).toInt()
        }
        
        // Chronic load (average of last 4 weeks, simplified to 2 weeks here)
        val chronicLoad = (acuteLoad * 0.7).toInt() // Simplified calculation
        
        // Acute:Chronic ratio
        val acwr = if (chronicLoad > 0) acuteLoad.toFloat() / chronicLoad else 1f
        
        // Training status based on ACWR
        val status = when {
            acwr < 0.8 -> TrainingStatus.DETRAINING
            acwr in 0.8..1.3 -> TrainingStatus.OPTIMAL
            acwr in 1.3..1.5 -> TrainingStatus.HIGH_RISK
            else -> TrainingStatus.VERY_HIGH_RISK
        }
        
        _uiState.update { 
            it.copy(
                trainingLoad = TrainingLoad(
                    acuteLoad = acuteLoad,
                    chronicLoad = chronicLoad,
                    acuteChronicRatio = acwr,
                    status = status,
                    weeklyDistance = acuteDistance,
                    weeklyDuration = acuteDuration,
                    weeklyRuns = acuteRuns.size
                )
            ) 
        }
    }

    private fun calculateOverallWellness() {
        val state = _uiState.value
        
        var score = 50 // Base score
        var factors = 0
        
        // Training consistency (from gamification)
        state.gamification?.let { g ->
            factors++
            score += when {
                g.currentStreak >= 7 -> 15
                g.currentStreak >= 3 -> 10
                g.currentStreak >= 1 -> 5
                else -> -5
            }
        }
        
        // Daily activity (from rings)
        state.todayRings?.let { rings ->
            factors++
            val ringCompletion = (rings.moveProgress + rings.exerciseProgress) / 2
            score += (ringCompletion * 15).toInt()
        }
        
        // Readiness from check-in
        state.todayCheckin?.readinessScore?.let { readiness ->
            factors++
            score += ((readiness - 50) / 5) // -10 to +10
        }
        
        // Training load balance
        state.trainingLoad.let { load ->
            if (load.acuteLoad > 0) {
                factors++
                score += when (load.status) {
                    TrainingStatus.OPTIMAL -> 10
                    TrainingStatus.DETRAINING -> -5
                    TrainingStatus.HIGH_RISK -> -5
                    TrainingStatus.VERY_HIGH_RISK -> -15
                }
            }
        }
        
        // Mindfulness bonus
        if (state.mindfulnessMinutesThisWeek > 0) {
            factors++
            score += minOf(state.mindfulnessMinutesThisWeek / 2, 10)
        }
        
        val finalScore = score.coerceIn(0, 100)
        val status = when {
            finalScore >= 80 -> WellnessStatus.EXCELLENT
            finalScore >= 60 -> WellnessStatus.GOOD
            finalScore >= 40 -> WellnessStatus.FAIR
            else -> WellnessStatus.NEEDS_ATTENTION
        }
        
        _uiState.update { 
            it.copy(
                overallWellnessScore = finalScore,
                wellnessStatus = status,
                recommendations = generateRecommendations(state, finalScore)
            ) 
        }
    }

    private fun generateRecommendations(state: WellnessUiState, score: Int): List<WellnessRecommendation> {
        val recommendations = mutableListOf<WellnessRecommendation>()
        
        // Check-in recommendation
        if (state.todayCheckin == null) {
            recommendations.add(
                WellnessRecommendation(
                    icon = "📋",
                    title = "Complete Daily Check-in",
                    description = "Log your sleep, mood, and energy to get personalized insights",
                    priority = RecommendationPriority.HIGH
                )
            )
        }
        
        // Streak recommendation
        state.gamification?.let { g ->
            if (g.currentStreak == 0) {
                recommendations.add(
                    WellnessRecommendation(
                        icon = "🔥",
                        title = "Start Your Streak",
                        description = "Complete a workout today to begin building consistency",
                        priority = RecommendationPriority.MEDIUM
                    )
                )
            }
        }
        
        // Training load recommendations
        when (state.trainingLoad.status) {
            TrainingStatus.DETRAINING -> {
                recommendations.add(
                    WellnessRecommendation(
                        icon = "📈",
                        title = "Increase Training",
                        description = "Your training load is low. Consider adding more workouts this week.",
                        priority = RecommendationPriority.MEDIUM
                    )
                )
            }
            TrainingStatus.HIGH_RISK, TrainingStatus.VERY_HIGH_RISK -> {
                recommendations.add(
                    WellnessRecommendation(
                        icon = "⚠️",
                        title = "Recovery Needed",
                        description = "Your training load is high. Consider a rest day or easy workout.",
                        priority = RecommendationPriority.HIGH
                    )
                )
            }
            else -> {}
        }
        
        // Mindfulness recommendation
        if (state.mindfulnessMinutesThisWeek < 10) {
            recommendations.add(
                WellnessRecommendation(
                    icon = "🧘",
                    title = "Add Mindfulness",
                    description = "Try a breathing exercise to reduce stress and improve recovery",
                    priority = RecommendationPriority.LOW
                )
            )
        }
        
        // Sleep recommendation
        state.todayCheckin?.let { checkin ->
            checkin.sleepHours?.let { hours ->
                if (hours < 7) {
                    recommendations.add(
                        WellnessRecommendation(
                            icon = "😴",
                            title = "Prioritize Sleep",
                            description = "You got ${hours}h of sleep. Aim for 7-9 hours for optimal recovery.",
                            priority = RecommendationPriority.HIGH
                        )
                    )
                }
            }
        }
        
        return recommendations.sortedBy { it.priority.ordinal }
    }

    fun refresh() {
        loadWellnessData()
    }
}

data class WellnessUiState(
    val overallWellnessScore: Int = 50,
    val wellnessStatus: WellnessStatus = WellnessStatus.FAIR,
    val gamification: UserGamification? = null,
    val todayRings: DailyRings? = null,
    val todayCheckin: WellnessCheckin? = null,
    val recentCheckins: List<WellnessCheckin> = emptyList(),
    val recentRuns: List<Run> = emptyList(),
    val trainingLoad: TrainingLoad = TrainingLoad(),
    val mindfulnessMinutesThisWeek: Int = 0,
    val recommendations: List<WellnessRecommendation> = emptyList(),
    val isLoading: Boolean = false
)

data class TrainingLoad(
    val acuteLoad: Int = 0,
    val chronicLoad: Int = 0,
    val acuteChronicRatio: Float = 1f,
    val status: TrainingStatus = TrainingStatus.OPTIMAL,
    val weeklyDistance: Double = 0.0,
    val weeklyDuration: Long = 0,
    val weeklyRuns: Int = 0
)

enum class TrainingStatus(val label: String, val color: Long) {
    DETRAINING("Detraining", 0xFFFF9800),
    OPTIMAL("Optimal", 0xFF4CAF50),
    HIGH_RISK("High Risk", 0xFFFF9800),
    VERY_HIGH_RISK("Very High Risk", 0xFFF44336)
}

enum class WellnessStatus(val label: String, val emoji: String) {
    EXCELLENT("Excellent", "🌟"),
    GOOD("Good", "✅"),
    FAIR("Fair", "😐"),
    NEEDS_ATTENTION("Needs Attention", "⚠️")
}

data class WellnessRecommendation(
    val icon: String,
    val title: String,
    val description: String,
    val priority: RecommendationPriority
)

enum class RecommendationPriority {
    HIGH, MEDIUM, LOW
}
