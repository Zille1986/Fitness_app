package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.GamificationDao
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*

class GamificationRepository(
    private val gamificationDao: GamificationDao
) {
    // User Gamification
    fun getUserGamification(): Flow<UserGamification?> = gamificationDao.getUserGamification()
    
    suspend fun getOrCreateUserGamification(): UserGamification {
        val existing = gamificationDao.getUserGamificationOnce()
        if (existing != null) return existing
        
        val newGamification = UserGamification()
        gamificationDao.insertOrUpdateGamification(newGamification)
        return newGamification
    }
    
    suspend fun addXp(amount: Int, reason: XpReason, relatedId: Long? = null) {
        val current = getOrCreateUserGamification()
        val newTotalXp = current.totalXp + amount
        val newLevel = UserGamification.levelForXp(newTotalXp)
        
        gamificationDao.addXp(amount.toLong(), newLevel)
        gamificationDao.insertXpTransaction(XpTransaction(
            amount = amount,
            reason = reason,
            relatedId = relatedId
        ))
        
        // Check for level up bonus
        if (newLevel > current.currentLevel) {
            val levelUpBonus = newLevel * 25
            gamificationDao.addXp(levelUpBonus.toLong(), newLevel)
            gamificationDao.insertXpTransaction(XpTransaction(
                amount = levelUpBonus,
                reason = XpReason.LEVEL_UP_BONUS
            ))
        }
    }
    
    suspend fun recordWorkoutCompleted(distanceMeters: Double, durationMinutes: Int, runId: Long) {
        val gamification = getOrCreateUserGamification()
        
        // Update streak
        val today = getStartOfDay(System.currentTimeMillis())
        val lastActivity = gamification.lastActivityDate?.let { getStartOfDay(it) }
        val yesterday = getStartOfDay(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        
        val newStreak = when {
            lastActivity == today -> gamification.currentStreak // Already worked out today
            lastActivity == yesterday -> gamification.currentStreak + 1 // Continuing streak
            lastActivity == null -> 1 // First workout
            else -> 1 // Streak broken, start new
        }
        
        val newLongest = maxOf(newStreak, gamification.longestStreak)
        gamificationDao.updateStreak(newStreak, newLongest, System.currentTimeMillis())
        
        // Update workout stats
        gamificationDao.incrementWorkoutStats(distanceMeters, durationMinutes)
        
        // Calculate and add XP
        val xp = XpRewards.calculateWorkoutXp(distanceMeters, durationMinutes, newStreak)
        addXp(xp, XpReason.WORKOUT_COMPLETED, runId)
        
        // Update daily rings
        updateDailyRings(durationMinutes, distanceMeters)
        
        // Check achievements
        checkAchievements()
    }
    
    suspend fun updateDailyRings(exerciseMinutes: Int, distanceMeters: Double) {
        val today = getStartOfDay(System.currentTimeMillis())
        var rings = gamificationDao.getDailyRingsOnce(today)
        
        if (rings == null) {
            rings = DailyRings(date = today)
            gamificationDao.insertOrUpdateDailyRings(rings)
        }
        
        gamificationDao.addActivityMinutes(today, exerciseMinutes, exerciseMinutes)
        gamificationDao.addDistance(today, distanceMeters)
        
        // Check if all rings closed
        val updatedRings = gamificationDao.getDailyRingsOnce(today)
        if (updatedRings != null) {
            val allClosed = updatedRings.moveProgress >= 1f && 
                           updatedRings.exerciseProgress >= 1f
            if (allClosed && !updatedRings.allRingsClosed) {
                gamificationDao.setAllRingsClosed(today, true)
                addXp(XpRewards.DAILY_GOAL_BONUS, XpReason.DAILY_GOAL_COMPLETED)
            }
        }
    }
    
    suspend fun checkAchievements() {
        val gamification = getOrCreateUserGamification()
        val achievements = gamificationDao.getAllAchievementsOnce()
        
        for (achievement in achievements) {
            val userAchievement = gamificationDao.getUserAchievement(achievement.id)
            if (userAchievement?.isUnlocked == true) continue
            
            val currentProgress = when (achievement.category) {
                AchievementCategory.DISTANCE -> (gamification.totalDistanceMeters / 1000).toInt()
                AchievementCategory.WORKOUTS -> gamification.totalWorkoutsCompleted
                AchievementCategory.STREAKS -> gamification.longestStreak
                AchievementCategory.CONSISTENCY -> gamification.weeklyWorkouts
                else -> userAchievement?.progress ?: 0
            }
            
            val isUnlocked = currentProgress >= achievement.requirement
            
            if (userAchievement == null) {
                gamificationDao.insertUserAchievement(UserAchievement(
                    achievementId = achievement.id,
                    progress = currentProgress,
                    isUnlocked = isUnlocked,
                    unlockedAt = if (isUnlocked) System.currentTimeMillis() else 0
                ))
            } else if (currentProgress != userAchievement.progress || isUnlocked != userAchievement.isUnlocked) {
                gamificationDao.updateAchievementProgress(
                    achievement.id,
                    currentProgress,
                    isUnlocked,
                    if (isUnlocked && !userAchievement.isUnlocked) System.currentTimeMillis() else userAchievement.unlockedAt
                )
            }
            
            // Award XP for newly unlocked achievement
            if (isUnlocked && userAchievement?.isUnlocked != true) {
                addXp(achievement.xpReward, XpReason.ACHIEVEMENT_UNLOCKED)
            }
        }
    }
    
    suspend fun recordPersonalBest(runId: Long) {
        addXp(XpRewards.PERSONAL_BEST, XpReason.PERSONAL_BEST, runId)
        
        // Update PB achievement progress
        val pbAchievements = listOf("pb_first", "pb_5", "pb_10")
        for (achievementId in pbAchievements) {
            val userAchievement = gamificationDao.getUserAchievement(achievementId)
            val newProgress = (userAchievement?.progress ?: 0) + 1
            val achievement = gamificationDao.getAchievementById(achievementId) ?: continue
            val isUnlocked = newProgress >= achievement.requirement
            
            gamificationDao.updateAchievementProgress(
                achievementId,
                newProgress,
                isUnlocked,
                if (isUnlocked) System.currentTimeMillis() else 0
            )
            
            if (isUnlocked && userAchievement?.isUnlocked != true) {
                addXp(achievement.xpReward, XpReason.ACHIEVEMENT_UNLOCKED)
            }
        }
    }
    
    // Achievements
    fun getAllAchievements(): Flow<List<Achievement>> = gamificationDao.getAllAchievements()
    fun getUserAchievements(): Flow<List<UserAchievement>> = gamificationDao.getAllUserAchievements()
    fun getUnlockedAchievements(): Flow<List<UserAchievement>> = gamificationDao.getUnlockedAchievements()
    
    suspend fun getUnshownAchievementNotifications(): List<UserAchievement> = 
        gamificationDao.getUnshownAchievementNotifications()
    
    suspend fun markAchievementNotificationShown(achievementId: String) {
        gamificationDao.markAchievementNotificationShown(achievementId)
    }
    
    // Daily Rings
    fun getDailyRings(date: Long): Flow<DailyRings?> = gamificationDao.getDailyRings(date)
    fun getRecentDailyRings(days: Int = 7): Flow<List<DailyRings>> = gamificationDao.getRecentDailyRings(days)
    
    suspend fun getTodayRings(): DailyRings {
        val today = getStartOfDay(System.currentTimeMillis())
        var rings = gamificationDao.getDailyRingsOnce(today)
        if (rings == null) {
            rings = DailyRings(date = today)
            gamificationDao.insertOrUpdateDailyRings(rings)
        }
        return rings
    }
    
    // XP Transactions
    fun getRecentXpTransactions(limit: Int = 20): Flow<List<XpTransaction>> = 
        gamificationDao.getRecentXpTransactions(limit)
    
    // Seed default achievements
    suspend fun seedDefaultAchievements() {
        val existing = gamificationDao.getAllAchievementsOnce()
        if (existing.isEmpty()) {
            gamificationDao.insertAchievements(DefaultAchievements.getAll())
        }
    }
    
    // Helper
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
