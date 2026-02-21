package com.runtracker.app.ui.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runtracker.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisDashboardScreen(
    onStartBodyScan: () -> Unit,
    viewModel: AnalysisDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analysis",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartBodyScan,
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                text = { Text("New Scan") },
                containerColor = MaterialTheme.colorScheme.primary
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
            // Latest Body Scan Score
            item {
                LatestScanCard(
                    latestScan = uiState.latestScan,
                    onStartScan = onStartBodyScan
                )
            }

            // Comparison with Previous Scan
            if (uiState.comparison != null) {
                item {
                    ComparisonCard(comparison = uiState.comparison!!)
                }
            }

            // Detailed Improvements
            if (uiState.improvements.isNotEmpty()) {
                item {
                    Text(
                        text = "Improvements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(uiState.improvements) { improvement ->
                    ImprovementItem(improvement = improvement)
                }
            }

            // Areas Needing Work
            if (uiState.areasNeedingWork.isNotEmpty()) {
                item {
                    Text(
                        text = "Areas to Focus On",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(uiState.areasNeedingWork) { area ->
                    AreaNeedingWorkItem(area = area)
                }
            }

            // Posture Issues
            if (uiState.postureIssues.isNotEmpty()) {
                item {
                    PostureIssuesCard(issues = uiState.postureIssues)
                }
            }

            // Scan History
            if (uiState.scanHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Scan History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(uiState.scanHistory.take(5)) { scan ->
                    ScanHistoryItem(scan = scan)
                }
            }
        }
    }
}

@Composable
private fun LatestScanCard(
    latestScan: LatestScanInfo?,
    onStartScan: () -> Unit
) {
    GradientCard(
        modifier = Modifier.fillMaxWidth(),
        gradientColors = AppColors.AnalysisGradient
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Body Scan Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (latestScan != null) {
                Text(
                    text = latestScan.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (latestScan != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated score circle
                AnimatedScoreCircle(
                    score = latestScan.score,
                    maxScore = 100,
                    size = 100.dp,
                    strokeWidth = 10.dp
                )

                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoRowWhite(
                        label = "Body Type",
                        value = latestScan.bodyType
                    )
                    InfoRowWhite(
                        label = "Body Fat",
                        value = "${latestScan.bodyFatPercentage}%"
                    )
                    InfoRowWhite(
                        label = "Focus Areas",
                        value = "${latestScan.focusZoneCount} zones"
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No body scan yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(onClick = onStartScan) {
                    Text("Start Your First Scan")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.width(150.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoRowWhite(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.width(150.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun ComparisonCard(comparison: ScanComparison) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "vs Previous Scan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ComparisonStat(
                    label = "Score",
                    change = comparison.scoreChange,
                    suffix = "pts"
                )
                ComparisonStat(
                    label = "Body Fat",
                    change = comparison.bodyFatChange,
                    suffix = "%",
                    invertColors = true
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (comparison.overallImproved) 
                    Color(0xFF4CAF50).copy(alpha = 0.1f) 
                else 
                    Color(0xFFFF9800).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (comparison.overallImproved) Icons.Default.TrendingUp else Icons.Default.TrendingFlat,
                        contentDescription = null,
                        tint = if (comparison.overallImproved) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = comparison.summaryMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonStat(
    label: String,
    change: Float,
    suffix: String,
    invertColors: Boolean = false
) {
    val isPositive = if (invertColors) change < 0 else change > 0
    val color = when {
        change == 0f -> MaterialTheme.colorScheme.onSurfaceVariant
        isPositive -> Color(0xFF4CAF50)
        else -> Color(0xFFF44336)
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (change != 0f) {
                Icon(
                    if (change > 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
            }
            Text(
                text = String.format("%.1f%s", kotlin.math.abs(change), suffix),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun ImprovementItem(improvement: ImprovementInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = improvement.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = improvement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AreaNeedingWorkItem(area: AreaNeedingWorkInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = area.zoneName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = area.recommendation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PostureIssuesCard(issues: List<PostureIssueInfo>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Accessibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Posture Assessment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            issues.forEach { issue ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (issue.severity) {
                                    "SEVERE" -> Color(0xFFF44336)
                                    "MODERATE" -> Color(0xFFFF9800)
                                    else -> Color(0xFFFFEB3B)
                                }
                            )
                            .align(Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = issue.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = issue.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (issue.exercises.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try: ${issue.exercises.take(2).joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanHistoryItem(scan: ScanHistoryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = scan.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = scan.goalName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(getScoreColor(scan.score).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${scan.score}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = getScoreColor(scan.score)
                )
            }
        }
    }
}

private fun getScoreColor(score: Int): Color {
    return when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

// Data classes
data class LatestScanInfo(
    val score: Int,
    val date: String,
    val bodyType: String,
    val bodyFatPercentage: Float,
    val focusZoneCount: Int
)

data class ScanComparison(
    val scoreChange: Float,
    val bodyFatChange: Float,
    val overallImproved: Boolean,
    val summaryMessage: String
)

data class ImprovementInfo(
    val title: String,
    val description: String
)

data class AreaNeedingWorkInfo(
    val zoneName: String,
    val recommendation: String
)

data class PostureIssueInfo(
    val name: String,
    val description: String,
    val severity: String,
    val exercises: List<String>
)

data class ScanHistoryInfo(
    val id: Long,
    val date: String,
    val score: Int,
    val goalName: String
)
