package com.runtracker.app.ui.screens.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runtracker.shared.data.model.Run
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OverviewCard(uiState)
                }

                item {
                    WeeklyDistanceChart(uiState.distanceProgression)
                }

                item {
                    PaceProgressionCard(uiState.paceProgression)
                }

                item {
                    PersonalRecordsCard(uiState.personalRecords)
                }
            }
        }
    }
}

@Composable
fun OverviewCard(uiState: AnalyticsUiState) {
    val totalRuns = uiState.allRuns.size
    val totalDistance = uiState.allRuns.sumOf { it.distanceMeters } / 1000.0
    val totalDuration = uiState.allRuns.sumOf { it.durationMillis }
    val avgPace = if (totalDistance > 0) {
        (totalDuration / 1000.0) / totalDistance
    } else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "All Time Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OverviewStatItem(
                    value = String.format("%.0f", totalDistance),
                    unit = "km",
                    label = "Total Distance"
                )
                OverviewStatItem(
                    value = totalRuns.toString(),
                    unit = "",
                    label = "Total Runs"
                )
                OverviewStatItem(
                    value = Run.formatPace(avgPace),
                    unit = "/km",
                    label = "Avg Pace"
                )
            }
        }
    }
}

@Composable
fun OverviewStatItem(value: String, unit: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun WeeklyDistanceChart(data: List<DistanceDataPoint>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Distance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (data.isEmpty()) {
                Text(
                    text = "Not enough data yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxDistance = data.maxOfOrNull { it.totalDistanceKm } ?: 1.0
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEach { point ->
                        val heightFraction = (point.totalDistanceKm / maxDistance).toFloat()
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .fillMaxHeight(heightFraction.coerceAtLeast(0.05f))
                                    .padding(horizontal = 2.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small
                                ) {}
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    data.firstOrNull()?.let {
                        Text(
                            text = formatWeekLabel(it.weekStart),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    data.lastOrNull()?.let {
                        Text(
                            text = formatWeekLabel(it.weekStart),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaceProgressionCard(data: List<PaceDataPoint>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pace Progression",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (data.size < 2) {
                Text(
                    text = "Complete more runs to see your pace progression",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val firstPace = data.first().paceSecondsPerKm
                val lastPace = data.last().paceSecondsPerKm
                val improvement = firstPace - lastPace
                val isImproved = improvement > 0
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Recent trend",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isImproved) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isImproved) Color(0xFF4CAF50) else Color(0xFFE57373),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isImproved) "Improving" else "Slower",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isImproved) Color(0xFF4CAF50) else Color(0xFFE57373)
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Avg pace change",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${if (isImproved) "-" else "+"}${Run.formatPace(kotlin.math.abs(improvement))}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalRecordsCard(records: PersonalRecordsData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Personal Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val hasRecords = listOfNotNull(
                records.fastest1K,
                records.fastest5K,
                records.fastest10K,
                records.longestRun
            ).isNotEmpty()
            
            if (!hasRecords) {
                Text(
                    text = "Complete runs to set personal records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                records.fastest1K?.let { run ->
                    RecordItem(
                        icon = Icons.Default.EmojiEvents,
                        title = "Fastest 1K",
                        value = Run.formatPace(run.avgPaceSecondsPerKm),
                        subtitle = formatRecordDate(run.startTime)
                    )
                }
                
                records.fastest5K?.let { run ->
                    RecordItem(
                        icon = Icons.Default.EmojiEvents,
                        title = "Fastest 5K",
                        value = run.durationFormatted,
                        subtitle = formatRecordDate(run.startTime)
                    )
                }
                
                records.fastest10K?.let { run ->
                    RecordItem(
                        icon = Icons.Default.EmojiEvents,
                        title = "Fastest 10K",
                        value = run.durationFormatted,
                        subtitle = formatRecordDate(run.startTime)
                    )
                }
                
                records.longestRun?.let { run ->
                    RecordItem(
                        icon = Icons.Default.Hiking,
                        title = "Longest Run",
                        value = String.format("%.2f km", run.distanceKm),
                        subtitle = formatRecordDate(run.startTime)
                    )
                }
                
                records.mostElevation?.let { run ->
                    RecordItem(
                        icon = Icons.Default.Terrain,
                        title = "Most Elevation",
                        value = "${run.elevationGainMeters.toInt()}m",
                        subtitle = formatRecordDate(run.startTime)
                    )
                }
            }
        }
    }
}

@Composable
fun RecordItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatWeekLabel(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatRecordDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
