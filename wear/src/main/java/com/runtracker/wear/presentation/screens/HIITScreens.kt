package com.runtracker.wear.presentation.screens

import androidx.compose.foundation.layout.*
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
import com.runtracker.shared.data.model.HIITExerciseLibrary
import com.runtracker.shared.data.model.HIITWorkoutTemplate

val HIITColor = Color(0xFFFF6D00)

@Composable
fun HIITHomeScreen(
    onStartHIIT: (String) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    val templates = HIITExerciseLibrary.allTemplates

    val config = LocalConfiguration.current
    val horizontalPad = if (config.isScreenRound) 14.dp else 8.dp

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        onDismissed = { onBack() }
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
                item {
                    Text(
                        text = "\uD83D\uDD25 HIIT",
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = HIITColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    Text(
                        text = "Choose a Workout",
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                templates.forEach { template ->
                    item {
                        HIITTemplateChip(
                            template = template,
                            onClick = { onStartHIIT(template.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HIITTemplateChip(
    template: HIITWorkoutTemplate,
    onClick: () -> Unit
) {
    Chip(
        onClick = onClick,
        label = {
            Column {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${template.formattedDuration} \u2022 ${template.exercises.size} exercises \u2022 ${template.rounds}x",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ChipDefaults.chipColors(
            backgroundColor = HIITColor.copy(alpha = 0.3f)
        )
    )
}
