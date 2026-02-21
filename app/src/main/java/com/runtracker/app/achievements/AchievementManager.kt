package com.runtracker.app.achievements

import android.content.Context
import android.content.SharedPreferences
import com.runtracker.app.notifications.AppNotificationManager
import com.runtracker.shared.data.repository.GymRepository
import com.runtracker.shared.data.repository.RunRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runRepository: RunRepository,
    private val gymRepository: GymRepository,
    private val notificationManager: AppNotificationManager
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)
    
    private val _unlockedBadges = MutableStateFlow<List<Badge>>(emptyList())
    val unlockedBadges: StateFlow<List<Badge>> = _unlockedBadges.asStateFlow()
    
    private val _recentlyUnlocked = MutableStateFlow<Badge?>(null)
    val recentlyUnlocked: StateFlow<Badge?> = _recentlyUnlocked.asStateFlow()

    init {
        loadUnlockedBadges()
    }

    private fun loadUnlockedBadges() {
        val unlocked = AllBadges.badges.filter { badge ->
            prefs.getBoolean(badge.id, false)
        }
        _unlockedBadges.value = unlocked
    }

    suspend fun checkAndUnlockBadges() {
        val runs = runRepository.getAllRunsOnce()
        val gymWorkouts = gymRepository.getAllWorkoutsOnce()
        
        // Calculate stats
        val totalRuns = runs.size
        val totalGymWorkouts = gymWorkouts.size
        val totalWorkouts = totalRuns + totalGymWorkouts
        val totalRunDistance = runs.sumOf { it.distanceMeters.toDouble() } / 1000.0 // km
        val totalRunTime = runs.sumOf { it.durationMillis / 1000 } / 60 // minutes
        
        // Calculate streak
        val workoutDates = mutableSetOf<String>()
        runs.forEach { run ->
            val cal = Calendar.getInstance().apply { timeInMillis = run.startTime }
            workoutDates.add("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}")
        }
        gymWorkouts.forEach { workout ->
            val cal = Calendar.getInstance().apply { timeInMillis = workout.startTime }
            workoutDates.add("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}")
        }
        
        var currentStreak = 0
        val today = Calendar.getInstance()
        val checkDate = Calendar.getInstance()
        for (i in 0..365) {
            checkDate.timeInMillis = today.timeInMillis
            checkDate.add(Calendar.DAY_OF_YEAR, -i)
            val dateKey = "${checkDate.get(Calendar.YEAR)}-${checkDate.get(Calendar.DAY_OF_YEAR)}"
            if (workoutDates.contains(dateKey)) {
                currentStreak++
            } else if (i > 0) {
                break
            }
        }
        
        // Check each badge
        AllBadges.badges.forEach { badge ->
            if (!prefs.getBoolean(badge.id, false)) {
                val shouldUnlock = when (badge.id) {
                    // First workout badges
                    "first_run" -> totalRuns >= 1
                    "first_gym" -> totalGymWorkouts >= 1
                    
                    // Workout count badges
                    "workouts_10" -> totalWorkouts >= 10
                    "workouts_50" -> totalWorkouts >= 50
                    "workouts_100" -> totalWorkouts >= 100
                    "workouts_500" -> totalWorkouts >= 500
                    
                    // Running distance badges
                    "run_5k" -> runs.any { it.distanceMeters >= 5000 }
                    "run_10k" -> runs.any { it.distanceMeters >= 10000 }
                    "run_half_marathon" -> runs.any { it.distanceMeters >= 21097 }
                    "run_marathon" -> runs.any { it.distanceMeters >= 42195 }
                    "total_100km" -> totalRunDistance >= 100
                    "total_500km" -> totalRunDistance >= 500
                    "total_1000km" -> totalRunDistance >= 1000
                    
                    // Streak badges
                    "streak_3" -> currentStreak >= 3
                    "streak_7" -> currentStreak >= 7
                    "streak_14" -> currentStreak >= 14
                    "streak_30" -> currentStreak >= 30
                    "streak_100" -> currentStreak >= 100
                    "streak_365" -> currentStreak >= 365
                    
                    // Time badges
                    "early_bird" -> runs.any { 
                        val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
                        cal.get(Calendar.HOUR_OF_DAY) < 7
                    }
                    "night_owl" -> runs.any {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
                        cal.get(Calendar.HOUR_OF_DAY) >= 21
                    }
                    
                    else -> false
                }
                
                if (shouldUnlock) {
                    unlockBadge(badge)
                }
            }
        }
    }

    private fun unlockBadge(badge: Badge) {
        prefs.edit().putBoolean(badge.id, true).apply()
        prefs.edit().putLong("${badge.id}_date", System.currentTimeMillis()).apply()
        
        _recentlyUnlocked.value = badge
        loadUnlockedBadges()
        
        // Show notification
        notificationManager.showBadgeEarned(badge.name, badge.description)
    }

    fun clearRecentlyUnlocked() {
        _recentlyUnlocked.value = null
    }

    fun getUnlockDate(badgeId: String): Long? {
        val date = prefs.getLong("${badgeId}_date", 0)
        return if (date > 0) date else null
    }

    fun getProgress(badgeId: String): Float {
        // Returns 0-1 progress for locked badges
        return when {
            prefs.getBoolean(badgeId, false) -> 1f
            else -> 0f // Would need stats to calculate actual progress
        }
    }
}

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String, // Emoji
    val category: BadgeCategory,
    val rarity: BadgeRarity
)

