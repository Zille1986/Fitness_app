package com.runtracker.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.runtracker.app.MainActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProviders
import androidx.compose.ui.graphics.Color as ComposeColor

class StatsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        val weeklyDistance = prefs.getFloat("weekly_distance", 0f)
        val weeklyRuns = prefs.getInt("weekly_runs", 0)
        val todaySteps = prefs.getInt("today_steps", 0)
        val todayCalories = prefs.getInt("today_calories", 0)

        provideContent {
            StatsWidgetContent(
                weeklyDistance = weeklyDistance,
                weeklyRuns = weeklyRuns,
                todaySteps = todaySteps,
                todayCalories = todayCalories
            )
        }
    }
}

@Composable
fun StatsWidgetContent(
    weeklyDistance: Float,
    weeklyRuns: Int,
    todaySteps: Int,
    todayCalories: Int
) {
    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                // Header
                Text(
                    text = "RunTracker",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GlanceTheme.colors.primary
                    )
                )

                Spacer(modifier = GlanceModifier.height(12.dp))

                // Stats Grid
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatItem(
                        value = String.format("%.1f", weeklyDistance),
                        unit = "km",
                        label = "This Week",
                        modifier = GlanceModifier.defaultWeight()
                    )
                    StatItem(
                        value = weeklyRuns.toString(),
                        unit = "",
                        label = "Runs",
                        modifier = GlanceModifier.defaultWeight()
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatItem(
                        value = formatSteps(todaySteps),
                        unit = "",
                        label = "Steps",
                        modifier = GlanceModifier.defaultWeight()
                    )
                    StatItem(
                        value = todayCalories.toString(),
                        unit = "cal",
                        label = "Burned",
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    unit: String,
    label: String,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = GlanceTheme.colors.onSurface
                )
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        }
        Text(
            text = label,
            style = TextStyle(
                fontSize = 11.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
    }
}

private fun formatSteps(steps: Int): String {
    return if (steps >= 1000) {
        String.format("%.1fk", steps / 1000f)
    } else {
        steps.toString()
    }
}

class StatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatsWidget()
}
