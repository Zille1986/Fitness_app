package com.runtracker.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.runtracker.app.health.HealthConnectManager
import com.runtracker.shared.data.repository.RunRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetDataUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val healthConnectManager: HealthConnectManager,
    private val runRepository: RunRepository
) {
    
    suspend fun updateWidgetData() {
        withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
                
                // Get weekly stats
                val weekAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                val now = System.currentTimeMillis()
                
                val weeklyRuns = runRepository.getRunsInRangeOnce(weekAgo, now)
                val weeklyDistance = weeklyRuns.sumOf { it.distanceMeters } / 1000.0
                val weeklyRunCount = weeklyRuns.size
                val todayCalories = weeklyRuns
                    .filter { it.startTime >= getTodayStart() }
                    .sumOf { it.caloriesBurned }
                
                // Get steps from Health Connect
                var todaySteps = 0
                try {
                    healthConnectManager.checkAvailability()
                    if (healthConnectManager.hasAllPermissions()) {
                        todaySteps = healthConnectManager.getTodaySteps()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WidgetDataUpdater", "Failed to get steps", e)
                }
                
                // Save to SharedPreferences
                prefs.edit().apply {
                    putFloat("weekly_distance", weeklyDistance.toFloat())
                    putInt("weekly_runs", weeklyRunCount)
                    putInt("today_steps", todaySteps)
                    putInt("today_calories", todayCalories)
                    apply()
                }
                
                // Update the widget
                StatsWidget().updateAll(context)
                
            } catch (e: Exception) {
                android.util.Log.e("WidgetDataUpdater", "Failed to update widget data", e)
            }
        }
    }
    
    private fun getTodayStart(): Long {
        return com.runtracker.shared.util.TimeUtils.getStartOfDay()
    }
}
