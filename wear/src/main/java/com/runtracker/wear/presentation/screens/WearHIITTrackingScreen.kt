package com.runtracker.wear.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.google.gson.Gson
import com.runtracker.shared.data.model.HIITExerciseLibrary
import com.runtracker.shared.data.model.HIITWorkoutTemplate
import com.runtracker.wear.audio.WearHIITAudioCueManager
import kotlinx.coroutines.delay

enum class WearHIITPhase {
    WARMUP, WORK, REST, COOLDOWN, COMPLETE
}

data class WearHIITState(
    val template: HIITWorkoutTemplate? = null,
    val phase: WearHIITPhase = WearHIITPhase.WARMUP,
    val currentExerciseIndex: Int = 0,
    val currentRound: Int = 1,
    val remainingSeconds: Int = 0,
    val phaseDurationSeconds: Int = 0,
    val totalElapsedMs: Long = 0,
    val isPaused: Boolean = false,
    val isComplete: Boolean = false,
    val currentExerciseName: String = "Get Ready!",
    val nextExerciseName: String? = null,
    val caloriesEstimate: Int = 0
) {
    val phaseProgress: Float
        get() = if (phaseDurationSeconds > 0) {
            1f - (remainingSeconds.toFloat() / phaseDurationSeconds.toFloat())
        } else 0f
}

@Composable
fun WearHIITTrackingScreen(
    templateId: String,
    onComplete: (String) -> Unit, // JSON of session data
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val audioCueManager = remember { WearHIITAudioCueManager(context) }
    val template = remember { HIITExerciseLibrary.getTemplateById(templateId) }

    var state by remember {
        mutableStateOf(
            WearHIITState(
                template = template,
                phase = WearHIITPhase.WARMUP,
                remainingSeconds = template?.warmupSec ?: 10,
                phaseDurationSeconds = template?.warmupSec ?: 10,
                currentExerciseName = "Warm Up",
                nextExerciseName = template?.exercises?.firstOrNull()?.exercise?.name
            )
        )
    }

    val startTimeMs = remember { System.currentTimeMillis() }
    var pausedAccumulatedMs by remember { mutableLongStateOf(0L) }
    var pauseStartMs by remember { mutableLongStateOf(0L) }

    // Play initial phase tone
    LaunchedEffect(Unit) {
        audioCueManager.playPhaseTone()
    }

    // Timer tick
    LaunchedEffect(state.isPaused, state.isComplete) {
        if (state.isPaused || state.isComplete) return@LaunchedEffect
        while (true) {
            delay(1000)
            if (state.isPaused || state.isComplete) break

            val newRemaining = state.remainingSeconds - 1
            val elapsed = System.currentTimeMillis() - startTimeMs - pausedAccumulatedMs

            // Audio/haptic cue
            audioCueManager.onTick(
                remainingSeconds = newRemaining,
                isWorkPhase = state.phase == WearHIITPhase.WORK
            )

            val calPerSec = when (state.phase) {
                WearHIITPhase.WORK -> 10.0 / 60.0
                WearHIITPhase.REST -> 4.0 / 60.0
                else -> 3.0 / 60.0
            }

            if (newRemaining <= 0) {
                state = advancePhase(state, audioCueManager)
                if (state.isComplete) {
                    val sessionJson = buildSessionJson(state, elapsed)
                    onComplete(sessionJson)
                }
            } else {
                state = state.copy(
                    remainingSeconds = newRemaining,
                    totalElapsedMs = elapsed,
                    caloriesEstimate = (elapsed / 1000.0 * calPerSec).toInt()
                        .coerceAtLeast(state.caloriesEstimate)
                )
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { audioCueManager.release() }
    }

    val phaseColor by animateColorAsState(
        targetValue = when (state.phase) {
            WearHIITPhase.WORK -> Color(0xFF4CAF50)
            WearHIITPhase.REST -> Color(0xFF2196F3)
            WearHIITPhase.WARMUP, WearHIITPhase.COOLDOWN -> Color(0xFFFFC107)
            WearHIITPhase.COMPLETE -> HIITColor
        },
        animationSpec = tween(300)
    )

    val progressAnim by animateFloatAsState(
        targetValue = state.phaseProgress,
        animationSpec = tween(900)
    )

    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        onDismissed = {
            if (state.isPaused) {
                // If paused, dismiss goes back
                audioCueManager.release()
                onBack()
            } else {
                // If running, pause first
                pauseStartMs = System.currentTimeMillis()
                state = state.copy(isPaused = true)
            }
        }
    ) { isBackground ->
        if (!isBackground) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Arc progress around the edge
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 6.dp.toPx()
                    val padding = 4.dp.toPx()
                    val radius = (size.minDimension - strokeWidth - padding * 2) / 2
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    )

                    // Background arc
                    drawArc(
                        color = phaseColor.copy(alpha = 0.2f),
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

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    // Phase label
                    Text(
                        text = when (state.phase) {
                            WearHIITPhase.WORK -> "WORK"
                            WearHIITPhase.REST -> "REST"
                            WearHIITPhase.WARMUP -> "WARM UP"
                            WearHIITPhase.COOLDOWN -> "COOL DOWN"
                            WearHIITPhase.COMPLETE -> "DONE!"
                        },
                        style = MaterialTheme.typography.caption1,
                        color = phaseColor,
                        fontWeight = FontWeight.Bold
                    )

                    // Timer
                    val mins = state.remainingSeconds / 60
                    val secs = state.remainingSeconds % 60
                    Text(
                        text = String.format("%d:%02d", mins, secs),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor
                    )

                    // Exercise name
                    Text(
                        text = state.currentExerciseName,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Round counter
                    Text(
                        text = "Round ${state.currentRound}/${state.template?.rounds ?: 0}",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )

                    // Next exercise preview
                    if (state.nextExerciseName != null) {
                        Text(
                            text = "Next: ${state.nextExerciseName}",
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Pause indicator
                    if (state.isPaused) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PAUSED \u2022 Swipe to exit",
                            style = MaterialTheme.typography.caption2,
                            color = Color(0xFFFF9800)
                        )
                    }
                }

                // Tap to pause/resume
                if (!state.isComplete) {
                    Button(
                        onClick = {
                            if (state.isPaused) {
                                pausedAccumulatedMs += System.currentTimeMillis() - pauseStartMs
                                state = state.copy(isPaused = false)
                            } else {
                                pauseStartMs = System.currentTimeMillis()
                                state = state.copy(isPaused = true)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent
                        )
                    ) {
                        // Invisible button overlay for tap to pause
                    }
                }
            }
        }
    }
}

