package com.runtracker.app.ui.screens.hiit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ActiveHIITWorkoutScreen(
    onComplete: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ActiveHIITWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStopDialog by remember { mutableStateOf(false) }

    // Navigate to summary when complete and session is saved
    LaunchedEffect(uiState.savedSessionId) {
        uiState.savedSessionId?.let { sessionId ->
            onComplete(sessionId)
        }
    }

    val phaseColor by animateColorAsState(
        targetValue = when (uiState.phase) {
            HIITPhase.WORK -> Color(0xFF4CAF50)
            HIITPhase.REST -> Color(0xFF2196F3)
            HIITPhase.WARMUP, HIITPhase.COOLDOWN -> Color(0xFFFFC107)
            HIITPhase.COMPLETE -> HIITOrange
            HIITPhase.PAUSED -> Color(0xFF9E9E9E)
        },
        animationSpec = tween(300)
    )

    val progressAnim by animateFloatAsState(
        targetValue = uiState.phaseProgress,
        animationSpec = tween(900)
    )

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Stop Workout?") },
            text = { Text("Your progress will be saved.") },
            confirmButton = {
                TextButton(onClick = {
                    showStopDialog = false
                    viewModel.stopWorkout()
                }) {
                    Text("Stop", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Continue")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Phase label + round/exercise info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = phaseColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = when (uiState.phase) {
                            HIITPhase.WORK -> "WORK"
                            HIITPhase.REST -> "REST"
                            HIITPhase.WARMUP -> "WARM UP"
                            HIITPhase.COOLDOWN -> "COOL DOWN"
                            HIITPhase.COMPLETE -> "COMPLETE"
                            HIITPhase.PAUSED -> "PAUSED"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!uiState.isComplete) {
                    Text(
                        text = uiState.roundProgress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Center: Timer ring + exercise name
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(260.dp)
            ) {
                // Progress ring
                Canvas(modifier = Modifier.size(260.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    )

                    // Background ring
                    drawArc(
                        color = phaseColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = phaseColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progressAnim,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Timer
                    val mins = uiState.remainingSeconds / 60
                    val secs = uiState.remainingSeconds % 60
                    Text(
                        text = String.format("%d:%02d", mins, secs),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor
                    )

                    // Exercise name
                    Text(
                        text = uiState.currentExerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Exercise description
                    if (uiState.currentExerciseDescription.isNotEmpty() && uiState.phase == HIITPhase.WORK) {
                        Text(
                            text = uiState.currentExerciseDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Bottom: Next exercise + controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Next exercise preview
                if (uiState.nextExerciseName != null && !uiState.isComplete) {
                    Text(
                        text = "Next: ${uiState.nextExerciseName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Calories
                if (uiState.caloriesEstimate > 0) {
                    Text(
                        text = "${uiState.caloriesEstimate} cal burned",
                        style = MaterialTheme.typography.bodySmall,
                        color = HIITOrange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Control buttons
                if (!uiState.isComplete) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stop button
                        FilledTonalIconButton(
                            onClick = { showStopDialog = true },
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xFFE53935).copy(alpha = 0.15f)
                            )
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = Color(0xFFE53935)
                            )
                        }

                        // Pause/Resume button
                        FloatingActionButton(
                            onClick = { viewModel.togglePause() },
                            containerColor = phaseColor,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (uiState.isPaused) "Resume" else "Pause",
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
