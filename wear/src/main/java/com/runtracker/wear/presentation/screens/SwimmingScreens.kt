package com.runtracker.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*

@Composable
fun SwimmingHomeScreen(
    onStartSwim: (String, Int) -> Unit,
    onStartSwimWorkout: (SwimWorkoutTypeWatch, String, Int) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    var selectedSwimType by remember { mutableStateOf<String?>(null) }
    var showWorkouts by remember { mutableStateOf(false) }
    var showPoolLengthPicker by remember { mutableStateOf(false) }
    var selectedPoolLength by remember { mutableIntStateOf(25) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

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
                        Text("🏊 Swimming", style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold, color = WearColors.Swimming,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }

                    item {
                        Text("Quick Start", style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp))
                    }

                    // Pool length selector
                    item {
                        Chip(
                            onClick = { showPoolLengthPicker = true },
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Pool Length", style = MaterialTheme.typography.body1)
                                    Text("${selectedPoolLength}m", style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold, color = WearColors.Swimming)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ChipDefaults.secondaryChipColors()
                        )
                    }

                    item {
                        Chip(
                            onClick = { onStartSwim("POOL", selectedPoolLength) },
                            label = {
                                Column {
                                    Text("Pool Swim", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                    Text("Free swim • ${selectedPoolLength}m pool",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ChipDefaults.primaryChipColors(backgroundColor = WearColors.Swimming)
                        )
                    }

                    item {
                        Chip(
                            onClick = { onStartSwim("OPEN_WATER", 0) },
                            label = {
                                Column {
                                    Text("Open Water", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                    Text("Free swim • GPS tracking",
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
                            onClick = { selectedSwimType = "POOL"; showWorkouts = true },
                            label = {
                                Column {
                                    Text("Pool Workouts", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                    Text("Structured with HR guidance",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ChipDefaults.chipColors(backgroundColor = WearColors.Swimming.copy(alpha = 0.3f))
                        )
                    }

                    item {
                        Chip(
                            onClick = { selectedSwimType = "OPEN_WATER"; showWorkouts = true },
                            label = {
                                Column {
                                    Text("Open Water Workouts", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                    Text("Distance based with HR",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ChipDefaults.chipColors(backgroundColor = WearColors.Swimming.copy(alpha = 0.3f))
                        )
                    }
                } else {
                    item {
                        Text("Select Workout", style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold, color = WearColors.Swimming,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }

                    SwimWorkoutTypeWatch.values().forEach { workout ->
                        item {
                            Chip(
                                onClick = {
                                    onStartSwimWorkout(workout, selectedSwimType ?: "POOL", selectedPoolLength)
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

    // Pool length picker as full-screen scrollable list
    if (showPoolLengthPicker) {
        val poolLengths = listOf(15, 20, 25, 33, 50)
        val pickerListState = rememberScalingLazyListState()
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPoolLengthPicker = false }) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = pickerListState,
                horizontalAlignment = Alignment.CenterHorizontally,
                autoCentering = AutoCenteringParams(itemIndex = 0),
                contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 26.dp),
                flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = pickerListState)
            ) {
                item {
                    Text("Pool Length", style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(bottom = 4.dp))
                }
                poolLengths.forEach { length ->
                    item {
                        Chip(
                            onClick = {
                                selectedPoolLength = length
                                showPoolLengthPicker = false
                            },
                            label = { Text("${length}m", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = if (length == selectedPoolLength) ChipDefaults.primaryChipColors()
                            else ChipDefaults.secondaryChipColors()
                        )
                    }
                }
            }
        }
    }
}
