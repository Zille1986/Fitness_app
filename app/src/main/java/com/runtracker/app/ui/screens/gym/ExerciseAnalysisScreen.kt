package com.runtracker.app.ui.screens.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExerciseAnalysisViewModel @Inject constructor(
    private val gymRepository: GymRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseAnalysisUiState())
    val uiState: StateFlow<ExerciseAnalysisUiState> = _uiState.asStateFlow()

    init {
        loadExercises()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            gymRepository.getAllExercises().collect { exercises ->
                _uiState.update { it.copy(allExercises = exercises, isLoading = false) }
                
                // Auto-select first exercise if none selected
                if (_uiState.value.selectedExercise == null && exercises.isNotEmpty()) {
                    selectExercise(exercises.first())
                }
            }
        }
    }

    fun selectExercise(exercise: Exercise) {
        _uiState.update { it.copy(selectedExercise = exercise, isLoadingHistory = true) }
        loadExerciseHistory(exercise.id)
    }

    private fun loadExerciseHistory(exerciseId: Long) {
        viewModelScope.launch {
            gymRepository.getHistoryForExercise(exerciseId).collect { history ->
                val sortedHistory = history.sortedBy { it.date }
                
                // Calculate stats
                val stats = calculateStats(sortedHistory)
                
                // Prepare chart data
                val oneRepMaxData = sortedHistory.mapIndexed { index, h ->
                    entryOf(index.toFloat(), h.estimatedOneRepMax.toFloat())
                }
                
                val volumeData = sortedHistory.mapIndexed { index, h ->
                    entryOf(index.toFloat(), h.totalVolume.toFloat())
                }
                
                val weightData = sortedHistory.mapIndexed { index, h ->
                    entryOf(index.toFloat(), h.bestWeight.toFloat())
                }
                
                _uiState.update { state ->
                    state.copy(
                        exerciseHistory = sortedHistory,
                        exerciseStats = stats,
                        oneRepMaxChartData = oneRepMaxData,
                        volumeChartData = volumeData,
                        weightChartData = weightData,
                        chartDates = sortedHistory.map { it.date },
                        isLoadingHistory = false
                    )
                }
            }
        }
    }

    private fun calculateStats(history: List<ExerciseHistory>): ExerciseAnalysisStats {
        if (history.isEmpty()) {
            return ExerciseAnalysisStats()
        }

        val currentOneRepMax = history.maxOfOrNull { it.estimatedOneRepMax } ?: 0.0
        val currentMaxWeight = history.maxOfOrNull { it.bestWeight } ?: 0.0
        val totalVolume = history.sumOf { it.totalVolume }
        val totalSets = history.sumOf { it.totalSets }
        val totalReps = history.sumOf { it.totalReps }
        val timesPerformed = history.size
        
        // Calculate 30-day stats
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val last30Days = history.filter { it.date >= thirtyDaysAgo }
        val volumeLast30Days = last30Days.sumOf { it.totalVolume }
        val setsLast30Days = last30Days.sumOf { it.totalSets }
        
        // Calculate trend (compare last 5 vs previous 5)
        val trend = if (history.size >= 10) {
            val recent = history.takeLast(5).map { it.estimatedOneRepMax }.average()
            val previous = history.dropLast(5).takeLast(5).map { it.estimatedOneRepMax }.average()
            when {
                recent > previous * 1.02 -> ProgressTrend.IMPROVING
                recent < previous * 0.98 -> ProgressTrend.DECLINING
                else -> ProgressTrend.MAINTAINING
            }
        } else if (history.size >= 2) {
            val recent = history.last().estimatedOneRepMax
            val first = history.first().estimatedOneRepMax
            when {
                recent > first * 1.02 -> ProgressTrend.IMPROVING
                recent < first * 0.98 -> ProgressTrend.DECLINING
                else -> ProgressTrend.MAINTAINING
            }
        } else {
            ProgressTrend.NEW
        }
        
        // Find PB date
        val pbEntry = history.maxByOrNull { it.estimatedOneRepMax }
        
        // Calculate average rest between sessions
        val avgDaysBetweenSessions = if (history.size >= 2) {
            val sortedDates = history.map { it.date }.sorted()
            val gaps = sortedDates.zipWithNext { a, b -> (b - a) / (24 * 60 * 60 * 1000) }
            gaps.average()
        } else 0.0

        return ExerciseAnalysisStats(
            currentOneRepMax = currentOneRepMax,
            currentMaxWeight = currentMaxWeight,
            totalVolumeAllTime = totalVolume,
            totalSetsAllTime = totalSets,
            totalRepsAllTime = totalReps,
            timesPerformed = timesPerformed,
            volumeLast30Days = volumeLast30Days,
            setsLast30Days = setsLast30Days,
            progressTrend = trend,
            pbDate = pbEntry?.date,
            avgDaysBetweenSessions = avgDaysBetweenSessions,
            lastPerformed = history.lastOrNull()?.date
        )
    }

    fun setChartType(type: ChartType) {
        _uiState.update { it.copy(selectedChartType = type) }
    }
}

