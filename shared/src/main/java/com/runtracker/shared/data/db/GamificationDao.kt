package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationDao {
    
    // User Gamification
    @Query("SELECT * FROM user_gamification WHERE id = 1")
    fun getUserGamification(): Flow<UserGamification?>
    
    @Query("SELECT * FROM user_gamification WHERE id = 1")
    suspend fun getUserGamificationOnce(): UserGamification?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGamification(gamification: UserGamification)
    
    @Query("UPDATE user_gamification SET totalXp = totalXp + :xp, currentLevel = :level WHERE id = 1")
    suspend fun addXp(xp: Long, level: Int)
    
    @Query("UPDATE user_gamification SET currentStreak = :streak, longestStreak = :longest, lastActivityDate = :date WHERE id = 1")
    suspend fun updateStreak(streak: Int, longest: Int, date: Long)
    
    @Query("UPDATE user_gamification SET totalWorkoutsCompleted = totalWorkoutsCompleted + 1, totalDistanceMeters = totalDistanceMeters + :distance, totalDurationMinutes = totalDurationMinutes + :duration WHERE id = 1")
    suspend fun incrementWorkoutStats(distance: Double, duration: Int)
    
    @Query("UPDATE user_gamification SET dailyGoalCompleted = :completed WHERE id = 1")
    suspend fun setDailyGoalCompleted(completed: Boolean)
    
    @Query("UPDATE user_gamification SET weeklyGoalProgress = :progress WHERE id = 1")
    suspend fun setWeeklyGoalProgress(progress: Int)
    
    // Achievements
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>
    
    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsOnce(): List<Achievement>
    
    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievementById(id: String): Achievement?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)
    
    // User Achievements
    @Query("SELECT * FROM user_achievements")
    fun getAllUserAchievements(): Flow<List<UserAchievement>>
    
    @Query("SELECT * FROM user_achievements WHERE isUnlocked = 1")
    fun getUnlockedAchievements(): Flow<List<UserAchievement>>
    
    @Query("SELECT * FROM user_achievements WHERE achievementId = :achievementId")
    suspend fun getUserAchievement(achievementId: String): UserAchievement?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAchievement(userAchievement: UserAchievement)
    
    @Query("UPDATE user_achievements SET progress = :progress, isUnlocked = :unlocked, unlockedAt = :unlockedAt WHERE achievementId = :achievementId")
    suspend fun updateAchievementProgress(achievementId: String, progress: Int, unlocked: Boolean, unlockedAt: Long)
    
    @Query("UPDATE user_achievements SET notificationShown = 1 WHERE achievementId = :achievementId")
    suspend fun markAchievementNotificationShown(achievementId: String)
    
    @Query("SELECT * FROM user_achievements WHERE isUnlocked = 1 AND notificationShown = 0")
    suspend fun getUnshownAchievementNotifications(): List<UserAchievement>
    
    // Daily Rings
    @Query("SELECT * FROM daily_rings WHERE date = :date")
    fun getDailyRings(date: Long): Flow<DailyRings?>
    
    @Query("SELECT * FROM daily_rings WHERE date = :date")
    suspend fun getDailyRingsOnce(date: Long): DailyRings?
    
    @Query("SELECT * FROM daily_rings ORDER BY date DESC LIMIT :limit")
    fun getRecentDailyRings(limit: Int = 7): Flow<List<DailyRings>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyRings(rings: DailyRings)
    
    @Query("UPDATE daily_rings SET moveMinutes = moveMinutes + :minutes, exerciseMinutes = exerciseMinutes + :exerciseMinutes WHERE date = :date")
    suspend fun addActivityMinutes(date: Long, minutes: Int, exerciseMinutes: Int)
    
    @Query("UPDATE daily_rings SET caloriesBurned = caloriesBurned + :calories WHERE date = :date")
    suspend fun addCalories(date: Long, calories: Int)
    
    @Query("UPDATE daily_rings SET distanceMeters = distanceMeters + :distance WHERE date = :date")
    suspend fun addDistance(date: Long, distance: Double)
    
    @Query("UPDATE daily_rings SET allRingsClosed = :closed WHERE date = :date")
    suspend fun setAllRingsClosed(date: Long, closed: Boolean)
    
    @Query("SELECT COUNT(*) FROM daily_rings WHERE allRingsClosed = 1")
    suspend fun getTotalRingsClosedDays(): Int
    
    // XP Transactions
    @Insert
    suspend fun insertXpTransaction(transaction: XpTransaction)
    
    @Query("SELECT * FROM xp_transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentXpTransactions(limit: Int = 20): Flow<List<XpTransaction>>
    
    @Query("SELECT SUM(amount) FROM xp_transactions WHERE timestamp > :since")
    suspend fun getXpSince(since: Long): Int?
}
