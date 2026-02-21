package com.runtracker.app.ui.screens.cycling

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
import com.runtracker.app.service.SmartTrainerService
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.CyclingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CyclingDashboardViewModel @Inject constructor(
    private val cyclingRepository: CyclingRepository,
    private val smartTrainerService: SmartTrainerService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CyclingDashboardUiState())
    val uiState: StateFlow<CyclingDashboardUiState> = _uiState.asStateFlow()
    
    val trainerStatus = smartTrainerService.trainerStatus
    val connectionState = smartTrainerService.connectionState
    val discoveredDevices = smartTrainerService.discoveredDevices
    val isScanning = smartTrainerService.isScanning
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            cyclingRepository.getRecentCompletedWorkouts(5).collect { workouts ->
                _uiState.update { it.copy(recentWorkouts = workouts) }
            }
        }
        
        viewModelScope.launch {
            cyclingRepository.getActivePlan().collect { plan ->
                _uiState.update { it.copy(activePlan = plan) }
            }
        }
        
        viewModelScope.launch {
            val stats = cyclingRepository.getWeeklyStats()
            _uiState.update { it.copy(weeklyStats = stats) }
        }
        
        viewModelScope.launch {
            val nextWorkout = cyclingRepository.getNextUpcomingWorkout()
            _uiState.update { it.copy(nextWorkout = nextWorkout) }
        }
    }
    
    fun startScanning() {
        smartTrainerService.startScanning()
    }
    
    fun stopScanning() {
        smartTrainerService.stopScanning()
    }
    
    fun connectToDevice(device: SmartTrainerDevice) {
        smartTrainerService.connectToDevice(device)
    }
    
    fun disconnect() {
        smartTrainerService.disconnect()
    }
    
    fun hasBluetoothPermissions(): Boolean = smartTrainerService.hasBluetoothPermissions()
    
    fun isBluetoothEnabled(): Boolean = smartTrainerService.isBluetoothEnabled()
    
    fun refresh() {
        loadData()
    }
}

data class CyclingDashboardUiState(
    val recentWorkouts: List<CyclingWorkout> = emptyList(),
    val activePlan: CyclingTrainingPlan? = null,
    val weeklyStats: com.runtracker.shared.data.repository.CyclingWeeklyStats? = null,
    val nextWorkout: Pair<ScheduledCyclingWorkout, Int>? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyclingDashboardScreen(
    onStartWorkout: (CyclingType) -> Unit = {},
    onViewHistory: () -> Unit = {},
    onViewPlans: () -> Unit = {},
    viewModel: CyclingDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trainerStatus by viewModel.trainerStatus.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    
    var showCyclingTypeDialog by remember { mutableStateOf(false) }
    var showTrainerDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Cycling",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = { showTrainerDialog = true }) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = "Connect Trainer",
                            tint = if (connectionState == SmartTrainerService.ConnectionState.READY) 
                                MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCyclingTypeDialog = true },
                icon = { Icon(Icons.Default.DirectionsBike, contentDescription = null) },
                text = { Text("Start Ride") },
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
            // Smart Trainer Status Card (if connected)
            if (connectionState == SmartTrainerService.ConnectionState.READY) {
                item {
                    SmartTrainerStatusCard(status = trainerStatus)
                }
            }
            
            // Next Workout Card
            item {
                NextCyclingWorkoutCard(
                    nextWorkout = uiState.nextWorkout,
                    onStartWorkout = { showCyclingTypeDialog = true }
                )
            }
            
            // Weekly Stats Card
            item {
                WeeklyCyclingStatsCard(stats = uiState.weeklyStats)
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
                    text = "Recent Rides",
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
                                Icons.Default.DirectionsBike,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No rides yet",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Start your first ride to track your progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.recentWorkouts) { workout ->
                    RecentCyclingCard(workout = workout)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // Cycling Type Selection Dialog
    if (showCyclingTypeDialog) {
        CyclingTypeSelectionDialog(
            isTrainerConnected = connectionState == SmartTrainerService.ConnectionState.READY,
            onDismiss = { showCyclingTypeDialog = false },
            onSelectType = { cyclingType ->
                showCyclingTypeDialog = false
                onStartWorkout(cyclingType)
            }
        )
    }
    
    // Smart Trainer Connection Dialog
    if (showTrainerDialog) {
        SmartTrainerDialog(
            connectionState = connectionState,
            discoveredDevices = discoveredDevices,
            isScanning = isScanning,
            hasPermissions = viewModel.hasBluetoothPermissions(),
            isBluetoothEnabled = viewModel.isBluetoothEnabled(),
            onStartScan = { viewModel.startScanning() },
            onStopScan = { viewModel.stopScanning() },
            onConnect = { viewModel.connectToDevice(it) },
            onDisconnect = { viewModel.disconnect() },
            onDismiss = { showTrainerDialog = false }
        )
    }
}

@Composable
private fun SmartTrainerStatusCard(status: SmartTrainerStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Trainer Connected",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrainerStatItem(value = "${status.currentPowerWatts}", unit = "W", label = "Power")
                TrainerStatItem(value = "${status.currentCadenceRpm}", unit = "rpm", label = "Cadence")
                TrainerStatItem(value = String.format("%.1f", status.currentSpeedKmh), unit = "km/h", label = "Speed")
                status.currentHeartRate?.let {
                    TrainerStatItem(value = "$it", unit = "bpm", label = "HR")
                }
            }
        }
    }
}

