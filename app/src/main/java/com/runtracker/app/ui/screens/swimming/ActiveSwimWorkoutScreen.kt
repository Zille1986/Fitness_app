package com.runtracker.app.ui.screens.swimming

import android.Manifest
import android.content.Context
import android.os.Looper
import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.runtracker.app.service.SwimLapDetector
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.SwimmingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveSwimWorkoutViewModel @Inject constructor(
    private val swimmingRepository: SwimmingRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveSwimUiState())
    val uiState: StateFlow<ActiveSwimUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var autoSaveJob: Job? = null
    private var startTime: Long = 0

    // Accelerometer lap detector (pool swims)
    private var lapDetector: SwimLapDetector? = null
    private var lapCollectorJob: Job? = null

    // GPS tracking (open water swims)
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val gpsPoints = mutableListOf<Pair<Double, Double>>() // lat, lon

    fun startWorkout(swimType: SwimType, poolLength: PoolLength?) {
        startTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                swimType = swimType,
                poolLength = if (swimType == SwimType.POOL) poolLength else null,
                isActive = true,
                isPaused = false,
                startTime = startTime,
                autoDetectEnabled = swimType == SwimType.POOL
            )
        }
        startTimer()
        startAutoSave()

        if (swimType == SwimType.POOL) {
            startLapDetection()
        } else {
            startGpsTracking()
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

    /** Periodically saves current state so data isn't lost on crash. */
    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(30_000) // every 30 seconds
                val state = _uiState.value
                if (state.isActive && state.totalDistance > 0) {
                    saveWorkoutSnapshot(state, isCompleted = false)
                }
            }
        }
    }

    private fun startLapDetection() {
        lapDetector = SwimLapDetector(appContext).also { detector ->
            detector.start()
            lapCollectorJob = viewModelScope.launch {
                detector.lapDetected.collect { detectedCount ->
                    if (detectedCount > 0) {
                        val currentState = _uiState.value
                        val expectedLaps = currentState.autoDetectedLaps
                        if (detectedCount > expectedLaps) {
                            val lapDistance = currentState.poolLength?.meters?.toDouble() ?: 25.0
                            val newLaps = detectedCount - expectedLaps
                            _uiState.update {
                                it.copy(
                                    laps = it.laps + newLaps,
                                    totalDistance = it.totalDistance + (lapDistance * newLaps),
                                    autoDetectedLaps = detectedCount
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("MissingPermission") // Permission checked in the composable
    private fun startGpsTracking() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
        gpsPoints.clear()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(3f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val newPoint = loc.latitude to loc.longitude

                if (gpsPoints.isNotEmpty()) {
                    val last = gpsPoints.last()
                    val dist = distanceBetween(last.first, last.second, newPoint.first, newPoint.second)
                    _uiState.update { it.copy(totalDistance = it.totalDistance + dist) }
                }
                gpsPoints.add(newPoint)
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } catch (_: SecurityException) {
            // Location permission not granted — fall back to manual
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

    fun undoLap() {
        val currentState = _uiState.value
        if (currentState.laps <= 0) return
        val lapDistance = currentState.poolLength?.meters?.toDouble() ?: 25.0
        _uiState.update {
            it.copy(
                laps = (it.laps - 1).coerceAtLeast(0),
                totalDistance = (it.totalDistance - lapDistance).coerceAtLeast(0.0)
            )
        }
    }

    fun toggleAutoDetect() {
        val current = _uiState.value.autoDetectEnabled
        _uiState.update { it.copy(autoDetectEnabled = !current) }
        if (current) {
            lapDetector?.stop()
        } else {
            startLapDetection()
        }
    }

    fun updateDistance(distance: Double) {
        _uiState.update { it.copy(totalDistance = distance) }
    }

    fun finishWorkout(onComplete: (Long) -> Unit) {
        stopTracking()
        val state = _uiState.value

        viewModelScope.launch {
            val id = saveWorkoutSnapshot(state, isCompleted = true)
            onComplete(id)
        }
    }

    fun discardWorkout() {
        stopTracking()
        _uiState.update { ActiveSwimUiState() }
    }

    private fun stopTracking() {
        timerJob?.cancel()
        autoSaveJob?.cancel()
        lapCollectorJob?.cancel()
        lapDetector?.stop()
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
    }

    private suspend fun saveWorkoutSnapshot(state: ActiveSwimUiState, isCompleted: Boolean): Long {
        val routePoints = gpsPoints.mapIndexed { i, (lat, lon) ->
            RoutePoint(latitude = lat, longitude = lon, altitude = 0.0, timestamp = state.startTime + (i * 5000L))
        }

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
            routePoints = routePoints,
            source = SwimSource.PHONE,
            isCompleted = isCompleted
        )

        return swimmingRepository.insertWorkout(workout)
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }

    /** Haversine distance in meters. */
    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}

data class ActiveSwimUiState(
    val swimType: SwimType = SwimType.POOL,
    val poolLength: PoolLength? = null,
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val startTime: Long = 0,
    val elapsedMillis: Long = 0,
    val pauseStartTime: Long = 0,
    val pausedDuration: Long = 0,
    val laps: Int = 0,
    val totalDistance: Double = 0.0,
    val autoDetectEnabled: Boolean = false,
    val autoDetectedLaps: Int = 0
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
    val view = LocalView.current
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
                    containerColor = Color(0xFF0288D1),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
                StatColumn(value = "${uiState.laps}", label = "Laps")
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

            // Pool: Lap button + undo + auto-detect toggle
            if (swimType == SwimType.POOL) {
                // Lap button with haptic feedback
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        viewModel.addLap()
                    },
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

                Spacer(modifier = Modifier.height(12.dp))

                // Undo button
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        viewModel.undoLap()
                    },
                    enabled = uiState.laps > 0
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Undo last lap")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap after each lap (${uiState.poolLength?.displayName ?: "25m"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-detect toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Auto-detect laps",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Uses accelerometer to detect wall turns",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.autoDetectEnabled,
                            onCheckedChange = { viewModel.toggleAutoDetect() }
                        )
                    }
                }

                if (uiState.autoDetectEnabled && uiState.autoDetectedLaps > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.autoDetectedLaps} laps auto-detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0288D1),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Open water: GPS-tracked distance + manual override
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = Color(0xFF0288D1)
                        )
                        Column {
                            Text(
                                text = "GPS tracking active",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Distance updates automatically between dips",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual distance override
                var distanceText by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = {
                        distanceText = it
                        it.toDoubleOrNull()?.let { d -> viewModel.updateDistance(d) }
                    },
                    label = { Text("Override distance (meters)") },
                    modifier = Modifier.fillMaxWidth(0.6f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Auto-save indicator
            Text(
                text = "Auto-saving every 30s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Pause/Resume Button
                FilledTonalButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        showFinishDialog = true
                    },
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