data class ExerciseAnalysisStats(
    val currentOneRepMax: Double = 0.0,
    val currentMaxWeight: Double = 0.0,
    val totalVolumeAllTime: Double = 0.0,
    val totalSetsAllTime: Int = 0,
    val totalRepsAllTime: Int = 0,
    val timesPerformed: Int = 0,
    val volumeLast30Days: Double = 0.0,
    val setsLast30Days: Int = 0,
    val progressTrend: ProgressTrend = ProgressTrend.NEW,
    val pbDate: Long? = null,
    val avgDaysBetweenSessions: Double = 0.0,
    val lastPerformed: Long? = null
)

enum class ChartType {
    ONE_REP_MAX, WEIGHT, VOLUME
}

data class ExerciseAnalysisUiState(
    val allExercises: List<Exercise> = emptyList(),
    val selectedExercise: Exercise? = null,
    val exerciseHistory: List<ExerciseHistory> = emptyList(),
    val exerciseStats: ExerciseAnalysisStats = ExerciseAnalysisStats(),
    val oneRepMaxChartData: List<com.patrykandpatrick.vico.core.entry.FloatEntry> = emptyList(),
    val volumeChartData: List<com.patrykandpatrick.vico.core.entry.FloatEntry> = emptyList(),
    val weightChartData: List<com.patrykandpatrick.vico.core.entry.FloatEntry> = emptyList(),
    val chartDates: List<Long> = emptyList(),
    val selectedChartType: ChartType = ChartType.ONE_REP_MAX,
    val isLoading: Boolean = true,
    val isLoadingHistory: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseAnalysisScreen(
    onBack: () -> Unit,
    viewModel: ExerciseAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExercisePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise Analysis") },
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
                // Exercise Selector
                item {
                    ExerciseSelectorCard(
                        selectedExercise = uiState.selectedExercise,
                        onClick = { showExercisePicker = true }
                    )
                }

                // Stats Summary
                uiState.selectedExercise?.let { exercise ->
                    item {
                        StatsOverviewCard(
                            exercise = exercise,
                            stats = uiState.exerciseStats
                        )
                    }

                    // Progress Chart
                    if (uiState.exerciseHistory.isNotEmpty()) {
                        item {
                            ProgressChartCard(
                                selectedChartType = uiState.selectedChartType,
                                onChartTypeChange = { viewModel.setChartType(it) },
                                oneRepMaxData = uiState.oneRepMaxChartData,
                                volumeData = uiState.volumeChartData,
                                weightData = uiState.weightChartData,
                                dates = uiState.chartDates
                            )
                        }

                        // Personal Records
                        item {
                            PersonalRecordsCard(
                                stats = uiState.exerciseStats,
                                history = uiState.exerciseHistory
                            )
                        }

                        // Recent History
                        item {
                            RecentHistoryCard(
                                history = uiState.exerciseHistory.takeLast(10).reversed()
                            )
                        }
                    } else if (!uiState.isLoadingHistory) {
                        item {
                            NoDataCard()
                        }
                    }
                }
            }
        }
    }

    // Exercise Picker Bottom Sheet
    if (showExercisePicker) {
        ExercisePickerSheet(
            exercises = uiState.allExercises,
            selectedExercise = uiState.selectedExercise,
            onExerciseSelect = { exercise ->
                viewModel.selectExercise(exercise)
                showExercisePicker = false
            },
            onDismiss = { showExercisePicker = false }
        )
    }
}

