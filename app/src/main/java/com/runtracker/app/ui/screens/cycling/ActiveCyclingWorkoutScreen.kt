package com.runtracker.app.ui.screens.cycling

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.service.SmartTrainerService
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.CyclingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveCyclingWorkoutViewModel @Inject constructor(
    private val cyclingRepository: CyclingRepository,
    private val smartTrainerService: SmartTrainerService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ActiveCyclingUiState())
    val uiState: StateFlow<ActiveCyclingUiState> = _uiState.asStateFlow()
    
    val trainerStatus = smartTrainerService.trainerStatus
    val connectionState = smartTrainerService.connectionState
    
    private var timerJob: Job? = null
    private var startTime: Long = 0
    private val powerReadings = mutableListOf<Int>()
    private val cadenceReadings = mutableListOf<Int>()
    
    fun startWorkout(cyclingType: CyclingType) {
        startTime = System.currentTimeMillis()
        _uiState.update { 
            it.copy(
                cyclingType = cyclingType,
                isActive = true,
                isPaused = false,
                startTime = startTime,
                isSmartTrainer = cyclingType == CyclingType.SMART_TRAINER
            )
        }
        startTimer()
        
        if (cyclingType == CyclingType.SMART_TRAINER) {
            collectTrainerData()
        }
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
    
    private fun collectTrainerData() {
        viewModelScope.launch {
            trainerStatus.collect { status ->
                if (_uiState.value.isActive && !_uiState.value.isPaused) {
                    if (status.currentPowerWatts > 0) {
                        powerReadings.add(status.currentPowerWatts)
                    }
                    if (status.currentCadenceRpm > 0) {
                        cadenceReadings.add(status.currentCadenceRpm)
                    }
                    
                    // Update distance based on speed
                    val speedMs = status.currentSpeedKmh / 3.6
                    _uiState.update { 
                        it.copy(
                            totalDistance = it.totalDistance + speedMs,
                            currentPower = status.currentPowerWatts,
                            currentCadence = status.currentCadenceRpm,
                            currentSpeed = status.currentSpeedKmh,
                            currentHeartRate = status.currentHeartRate
                        )
                    }
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
    
    fun updateDistance(distance: Double) {
        _uiState.update { it.copy(totalDistance = distance) }
    }
    
    fun finishWorkout(onComplete: (Long) -> Unit) {
        timerJob?.cancel()
        val state = _uiState.value
        
        viewModelScope.launch {
            val avgPower = if (powerReadings.isNotEmpty()) powerReadings.average().toInt() else null
            val maxPower = powerReadings.maxOrNull()
            val avgCadence = if (cadenceReadings.isNotEmpty()) cadenceReadings.average().toInt() else null
            
            val workout = CyclingWorkout(
                startTime = state.startTime,
                endTime = System.currentTimeMillis(),
                cyclingType = state.cyclingType,
                distanceMeters = state.totalDistance,
                durationMillis = state.elapsedMillis,
                avgSpeedKmh = if (state.elapsedMillis > 0) {
                    (state.totalDistance / 1000.0) / (state.elapsedMillis / 3600000.0)
                } else 0.0,
                avgPowerWatts = avgPower,
                maxPowerWatts = maxPower,
                avgCadenceRpm = avgCadence,
                isCompleted = true
            )
            
            val id = cyclingRepository.insertWorkout(workout)
            onComplete(id)
        }
    }
    
    fun discardWorkout() {
        timerJob?.cancel()
        _uiState.update { ActiveCyclingUiState() }
    }
}

data class ActiveCyclingUiState(
    val cyclingType: CyclingType = CyclingType.OUTDOOR,
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val isSmartTrainer: Boolean = false,
    val startTime: Long = 0,
    val elapsedMillis: Long = 0,
    val pauseStartTime: Long = 0,
    val pausedDuration: Long = 0,
    val totalDistance: Double = 0.0,
    val currentPower: Int = 0,
    val currentCadence: Int = 0,
    val currentSpeed: Double = 0.0,
    val currentHeartRate: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveCyclingWorkoutScreen(
    cyclingType: CyclingType,
    onFinish: (Long) -> Unit,
    onDiscard: () -> Unit,
    viewModel: ActiveCyclingWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trainerStatus by viewModel.trainerStatus.collectAsState()
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(cyclingType) {
        if (!uiState.isActive) {
            viewModel.startWorkout(cyclingType)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${cyclingType.displayName}") },
                navigationIcon = {
                    IconButton(onClick = { showDiscardDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Discard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF5722)
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
            
            // Stats Grid
            if (uiState.isSmartTrainer) {
                // Smart trainer stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn(
                        value = "${trainerStatus.currentPowerWatts}",
                        label = "Watts"
                    )
                    StatColumn(
                        value = "${trainerStatus.currentCadenceRpm}",
                        label = "RPM"
                    )
                    StatColumn(
                        value = String.format("%.1f", trainerStatus.currentSpeedKmh),
                        label = "km/h"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn(
                        value = String.format("%.1f", uiState.totalDistance / 1000),
                        label = "km"
                    )
                    trainerStatus.currentHeartRate?.let { hr ->
                        StatColumn(
                            value = "$hr",
                            label = "BPM"
                        )
                    }
                }
            } else {
                // Manual entry for non-trainer rides
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn(
                        value = String.format("%.1f", uiState.totalDistance / 1000),
                        label = "km"
                    )
                    StatColumn(
                        value = if (uiState.elapsedMillis > 0 && uiState.totalDistance > 0) {
                            String.format("%.1f", (uiState.totalDistance / 1000.0) / (uiState.elapsedMillis / 3600000.0))
                        } else "0.0",
                        label = "km/h avg"
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Distance input for manual rides
                var distanceText by remember { mutableStateOf("") }
                
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { 
                        distanceText = it
                        it.toDoubleOrNull()?.let { d -> viewModel.updateDistance(d * 1000) }
                    },
                    label = { Text("Distance (km)") },
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
            title = { Text("Finish Ride?") },
            text = { 
                Text("Save this ${formatDuration(uiState.elapsedMillis)} ride (${String.format("%.1f", uiState.totalDistance / 1000)} km)?")
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
            title = { Text("Discard Ride?") },
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
