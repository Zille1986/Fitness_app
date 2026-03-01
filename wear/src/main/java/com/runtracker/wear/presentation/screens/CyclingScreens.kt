package com.runtracker.wear.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*

@Composable
fun CyclingHomeScreen(
    onStartCycling: (String) -> Unit,
    onStartCyclingWorkout: (CycleWorkoutTypeWatch, String) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    var selectedCycleType by remember { mutableStateOf<String?>(null) }
    var showWorkouts by remember { mutableStateOf(false) }

    val config = LocalConfiguration.current
    val horizontalPad = if (config.isScreenRound) 14.dp else 8.dp

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        onDismissed = {
            if (showWorkouts) showWorkouts = false else onBack()
        }
    ) { isBackground ->
        if (!isBackground) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                autoCentering = AutoCenteringParams(itemIndex = 0),
                contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 26.dp),
                flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState)
            ) {
                if (!showWorkouts) {
                    item {
                        Text("🚴 Cycling", style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold, color = WearColors.Cycling,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }

                    item {
                        Text("Quick Start", style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp))
                    }

                    item {
                        Chip(
                            onClick = { onStartCycling("OUTDOOR") },
                            label = {
                                Column {
                                    Text("Outdoor Ride", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                    Text("GPS tracking • Speed & distance",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ChipDefaults.primaryChipColors(backgroundColor = WearColors.Cycling)
                        )
                    }

                    item {
                        Chip(
                            onClick = { onStartCycling("INDOOR") },
                            label = {
                                Column {
                                    Text("Indoor Ride", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                    Text("Stationary bike • HR tracking",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ChipDefaults.secondaryChipColors()
                        )
                    }

                    item {
                        Chip(
                            onClick = { onStartCycling("SMART_TRAINER") },
                            label = {
                                Column {
                                    Text("Smart Trainer", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                    Text("Connected trainer • Power",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ChipDefaults.secondaryChipColors()
                        )
                    }

                    item {
                        Text("Workouts with HR Zones", style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp))
                    }

                    item {
                        Chip(
                            onClick = { selectedCycleType = "OUTDOOR"; showWorkouts = true },
                            label = {
                                Column {
                                    Text("Outdoor Workouts", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                    Text("Structured with HR guidance",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ChipDefaults.chipColors(backgroundColor = WearColors.Cycling.copy(alpha = 0.3f))
                        )
                    }

                    item {
                        Chip(
                            onClick = { selectedCycleType = "INDOOR"; showWorkouts = true },
                            label = {
                                Column {
                                    Text("Indoor Workouts", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                    Text("Trainer sessions with HR",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ChipDefaults.chipColors(backgroundColor = WearColors.Cycling.copy(alpha = 0.3f))
                        )
                    }
                } else {
                    item {
                        Text("Select Workout", style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold, color = WearColors.Cycling,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }

                    CycleWorkoutTypeWatch.values().forEach { workout ->
                        item {
                            Chip(
                                onClick = {
                                    onStartCyclingWorkout(workout, selectedCycleType ?: "OUTDOOR")
                                },
                                label = {
                                    Column {
                                        Text(workout.displayName, style = MaterialTheme.typography.body1,
                                            fontWeight = FontWeight.Bold)
                                        Text(workout.description,
                                            style = MaterialTheme.typography.caption2,
                                            color = MaterialTheme.colors.onSurfaceVariant)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = ChipDefaults.secondaryChipColors()
                            )
                        }
                    }
                }
            }
        }
    }
}
