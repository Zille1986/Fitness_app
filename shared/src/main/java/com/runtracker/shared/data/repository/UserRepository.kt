package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.UserProfileDao
import com.runtracker.shared.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userProfileDao: UserProfileDao) {

    fun getProfile(): Flow<UserProfile?> = userProfileDao.getProfileFlow()

    suspend fun getProfileOnce(): UserProfile? = userProfileDao.getProfile()

    suspend fun saveProfile(profile: UserProfile) {
        val existing = userProfileDao.getProfile()
        if (existing != null) {
            userProfileDao.updateProfile(profile.copy(updatedAt = System.currentTimeMillis()))
        } else {
            userProfileDao.insertProfile(profile)
        }
    }

    suspend fun updateStravaTokens(
        accessToken: String?,
        refreshToken: String?,
        expiry: Long?,
        athleteId: String?
    ) {
        userProfileDao.updateStravaTokens(accessToken, refreshToken, expiry, athleteId)
    }

    suspend fun clearStravaConnection() {
        userProfileDao.updateStravaTokens(null, null, null, null)
    }

    suspend fun ensureProfileExists() {
        if (userProfileDao.getProfile() == null) {
            userProfileDao.insertProfile(UserProfile())
        }
    }
}
