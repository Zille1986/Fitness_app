package com.runtracker.app.ui.screens.swimming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.SwimmingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveSwimWorkoutViewModel @Inject constructor(
    private val swimmingRepository: SwimmingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ActiveSwimUiState())
    val uiState: StateFlow<ActiveSwimUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    private var startTime: Long = 0
    
    fun startWorkout(swimType: SwimType, poolLength: PoolLength?) {
        startTime = System.currentTimeMillis()
        _uiState.update { 
            it.copy(
                swimType = swimType,
                poolLength = poolLength,
                isActive = true,
                isPaused = false,
                startTime = startTime
            )
        }
        startTimer()
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_uiState.value.isPaused) {
                    val elapsed = System.currentTimeMillis() - startTime - _uiState.value.pausedDuration
                    _uiState.update { it.copy(elapsedMillis = elapsed) }
                }
            }
        }
    }
    
    fun pauseWorkout() {
        _uiState.update { it.copy(isPaused = true, pauseStartTime = System.currentTimeMillis()) }
    }
    
    fun resumeWorkout() {
        val pausedTime = System.currentTimeMillis() - _uiState.value.pauseStartTime
        _uiState.update { 
            it.copy(
                isPaused = false, 
                pausedDuration = it.pausedDuration + pausedTime
            ) 
        }
    }
    
    fun addLap() {
        val currentState = _uiState.value
        val lapDistance = currentState.poolLength?.meters?.toDouble() ?: 25.0
        _uiState.update { 
            it.copy(
                laps = it.laps + 1,
                totalDistance = it.totalDistance + lapDistance
            )
        }
    }
    
    fun updateDistance(distance: Double) {
        _uiState.update { it.copy(totalDistance = distance) }
    }
    
    fun finishWorkout(onComplete: (Long) -> Unit) {
        timerJob?.cancel()
        val state = _uiState.value
        
        viewModelScope.launch {
            val workout = SwimmingWorkout(
                startTime = state.startTime,
                endTime = System.currentTimeMillis(),
                swimType = state.swimType,
                poolLength = state.poolLength,
                distanceMeters = state.totalDistance,
                durationMillis = state.elapsedMillis,
                laps = state.laps,
                avgPaceSecondsPer100m = if (state.totalDistance > 0) {
                    (state.elapsedMillis / 1000.0) / (state.totalDistance / 100.0)
                } else 0.0,
                isCompleted = true
            )
            
            val id = swimmingRepository.insertWorkout(workout)
            onComplete(id)
        }
    }
    
    fun discardWorkout() {
        timerJob?.cancel()
        _uiState.update { ActiveSwimUiState() }
    }
}

data class ActiveSwimUiState(
    val swimType: SwimType = SwimType.POOL,
    val poolLength: PoolLength? = PoolLength.SHORT_COURSE_METERS,
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val startTime: Long = 0,
    val elapsedMillis: Long = 0,
    val pauseStartTime: Long = 0,
    val pausedDuration: Long = 0,
    val laps: Int = 0,
    val totalDistance: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSwimWorkoutScreen(
    swimType: SwimType,
    poolLength: PoolLength? = null,
    onFinish: (Long) -> Unit,
    onDiscard: () -> Unit,
    viewModel: ActiveSwimWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(swimType) {
        if (!uiState.isActive) {
            viewModel.startWorkout(swimType, poolLength)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${swimType.displayName} Swim") },
                navigationIcon = {
                    IconButton(onClick = { showDiscardDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Discard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0288D1)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Timer Display
            Text(
                text = formatDuration(uiState.elapsedMillis),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    value = "${uiState.laps}",
                    label = "Laps"
                )
                StatColumn(
                    value = String.format("%.0f", uiState.totalDistance),
                    label = "Meters"
                )
                StatColumn(
                    value = if (uiState.totalDistance > 0) {
                        formatPace((uiState.elapsedMillis / 1000.0) / (uiState.totalDistance / 100.0))
                    } else "--:--",
                    label = "Pace/100m"
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Lap Button (for pool swims)
            if (swimType == SwimType.POOL) {
                Button(
                    onClick = { viewModel.addLap() },
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0288D1)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Text("Lap", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tap after each lap (${uiState.poolLength?.displayName ?: "25m"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // For open water, show distance input
                var distanceText by remember { mutableStateOf("") }
                
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { 
                        distanceText = it
                        it.toDoubleOrNull()?.let { d -> viewModel.updateDistance(d) }
                    },
                    label = { Text("Distance (meters)") },
                    modifier = Modifier.fillMaxWidth(0.6f),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Pause/Resume Button
                FilledTonalButton(
                    onClick = {
                        if (uiState.isPaused) viewModel.resumeWorkout()
                        else viewModel.pauseWorkout()
                    },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (uiState.isPaused) "Resume" else "Pause",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Finish Button
                Button(
                    onClick = { showFinishDialog = true },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Finish",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Finish Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finish Swim?") },
            text = { 
                Text("Save this ${formatDuration(uiState.elapsedMillis)} swim with ${uiState.laps} laps (${uiState.totalDistance.toInt()}m)?")
            },
            confirmButton = {
                Button(onClick = {
                    showFinishDialog = false
                    viewModel.finishWorkout(onFinish)
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Discard Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Swim?") },
            text = { Text("Are you sure you want to discard this workout?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardWorkout()
                        onDiscard()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun formatPace(secondsPer100m: Double): String {
    if (secondsPer100m <= 0 || secondsPer100m.isInfinite() || secondsPer100m.isNaN()) return "--:--"
    val minutes = (secondsPer100m / 60).toInt()
    val seconds = (secondsPer100m % 60).toInt()
    return String.format("%d:%02d", minutes, seconds)
}
