package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

/**
 * User's gamification profile tracking XP, level, and streaks
 */
@Entity(tableName = "user_gamification")
@TypeConverters(Converters::class)
data class UserGamification(
    @PrimaryKey
    val id: Long = 1, // Single row for user
    val totalXp: Long = 0,
    val currentLevel: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActivityDate: Long? = null,
    val streakProtectionAvailable: Boolean = true,
    val totalWorkoutsCompleted: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val totalDurationMinutes: Int = 0,
    val weeklyWorkouts: Int = 0,
    val weekStartDate: Long = 0,
    val dailyGoalMinutes: Int = 30,
    val dailyGoalCompleted: Boolean = false,
    val weeklyGoalDays: Int = 4,
    val weeklyGoalProgress: Int = 0
) {
    val levelProgress: Float
        get() {
            val xpForCurrentLevel = xpRequiredForLevel(currentLevel)
            val xpForNextLevel = xpRequiredForLevel(currentLevel + 1)
            val xpInCurrentLevel = totalXp - xpForCurrentLevel
            val xpNeededForNext = xpForNextLevel - xpForCurrentLevel
            return (xpInCurrentLevel.toFloat() / xpNeededForNext).coerceIn(0f, 1f)
        }
    
    val xpToNextLevel: Long
        get() = xpRequiredForLevel(currentLevel + 1) - totalXp
    
    companion object {
        fun xpRequiredForLevel(level: Int): Long {
            // Exponential curve: each level requires more XP
            return when {
                level <= 1 -> 0
                else -> (100 * level * level * 0.5).toLong()
            }
        }
        
        fun levelForXp(xp: Long): Int {
            var level = 1
            while (xpRequiredForLevel(level + 1) <= xp) {
                level++
            }
            return level
        }
    }
}

/**
 * Achievement/Badge definition
 */
@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val category: AchievementCategory,
    val requirement: Int, // The threshold to unlock
    val xpReward: Int = 50,
    val isSecret: Boolean = false
)

/**
 * User's earned achievements
 */
@Entity(tableName = "user_achievements")
data class UserAchievement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val achievementId: String,
    val unlockedAt: Long = System.currentTimeMillis(),
    val progress: Int = 0, // Current progress toward achievement
    val isUnlocked: Boolean = false,
    val notificationShown: Boolean = false
)

enum class AchievementCategory {
    DISTANCE,       // Total distance milestones
    WORKOUTS,       // Number of workouts
    STREAKS,        // Streak achievements
    SPEED,          // Speed/pace achievements
    CONSISTENCY,    // Weekly/monthly consistency
    SOCIAL,         // Social interactions
    EXPLORATION,    // New routes, locations
    PERSONAL_BEST,  // PB achievements
    SPECIAL         // Special/seasonal achievements
}

/**
 * Daily activity ring progress
 */
@Entity(tableName = "daily_rings")
data class DailyRings(
    @PrimaryKey
    val date: Long, // Start of day timestamp
    val moveMinutes: Int = 0,
    val moveGoal: Int = 30,
    val exerciseMinutes: Int = 0,
    val exerciseGoal: Int = 30,
    val standHours: Int = 0,
    val standGoal: Int = 12,
    val caloriesBurned: Int = 0,
    val caloriesGoal: Int = 500,
    val distanceMeters: Double = 0.0,
    val distanceGoal: Double = 5000.0,
    val allRingsClosed: Boolean = false
) {
    val moveProgress: Float get() = (moveMinutes.toFloat() / moveGoal).coerceIn(0f, 1f)
    val exerciseProgress: Float get() = (exerciseMinutes.toFloat() / exerciseGoal).coerceIn(0f, 1f)
    val standProgress: Float get() = (standHours.toFloat() / standGoal).coerceIn(0f, 1f)
    val caloriesProgress: Float get() = (caloriesBurned.toFloat() / caloriesGoal).coerceIn(0f, 1f)
    val distanceProgress: Float get() = (distanceMeters / distanceGoal).toFloat().coerceIn(0f, 1f)
}

/**
 * XP transaction log
 */
@Entity(tableName = "xp_transactions")
data class XpTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Int,
    val reason: XpReason,
    val relatedId: Long? = null, // Run ID, achievement ID, etc.
    val timestamp: Long = System.currentTimeMillis()
)

enum class XpReason {
    WORKOUT_COMPLETED,
    STREAK_BONUS,
    ACHIEVEMENT_UNLOCKED,
    DAILY_GOAL_COMPLETED,
    WEEKLY_GOAL_COMPLETED,
    PERSONAL_BEST,
    CHALLENGE_COMPLETED,
    FIRST_WORKOUT,
    LEVEL_UP_BONUS
}

/**
 * Pre-defined achievements
 */
object DefaultAchievements {
    
    fun getAll(): List<Achievement> = distanceAchievements + workoutAchievements + 
            streakAchievements + speedAchievements + consistencyAchievements + pbAchievements
    
