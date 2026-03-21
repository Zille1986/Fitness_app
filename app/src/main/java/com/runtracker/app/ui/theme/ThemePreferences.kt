package com.runtracker.app.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {

    private val isDarkModeKey = booleanPreferencesKey("is_dark_mode")

    val isDarkMode: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[isDarkModeKey] ?: true // Default to dark mode
    }

    suspend fun setDarkMode(isDark: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[isDarkModeKey] = isDark
        }
    }
}
