package com.runtracker.app.ui.screens.gym

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryOf
import com.runtracker.shared.data.model.ExerciseHistory
import com.runtracker.shared.data.model.ProgressTrend
import com.runtracker.shared.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ── Data classes ──────────────────────────────────────────────

enum class ProgressChartType {
    WEIGHT, REPS, VOLUME
}

data class ExerciseProgressUiState(
    val isLoading: Boolean = true,
    val history: List<ExerciseHistory> = emptyList(),
    val weightData: List<FloatEntry> = emptyList(),
    val repsData: List<FloatEntry> = emptyList(),
    val volumeData: List<FloatEntry> = emptyList(),
    val dates: List<Long> = emptyList(),
    val bestWeight: Double = 0.0,
    val bestReps: Int = 0,
    val trend: ProgressTrend = ProgressTrend.NEW
)

// ── ViewModel ─────────────────────────────────────────────────

@HiltViewModel
class ExerciseProgressChartViewModel @Inject constructor(
    private val gymRepository: GymRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseProgressUiState())
    val uiState: StateFlow<ExerciseProgressUiState> = _uiState.asStateFlow()

    private var loadedExerciseId: Long = -1

    fun loadHistory(exerciseId: Long) {
        if (exerciseId == loadedExerciseId) return
        loadedExerciseId = exerciseId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val twelveWeeksAgo = System.currentTimeMillis() - (12L * 7 * 24 * 60 * 60 * 1000)
            val history = gymRepository.getHistorySince(exerciseId, twelveWeeksAgo)
            val sorted = history.sortedBy { it.date }

            if (sorted.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, history = emptyList()) }
                return@launch
            }

            val weightData = sorted.mapIndexed { i, h -> entryOf(i.toFloat(), h.bestWeight.toFloat()) }
            val repsData = sorted.mapIndexed { i, h -> entryOf(i.toFloat(), h.bestReps.toFloat()) }
            val volumeData = sorted.mapIndexed { i, h -> entryOf(i.toFloat(), h.totalVolume.toFloat()) }

            val bestWeight = sorted.maxOf { it.bestWeight }
            val bestReps = sorted.maxOf { it.bestReps }
            val trend = calculateTrend(sorted)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    history = sorted,
                    weightData = weightData,
                    repsData = repsData,
                    volumeData = volumeData,
                    dates = sorted.map { h -> h.date },
                    bestWeight = bestWeight,
                    bestReps = bestReps,
                    trend = trend
                )
            }
        }
    }

    private fun calculateTrend(sorted: List<ExerciseHistory>): ProgressTrend {
        if (sorted.size < 3) return ProgressTrend.NEW

        val midpoint = sorted.size / 2
        val recentAvgWeight = sorted.subList(midpoint, sorted.size).map { it.bestWeight }.average()
        val earlierAvgWeight = sorted.subList(0, midpoint).map { it.bestWeight }.average()

        val changePercent = if (earlierAvgWeight > 0) {
            ((recentAvgWeight - earlierAvgWeight) / earlierAvgWeight) * 100
        } else 0.0

        return when {
            changePercent > 5 -> ProgressTrend.IMPROVING
            changePercent < -5 -> ProgressTrend.DECLINING
            else -> ProgressTrend.MAINTAINING
        }
    }
}

// ── Composable ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressChart(
    exerciseId: Long,
    exerciseName: String,
    modifier: Modifier = Modifier,
    viewModel: ExerciseProgressChartViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(exerciseId) {
        viewModel.loadHistory(exerciseId)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (uiState.history.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "No history yet — complete sets to start tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            ProgressChartContent(uiState = uiState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressChartContent(uiState: ExerciseProgressUiState) {
    var selectedChartType by remember { mutableStateOf(ProgressChartType.WEIGHT) }

    val chartData = when (selectedChartType) {
        ProgressChartType.WEIGHT -> uiState.weightData
        ProgressChartType.REPS -> uiState.repsData
        ProgressChartType.VOLUME -> uiState.volumeData
    }

    val chartEntryModelProducer = remember(chartData) {
        ChartEntryModelProducer(listOf(chartData))
    }

    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val axisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        val index = value.toInt()
        if (index in uiState.dates.indices) {
            dateFormatter.format(Date(uiState.dates[index]))
        } else ""
    }

    Column {
        // Chart type selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedChartType == ProgressChartType.WEIGHT,
                onClick = { selectedChartType = ProgressChartType.WEIGHT },
                label = { Text("Weight", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedChartType == ProgressChartType.REPS,
                onClick = { selectedChartType = ProgressChartType.REPS },
                label = { Text("Reps", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedChartType == ProgressChartType.VOLUME,
                onClick = { selectedChartType = ProgressChartType.VOLUME },
                label = { Text("Volume", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chart
        val weightColor = MaterialTheme.colorScheme.primary
        val repsColor = Color(0xFF4CAF50)
        val volumeColor = Color(0xFFFF9800)

        val chartColor = when (selectedChartType) {
            ProgressChartType.WEIGHT -> weightColor
            ProgressChartType.REPS -> repsColor
            ProgressChartType.VOLUME -> volumeColor
        }

        if (chartData.isNotEmpty()) {
            Chart(
                chart = lineChart(
                    lines = listOf(
                        LineChart.LineSpec(
                            lineColor = chartColor.toArgb(),
                            lineBackgroundShader = DynamicShaders.fromBrush(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(
                                        chartColor.copy(alpha = 0.4f),
                                        chartColor.copy(alpha = 0f)
                                    )
                                )
                            )
                        )
                    )
                ),
                chartModelProducer = chartEntryModelProducer,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(valueFormatter = axisValueFormatter),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Summary row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Best stats
            Text(
                text = "Best: ${formatWeight(uiState.bestWeight)} × ${uiState.bestReps}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Trend indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val (trendIcon, trendColor, trendText) = when (uiState.trend) {
                    ProgressTrend.IMPROVING -> Triple(
                        Icons.Default.TrendingUp,
                        Color(0xFF4CAF50),
                        "Improving"
                    )
                    ProgressTrend.MAINTAINING -> Triple(
                        Icons.Default.TrendingFlat,
                        Color(0xFFFF9800),
                        "Steady"
                    )
                    ProgressTrend.DECLINING -> Triple(
                        Icons.Default.TrendingDown,
                        Color(0xFFF44336),
                        "Declining"
                    )
                    ProgressTrend.NEW -> Triple(
                        Icons.Default.TrendingFlat,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        "New"
                    )
                }
                Icon(
                    imageVector = trendIcon,
                    contentDescription = trendText,
                    tint = trendColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = trendText,
                    style = MaterialTheme.typography.labelSmall,
                    color = trendColor
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle
        Text(
            text = "${uiState.history.size} sessions · Last 12 weeks",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

private fun formatWeight(weight: Double): String {
    return if (weight == weight.toLong().toDouble()) {
        "${weight.toLong()}kg"
    } else {
        "${"%.1f".format(weight)}kg"
    }
}
