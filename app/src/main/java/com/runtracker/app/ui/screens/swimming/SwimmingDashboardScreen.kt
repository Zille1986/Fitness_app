package com.runtracker.app.ui.screens.swimming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.SwimmingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SwimmingDashboardViewModel @Inject constructor(
    private val swimmingRepository: SwimmingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SwimmingDashboardUiState())
    val uiState: StateFlow<SwimmingDashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Load recent workouts
            swimmingRepository.getRecentCompletedWorkouts(5).collect { workouts ->
                _uiState.update { it.copy(recentWorkouts = workouts) }
            }
        }
        
        viewModelScope.launch {
            // Load active plan
            swimmingRepository.getActivePlan().collect { plan ->
                _uiState.update { it.copy(activePlan = plan) }
            }
        }
        
        viewModelScope.launch {
            // Load weekly stats
            val stats = swimmingRepository.getWeeklyStats()
            _uiState.update { it.copy(weeklyStats = stats) }
        }
        
        viewModelScope.launch {
            // Load next workout
            val nextWorkout = swimmingRepository.getNextUpcomingWorkout()
            _uiState.update { it.copy(nextWorkout = nextWorkout) }
        }
    }
    
    fun refresh() {
        loadData()
    }
}

data class SwimmingDashboardUiState(
    val recentWorkouts: List<SwimmingWorkout> = emptyList(),
    val activePlan: SwimmingTrainingPlan? = null,
    val weeklyStats: com.runtracker.shared.data.repository.SwimmingWeeklyStats? = null,
    val nextWorkout: Pair<ScheduledSwimWorkout, Int>? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwimmingDashboardScreen(
    onStartWorkout: (SwimType, PoolLength?) -> Unit = { _, _ -> },
    onViewHistory: () -> Unit = {},
    onViewPlans: () -> Unit = {},
    viewModel: SwimmingDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSwimTypeDialog by remember { mutableStateOf(false) }
    var showPoolLengthDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Swimming",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSwimTypeDialog = true },
                icon = { Icon(Icons.Default.Pool, contentDescription = null) },
                text = { Text("Start Swim") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
            // Next Workout Card
            item {
                NextSwimWorkoutCard(
                    nextWorkout = uiState.nextWorkout,
                    onStartWorkout = { showSwimTypeDialog = true }
                )
            }
            
            // Weekly Stats Card
            item {
                WeeklySwimStatsCard(stats = uiState.weeklyStats)
            }
            
            // Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.History,
                        title = "History",
                        onClick = onViewHistory
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarMonth,
                        title = "Plans",
                        onClick = onViewPlans
                    )
                }
            }
            
            // Recent Workouts
            item {
                Text(
                    text = "Recent Swims",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (uiState.recentWorkouts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Pool,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No swims yet",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Start your first swim to track your progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.recentWorkouts) { workout ->
                    RecentSwimCard(workout = workout)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // Swim Type Selection Dialog
    if (showSwimTypeDialog) {
        SwimTypeSelectionDialog(
            onDismiss = { showSwimTypeDialog = false },
            onSelectType = { swimType ->
                showSwimTypeDialog = false
                if (swimType == SwimType.POOL) {
                    showPoolLengthDialog = true
                } else {
                    onStartWorkout(swimType, null)
                }
            }
        )
    }

    // Pool Length Selection Dialog
    if (showPoolLengthDialog) {
        PoolLengthSelectionDialog(
            onDismiss = { showPoolLengthDialog = false },
            onSelectLength = { poolLength ->
                showPoolLengthDialog = false
                onStartWorkout(SwimType.POOL, poolLength)
            }
        )
    }
}

@Composable
private fun NextSwimWorkoutCard(
    nextWorkout: Pair<ScheduledSwimWorkout, Int>?,
    onStartWorkout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "NEXT SWIM",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (nextWorkout != null) {
                val (workout, daysAhead) = nextWorkout
                val dayLabel = when (daysAhead) {
                    0 -> "Today"
                    1 -> "Tomorrow"
                    else -> "In $daysAhead days"
                }
                
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = workout.workoutType.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = workout.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No scheduled swim",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Start a quick swim or create a plan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeeklySwimStatsCard(
    stats: com.runtracker.shared.data.repository.SwimmingWeeklyStats?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "THIS WEEK",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = String.format("%.1f", (stats?.totalDistanceKm ?: 0.0)),
                    unit = "km",
                    label = "Distance"
                )
                StatItem(
                    value = "${stats?.workoutCount ?: 0}",
                    unit = "",
                    label = "Swims"
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, unit: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0288D1))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RecentSwimCard(workout: SwimmingWorkout) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = workout.swimType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormat.format(Date(workout.startTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%.0fm", workout.distanceMeters),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = workout.durationFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SwimTypeSelectionDialog(
    onDismiss: () -> Unit,
    onSelectType: (SwimType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Swim Type") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SwimType.values().forEach { swimType ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectType(swimType) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (swimType) {
                                    SwimType.POOL -> Icons.Default.Pool
                                    SwimType.OCEAN -> Icons.Default.Waves
                                    SwimType.LAKE -> Icons.Default.Water
                                    SwimType.RIVER -> Icons.Default.Water
                                },
                                contentDescription = null,
                                tint = Color(0xFF0288D1)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = swimType.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PoolLengthSelectionDialog(
    onDismiss: () -> Unit,
    onSelectLength: (PoolLength) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pool Length") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PoolLength.SHORT_COURSE_METERS,
                    PoolLength.LONG_COURSE_METERS,
                    PoolLength.SHORT_COURSE_YARDS
                ).forEach { poolLength ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLength(poolLength) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Pool,
                                contentDescription = null,
                                tint = Color(0xFF0288D1)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = poolLength.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
