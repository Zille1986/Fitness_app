package com.runtracker.app.ui.screens.wellness

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.runtracker.shared.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessDashboardScreen(
    viewModel: WellnessViewModel = hiltViewModel(),
    onNavigateToGamification: () -> Unit = {},
    onNavigateToMindfulness: () -> Unit = {},
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wellness Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Wellness Score
            item {
                OverallWellnessCard(
                    score = uiState.overallWellnessScore,
                    status = uiState.wellnessStatus
                )
            }
            
            // Quick Stats Row
            item {
                QuickStatsRow(uiState = uiState)
            }
            
            // Training Load Card
            item {
                TrainingLoadCard(trainingLoad = uiState.trainingLoad)
            }
            
            // Daily Readiness
            item {
                DailyReadinessCard(
                    checkin = uiState.todayCheckin,
                    rings = uiState.todayRings
                )
            }
            
            // Recommendations
            if (uiState.recommendations.isNotEmpty()) {
                item {
                    Text(
                        text = "Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(uiState.recommendations) { recommendation ->
                    RecommendationCard(recommendation = recommendation)
                }
            }
            
            // Weekly Trends
            item {
                WeeklyTrendsCard(checkins = uiState.recentCheckins)
            }
        }
    }
}

@Composable
fun OverallWellnessCard(
    score: Int,
    status: WellnessStatus
) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(1000),
        label = "score"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                WellnessStatus.EXCELLENT -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                WellnessStatus.GOOD -> Color(0xFF8BC34A).copy(alpha = 0.15f)
                WellnessStatus.FAIR -> Color(0xFFFF9800).copy(alpha = 0.15f)
                WellnessStatus.NEEDS_ATTENTION -> Color(0xFFF44336).copy(alpha = 0.15f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Overall Wellness",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Circular progress indicator
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    // Background circle
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.2f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Progress arc
                    val sweepAngle = (animatedScore / 100f) * 360f
                    drawArc(
                        color = when (status) {
                            WellnessStatus.EXCELLENT -> Color(0xFF4CAF50)
                            WellnessStatus.GOOD -> Color(0xFF8BC34A)
                            WellnessStatus.FAIR -> Color(0xFFFF9800)
                            WellnessStatus.NEEDS_ATTENTION -> Color(0xFFF44336)
                        },
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$animatedScore",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = status.emoji,
                        fontSize = 24.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = status.label,
                style = MaterialTheme.typography.titleSmall,
                color = when (status) {
                    WellnessStatus.EXCELLENT -> Color(0xFF4CAF50)
                    WellnessStatus.GOOD -> Color(0xFF8BC34A)
                    WellnessStatus.FAIR -> Color(0xFFFF9800)
                    WellnessStatus.NEEDS_ATTENTION -> Color(0xFFF44336)
                }
            )
        }
    }
}

@Composable
fun QuickStatsRow(uiState: WellnessUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = "🔥",
            value = "${uiState.gamification?.currentStreak ?: 0}",
            label = "Day Streak"
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = "🏃",
            value = "${uiState.trainingLoad.weeklyRuns}",
            label = "Runs/Week"
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = "🧘",
            value = "${uiState.mindfulnessMinutesThisWeek}",
            label = "Mindful Min"
        )
    }
}

@Composable
fun QuickStatCard(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 20.sp)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TrainingLoadCard(trainingLoad: TrainingLoad) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Training Load",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(trainingLoad.status.color).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = trainingLoad.status.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(trainingLoad.status.color)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ACWR Gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${trainingLoad.acuteLoad}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Acute Load",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.2f".format(trainingLoad.acuteChronicRatio),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(trainingLoad.status.color)
                    )
                    Text(
                        text = "AC Ratio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${trainingLoad.chronicLoad}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Chronic Load",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Weekly stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "%.1f km".format(trainingLoad.weeklyDistance / 1000),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${trainingLoad.weeklyDuration / 60000} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${trainingLoad.weeklyRuns} runs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DailyReadinessCard(
    checkin: WellnessCheckin?,
    rings: DailyRings?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Today's Readiness",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (checkin != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    checkin.sleepHours?.let { hours ->
                        ReadinessItem(
                            icon = "😴",
                            value = "${hours}h",
                            label = "Sleep"
                        )
                    }
                    checkin.mood?.let { mood ->
                        ReadinessItem(
                            icon = mood.emoji,
                            value = mood.label,
                            label = "Mood"
                        )
                    }
                    checkin.energy?.let { energy ->
                        ReadinessItem(
                            icon = energy.emoji,
                            value = energy.label,
                            label = "Energy"
                        )
                    }
                    checkin.readinessScore?.let { score ->
                        ReadinessItem(
                            icon = when {
                                score >= 70 -> "✅"
                                score >= 50 -> "😐"
                                else -> "⚠️"
                            },
                            value = "$score",
                            label = "Score"
                        )
                    }
                }
            } else {
                Text(
                    text = "Complete your daily check-in to see readiness data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Activity rings summary
            rings?.let { r ->
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniRing(
                        progress = r.moveProgress,
                        color = Color(0xFFFF2D55),
                        label = "Move"
                    )
                    MiniRing(
                        progress = r.exerciseProgress,
                        color = Color(0xFF30D158),
                        label = "Exercise"
                    )
                    MiniRing(
                        progress = r.distanceProgress,
                        color = Color(0xFF007AFF),
                        label = "Distance"
                    )
                }
            }
        }
    }
}

@Composable
fun ReadinessItem(
    icon: String,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 20.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MiniRing(
    progress: Float,
    color: Color,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                
                drawCircle(
                    color = color.copy(alpha = 0.2f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
                
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceAtMost(1f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RecommendationCard(recommendation: WellnessRecommendation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (recommendation.priority) {
                RecommendationPriority.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                RecommendationPriority.MEDIUM -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                RecommendationPriority.LOW -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = recommendation.icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WeeklyTrendsCard(checkins: List<WellnessCheckin>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Weekly Readiness Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (checkins.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    checkins.reversed().take(7).forEach { checkin ->
                        val score = checkin.readinessScore ?: 50
                        val height = (score / 100f * 50).dp
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(height)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        when {
                                            score >= 70 -> Color(0xFF4CAF50)
                                            score >= 50 -> Color(0xFFFF9800)
                                            else -> Color(0xFFF44336)
                                        }
                                    )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Day labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val dayFormat = java.text.SimpleDateFormat("E", java.util.Locale.getDefault())
                    checkins.reversed().take(7).forEach { checkin ->
                        Text(
                            text = dayFormat.format(java.util.Date(checkin.date)).first().toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "Complete daily check-ins to see trends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
