package com.runtracker.app.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runtracker.shared.ai.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveTrainingScreen(
    viewModel: AdaptiveTrainingViewModel = hiltViewModel(),
    onStartWorkout: () -> Unit,
    onStartModifiedWorkout: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Training Coach") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Readiness Score Card
            item {
                ReadinessScoreCard(
                    score = uiState.recommendation?.readinessScore ?: 70,
                    fatigueLevel = uiState.recommendation?.fatigueLevel ?: FatigueLevel.MODERATE
                )
            }
            
            // AI Recommendation Card
            uiState.recommendation?.let { rec ->
                item {
                    AIRecommendationCard(
                        adjustment = rec.adjustment,
                        originalWorkout = rec.originalWorkout,
                        suggestedWorkout = rec.suggestedWorkout
                    )
                }
                
                // Reasoning
                item {
                    ReasoningCard(reasoning = rec.reasoning)
                }
                
                // Insights
                if (rec.insights.isNotEmpty()) {
                    item {
                        Text(
                            text = "Insights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(rec.insights) { insight ->
                        InsightCard(insight = insight)
                    }
                }
            }
            
            // Action Buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                uiState.recommendation?.let { rec ->
                    when (rec.adjustment.type) {
                        AdjustmentType.REST_DAY -> {
                            Button(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Default.SelfImprovement, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Take a Rest Day")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedButton(
                                onClick = onStartWorkout,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Start Anyway (Not Recommended)")
                            }
                        }
                        
                        AdjustmentType.PROCEED_AS_PLANNED -> {
                            Button(
                                onClick = onStartWorkout,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Workout")
                            }
                        }
                        
                        else -> {
                            // Modified workout recommended
                            Button(
                                onClick = onStartModifiedWorkout,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start AI-Adjusted Workout")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedButton(
                                onClick = onStartWorkout,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Start Original Workout")
                            }
                        }
                    }
                } ?: run {
                    Button(
                        onClick = onStartWorkout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Workout")
                    }
                }
            }
        }
    }
}

@Composable
fun ReadinessScoreCard(
    score: Int,
    fatigueLevel: FatigueLevel
) {
    val scoreColor = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFF8BC34A)
        score >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Today's Readiness",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Score circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.labelSmall,
                        color = scoreColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when {
                            score >= 80 -> "🌟"
                            score >= 60 -> "✅"
                            score >= 40 -> "😐"
                            else -> "⚠️"
                        },
                        fontSize = 24.sp
                    )
                    Text(
                        text = when {
                            score >= 80 -> "Excellent"
                            score >= 60 -> "Good"
                            score >= 40 -> "Moderate"
                            else -> "Low"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (fatigueLevel) {
                            FatigueLevel.VERY_LOW -> "💪"
                            FatigueLevel.LOW -> "😊"
                            FatigueLevel.MODERATE -> "😐"
                            FatigueLevel.HIGH -> "😓"
                            FatigueLevel.VERY_HIGH -> "😴"
                        },
                        fontSize = 24.sp
                    )
                    Text(
                        text = "Fatigue: ${fatigueLevel.label}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AIRecommendationCard(
    adjustment: WorkoutAdjustment,
    originalWorkout: com.runtracker.shared.data.model.ScheduledWorkout?,
    suggestedWorkout: com.runtracker.shared.data.model.ScheduledWorkout?
) {
    val adjustmentColor = when (adjustment.type) {
        AdjustmentType.PROCEED_AS_PLANNED -> Color(0xFF4CAF50)
        AdjustmentType.INCREASE_INTENSITY, AdjustmentType.INCREASE_VOLUME -> Color(0xFF2196F3)
        AdjustmentType.REDUCE_INTENSITY, AdjustmentType.REDUCE_VOLUME -> Color(0xFFFF9800)
        AdjustmentType.SWAP_WORKOUT -> Color(0xFFFF9800)
        AdjustmentType.REST_DAY -> Color(0xFFF44336)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = adjustmentColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = adjustmentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Recommendation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Adjustment badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = adjustmentColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = when (adjustment.type) {
                        AdjustmentType.PROCEED_AS_PLANNED -> "✅ Proceed as Planned"
                        AdjustmentType.REDUCE_INTENSITY -> "⬇️ Reduce Intensity"
                        AdjustmentType.REDUCE_VOLUME -> "📉 Reduce Volume"
                        AdjustmentType.INCREASE_INTENSITY -> "⬆️ Push Harder"
                        AdjustmentType.INCREASE_VOLUME -> "📈 Increase Volume"
                        AdjustmentType.SWAP_WORKOUT -> "🔄 Swap Workout"
                        AdjustmentType.REST_DAY -> "🛌 Rest Day"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = adjustmentColor
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = adjustment.reason,
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Show workout comparison if modified
            if (originalWorkout != null && suggestedWorkout != null && 
                adjustment.type != AdjustmentType.PROCEED_AS_PLANNED) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Original",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = originalWorkout.workoutType.name.replace("_", " "),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${originalWorkout.targetDurationMinutes ?: "?"} min",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = adjustmentColor,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Suggested",
                            style = MaterialTheme.typography.labelSmall,
                            color = adjustmentColor
                        )
                        Text(
                            text = suggestedWorkout.workoutType.name.replace("_", " "),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = adjustmentColor
                        )
                        Text(
                            text = "${suggestedWorkout.targetDurationMinutes ?: "?"} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = adjustmentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReasoningCard(reasoning: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Analysis",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            reasoning.forEach { reason ->
                Text(
                    text = "• $reason",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun InsightCard(insight: TrainingInsight) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (insight.priority) {
                InsightPriority.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                InsightPriority.MEDIUM -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                InsightPriority.LOW -> MaterialTheme.colorScheme.surfaceVariant
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
                text = insight.icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
