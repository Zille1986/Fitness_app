package com.runtracker.app.ui.screens.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.TrainingPlanRepository
import com.runtracker.app.service.WatchSyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutPreviewViewModel @Inject constructor(
    private val trainingPlanRepository: TrainingPlanRepository,
    private val watchSyncService: WatchSyncService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val planId: Long = savedStateHandle.get<Long>("planId") ?: 0L
    private val workoutId: String = savedStateHandle.get<String>("workoutId") ?: ""

    private val _uiState = MutableStateFlow(WorkoutPreviewUiState())
    val uiState: StateFlow<WorkoutPreviewUiState> = _uiState.asStateFlow()

    init {
        loadWorkout()
    }

    private fun loadWorkout() {
        viewModelScope.launch {
            trainingPlanRepository.getPlanByIdFlow(planId).collect { plan: TrainingPlan? ->
                if (plan != null) {
                    val workout: ScheduledWorkout? = plan.weeklySchedule.find { it.id == workoutId }
                    _uiState.update { state: WorkoutPreviewUiState ->
                        state.copy(
                            workout = workout,
                            planName = plan.name,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun sendToWatch(autoStart: Boolean = false) {
        viewModelScope.launch {
            _uiState.value.workout?.let { workout ->
                _uiState.update { it.copy(isSyncing = true) }
                
                // Generate intervals if they're missing
                val workoutWithIntervals = if (workout.intervals.isNullOrEmpty()) {
                    workout.copy(intervals = generateIntervalsForWorkout(workout))
                } else {
                    workout
                }
                
                val success = watchSyncService.sendWorkoutToWatch(workoutWithIntervals, autoStart)
                _uiState.update { it.copy(
                    isSyncing = false,
                    syncMessage = if (success) {
                        if (autoStart) "Starting on watch!" else "Sent to watch!"
                    } else "Watch not connected"
                )}
            }
        }
    }
    
    private fun generateIntervalsForWorkout(workout: ScheduledWorkout): List<Interval> {
        val intervals = mutableListOf<Interval>()
        
        // Calculate warm-up pace (slower than target)
        val warmupPaceMin = (workout.targetPaceMaxSecondsPerKm ?: 390.0) + 30
        val warmupPaceMax = (workout.targetPaceMaxSecondsPerKm ?: 390.0) + 60
        val warmupHrMin = ((workout.targetHeartRateMin ?: 140) * 0.7).toInt()
        val warmupHrMax = ((workout.targetHeartRateMin ?: 140) * 0.85).toInt()
        
        // Warm-up: 10 minutes
        val warmup = Interval(
            type = IntervalType.WARMUP,
            durationSeconds = 600,
            targetPaceMinSecondsPerKm = warmupPaceMin,
            targetPaceMaxSecondsPerKm = warmupPaceMax,
            targetHeartRateZone = HeartRateZone.ZONE_1,
            targetHeartRateMin = warmupHrMin,
            targetHeartRateMax = warmupHrMax
        )
        
        // Cool-down: 10 minutes
        val cooldown = Interval(
            type = IntervalType.COOLDOWN,
            durationSeconds = 600,
            targetPaceMinSecondsPerKm = warmupPaceMin,
            targetPaceMaxSecondsPerKm = warmupPaceMax,
            targetHeartRateZone = HeartRateZone.ZONE_1,
            targetHeartRateMin = warmupHrMin,
            targetHeartRateMax = warmupHrMax
        )
        
        when (workout.workoutType) {
            WorkoutType.INTERVAL_TRAINING -> {
                intervals.add(warmup)
                
                // 6x800m with 400m recovery
                repeat(6) { index ->
                    intervals.add(Interval(
                        type = IntervalType.WORK,
                        distanceMeters = 800.0,
                        targetPaceMinSecondsPerKm = workout.targetPaceMinSecondsPerKm,
                        targetPaceMaxSecondsPerKm = workout.targetPaceMaxSecondsPerKm,
                        targetHeartRateZone = workout.targetHeartRateZone ?: HeartRateZone.ZONE_5,
                        targetHeartRateMin = workout.targetHeartRateMin,
                        targetHeartRateMax = workout.targetHeartRateMax
                    ))
                    if (index < 5) {
                        intervals.add(Interval(
                            type = IntervalType.RECOVERY,
                            distanceMeters = 400.0,
                            targetPaceMinSecondsPerKm = warmupPaceMin,
                            targetPaceMaxSecondsPerKm = warmupPaceMax,
                            targetHeartRateZone = HeartRateZone.ZONE_2,
                            targetHeartRateMin = warmupHrMin,
                            targetHeartRateMax = warmupHrMax
                        ))
                    }
                }
                
                intervals.add(cooldown)
            }
            
            WorkoutType.TEMPO_RUN -> {
                intervals.add(warmup)
                
                // Main tempo portion
                val tempoDuration = ((workout.targetDurationMinutes ?: 40) - 20) * 60
                intervals.add(Interval(
                    type = IntervalType.WORK,
                    durationSeconds = tempoDuration,
                    targetPaceMinSecondsPerKm = workout.targetPaceMinSecondsPerKm,
                    targetPaceMaxSecondsPerKm = workout.targetPaceMaxSecondsPerKm,
                    targetHeartRateZone = workout.targetHeartRateZone ?: HeartRateZone.ZONE_4,
                    targetHeartRateMin = workout.targetHeartRateMin,
                    targetHeartRateMax = workout.targetHeartRateMax
                ))
                
                intervals.add(cooldown)
            }
            
            else -> {
                // Easy run, long run, etc. - simple structure
                intervals.add(warmup.copy(durationSeconds = 300)) // 5 min warm-up
                
                // Main run
                val mainDuration = ((workout.targetDurationMinutes ?: 30) - 10) * 60
                intervals.add(Interval(
                    type = IntervalType.WORK,
                    durationSeconds = mainDuration,
                    targetPaceMinSecondsPerKm = workout.targetPaceMinSecondsPerKm,
                    targetPaceMaxSecondsPerKm = workout.targetPaceMaxSecondsPerKm,
                    targetHeartRateZone = workout.targetHeartRateZone ?: HeartRateZone.ZONE_2,
                    targetHeartRateMin = workout.targetHeartRateMin,
                    targetHeartRateMax = workout.targetHeartRateMax
                ))
                
                intervals.add(cooldown.copy(durationSeconds = 300)) // 5 min cool-down
            }
        }
        
        return intervals
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }
}

data class WorkoutPreviewUiState(
    val workout: ScheduledWorkout? = null,
    val planName: String = "",
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPreviewScreen(
    onStartWorkout: () -> Unit,
    onBack: () -> Unit,
    viewModel: WorkoutPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSyncMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            uiState.workout?.let { workout ->
                if (!workout.isCompleted) {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.sendToWatch(autoStart = false) },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSyncing
                            ) {
                                Icon(Icons.Default.Watch, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send to Watch")
                            }
                            Button(
                                onClick = { viewModel.sendToWatch(autoStart = true) },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSyncing
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start on Watch")
                            }
                        }
                    }
                }
            }
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
            uiState.workout?.let { workout ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card
                    item {
                        WorkoutHeaderCard(workout)
                    }

                    // Overview Card
                    item {
                        WorkoutOverviewCard(workout)
                    }

                    // Target Zones Card
                    item {
                        TargetZonesCard(workout)
                    }

                    // Detailed Workout Breakdown
                    item {
                        WorkoutBreakdownSection(workout)
                    }

                    // Description
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Notes",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = workout.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutHeaderCard(workout: ScheduledWorkout) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getWorkoutIcon(workout.workoutType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = formatWorkoutType(workout.workoutType),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Week ${workout.weekNumber} • ${getDayName(workout.dayOfWeek)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun WorkoutOverviewCard(workout: ScheduledWorkout) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                workout.targetDistanceMeters?.let { distance ->
                    OverviewStat(
                        value = String.format("%.1f", distance / 1000),
                        unit = "km",
                        label = "Distance"
                    )
                }

                workout.targetDurationMinutes?.let { duration ->
                    OverviewStat(
                        value = duration.toString(),
                        unit = "min",
                        label = "Duration"
                    )
                }

                // Estimated duration based on intervals
                workout.intervals?.let { intervals ->
                    val totalSeconds = intervals.sumOf { it.durationSeconds ?: 0 }
                    if (totalSeconds > 0) {
                        OverviewStat(
                            value = (totalSeconds / 60).toString(),
                            unit = "min",
                            label = "Est. Time"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewStat(value: String, unit: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TargetZonesCard(workout: ScheduledWorkout) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Target Zones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pace Zone
            if (workout.targetPaceMinSecondsPerKm != null || workout.targetPaceMaxSecondsPerKm != null) {
                ZoneRow(
                    icon = Icons.Default.Speed,
                    label = "Pace",
                    value = formatPaceRange(
                        workout.targetPaceMinSecondsPerKm,
                        workout.targetPaceMaxSecondsPerKm
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Heart Rate Zone
            if (workout.targetHeartRateMin != null || workout.targetHeartRateMax != null) {
                ZoneRow(
                    icon = Icons.Default.Favorite,
                    label = "Heart Rate",
                    value = "${workout.targetHeartRateMin ?: "?"} - ${workout.targetHeartRateMax ?: "?"} bpm",
                    color = Color(0xFFE53935)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // HR Zone Name
            workout.targetHeartRateZone?.let { zone ->
                ZoneRow(
                    icon = Icons.Default.FitnessCenter,
                    label = "Zone",
                    value = formatHeartRateZone(zone),
                    color = getZoneColor(zone)
                )
            }
        }
    }
}

@Composable
fun ZoneRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun WorkoutBreakdownSection(workout: ScheduledWorkout) {
    val intervals = workout.intervals
    
    // If no intervals data, generate a basic breakdown from workout type
    if (intervals.isNullOrEmpty()) {
        GeneratedWorkoutBreakdown(workout)
        return
    }
    
    // Group intervals by phase
    val warmupIntervals = intervals.filter { it.type == IntervalType.WARMUP }
    val workIntervals = intervals.filter { it.type == IntervalType.WORK || it.type == IntervalType.RECOVERY }
    val cooldownIntervals = intervals.filter { it.type == IntervalType.COOLDOWN }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Workout Breakdown",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        // WARM-UP SECTION
        if (warmupIntervals.isNotEmpty()) {
            PhaseCard(
                title = "🔥 WARM-UP",
                subtitle = "Prepare your body",
                backgroundColor = Color(0xFFFFE0B2),
                accentColor = Color(0xFFFF9800),
                intervals = warmupIntervals
            )
        }
        
        // MAIN WORKOUT SECTION
        if (workIntervals.isNotEmpty()) {
            MainWorkoutCard(
                workoutType = workout.workoutType,
                intervals = workIntervals
            )
        }
        
        // COOL-DOWN SECTION
        if (cooldownIntervals.isNotEmpty()) {
            PhaseCard(
                title = "❄️ COOL-DOWN",
                subtitle = "Recovery & stretch",
                backgroundColor = Color(0xFFE3F2FD),
                accentColor = Color(0xFF2196F3),
                intervals = cooldownIntervals
            )
        }
        
        // WORKOUT SUMMARY
        WorkoutSummaryCard(workout, intervals)
    }
}

@Composable
fun GeneratedWorkoutBreakdown(workout: ScheduledWorkout) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Workout Breakdown",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        // Generate breakdown based on workout type
        when (workout.workoutType) {
            WorkoutType.INTERVAL_TRAINING -> {
                // WARM-UP
                GeneratedPhaseCard(
                    title = "🔥 WARM-UP",
                    subtitle = "Prepare your body",
                    backgroundColor = Color(0xFFFFE0B2),
                    accentColor = Color(0xFFFF9800),
                    duration = "10 min",
                    description = "Easy jog to warm up muscles",
                    paceRange = workout.targetPaceMaxSecondsPerKm?.let { 
                        formatPaceRange(it + 60, it + 90) 
                    } ?: "Easy pace",
                    hrZone = "Zone 1-2 (Recovery/Aerobic)"
                )
                
                // INTERVALS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "💪 INTERVALS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "High intensity repeats",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 6x800m intervals
                        repeat(6) { index ->
                            // Work interval
                            GeneratedIntervalRow(
                                label = "Interval ${index + 1}",
                                duration = "800m",
                                isWork = true,
                                paceRange = formatPaceRange(workout.targetPaceMinSecondsPerKm, workout.targetPaceMaxSecondsPerKm),
                                hrRange = "${workout.targetHeartRateMin ?: "?"} - ${workout.targetHeartRateMax ?: "?"} bpm",
                                hrZone = workout.targetHeartRateZone?.let { formatHeartRateZone(it) }
                            )
                            
                            if (index < 5) {
                                Spacer(modifier = Modifier.height(8.dp))
                                // Recovery
                                GeneratedIntervalRow(
                                    label = "Recovery Jog",
                                    duration = "400m",
                                    isWork = false,
                                    paceRange = "Easy pace",
                                    hrRange = null,
                                    hrZone = "Zone 2 (Aerobic)"
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
                
                // COOL-DOWN
                GeneratedPhaseCard(
                    title = "❄️ COOL-DOWN",
                    subtitle = "Recovery & stretch",
                    backgroundColor = Color(0xFFE3F2FD),
                    accentColor = Color(0xFF2196F3),
                    duration = "10 min",
                    description = "Easy jog, then walk and stretch",
                    paceRange = "Very easy pace",
                    hrZone = "Zone 1 (Recovery)"
                )
            }
            
            WorkoutType.TEMPO_RUN -> {
                GeneratedPhaseCard(
                    title = "🔥 WARM-UP",
                    subtitle = "Prepare your body",
                    backgroundColor = Color(0xFFFFE0B2),
                    accentColor = Color(0xFFFF9800),
                    duration = "10 min",
                    description = "Easy jog",
                    paceRange = "Easy pace",
                    hrZone = "Zone 1-2"
                )
                
                GeneratedPhaseCard(
                    title = "💪 TEMPO",
                    subtitle = "Sustained effort",
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    accentColor = MaterialTheme.colorScheme.primary,
                    duration = workout.targetDurationMinutes?.let { "${it - 20} min" } ?: "20-30 min",
                    description = "Comfortably hard pace",
                    paceRange = formatPaceRange(workout.targetPaceMinSecondsPerKm, workout.targetPaceMaxSecondsPerKm),
                    hrZone = workout.targetHeartRateZone?.let { formatHeartRateZone(it) } ?: "Zone 3-4 (Tempo/Threshold)"
                )
                
                GeneratedPhaseCard(
                    title = "❄️ COOL-DOWN",
                    subtitle = "Recovery",
                    backgroundColor = Color(0xFFE3F2FD),
                    accentColor = Color(0xFF2196F3),
                    duration = "10 min",
                    description = "Easy jog and stretch",
                    paceRange = "Very easy pace",
                    hrZone = "Zone 1"
                )
            }
            
            WorkoutType.LONG_RUN -> {
                GeneratedPhaseCard(
                    title = "🔥 WARM-UP",
                    subtitle = "Start easy",
                    backgroundColor = Color(0xFFFFE0B2),
                    accentColor = Color(0xFFFF9800),
                    duration = "10 min",
                    description = "Very easy start",
                    paceRange = "Easy pace",
                    hrZone = "Zone 1-2"
                )
                
                GeneratedPhaseCard(
                    title = "💪 MAIN RUN",
                    subtitle = "Steady endurance",
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    accentColor = MaterialTheme.colorScheme.primary,
                    duration = workout.targetDistanceMeters?.let { "${((it / 1000) - 2.5).toInt()} km" } ?: "Main portion",
                    description = "Maintain steady effort throughout",
                    paceRange = formatPaceRange(workout.targetPaceMinSecondsPerKm, workout.targetPaceMaxSecondsPerKm),
                    hrZone = workout.targetHeartRateZone?.let { formatHeartRateZone(it) } ?: "Zone 2 (Aerobic)"
                )
                
                GeneratedPhaseCard(
                    title = "❄️ COOL-DOWN",
                    subtitle = "Recovery",
                    backgroundColor = Color(0xFFE3F2FD),
                    accentColor = Color(0xFF2196F3),
                    duration = "10 min",
                    description = "Easy jog, walk, stretch",
                    paceRange = "Very easy pace",
                    hrZone = "Zone 1"
                )
            }
            
            else -> {
                // Default for easy run, recovery run, etc.
                GeneratedPhaseCard(
                    title = "🔥 WARM-UP",
                    subtitle = "Start easy",
                    backgroundColor = Color(0xFFFFE0B2),
                    accentColor = Color(0xFFFF9800),
                    duration = "5 min",
                    description = "Easy start",
                    paceRange = "Easy pace",
                    hrZone = "Zone 1-2"
                )
                
                GeneratedPhaseCard(
                    title = "💪 MAIN RUN",
                    subtitle = formatWorkoutType(workout.workoutType),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    accentColor = MaterialTheme.colorScheme.primary,
                    duration = workout.targetDistanceMeters?.let { "${(it / 1000).toInt()} km" } 
                        ?: workout.targetDurationMinutes?.let { "$it min" } 
                        ?: "As planned",
                    description = "Maintain consistent effort",
                    paceRange = formatPaceRange(workout.targetPaceMinSecondsPerKm, workout.targetPaceMaxSecondsPerKm),
                    hrZone = workout.targetHeartRateZone?.let { formatHeartRateZone(it) } ?: "Zone 2"
                )
                
                GeneratedPhaseCard(
                    title = "❄️ COOL-DOWN",
                    subtitle = "Recovery",
                    backgroundColor = Color(0xFFE3F2FD),
                    accentColor = Color(0xFF2196F3),
                    duration = "5 min",
                    description = "Easy finish and stretch",
                    paceRange = "Very easy pace",
                    hrZone = "Zone 1"
                )
            }
        }
    }
}

@Composable
fun GeneratedPhaseCard(
    title: String,
    subtitle: String,
    backgroundColor: Color,
    accentColor: Color,
    duration: String,
    description: String,
    paceRange: String,
    hrZone: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    color = accentColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = duration,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.7f))
                    .padding(12.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(accentColor.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "TARGET ZONES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pace: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = paceRange,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zone: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = hrZone,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GeneratedIntervalRow(
    label: String,
    duration: String,
    isWork: Boolean,
    paceRange: String,
    hrRange: String?,
    hrZone: String?
) {
    val color = if (isWork) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isWork) color.copy(alpha = 0.15f) else color.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isWork) FontWeight.Bold else FontWeight.Medium,
                color = color
            )
            Surface(
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = duration,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color.copy(alpha = 0.2f))
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "TARGET ZONES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Speed,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Pace: $paceRange",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        if (hrRange != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFE53935)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "HR: $hrRange",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE53935)
                )
            }
        }
        
        if (hrZone != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = color
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Zone: $hrZone",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun PhaseCard(
    title: String,
    subtitle: String,
    backgroundColor: Color,
    accentColor: Color,
    intervals: List<Interval>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor.copy(alpha = 0.7f)
                    )
                }
                
                // Total duration for this phase
                val totalSeconds = intervals.sumOf { it.durationSeconds ?: 0 }
                if (totalSeconds > 0) {
                    Surface(
                        color = accentColor,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = formatDuration(totalSeconds),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            intervals.forEach { interval ->
                PhaseIntervalCard(interval, accentColor)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun PhaseIntervalCard(interval: Interval, accentColor: Color) {
    val duration: Int? = interval.durationSeconds
    val distance: Double? = interval.distanceMeters
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .padding(12.dp)
    ) {
        // Duration/Distance row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (duration != null) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (distance != null) {
                    if (duration != null) Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        Icons.Default.Straighten,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDistance(distance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Target zones section
        val hasPaceTarget = interval.targetPaceMinSecondsPerKm != null || interval.targetPaceMaxSecondsPerKm != null
        val hasHrTarget = interval.targetHeartRateMin != null || interval.targetHeartRateMax != null
        val hasHrZone = interval.targetHeartRateZone != null
        
        if (hasPaceTarget || hasHrTarget || hasHrZone) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(accentColor.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "TARGET ZONES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pace target
            if (hasPaceTarget) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pace: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatPaceRange(interval.targetPaceMinSecondsPerKm, interval.targetPaceMaxSecondsPerKm),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            
            // Heart rate target
            if (hasHrTarget) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFE53935)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Heart Rate: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${interval.targetHeartRateMin ?: "?"} - ${interval.targetHeartRateMax ?: "?"} bpm",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            
            // HR Zone
            val hrZone = interval.targetHeartRateZone
            if (hrZone != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = getZoneColor(hrZone)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zone: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatHeartRateZone(hrZone),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = getZoneColor(hrZone)
                    )
                }
            }
        }
    }
}

@Composable
fun MainWorkoutCard(
    workoutType: WorkoutType,
    intervals: List<Interval>
) {
    val isIntervalWorkout = intervals.any { it.type == IntervalType.RECOVERY }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "💪 MAIN WORKOUT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isIntervalWorkout) "Interval Training" else formatWorkoutType(workoutType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
                
                val totalSeconds = intervals.sumOf { (it.durationSeconds ?: 0) * it.repetitions }
                if (totalSeconds > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = formatDuration(totalSeconds),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isIntervalWorkout) {
                // Show intervals in a structured way
                val workPhases = intervals.filter { it.type == IntervalType.WORK }
                val recoveryPhases = intervals.filter { it.type == IntervalType.RECOVERY }
                
                if (workPhases.isNotEmpty()) {
                    Text(
                        text = "INTERVALS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    workPhases.forEachIndexed { index, workInterval ->
                        // Work interval
                        IntervalBlock(
                            label = "Interval ${index + 1}",
                            interval = workInterval,
                            color = MaterialTheme.colorScheme.primary,
                            isWork = true
                        )
                        
                        // Recovery after (if exists)
                        if (index < recoveryPhases.size) {
                            Spacer(modifier = Modifier.height(4.dp))
                            IntervalBlock(
                                label = "Recovery",
                                interval = recoveryPhases[index],
                                color = MaterialTheme.colorScheme.secondary,
                                isWork = false
                            )
                        }
                        
                        if (index < workPhases.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            } else {
                // Non-interval workout - show single block
                intervals.forEach { interval ->
                    IntervalDetailRow(interval, MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun IntervalBlock(
    label: String,
    interval: Interval,
    color: Color,
    isWork: Boolean
) {
    val duration: Int? = interval.durationSeconds
    val distance: Double? = interval.distanceMeters
    val hasPaceTarget = interval.targetPaceMinSecondsPerKm != null || interval.targetPaceMaxSecondsPerKm != null
    val hasHrTarget = interval.targetHeartRateMin != null || interval.targetHeartRateMax != null
    val hasHrZone = interval.targetHeartRateZone != null
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isWork) color.copy(alpha = 0.15f) else color.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        // Header row with label and duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isWork) FontWeight.Bold else FontWeight.Medium,
                color = color
            )
            
            // Duration/Distance badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (duration != null) {
                    Surface(
                        color = color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = color
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatDuration(duration),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }
                if (distance != null) {
                    Surface(
                        color = color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Straighten,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = color
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatDistance(distance),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }
            }
        }
        
        // Target zones section
        if (hasPaceTarget || hasHrTarget || hasHrZone) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(color.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "TARGET ZONES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            // Pace target
            if (hasPaceTarget) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pace: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatPaceRange(interval.targetPaceMinSecondsPerKm, interval.targetPaceMaxSecondsPerKm),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (hasHrTarget || hasHrZone) Spacer(modifier = Modifier.height(4.dp))
            }
            
            // Heart rate target
            if (hasHrTarget) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFE53935)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HR: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${interval.targetHeartRateMin ?: "?"} - ${interval.targetHeartRateMax ?: "?"} bpm",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    )
                }
                if (hasHrZone) Spacer(modifier = Modifier.height(4.dp))
            }
            
            // HR Zone
            val hrZone2 = interval.targetHeartRateZone
            if (hrZone2 != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = getZoneColor(hrZone2)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zone: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatHeartRateZone(hrZone2),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = getZoneColor(hrZone2)
                    )
                }
            }
        }
    }
}

@Composable
fun IntervalDetailRow(interval: Interval, accentColor: Color) {
    val duration: Int? = interval.durationSeconds
    val distance: Double? = interval.distanceMeters
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side - duration/distance
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (duration != null) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (distance != null) {
                if (duration != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Icon(
                    Icons.Default.Straighten,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDistance(distance),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        // Right side - targets
        Column(horizontalAlignment = Alignment.End) {
            if (interval.targetPaceMinSecondsPerKm != null || interval.targetPaceMaxSecondsPerKm != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatPaceRange(interval.targetPaceMinSecondsPerKm, interval.targetPaceMaxSecondsPerKm),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (interval.targetHeartRateMin != null || interval.targetHeartRateMax != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFE53935)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${interval.targetHeartRateMin ?: "?"}-${interval.targetHeartRateMax ?: "?"} bpm",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutSummaryCard(workout: ScheduledWorkout, intervals: List<Interval>) {
    val totalDuration = intervals.sumOf { (it.durationSeconds ?: 0) * it.repetitions }
    val totalDistance = intervals.sumOf { (it.distanceMeters ?: 0.0) * it.repetitions }
    val warmupTime = intervals.filter { it.type == IntervalType.WARMUP }.sumOf { it.durationSeconds ?: 0 }
    val workTime = intervals.filter { it.type == IntervalType.WORK }.sumOf { (it.durationSeconds ?: 0) * it.repetitions }
    val recoveryTime = intervals.filter { it.type == IntervalType.RECOVERY }.sumOf { (it.durationSeconds ?: 0) * it.repetitions }
    val cooldownTime = intervals.filter { it.type == IntervalType.COOLDOWN }.sumOf { it.durationSeconds ?: 0 }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 WORKOUT SUMMARY",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Time breakdown bar
            if (totalDuration > 0) {
                Text(
                    text = "Time Breakdown",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (warmupTime > 0) {
                        Box(
                            modifier = Modifier
                                .weight(warmupTime.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFFFF9800))
                        )
                    }
                    if (workTime > 0) {
                        Box(
                            modifier = Modifier
                                .weight(workTime.toFloat())
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (recoveryTime > 0) {
                        Box(
                            modifier = Modifier
                                .weight(recoveryTime.toFloat())
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                    if (cooldownTime > 0) {
                        Box(
                            modifier = Modifier
                                .weight(cooldownTime.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFF2196F3))
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (warmupTime > 0) LegendItem("Warm-up", Color(0xFFFF9800), formatDuration(warmupTime))
                    if (workTime > 0) LegendItem("Work", MaterialTheme.colorScheme.primary, formatDuration(workTime))
                    if (recoveryTime > 0) LegendItem("Recovery", MaterialTheme.colorScheme.secondary, formatDuration(recoveryTime))
                    if (cooldownTime > 0) LegendItem("Cool-down", Color(0xFF2196F3), formatDuration(cooldownTime))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatItem(
                    label = "Total Time",
                    value = formatDuration(totalDuration),
                    icon = Icons.Default.Timer
                )
                if (totalDistance > 0) {
                    SummaryStatItem(
                        label = "Est. Distance",
                        value = formatDistance(totalDistance),
                        icon = Icons.Default.Straighten
                    )
                }
                SummaryStatItem(
                    label = "Intervals",
                    value = intervals.filter { it.type == IntervalType.WORK }.size.toString(),
                    icon = Icons.Default.Repeat
                )
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, duration: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SummaryStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatIntervalType(type: IntervalType): String {
    return when (type) {
        IntervalType.WARMUP -> "🔥 Warm-up"
        IntervalType.WORK -> "💪 Work"
        IntervalType.RECOVERY -> "😮‍💨 Recovery"
        IntervalType.COOLDOWN -> "❄️ Cool-down"
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (secs == 0) "$minutes min" else "$minutes:${secs.toString().padStart(2, '0')} min"
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        String.format("%.1f km", meters / 1000)
    } else {
        "${meters.toInt()} m"
    }
}

private fun formatPaceRange(minPace: Double?, maxPace: Double?): String {
    val minFormatted = minPace?.let { formatPace(it) } ?: "?"
    val maxFormatted = maxPace?.let { formatPace(it) } ?: "?"
    return "$minFormatted - $maxFormatted /km"
}

private fun formatPace(secondsPerKm: Double): String {
    val minutes = (secondsPerKm / 60).toInt()
    val seconds = (secondsPerKm % 60).toInt()
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun formatHeartRateZone(zone: HeartRateZone): String {
    return when (zone) {
        HeartRateZone.ZONE_1 -> "Zone 1 - Recovery (50-60%)"
        HeartRateZone.ZONE_2 -> "Zone 2 - Aerobic (60-70%)"
        HeartRateZone.ZONE_3 -> "Zone 3 - Tempo (70-80%)"
        HeartRateZone.ZONE_4 -> "Zone 4 - Threshold (80-90%)"
        HeartRateZone.ZONE_5 -> "Zone 5 - VO2 Max (90-100%)"
    }
}

private fun getZoneColor(zone: HeartRateZone): Color {
    return when (zone) {
        HeartRateZone.ZONE_1 -> Color(0xFF4CAF50) // Green
        HeartRateZone.ZONE_2 -> Color(0xFF8BC34A) // Light Green
        HeartRateZone.ZONE_3 -> Color(0xFFFFEB3B) // Yellow
        HeartRateZone.ZONE_4 -> Color(0xFFFF9800) // Orange
        HeartRateZone.ZONE_5 -> Color(0xFFF44336) // Red
    }
}

private fun getWorkoutIcon(workoutType: WorkoutType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (workoutType) {
        WorkoutType.EASY_RUN -> Icons.Default.DirectionsRun
        WorkoutType.LONG_RUN -> Icons.Default.Hiking
        WorkoutType.TEMPO_RUN -> Icons.Default.Speed
        WorkoutType.INTERVAL_TRAINING -> Icons.Default.Timer
        WorkoutType.HILL_REPEATS -> Icons.Default.Terrain
        WorkoutType.RECOVERY_RUN -> Icons.Default.SelfImprovement
        WorkoutType.RACE_PACE -> Icons.Default.EmojiEvents
        WorkoutType.FARTLEK -> Icons.Default.Shuffle
        WorkoutType.REST_DAY -> Icons.Default.Hotel
        WorkoutType.CROSS_TRAINING -> Icons.Default.FitnessCenter
        else -> Icons.Default.DirectionsRun
    }
}

private fun formatWorkoutType(workoutType: WorkoutType): String {
    return workoutType.name.replace("_", " ").lowercase()
        .replaceFirstChar { it.uppercase() }
}

private fun getDayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        java.util.Calendar.SUNDAY -> "Sunday"
        java.util.Calendar.MONDAY -> "Monday"
        java.util.Calendar.TUESDAY -> "Tuesday"
        java.util.Calendar.WEDNESDAY -> "Wednesday"
        java.util.Calendar.THURSDAY -> "Thursday"
        java.util.Calendar.FRIDAY -> "Friday"
        java.util.Calendar.SATURDAY -> "Saturday"
        else -> ""
    }
}
