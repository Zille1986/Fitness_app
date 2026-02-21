package com.runtracker.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

object HapticFeedback {
    fun light(context: Context) {
        vibrate(context, 10L, VibrationEffect.EFFECT_TICK)
    }

    fun medium(context: Context) {
        vibrate(context, 20L, VibrationEffect.EFFECT_CLICK)
    }

    fun heavy(context: Context) {
        vibrate(context, 30L, VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    fun success(context: Context) {
        vibrate(context, 50L, VibrationEffect.EFFECT_DOUBLE_CLICK)
    }

    fun error(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = getVibrator(context)
            val pattern = longArrayOf(0, 50, 50, 50)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    private fun vibrate(context: Context, duration: Long, effectId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = getVibrator(context)
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}

@Composable
fun PulseAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier.scale(scale),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun BounceAnimation(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (trigger && !animationPlayed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { animationPlayed = true },
        label = "bounce"
    )

    LaunchedEffect(trigger) {
        if (trigger) {
            animationPlayed = false
        }
    }

    Box(
        modifier = modifier.scale(scale),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun ShakeAnimation(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shakeOffset by animateFloatAsState(
        targetValue = if (trigger) 10f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "shake"
    )

    Box(
        modifier = modifier.graphicsLayer {
            translationX = shakeOffset
        },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun CountUpAnimation(
    targetValue: Int,
    modifier: Modifier = Modifier,
    durationMillis: Int = 1000,
    suffix: String = ""
) {
    var displayValue by remember { mutableStateOf(0) }

    LaunchedEffect(targetValue) {
        val startValue = displayValue
        val diff = targetValue - startValue
        val steps = 30
        val stepDelay = durationMillis / steps

        repeat(steps) { step ->
            delay(stepDelay.toLong())
            displayValue = startValue + (diff * (step + 1) / steps)
        }
        displayValue = targetValue
    }

    Text(
        text = "$displayValue$suffix",
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun CelebrationAnimation(
    show: Boolean,
    onComplete: () -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(show) {
        if (show) {
            HapticFeedback.success(context)
            delay(2000)
            onComplete()
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = fadeIn() + scaleIn(initialScale = 0.5f),
        exit = fadeOut() + scaleOut(targetScale = 1.5f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎉",
                fontSize = 80.sp
            )
        }
    }
}

@Composable
fun SlideInFromBottom(
    visible: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(200, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(200)),
        content = content
    )
}

@Composable
fun SlideInFromRight(
    visible: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(200, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(200)),
        content = content
    )
}

@Composable
fun FadeInOnLoad(
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500))
    ) {
        content()
    }
}

@Composable
fun StaggeredList(
    itemCount: Int,
    delayPerItem: Int = 50,
    content: @Composable (index: Int, animatedModifier: Modifier) -> Unit
) {
    Column {
        repeat(itemCount) { index ->
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay((index * delayPerItem).toLong())
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(300)
                        )
            ) {
                content(index, Modifier)
            }
        }
    }
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val EaseInCubic = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)
