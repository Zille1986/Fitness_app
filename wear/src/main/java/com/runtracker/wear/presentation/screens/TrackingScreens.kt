package com.runtracker.wear.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.runtracker.wear.service.WearTrackingState

@Composable
fun TrackingPagerScreen(
    trackingState: WearTrackingState,
    isAmbient: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    var showStopConfirmation by remember { mutableStateOf(false) }
    val pageCount = 6

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (totalDrag < -30 && currentPage < pageCount - 1) {
                            currentPage++
                        } else if (totalDrag > 30 && currentPage > 0) {
                            currentPage--
                        }
                        totalDrag = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    }
                )
            }
    ) {
        Crossfade(
            targetState = currentPage,
            animationSpec = tween(durationMillis = 150),
            label = "page_transition"
        ) { page ->
            when (page) {
                0 -> MainTrackingPage(
                    trackingState = trackingState,
                    isAmbient = isAmbient,
                    onPause = onPause,
                    onResume = onResume,
                    onShowStopConfirmation = { showStopConfirmation = true },
                    onOpenSafetyMenu = { currentPage = 5 }
                )
                1 -> HeartRateZonePage(trackingState = trackingState, isAmbient = isAmbient)
                2 -> PaceZonePage(trackingState = trackingState, isAmbient = isAmbient)
                3 -> CompeteModePage(trackingState = trackingState, isAmbient = isAmbient)
                4 -> MusicControlPage(isAmbient = isAmbient)
                5 -> SafetyPage(isAmbient = isAmbient)
            }
        }

        // Page indicator dots
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = if (currentPage == index)
                                MaterialTheme.colors.primary
                            else
                                MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }
    }

    if (showStopConfirmation) {
        StopConfirmationDialog(
            onConfirm = {
                showStopConfirmation = false
                onStop()
            },
            onDismiss = { showStopConfirmation = false },
            activityType = trackingState.activityType
        )
    }
}