private fun advancePhase(
    state: WearHIITState,
    audioCueManager: WearHIITAudioCueManager
): WearHIITState {
    val template = state.template ?: return state.copy(isComplete = true)

    return when (state.phase) {
        WearHIITPhase.WARMUP -> {
            val firstExercise = template.exercises[0]
            state.copy(
                phase = WearHIITPhase.WORK,
                currentExerciseIndex = 0,
                remainingSeconds = firstExercise.durationOverrideSec ?: template.workDurationSec,
                phaseDurationSeconds = firstExercise.durationOverrideSec ?: template.workDurationSec,
                currentExerciseName = firstExercise.exercise.name,
                nextExerciseName = if (template.exercises.size > 1) template.exercises[1].exercise.name else null
            )
        }
        WearHIITPhase.WORK -> {
            val nextExIndex = state.currentExerciseIndex + 1
            if (nextExIndex < template.exercises.size) {
                state.copy(
                    phase = WearHIITPhase.REST,
                    remainingSeconds = template.restDurationSec,
                    phaseDurationSeconds = template.restDurationSec,
                    currentExerciseName = "Rest",
                    nextExerciseName = template.exercises[nextExIndex].exercise.name
                )
            } else if (state.currentRound < template.rounds) {
                state.copy(
                    phase = WearHIITPhase.REST,
                    remainingSeconds = template.restDurationSec,
                    phaseDurationSeconds = template.restDurationSec,
                    currentExerciseName = "Rest",
                    nextExerciseName = template.exercises[0].exercise.name
                )
            } else {
                audioCueManager.playPhaseTone()
                state.copy(
                    phase = WearHIITPhase.COOLDOWN,
                    remainingSeconds = template.cooldownSec,
                    phaseDurationSeconds = template.cooldownSec,
                    currentExerciseName = "Cool Down",
                    nextExerciseName = null
                )
            }
        }
        WearHIITPhase.REST -> {
            val nextExIndex = state.currentExerciseIndex + 1
            val (newIndex, newRound) = if (nextExIndex < template.exercises.size) {
                nextExIndex to state.currentRound
            } else {
                0 to (state.currentRound + 1)
            }
            val exercise = template.exercises[newIndex]
            val afterNext = if (newIndex + 1 < template.exercises.size) {
                template.exercises[newIndex + 1].exercise.name
            } else null

            state.copy(
                phase = WearHIITPhase.WORK,
                currentExerciseIndex = newIndex,
                currentRound = newRound,
                remainingSeconds = exercise.durationOverrideSec ?: template.workDurationSec,
                phaseDurationSeconds = exercise.durationOverrideSec ?: template.workDurationSec,
                currentExerciseName = exercise.exercise.name,
                nextExerciseName = afterNext
            )
        }
        WearHIITPhase.COOLDOWN -> {
            audioCueManager.playCompleteTone()
            state.copy(
                phase = WearHIITPhase.COMPLETE,
                isComplete = true,
                remainingSeconds = 0
            )
        }
        WearHIITPhase.COMPLETE -> state
    }
}

private fun buildSessionJson(state: WearHIITState, elapsedMs: Long): String {
    val template = state.template
    val data = mapOf(
        "templateId" to (template?.id ?: ""),
        "templateName" to (template?.name ?: ""),
        "totalDurationMs" to elapsedMs,
        "exerciseCount" to (template?.exercises?.size ?: 0),
        "roundsCompleted" to state.currentRound,
        "totalRounds" to (template?.rounds ?: 0),
        "caloriesEstimate" to state.caloriesEstimate,
        "isCompleted" to (state.currentRound >= (template?.rounds ?: 0)),
        "source" to "watch"
    )
    return Gson().toJson(data)
}
