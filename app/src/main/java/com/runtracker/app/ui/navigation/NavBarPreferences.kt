package com.runtracker.app.ui.navigation

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Persists the user's customized bottom navigation bar configuration.
 * Home is always first, More is always last. The user picks up to 4 middle tabs.
 */
class NavBarPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nav_bar_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SELECTED_ITEMS = "selected_items"
        val DEFAULT_ITEMS = listOf("Home", "Running", "Swimming", "Cycling", "Gym", "More")
        const val MAX_MIDDLE_TABS = 4
    }

    fun getSelectedItems(): List<String> {
        val json = prefs.getString(KEY_SELECTED_ITEMS, null) ?: return DEFAULT_ITEMS
        return try {
            val array = JSONArray(json)
            val items = mutableListOf<String>()
            for (i in 0 until array.length()) {
                items.add(array.getString(i))
            }
            // Always enforce Home first, More last
            if (items.firstOrNull() != "Home") items.add(0, "Home")
            if (items.lastOrNull() != "More") items.add("More")
            items.distinct()
        } catch (e: Exception) {
            DEFAULT_ITEMS
        }
    }

    fun saveSelectedItems(items: List<String>) {
        // Enforce constraints: Home first, More last
        val sanitized = mutableListOf("Home")
        val middle = items.filter { it != "Home" && it != "More" }.take(MAX_MIDDLE_TABS)
        sanitized.addAll(middle)
        sanitized.add("More")

        val array = JSONArray()
        sanitized.forEach { array.put(it) }
        prefs.edit().putString(KEY_SELECTED_ITEMS, array.toString()).apply()
    }

    fun getMiddleItems(): List<String> {
        return getSelectedItems().filter { it != "Home" && it != "More" }
    }
}