@Composable
fun ExerciseSelectorCard(
    selectedExercise: Exercise?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    text = "Selected Exercise",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedExercise?.name ?: "Select an exercise",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                selectedExercise?.let {
                    Text(
                        text = it.muscleGroup.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatsOverviewCard(
    exercise: Exercise,
    stats: ExerciseAnalysisStats
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Trend indicator
                val trendColor = when (stats.progressTrend) {
                    ProgressTrend.IMPROVING -> Color(0xFF4CAF50)
                    ProgressTrend.DECLINING -> Color(0xFFE53935)
                    ProgressTrend.MAINTAINING -> Color(0xFFFFC107)
                    ProgressTrend.NEW -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val trendIcon = when (stats.progressTrend) {
                    ProgressTrend.IMPROVING -> Icons.Default.TrendingUp
                    ProgressTrend.DECLINING -> Icons.Default.TrendingDown
                    ProgressTrend.MAINTAINING -> Icons.Default.TrendingFlat
                    ProgressTrend.NEW -> Icons.Default.NewReleases
                }
                
                Surface(
                    color = trendColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            trendIcon,
                            contentDescription = null,
                            tint = trendColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stats.progressTrend.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = trendColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = String.format("%.1f", stats.currentOneRepMax),
                    unit = "kg",
                    label = "Est. 1RM"
                )
                StatItem(
                    value = String.format("%.1f", stats.currentMaxWeight),
                    unit = "kg",
                    label = "Max Weight"
                )
                StatItem(
                    value = stats.timesPerformed.toString(),
                    unit = "",
                    label = "Sessions"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = formatVolume(stats.totalVolumeAllTime),
                    unit = "",
                    label = "Total Volume"
                )
                StatItem(
                    value = stats.totalSetsAllTime.toString(),
                    unit = "",
                    label = "Total Sets"
                )
                StatItem(
                    value = String.format("%.1f", stats.avgDaysBetweenSessions),
                    unit = "days",
                    label = "Avg. Gap"
                )
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    unit: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressChartCard(
    selectedChartType: ChartType,
    onChartTypeChange: (ChartType) -> Unit,
    oneRepMaxData: List<com.patrykandpatrick.vico.core.entry.FloatEntry>,
    volumeData: List<com.patrykandpatrick.vico.core.entry.FloatEntry>,
    weightData: List<com.patrykandpatrick.vico.core.entry.FloatEntry>,
    dates: List<Long>
) {
    val chartData = when (selectedChartType) {
        ChartType.ONE_REP_MAX -> oneRepMaxData
        ChartType.WEIGHT -> weightData
        ChartType.VOLUME -> volumeData
    }

    if (chartData.isEmpty()) return

    val chartEntryModelProducer = remember(chartData) {
        ChartEntryModelProducer(listOf(chartData))
    }

    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val axisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        val index = value.toInt()
        if (index in dates.indices) {
            dateFormatter.format(Date(dates[index]))
        } else ""
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Progress Over Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Chart type selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedChartType == ChartType.ONE_REP_MAX,
                    onClick = { onChartTypeChange(ChartType.ONE_REP_MAX) },
                    label = { Text("1RM") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedChartType == ChartType.WEIGHT,
                    onClick = { onChartTypeChange(ChartType.WEIGHT) },
                    label = { Text("Weight") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedChartType == ChartType.VOLUME,
                    onClick = { onChartTypeChange(ChartType.VOLUME) },
                    label = { Text("Volume") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val chartColor = MaterialTheme.colorScheme.primary

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
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (selectedChartType) {
                    ChartType.ONE_REP_MAX -> "Estimated One Rep Max (kg)"
                    ChartType.WEIGHT -> "Best Weight Per Session (kg)"
                    ChartType.VOLUME -> "Total Volume Per Session (kg)"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PersonalRecordsCard(
    stats: ExerciseAnalysisStats,
    history: List<ExerciseHistory>
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    
    // Find various records
    val maxWeightEntry = history.maxByOrNull { it.bestWeight }
    val maxVolumeEntry = history.maxByOrNull { it.totalVolume }
    val maxRepsEntry = history.maxByOrNull { it.bestReps }
    val max1RMEntry = history.maxByOrNull { it.estimatedOneRepMax }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Personal Records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            max1RMEntry?.let { entry ->
                RecordRow(
                    icon = Icons.Default.FitnessCenter,
                    label = "Best Estimated 1RM",
                    value = String.format("%.1f kg", entry.estimatedOneRepMax),
                    date = dateFormatter.format(Date(entry.date))
                )
            }

            maxWeightEntry?.let { entry ->
                RecordRow(
                    icon = Icons.Default.Scale,
                    label = "Heaviest Weight",
                    value = "${entry.bestWeight} kg × ${entry.bestReps}",
                    date = dateFormatter.format(Date(entry.date))
                )
            }

            maxVolumeEntry?.let { entry ->
                RecordRow(
                    icon = Icons.Default.BarChart,
                    label = "Most Volume (Single Session)",
                    value = formatVolume(entry.totalVolume),
                    date = dateFormatter.format(Date(entry.date))
                )
            }

            maxRepsEntry?.let { entry ->
                RecordRow(
                    icon = Icons.Default.Repeat,
                    label = "Most Reps (Single Set)",
                    value = "${entry.bestReps} reps @ ${entry.bestWeight} kg",
                    date = dateFormatter.format(Date(entry.date))
                )
            }
        }
    }
}

@Composable
fun RecordRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    date: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RecentHistoryCard(history: List<ExerciseHistory>) {
    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            history.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = dateFormatter.format(Date(entry.date)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${entry.totalSets} sets • ${entry.totalReps} reps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (entry.isPersonalRecord) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "PR",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "${entry.bestWeight} kg × ${entry.bestReps}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = formatVolume(entry.totalVolume),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (entry != history.last()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun NoDataCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No data yet",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Complete a workout with this exercise to see your progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    exercises: List<Exercise>,
    selectedExercise: Exercise?,
    onExerciseSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }

    val filteredExercises = exercises.filter { exercise ->
        (searchQuery.isEmpty() || exercise.name.contains(searchQuery, ignoreCase = true)) &&
        (selectedMuscleGroup == null || exercise.muscleGroup == selectedMuscleGroup)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Select Exercise",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search exercises...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Muscle group filter
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedMuscleGroup == null,
                        onClick = { selectedMuscleGroup = null },
                        label = { Text("All") }
                    )
                }
                items(MuscleGroup.values().toList()) { muscleGroup ->
                    FilterChip(
                        selected = selectedMuscleGroup == muscleGroup,
                        onClick = { 
                            selectedMuscleGroup = if (selectedMuscleGroup == muscleGroup) null else muscleGroup 
                        },
                        label = { Text(muscleGroup.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Exercise list
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredExercises) { exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = { Text(exercise.muscleGroup.displayName) },
                        leadingContent = {
                            RadioButton(
                                selected = exercise == selectedExercise,
                                onClick = { onExerciseSelect(exercise) }
                            )
                        },
                        modifier = Modifier.clickable { onExerciseSelect(exercise) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatVolume(volume: Double): String {
    return if (volume >= 1000) {
        String.format("%.1fk kg", volume / 1000)
    } else {
        String.format("%.0f kg", volume)
    }
}
