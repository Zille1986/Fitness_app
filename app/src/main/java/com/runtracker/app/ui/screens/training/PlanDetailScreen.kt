package com.runtracker.app.ui.screens.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.RunRepository
import com.runtracker.shared.data.repository.TrainingPlanRepository
import com.runtracker.shared.data.repository.UserRepository
import com.runtracker.shared.training.TrainingPlanGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.runtracker.shared.util.TimeUtils
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PlanDetailViewModel @Inject constructor(
    private val trainingPlanRepository: TrainingPlanRepository,
    private val runRepository: RunRepository,
    private val userRepository: UserRepository,
    private val watchSyncService: com.runtracker.app.service.WatchSyncService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val planId: Long = savedStateHandle.get<Long>("planId") ?: 0L

    private val _uiState = MutableStateFlow(PlanDetailUiState())
    val uiState: StateFlow<PlanDetailUiState> = _uiState.asStateFlow()

    init {
        loadPlan()
    }

    private fun loadPlan() {
        viewModelScope.launch {
            trainingPlanRepository.getPlanByIdFlow(planId).collect { plan ->
                plan?.let {
                    val currentWeek = calculateCurrentWeek(it.startDate)
                    val groupedWorkouts = it.weeklySchedule.groupBy { w -> w.weekNumber }
                    
                    _uiState.update { state ->
                        state.copy(
                            plan = it,
                            currentWeek = currentWeek,
                            workoutsByWeek = groupedWorkouts,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    private fun calculateCurrentWeek(planStartDate: Long): Int {
        return TimeUtils.calculateWeekNumber(planStartDate, System.currentTimeMillis()).coerceAtLeast(1)
    }

    fun markWorkoutComplete(workoutId: String) {
        viewModelScope.launch {
            _uiState.value.plan?.let { plan ->
                val updatedSchedule = plan.weeklySchedule.map { workout ->
                    if (workout.id == workoutId) {
                        workout.copy(isCompleted = true)
                    } else workout
                }
                trainingPlanRepository.updatePlan(plan.copy(weeklySchedule = updatedSchedule))
            }
        }
    }
    
    fun toggleWorkoutComplete(workoutId: String) {
        viewModelScope.launch {
            _uiState.value.plan?.let { plan ->
                val updatedSchedule = plan.weeklySchedule.map { workout ->
                    if (workout.id == workoutId) {
                        workout.copy(isCompleted = !workout.isCompleted)
                    } else workout
                }
                trainingPlanRepository.updatePlan(plan.copy(weeklySchedule = updatedSchedule))
            }
        }
    }
    
    fun sendWorkoutToWatch(workout: ScheduledWorkout, autoStart: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingToWatch = true) }
            
            // Generate intervals if they're missing
            val workoutWithIntervals = if (workout.intervals.isNullOrEmpty()) {
                workout.copy(intervals = generateIntervalsForWorkout(workout))
            } else {
                workout
            }
            
            val success = watchSyncService.sendWorkoutToWatch(workoutWithIntervals, autoStart)
            _uiState.update { it.copy(
                isSyncingToWatch = false,
                watchSyncMessage = if (success) {
                    if (autoStart) "Starting on watch!" else "Sent to watch!"
                } else "Watch not connected"
            )}
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
                intervals.add(warmup.copy(durationSeconds = 300))
                
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
                
                intervals.add(cooldown.copy(durationSeconds = 300))
            }
        }
        
        return intervals
    }
    
    fun clearWatchSyncMessage() {
        _uiState.update { it.copy(watchSyncMessage = null) }
    }
    
    fun recalculateZones() {
        viewModelScope.launch {
            _uiState.value.plan?.let { plan ->
                val previousRuns = runRepository.getAllRunsOnce()
                val userProfile = userRepository.getProfile().first()
                val userAge = userProfile?.age
                
                if (previousRuns.isEmpty()) {
                    _uiState.update { it.copy(watchSyncMessage = "No runs yet - complete some runs first") }
                    return@launch
                }
                
                val updatedPlan = TrainingPlanGenerator.recalculateZonesForPlan(
                    plan = plan,
                    previousRuns = previousRuns,
                    userAge = userAge
                )
                
                trainingPlanRepository.updatePlan(updatedPlan)
                _uiState.update { it.copy(watchSyncMessage = "Zones updated based on your runs!") }
            }
        }
    }
}

data class PlanDetailUiState(
    val plan: TrainingPlan? = null,
    val currentWeek: Int = 1,
    val workoutsByWeek: Map<Int, List<ScheduledWorkout>> = emptyMap(),
    val isLoading: Boolean = true,
    val isSyncingToWatch: Boolean = false,
    val watchSyncMessage: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    planId: Long,
    onBack: () -> Unit,
    onStartWorkout: () -> Unit,
    onPreviewWorkout: (String) -> Unit = {},
    viewModel: PlanDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.watchSyncMessage) {
        uiState.watchSyncMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearWatchSyncMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.plan?.name ?: "Training Plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
            uiState.plan?.let { plan ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        PlanProgressCard(
                            plan = plan,
                            currentWeek = uiState.currentWeek,
                            onRecalculateZones = { viewModel.recalculateZones() }
                        )
                    }

                    uiState.workoutsByWeek.toSortedMap().forEach { (week, workouts) ->
                        item {
                            WeekHeader(
                                week = week,
                                isCurrentWeek = week == uiState.currentWeek,
                                completedCount = workouts.count { it.isCompleted },
                                totalCount = workouts.size
                            )
                        }

                        items(workouts.sortedBy { it.dayOfWeek }) { workout ->
                            WorkoutCard(
                                workout = workout,
                                onStart = onStartWorkout,
                                onMarkComplete = { viewModel.toggleWorkoutComplete(workout.id) },
                                onSendToWatch = { viewModel.sendWorkoutToWatch(workout, autoStart = false) },
                                onStartOnWatch = { viewModel.sendWorkoutToWatch(workout, autoStart = true) },
                                onPreview = { onPreviewWorkout(workout.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanProgressCard(
    plan: TrainingPlan, 
    currentWeek: Int,
    onRecalculateZones: () -> Unit = {}
) {
    val totalWeeks = plan.weeklySchedule.maxOfOrNull { it.weekNumber } ?: 1
    val completedWorkouts = plan.weeklySchedule.count { it.isCompleted }
    val totalWorkouts = plan.weeklySchedule.size
    val progress = completedWorkouts.toFloat() / totalWorkouts.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Week $currentWeek of $totalWeeks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$completedWorkouts of $totalWorkouts workouts completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onRecalculateZones,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update Zones from My Runs")
            }
        }
    }
}

@Composable
fun WeekHeader(week: Int, isCurrentWeek: Boolean, completedCount: Int, totalCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Week $week",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isCurrentWeek) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "CURRENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        Text(
            text = "$completedCount/$totalCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WorkoutCard(
    workout: ScheduledWorkout,
    onStart: () -> Unit,
    onMarkComplete: () -> Unit,
    onSendToWatch: () -> Unit = {},
    onStartOnWatch: () -> Unit = {},
    onPreview: () -> Unit = {}
) {
    var showWatchMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview() },
        colors = if (workout.isCompleted) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (workout.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (workout.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = getWorkoutIcon(workout.workoutType),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getDayName(workout.dayOfWeek),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatWorkoutType(workout.workoutType),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                workout.targetDistanceMeters?.let { distance ->
                    Text(
                        text = "${String.format("%.1f", distance / 1000)} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row {
                if (!workout.isCompleted) {
                    Box {
                        IconButton(onClick = { showWatchMenu = true }) {
                            Icon(
                                Icons.Default.Watch,
                                contentDescription = "Watch options",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        DropdownMenu(
                            expanded = showWatchMenu,
                            onDismissRequest = { showWatchMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Send to Watch") },
                                onClick = {
                                    showWatchMenu = false
                                    onSendToWatch()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Send, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Start on Watch") },
                                onClick = {
                                    showWatchMenu = false
                                    onStartOnWatch()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                }
                            )
                        }
                    }
                }
                IconButton(onClick = onMarkComplete) {
                    Icon(
                        if (workout.isCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                        contentDescription = if (workout.isCompleted) "Mark incomplete" else "Mark complete",
                        tint = if (workout.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!workout.isCompleted) {
                    IconButton(onClick = onStart) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start workout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun getWorkoutIcon(workoutType: WorkoutType): ImageVector {
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
        Calendar.SUNDAY -> "Sunday"
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> ""
    }
}