@Composable
fun MainTrackingPage(
    trackingState: WearTrackingState,
    isAmbient: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onShowStopConfirmation: () -> Unit,
    onOpenSafetyMenu: () -> Unit = {}
) {
    if (isAmbient) {
        // Ambient mode - minimal OLED-friendly UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = trackingState.durationFormatted,
                    style = MaterialTheme.typography.display2,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )
                Text(
                    text = "${String.format("%.2f", trackingState.distanceKm)} km",
                    style = MaterialTheme.typography.title2,
                    color = Color.Gray
                )
                Text(
                    text = "${trackingState.paceFormatted} /km",
                    style = MaterialTheme.typography.body1,
                    color = Color.DarkGray
                )
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Duration
            Text(
                text = trackingState.durationFormatted,
                style = MaterialTheme.typography.display1,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center
            )

            // Distance
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2f", trackingState.distanceKm),
                    style = MaterialTheme.typography.display3,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    text = "km",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }

            // Interval display
            if (trackingState.isIntervalWorkout) {
                val intervalColor = remember(trackingState.currentIntervalType) {
                    when (trackingState.currentIntervalType) {
                        "WARMUP" -> WearColors.IntervalWarmup
                        "WORK" -> WearColors.IntervalWork
                        "RECOVERY" -> WearColors.IntervalRecovery
                        "COOLDOWN" -> WearColors.IntervalCooldown
                        else -> Color.Unspecified // will use primary
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = trackingState.currentIntervalType,
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = if (intervalColor == Color.Unspecified) MaterialTheme.colors.primary else intervalColor
                    )
                    Text(
                        text = "${(trackingState.currentPhaseProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.caption1,
                        color = if (intervalColor == Color.Unspecified) MaterialTheme.colors.primary else intervalColor
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItemWithZone(
                    value = trackingState.paceFormatted,
                    label = "/km",
                    alert = trackingState.paceAlert
                )
                StatItemWithZone(
                    value = trackingState.heartRate?.toString() ?: "--",
                    label = "bpm",
                    alert = when (trackingState.hrAlert) {
                        com.runtracker.wear.service.HrAlert.TOO_LOW -> ZoneAlert.TOO_LOW
                        com.runtracker.wear.service.HrAlert.IN_ZONE -> ZoneAlert.IN_ZONE
                        com.runtracker.wear.service.HrAlert.TOO_HIGH -> ZoneAlert.TOO_HIGH
                    }
                )
            }

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Button(
                    onClick = onOpenSafetyMenu,
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = WearColors.Danger)
                ) {
                    Text("🛡", fontSize = 14.sp)
                }

                Button(
                    onClick = onShowStopConfirmation,
                    modifier = Modifier.size(42.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = WearColors.ZoneAbove)
                ) {
                    Text("■", fontSize = 18.sp, color = Color.White)
                }

                Button(
                    onClick = if (trackingState.isPaused) onResume else onPause,
                    modifier = Modifier.size(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (trackingState.isPaused)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.secondary
                    )
                ) {
                    Text(
                        text = if (trackingState.isPaused) "▶" else "❚❚",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StatItemWithZone(
    value: String,
    label: String,
    alert: Any? = null
) {
    val alertColor = when (alert) {
        ZoneAlert.TOO_LOW -> WearColors.ZoneBelow
        ZoneAlert.IN_ZONE -> WearColors.ZoneIn
        ZoneAlert.TOO_HIGH -> WearColors.ZoneAbove
        // Handle PaceAlert enum from service
        else -> {
            when (alert?.toString()) {
                "TOO_SLOW" -> WearColors.ZoneBelow
                "IN_ZONE" -> WearColors.ZoneIn
                "TOO_FAST" -> WearColors.ZoneAbove
                else -> MaterialTheme.colors.onSurface
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.title2,
            fontWeight = FontWeight.Bold,
            color = alertColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
fun HeartRateZonePage(
    trackingState: WearTrackingState,
    isAmbient: Boolean
) {
    val hrMin = trackingState.targetHrMin ?: 100
    val hrMax = trackingState.targetHrMax ?: 180
    val currentHr = trackingState.heartRate ?: 0

    val rangeSize = remember(hrMin, hrMax) { (hrMax - hrMin).coerceAtLeast(1) }
    val position = remember(currentHr, hrMin, rangeSize) {
        ((currentHr - hrMin).toFloat() / rangeSize).coerceIn(-0.3f, 1.3f)
    }

    val zoneColor = remember(currentHr, hrMin, hrMax) {
        when {
            currentHr < hrMin -> WearColors.ZoneBelow
            currentHr > hrMax -> WearColors.ZoneAbove
            else -> WearColors.ZoneIn
        }
    }

    val statusText = remember(currentHr, hrMin, hrMax) {
        when {
            currentHr < hrMin -> "TOO LOW"
            currentHr > hrMax -> "TOO HIGH"
            else -> "IN ZONE"
        }
    }

    val formattedDistance = remember(trackingState.distanceKm) {
        String.format("%.2f", trackingState.distanceKm)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAmbient) Color.Black else MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "❤️ HEART RATE",
                style = MaterialTheme.typography.caption1,
                fontWeight = FontWeight.Bold,
                color = if (isAmbient) Color.White else MaterialTheme.colors.primary
            )

            // Pre-computed arc values for Canvas
            ZoneArcIndicator(
                position = position,
                zoneColor = zoneColor,
                centerValue = currentHr.toString(),
                centerLabel = "bpm",
                isAmbient = isAmbient
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.caption1,
                    fontWeight = FontWeight.Bold,
                    color = if (isAmbient) Color.White else zoneColor
                )
                Text(
                    text = "Target: $hrMin - $hrMax bpm",
                    style = MaterialTheme.typography.caption2,
                    color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                )
            }

            DistanceAndProgress(
                formattedDistance = formattedDistance,
                trackingState = trackingState,
                isAmbient = isAmbient
            )
        }
    }
}

@Composable
fun PaceZonePage(
    trackingState: WearTrackingState,
    isAmbient: Boolean
) {
    val paceMin = trackingState.targetPaceMin ?: 300.0
    val paceMax = trackingState.targetPaceMax ?: 420.0
    val currentPace = trackingState.currentPaceSecondsPerKm

    val rangeSize = remember(paceMin, paceMax) { (paceMax - paceMin).coerceAtLeast(1.0) }
    val position = remember(currentPace, paceMax, rangeSize) {
        ((paceMax - currentPace) / rangeSize).toFloat().coerceIn(-0.3f, 1.3f)
    }

    val zoneColor = remember(currentPace, paceMin, paceMax) {
        when {
            currentPace <= 0 || currentPace.isInfinite() || currentPace.isNaN() -> Color.Gray
            currentPace < paceMin -> WearColors.ZoneAbove
            currentPace > paceMax -> WearColors.ZoneBelow
            else -> WearColors.ZoneIn
        }
    }

    val statusText = remember(currentPace, paceMin, paceMax) {
        when {
            currentPace <= 0 || currentPace.isInfinite() || currentPace.isNaN() -> "WAITING"
            currentPace < paceMin -> "TOO FAST"
            currentPace > paceMax -> "TOO SLOW"
            else -> "IN ZONE"
        }
    }

    val formattedDistance = remember(trackingState.distanceKm) {
        String.format("%.2f", trackingState.distanceKm)
    }

    val showIndicator = currentPace > 0 && !currentPace.isInfinite() && !currentPace.isNaN()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAmbient) Color.Black else MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "⚡ PACE",
                style = MaterialTheme.typography.caption1,
                fontWeight = FontWeight.Bold,
                color = if (isAmbient) Color.White else MaterialTheme.colors.primary
            )

            ZoneArcIndicator(
                position = if (showIndicator) position else 0f,
                zoneColor = zoneColor,
                centerValue = trackingState.paceFormatted,
                centerLabel = "/km",
                isAmbient = isAmbient,
                showIndicator = showIndicator
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.caption1,
                    fontWeight = FontWeight.Bold,
                    color = if (isAmbient) Color.White else zoneColor
                )
                Text(
                    text = "Target: ${formatPace(paceMin)} - ${formatPace(paceMax)}",
                    style = MaterialTheme.typography.caption2,
                    color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                )
            }

            DistanceAndProgress(
                formattedDistance = formattedDistance,
                trackingState = trackingState,
                isAmbient = isAmbient
            )
        }
    }
}

