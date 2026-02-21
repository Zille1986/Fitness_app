package com.runtracker.app.ui.screens.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.GamificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GamificationViewModel @Inject constructor(
    private val repository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamificationUiState())
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()

    private val _newAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    val newAchievements: StateFlow<List<Achievement>> = _newAchievements.asStateFlow()

    init {
        loadGamificationData()
        seedAchievements()
        checkForNewAchievements()
    }

    private fun seedAchievements() {
        viewModelScope.launch {
            repository.seedDefaultAchievements()
        }
    }

    private fun loadGamificationData() {
        viewModelScope.launch {
            // Load user gamification
            repository.getUserGamification().collect { gamification ->
                _uiState.update { it.copy(gamification = gamification) }
            }
        }
        
        viewModelScope.launch {
            // Load today's rings
            val rings = repository.getTodayRings()
            _uiState.update { it.copy(todayRings = rings) }
        }
        
        viewModelScope.launch {
            // Load recent rings for week view
            repository.getRecentDailyRings(7).collect { rings ->
                _uiState.update { it.copy(weeklyRings = rings) }
            }
        }
        
        viewModelScope.launch {
            // Load all achievements with progress
            combine(
                repository.getAllAchievements(),
                repository.getUserAchievements()
            ) { achievements, userAchievements ->
                achievements.map { achievement ->
                    val userProgress = userAchievements.find { it.achievementId == achievement.id }
                    AchievementWithProgress(
                        achievement = achievement,
                        progress = userProgress?.progress ?: 0,
                        isUnlocked = userProgress?.isUnlocked ?: false,
                        unlockedAt = userProgress?.unlockedAt
                    )
                }
            }.collect { achievementsWithProgress ->
                _uiState.update { it.copy(achievements = achievementsWithProgress) }
            }
        }
        
        viewModelScope.launch {
            // Load recent XP transactions
            repository.getRecentXpTransactions(20).collect { transactions ->
                _uiState.update { it.copy(recentXp = transactions) }
            }
        }
    }

    private fun checkForNewAchievements() {
        viewModelScope.launch {
            val unshown = repository.getUnshownAchievementNotifications()
            if (unshown.isNotEmpty()) {
                val achievements = _uiState.value.achievements
                    .filter { ap -> unshown.any { it.achievementId == ap.achievement.id } }
                    .map { it.achievement }
                _newAchievements.value = achievements
            }
        }
    }

    fun dismissAchievementNotification(achievementId: String) {
        viewModelScope.launch {
            repository.markAchievementNotificationShown(achievementId)
            _newAchievements.update { list -> list.filter { it.id != achievementId } }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val rings = repository.getTodayRings()
            _uiState.update { it.copy(todayRings = rings) }
        }
    }
}

data class GamificationUiState(
    val gamification: UserGamification? = null,
    val todayRings: DailyRings? = null,
    val weeklyRings: List<DailyRings> = emptyList(),
    val achievements: List<AchievementWithProgress> = emptyList(),
    val recentXp: List<XpTransaction> = emptyList(),
    val isLoading: Boolean = false
) {
    val unlockedAchievements: List<AchievementWithProgress>
        get() = achievements.filter { it.isUnlocked }
    
    val lockedAchievements: List<AchievementWithProgress>
        get() = achievements.filter { !it.isUnlocked && !it.achievement.isSecret }
    
    val achievementsByCategory: Map<AchievementCategory, List<AchievementWithProgress>>
        get() = achievements.groupBy { it.achievement.category }
}

data class AchievementWithProgress(
    val achievement: Achievement,
    val progress: Int,
    val isUnlocked: Boolean,
    val unlockedAt: Long?
) {
    val progressPercent: Float
        get() = (progress.toFloat() / achievement.requirement).coerceIn(0f, 1f)
}