    val distanceAchievements = listOf(
        Achievement("dist_1k", "First Kilometer", "Run your first kilometer", "directions_run", AchievementCategory.DISTANCE, 1000, 25),
        Achievement("dist_5k", "5K Club", "Run a total of 5 kilometers", "directions_run", AchievementCategory.DISTANCE, 5000, 50),
        Achievement("dist_10k", "Double Digits", "Run a total of 10 kilometers", "directions_run", AchievementCategory.DISTANCE, 10000, 75),
        Achievement("dist_42k", "Marathon Distance", "Run a total of 42.195 kilometers", "emoji_events", AchievementCategory.DISTANCE, 42195, 150),
        Achievement("dist_100k", "Century Club", "Run a total of 100 kilometers", "military_tech", AchievementCategory.DISTANCE, 100000, 200),
        Achievement("dist_500k", "Road Warrior", "Run a total of 500 kilometers", "military_tech", AchievementCategory.DISTANCE, 500000, 500),
        Achievement("dist_1000k", "Thousand K Legend", "Run a total of 1000 kilometers", "workspace_premium", AchievementCategory.DISTANCE, 1000000, 1000)
    )
    
    val workoutAchievements = listOf(
        Achievement("workout_1", "First Steps", "Complete your first workout", "fitness_center", AchievementCategory.WORKOUTS, 1, 50),
        Achievement("workout_10", "Getting Started", "Complete 10 workouts", "fitness_center", AchievementCategory.WORKOUTS, 10, 100),
        Achievement("workout_25", "Quarter Century", "Complete 25 workouts", "fitness_center", AchievementCategory.WORKOUTS, 25, 150),
        Achievement("workout_50", "Halfway Hero", "Complete 50 workouts", "fitness_center", AchievementCategory.WORKOUTS, 50, 250),
        Achievement("workout_100", "Century Runner", "Complete 100 workouts", "emoji_events", AchievementCategory.WORKOUTS, 100, 500),
        Achievement("workout_250", "Dedicated Athlete", "Complete 250 workouts", "military_tech", AchievementCategory.WORKOUTS, 250, 750),
        Achievement("workout_500", "Elite Runner", "Complete 500 workouts", "workspace_premium", AchievementCategory.WORKOUTS, 500, 1000)
    )
    
    val streakAchievements = listOf(
        Achievement("streak_3", "Hat Trick", "Maintain a 3-day streak", "local_fire_department", AchievementCategory.STREAKS, 3, 30),
        Achievement("streak_7", "Week Warrior", "Maintain a 7-day streak", "local_fire_department", AchievementCategory.STREAKS, 7, 75),
        Achievement("streak_14", "Fortnight Fighter", "Maintain a 14-day streak", "local_fire_department", AchievementCategory.STREAKS, 14, 150),
        Achievement("streak_30", "Monthly Master", "Maintain a 30-day streak", "whatshot", AchievementCategory.STREAKS, 30, 300),
        Achievement("streak_60", "Two Month Titan", "Maintain a 60-day streak", "whatshot", AchievementCategory.STREAKS, 60, 500),
        Achievement("streak_100", "Century Streak", "Maintain a 100-day streak", "workspace_premium", AchievementCategory.STREAKS, 100, 1000),
        Achievement("streak_365", "Year of Dedication", "Maintain a 365-day streak", "workspace_premium", AchievementCategory.STREAKS, 365, 5000, isSecret = true)
    )
    
    val speedAchievements = listOf(
        Achievement("pace_6min", "Sub-6 Minute Mile", "Run a kilometer under 6:00 pace", "speed", AchievementCategory.SPEED, 360, 100),
        Achievement("pace_5min", "Sub-5 Minute Mile", "Run a kilometer under 5:00 pace", "speed", AchievementCategory.SPEED, 300, 200),
        Achievement("pace_4min", "Sub-4 Minute Mile", "Run a kilometer under 4:00 pace", "speed", AchievementCategory.SPEED, 240, 500, isSecret = true)
    )
    
    val consistencyAchievements = listOf(
        Achievement("weekly_3", "Tri-Weekly", "Work out 3 times in a week", "date_range", AchievementCategory.CONSISTENCY, 3, 50),
        Achievement("weekly_5", "Five-a-Week", "Work out 5 times in a week", "date_range", AchievementCategory.CONSISTENCY, 5, 100),
        Achievement("weekly_7", "Perfect Week", "Work out every day for a week", "date_range", AchievementCategory.CONSISTENCY, 7, 200),
        Achievement("monthly_20", "Monthly Dedication", "Complete 20 workouts in a month", "calendar_month", AchievementCategory.CONSISTENCY, 20, 300)
    )
    
    val pbAchievements = listOf(
        Achievement("pb_first", "Personal Best!", "Set your first personal best", "emoji_events", AchievementCategory.PERSONAL_BEST, 1, 100),
        Achievement("pb_5", "Record Breaker", "Set 5 personal bests", "emoji_events", AchievementCategory.PERSONAL_BEST, 5, 200),
        Achievement("pb_10", "PB Machine", "Set 10 personal bests", "emoji_events", AchievementCategory.PERSONAL_BEST, 10, 400)
    )
}

/**
 * XP rewards for different activities
 */
object XpRewards {
    const val WORKOUT_BASE = 50
    const val PER_KILOMETER = 10
    const val PER_MINUTE = 2
    const val STREAK_BONUS_PER_DAY = 5
    const val DAILY_GOAL_BONUS = 25
    const val WEEKLY_GOAL_BONUS = 100
    const val PERSONAL_BEST = 75
    const val FIRST_WORKOUT = 100
    
    fun calculateWorkoutXp(distanceMeters: Double, durationMinutes: Int, currentStreak: Int): Int {
        val distanceXp = (distanceMeters / 1000 * PER_KILOMETER).toInt()
        val durationXp = durationMinutes * PER_MINUTE
        val streakBonus = currentStreak * STREAK_BONUS_PER_DAY
        return WORKOUT_BASE + distanceXp + durationXp + streakBonus
    }
}