/**
 * Reusable arc indicator for HR/Pace zone pages.
 * Pre-computes trig values with remember() to avoid recalculating on every frame.
 */
@Composable
fun ZoneArcIndicator(
    position: Float,
    zoneColor: Color,
    centerValue: String,
    centerLabel: String,
    isAmbient: Boolean,
    showIndicator: Boolean = true
) {
    // Pre-compute trig values to avoid recalculating in Canvas draw
    val clampedPosition = position.coerceIn(0f, 1f)
    val indicatorAngle = remember(clampedPosition) {
        Math.toRadians((135.0 + clampedPosition * 270.0))
    }
    val cosAngle = remember(indicatorAngle) { kotlin.math.cos(indicatorAngle).toFloat() }
    val sinAngle = remember(indicatorAngle) { kotlin.math.sin(indicatorAngle).toFloat() }

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val arcSize = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - arcSize) / 2, (size.height - arcSize) / 2)

            // Background arc
            drawArc(
                color = Color.Gray.copy(alpha = 0.3f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Target zone arc
            drawArc(
                color = WearColors.ZoneIn.copy(alpha = 0.5f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Position indicator (using pre-computed trig)
            if (showIndicator) {
                val indicatorRadius = arcSize / 2
                val centerX = size.width / 2
                val centerY = size.height / 2
                drawCircle(
                    color = zoneColor,
                    radius = 10.dp.toPx(),
                    center = Offset(
                        centerX + indicatorRadius * cosAngle,
                        centerY + indicatorRadius * sinAngle
                    )
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = MaterialTheme.typography.display2,
                fontWeight = FontWeight.Bold,
                color = if (isAmbient) Color.White else zoneColor
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.caption2,
                color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DistanceAndProgress(
    formattedDistance: String,
    trackingState: WearTrackingState,
    isAmbient: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formattedDistance,
                style = MaterialTheme.typography.title3,
                fontWeight = FontWeight.Bold,
                color = if (isAmbient) Color.White else MaterialTheme.colors.onSurface
            )
            Text(
                text = "km",
                style = MaterialTheme.typography.caption2,
                color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
            )
        }

        if (trackingState.isIntervalWorkout) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(trackingState.currentPhaseProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.Bold,
                    color = if (isAmbient) Color.White else MaterialTheme.colors.primary
                )
                Text(
                    text = trackingState.currentIntervalType.lowercase(),
                    style = MaterialTheme.typography.caption2,
                    color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompeteModePage(
    trackingState: WearTrackingState,
    isAmbient: Boolean
) {
    val pbTimeMillis = trackingState.competePbTimeMillis
    val targetDistance = trackingState.competeTargetDistance
    val distanceName = remember(targetDistance) {
        when (targetDistance) {
            1000 -> "1K"
            5000 -> "5K"
            10000 -> "10K"
            21097 -> "Half"
            42195 -> "Marathon"
            else -> "${targetDistance / 1000}K"
        }
    }

    val currentProgress = (trackingState.distanceMeters / targetDistance).coerceIn(0.0, 1.0).toFloat()
    val pbProgress = if (pbTimeMillis != null && pbTimeMillis > 0) {
        val pbPaceSecondsPerKm = pbTimeMillis / 1000.0 / (targetDistance / 1000.0)
        val pbDistanceAtCurrentTime = (trackingState.durationMillis / 1000.0) / pbPaceSecondsPerKm * 1000
        (pbDistanceAtCurrentTime / targetDistance).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }

    val isAhead = currentProgress > pbProgress
    val timeDiff = if (pbTimeMillis != null && trackingState.distanceMeters > 0) {
        val expectedTimeAtDistance = (trackingState.distanceMeters / targetDistance) * pbTimeMillis
        trackingState.durationMillis - expectedTimeAtDistance.toLong()
    } else 0L

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAmbient) Color.Black else MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🏁 COMPETE",
                style = MaterialTheme.typography.caption1,
                fontWeight = FontWeight.Bold,
                color = if (isAmbient) Color.Gray else MaterialTheme.colors.primary
            )

            Text(
                text = "vs $distanceName PB",
                style = MaterialTheme.typography.caption2,
                color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
            )

            // Track with runners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val runnerColor = if (pbTimeMillis == null || isAhead)
                    WearColors.Success else WearColors.ZoneAbove

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val trackWidth = size.width - 40f
                    val trackY = size.height / 2
                    val trackStartX = 20f

                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(trackStartX, trackY),
                        end = Offset(trackStartX + trackWidth, trackY),
                        strokeWidth = 12f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(trackStartX + trackWidth, trackY - 20),
                        end = Offset(trackStartX + trackWidth, trackY + 20),
                        strokeWidth = 3f
                    )

                    if (pbTimeMillis != null) {
                        drawCircle(
                            color = WearColors.ZoneBelow.copy(alpha = 0.7f),
                            radius = 14f,
                            center = Offset(trackStartX + trackWidth * pbProgress, trackY - 12)
                        )
                    }

                    drawCircle(
                        color = runnerColor,
                        radius = 16f,
                        center = Offset(trackStartX + trackWidth * currentProgress, trackY + 12)
                    )
                }

                if (pbTimeMillis != null) {
                    Text(
                        text = "PB",
                        style = MaterialTheme.typography.caption2,
                        color = WearColors.ZoneBelow,
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp)
                    )
                }
                Text(
                    text = "YOU",
                    style = MaterialTheme.typography.caption2,
                    color = if (isAhead) WearColors.Success else WearColors.ZoneAbove,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 4.dp)
                )
            }

            // Time difference
            if (pbTimeMillis != null && trackingState.distanceMeters > 100) {
                val absDiff = kotlin.math.abs(timeDiff)
                val diffSeconds = absDiff / 1000
                val diffMins = diffSeconds / 60
                val diffSecs = diffSeconds % 60
                val sign = if (timeDiff < 0) "-" else "+"
                val diffText = if (diffMins > 0) {
                    "$sign${diffMins}:${String.format("%02d", diffSecs)}"
                } else {
                    "$sign${diffSecs}s"
                }

                Text(
                    text = diffText,
                    style = MaterialTheme.typography.display3,
                    fontWeight = FontWeight.Bold,
                    color = if (isAhead) WearColors.Success else WearColors.ZoneAbove
                )
                Text(
                    text = if (isAhead) "AHEAD" else "BEHIND",
                    style = MaterialTheme.typography.caption1,
                    fontWeight = FontWeight.Bold,
                    color = if (isAhead) WearColors.Success else WearColors.ZoneAbove
                )
            } else if (pbTimeMillis == null) {
                Text("No PB yet", style = MaterialTheme.typography.body1, color = MaterialTheme.colors.onSurfaceVariant)
                Text("Complete a $distanceName to set one!", style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center)
            } else {
                Text("Starting...", style = MaterialTheme.typography.body1, color = MaterialTheme.colors.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.2f", trackingState.distanceKm),
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = if (isAmbient) Color.White else MaterialTheme.colors.onSurface
                    )
                    Text("km", style = MaterialTheme.typography.caption2,
                        color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(currentProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = if (isAmbient) Color.White else MaterialTheme.colors.primary
                    )
                    Text("of $distanceName", style = MaterialTheme.typography.caption2,
                        color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun StopConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    activityType: String = "RUNNING"
) {
    val (title, subtitle) = when (activityType) {
        "SWIMMING" -> "Finish Swim?" to "Save this swim?"
        "CYCLING" -> "Finish Ride?" to "Save this ride?"
        else -> "Finish Run?" to "Save this run?"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(
                    MaterialTheme.colors.surface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.title3, textAlign = TextAlign.Center)
            Text(text = subtitle, style = MaterialTheme.typography.body2, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDismiss, colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.size(48.dp)) { Text("X", fontSize = 16.sp) }
                Button(onClick = onConfirm, colors = ButtonDefaults.primaryButtonColors(),
                    modifier = Modifier.size(48.dp)) { Text("OK", fontSize = 12.sp) }
            }
        }
    }
}
