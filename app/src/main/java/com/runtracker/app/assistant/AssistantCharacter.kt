package com.runtracker.app.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.runtracker.app.R
import kotlin.math.roundToInt

@Composable
fun AssistantCharacter(
    mood: AssistantMood,
    modifier: Modifier = Modifier,
    size: Int = 80,
    onClick: () -> Unit = {}
) {
    // Floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    
    // Bounce animation for celebrating
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (mood == AssistantMood.CELEBRATING) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    // Thinking animation (slight rotation)
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinking"
    )
    
    Box(
        modifier = modifier
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .size(size.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
                if (mood == AssistantMood.THINKING) {
                    rotationZ = rotation
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Buddy character image
        AsyncImage(
            model = R.drawable.buddy_avatar,
            contentDescription = "Buddy",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AssistantOverlay(
    uiState: AssistantUiState,
    onDismiss: () -> Unit,
    onMinimize: () -> Unit,
    onExpand: () -> Unit,
    onQuickReply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // Position in bottom-right by default
    LaunchedEffect(Unit) {
        with(density) {
            offsetX = (configuration.screenWidthDp.dp - 100.dp).toPx()
            offsetY = (configuration.screenHeightDp.dp - 200.dp).toPx()
        }
    }
    
    AnimatedVisibility(
        visible = uiState.isVisible && !uiState.isMinimized,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Speech bubble
                uiState.currentMessage?.let { message ->
                    SpeechBubble(
                        message = message,
                        isThinking = uiState.isThinking,
                        onExpand = onExpand,
                        onDismiss = onDismiss,
                        onQuickReply = onQuickReply
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Character
                AssistantCharacter(
                    mood = uiState.currentMood,
                    size = 70,
                    onClick = onExpand
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeechBubble(
    message: AssistantMessage,
    isThinking: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onQuickReply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .widthIn(max = 280.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Buddy",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Message content
            if (isThinking) {
                ThinkingIndicator()
            } else {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Quick replies
            if (message.quickReplies.isNotEmpty() && !isThinking) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    message.quickReplies.forEach { reply ->
                        SuggestionChip(
                            onClick = { onQuickReply(reply) },
                            label = { 
                                Text(
                                    reply, 
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        repeat(3) { index ->
            val delay = index * 200
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
            )
        }
    }
}

@Composable
fun MinimizedAssistantButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = R.drawable.buddy_avatar,
            contentDescription = "Buddy",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun getMoodEmoji(mood: AssistantMood): String {
    return when (mood) {
        AssistantMood.IDLE -> "😊"
        AssistantMood.WAVING -> "👋"
        AssistantMood.CELEBRATING -> "🎉"
        AssistantMood.THINKING -> "🤔"
        AssistantMood.TALKING -> "💬"
        AssistantMood.ENCOURAGING -> "💪"
        AssistantMood.SLEEPING -> "😴"
    }
}

private fun getMoodColor(mood: AssistantMood): Color {
    return when (mood) {
        AssistantMood.IDLE -> Color(0xFF6366F1)
        AssistantMood.WAVING -> Color(0xFF22C55E)
        AssistantMood.CELEBRATING -> Color(0xFFF59E0B)
        AssistantMood.THINKING -> Color(0xFF8B5CF6)
        AssistantMood.TALKING -> Color(0xFF3B82F6)
        AssistantMood.ENCOURAGING -> Color(0xFFEF4444)
        AssistantMood.SLEEPING -> Color(0xFF6B7280)
    }
}