enum class BadgeCategory {
    FIRST_STEPS,
    CONSISTENCY,
    DISTANCE,
    STRENGTH,
    SPECIAL
}

enum class BadgeRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
}

object AllBadges {
    val badges = listOf(
        // First Steps
        Badge("first_run", "First Run", "Complete your first run", "🏃", BadgeCategory.FIRST_STEPS, BadgeRarity.COMMON),
        Badge("first_gym", "Iron Starter", "Complete your first gym workout", "🏋️", BadgeCategory.FIRST_STEPS, BadgeRarity.COMMON),
        
        // Consistency
        Badge("workouts_10", "Getting Started", "Complete 10 workouts", "⭐", BadgeCategory.CONSISTENCY, BadgeRarity.COMMON),
        Badge("workouts_50", "Dedicated", "Complete 50 workouts", "🌟", BadgeCategory.CONSISTENCY, BadgeRarity.UNCOMMON),
        Badge("workouts_100", "Committed", "Complete 100 workouts", "💫", BadgeCategory.CONSISTENCY, BadgeRarity.RARE),
        Badge("workouts_500", "Fitness Legend", "Complete 500 workouts", "🏆", BadgeCategory.CONSISTENCY, BadgeRarity.LEGENDARY),
        
        // Streaks
        Badge("streak_3", "On a Roll", "3 day workout streak", "🔥", BadgeCategory.CONSISTENCY, BadgeRarity.COMMON),
        Badge("streak_7", "Week Warrior", "7 day workout streak", "💪", BadgeCategory.CONSISTENCY, BadgeRarity.UNCOMMON),
        Badge("streak_14", "Fortnight Fighter", "14 day workout streak", "⚡", BadgeCategory.CONSISTENCY, BadgeRarity.RARE),
        Badge("streak_30", "Monthly Master", "30 day workout streak", "🎯", BadgeCategory.CONSISTENCY, BadgeRarity.EPIC),
        Badge("streak_100", "Unstoppable", "100 day workout streak", "👑", BadgeCategory.CONSISTENCY, BadgeRarity.LEGENDARY),
        Badge("streak_365", "Year of Iron", "365 day workout streak", "🏅", BadgeCategory.CONSISTENCY, BadgeRarity.LEGENDARY),
        
        // Running Distance
        Badge("run_5k", "5K Finisher", "Complete a 5K run", "🎽", BadgeCategory.DISTANCE, BadgeRarity.COMMON),
        Badge("run_10k", "10K Champion", "Complete a 10K run", "🏅", BadgeCategory.DISTANCE, BadgeRarity.UNCOMMON),
        Badge("run_half_marathon", "Half Marathon Hero", "Complete a half marathon", "🥈", BadgeCategory.DISTANCE, BadgeRarity.RARE),
        Badge("run_marathon", "Marathon Legend", "Complete a full marathon", "🥇", BadgeCategory.DISTANCE, BadgeRarity.EPIC),
        Badge("total_100km", "Century Club", "Run 100km total", "💯", BadgeCategory.DISTANCE, BadgeRarity.UNCOMMON),
        Badge("total_500km", "Road Warrior", "Run 500km total", "🛣️", BadgeCategory.DISTANCE, BadgeRarity.RARE),
        Badge("total_1000km", "Ultra Runner", "Run 1000km total", "🌍", BadgeCategory.DISTANCE, BadgeRarity.EPIC),
        
        // Special
        Badge("early_bird", "Early Bird", "Start a workout before 7 AM", "🌅", BadgeCategory.SPECIAL, BadgeRarity.UNCOMMON),
        Badge("night_owl", "Night Owl", "Start a workout after 9 PM", "🌙", BadgeCategory.SPECIAL, BadgeRarity.UNCOMMON)
    )
}
