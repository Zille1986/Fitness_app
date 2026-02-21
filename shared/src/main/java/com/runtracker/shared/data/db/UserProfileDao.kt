package com.runtracker.shared.data.db

import androidx.room.*
import com.runtracker.shared.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfile(): UserProfile?

    @Query("UPDATE user_profile SET stravaAccessToken = :accessToken, stravaRefreshToken = :refreshToken, stravaTokenExpiry = :expiry, stravaAthleteId = :athleteId WHERE id = 1")
    suspend fun updateStravaTokens(accessToken: String?, refreshToken: String?, expiry: Long?, athleteId: String?)
}
