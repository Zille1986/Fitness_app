package com.runtracker.wear.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*

@Composable
fun ActivitySelectScreen(
    onSelectRunning: () -> Unit,
    onSelectSwimming: () -> Unit,
    onSelectCycling: () -> Unit,
    hasPendingWorkout: Boolean,
    onViewPendingWorkout: () -> Unit,
    onMusicControl: () -> Unit = {}
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            autoCentering = AutoCenteringParams(itemIndex = 0),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState)
        ) {
            item {
                Text(
                    text = "Start Workout",
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Pending workout from phone
            if (hasPendingWorkout) {
                item {
                    Chip(
                        onClick = onViewPendingWorkout,
                        label = {
                            Column {
                                Text(
                                    text = "📱 From Phone",
                                    style = MaterialTheme.typography.body1,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "View synced workout",
                                    style = MaterialTheme.typography.caption2,
                                    color = MaterialTheme.colors.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = ChipDefaults.primaryChipColors(
                            backgroundColor = WearColors.Success
                        )
                    )
                }
            }

            // Running
            item {
                ActivityChip(
                    emoji = "🏃",
                    title = "Running",
                    subtitle = "Outdoor & treadmill",
                    onClick = onSelectRunning,
                    colors = ChipDefaults.secondaryChipColors()
                )
            }

            // Swimming
            item {
                ActivityChip(
                    emoji = "🏊",
                    title = "Swimming",
                    subtitle = "Pool & open water",
                    onClick = onSelectSwimming,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = WearColors.Swimming.copy(alpha = 0.3f)
                    )
                )
            }

            // Cycling
            item {
                ActivityChip(
                    emoji = "🚴",
                    title = "Cycling",
                    subtitle = "Outdoor & indoor",
                    onClick = onSelectCycling,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = WearColors.Cycling.copy(alpha = 0.3f)
                    )
                )
            }

            // Music Controls
            item {
                ActivityChip(
                    emoji = "🎵",
                    title = "Music",
                    subtitle = "Control playback",
                    onClick = onMusicControl,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = WearColors.Music.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

@Composable
private fun ActivityChip(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    colors: ChipColors
) {
    Chip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = emoji, fontSize = 24.sp)
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = colors
    )
}
