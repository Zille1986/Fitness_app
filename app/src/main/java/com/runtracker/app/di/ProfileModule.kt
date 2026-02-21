package com.runtracker.app.di

import com.runtracker.shared.data.db.RunTrackerDatabase
import com.runtracker.shared.data.db.UserProfileDao
import com.runtracker.shared.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {

    @Provides
    @Singleton
    fun provideUserProfileDao(database: RunTrackerDatabase): UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    @Singleton
    fun provideUserRepository(userProfileDao: UserProfileDao): UserRepository {
        return UserRepository(userProfileDao)
    }
}