@Composable
private fun TrainerStatItem(value: String, unit: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NextCyclingWorkoutCard(
    nextWorkout: Pair<ScheduledCyclingWorkout, Int>?,
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
                text = "NEXT RIDE",
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
                    text = "No scheduled ride",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Start a quick ride or create a plan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeeklyCyclingStatsCard(
    stats: com.runtracker.shared.data.repository.CyclingWeeklyStats?
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
                    label = "Rides"
                )
                if ((stats?.averagePowerWatts ?: 0.0) > 0) {
                    StatItem(
                        value = String.format("%.0f", stats?.averagePowerWatts ?: 0.0),
                        unit = "W",
                        label = "Avg Power"
                    )
                }
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
            Icon(icon, contentDescription = null, tint = Color(0xFFFF5722))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RecentCyclingCard(workout: CyclingWorkout) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = workout.cyclingType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (workout.isSmartTrainerWorkout) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = "Smart Trainer",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
                Text(
                    text = dateFormat.format(Date(workout.startTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%.1f km", workout.distanceKm),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    Text(
                        text = workout.durationFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    workout.avgPowerWatts?.let { power ->
                        Text(
                            text = " • ${power}W",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CyclingTypeSelectionDialog(
    isTrainerConnected: Boolean,
    onDismiss: () -> Unit,
    onSelectType: (CyclingType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Ride Type") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CyclingType.values().forEach { cyclingType ->
                    val isSmartTrainer = cyclingType == CyclingType.SMART_TRAINER
                    val enabled = !isSmartTrainer || isTrainerConnected
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { onSelectType(cyclingType) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (enabled) 
                                MaterialTheme.colorScheme.surface 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (cyclingType) {
                                    CyclingType.OUTDOOR -> Icons.Default.DirectionsBike
                                    CyclingType.SMART_TRAINER -> Icons.Default.Bluetooth
                                    CyclingType.STATIONARY_BIKE -> Icons.Default.FitnessCenter
                                    CyclingType.SPIN_CLASS -> Icons.Default.Groups
                                },
                                contentDescription = null,
                                tint = if (enabled) Color(0xFFFF5722) else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = cyclingType.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (enabled) 
                                        MaterialTheme.colorScheme.onSurface 
                                    else 
                                        Color.Gray
                                )
                                if (isSmartTrainer && !isTrainerConnected) {
                                    Text(
                                        text = "Connect trainer first",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
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
private fun SmartTrainerDialog(
    connectionState: SmartTrainerService.ConnectionState,
    discoveredDevices: List<SmartTrainerDevice>,
    isScanning: Boolean,
    hasPermissions: Boolean,
    isBluetoothEnabled: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (SmartTrainerDevice) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bluetooth, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Smart Trainer") 
            }
        },
        text = {
            Column {
                when {
                    !hasPermissions -> {
                        Text("Bluetooth permissions required. Please grant permissions in Settings.")
                    }
                    !isBluetoothEnabled -> {
                        Text("Please enable Bluetooth to connect to your smart trainer.")
                    }
                    connectionState == SmartTrainerService.ConnectionState.READY -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connected", color = Color(0xFF4CAF50))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Disconnect")
                        }
                    }
                    connectionState == SmartTrainerService.ConnectionState.CONNECTING ||
                    connectionState == SmartTrainerService.ConnectionState.DISCOVERING_SERVICES -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Connecting...")
                        }
                    }
                    else -> {
                        if (isScanning) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scanning for trainers...")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onStopScan) {
                                Text("Stop Scanning")
                            }
                        } else {
                            Button(onClick = onStartScan) {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan for Trainers")
                            }
                        }
                        
                        if (discoveredDevices.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Found Devices:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            discoveredDevices.forEach { device ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onConnect(device) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = device.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = device.brand,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
