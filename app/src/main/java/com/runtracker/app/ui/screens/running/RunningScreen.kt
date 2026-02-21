package com.runtracker.app.ui.screens.running

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RunningScreen(
    onFinish: (Long?) -> Unit,
    onBack: () -> Unit,
    viewModel: RunningViewModel = hiltViewModel()
) {
    val trackingState by viewModel.trackingState.collectAsState()
    var showStopConfirmation by remember { mutableStateOf(false) }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    if (!trackingState.isTracking) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!locationPermissions.allPermissionsGranted) {
                PermissionRequest(
                    onRequestPermission = { locationPermissions.launchMultiplePermissionRequest() }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(0.5f))
                    
                    // Main stats display
                    MainStatsDisplay(trackingState)
                    
                    Spacer(modifier = Modifier.weight(0.3f))
                    
                    // Secondary stats
                    SecondaryStatsRow(trackingState)
                    
                    Spacer(modifier = Modifier.weight(0.5f))
                    
                    // Control buttons
                    RunControls(
                        isTracking = trackingState.isTracking,
                        isPaused = trackingState.isPaused,
                        onStart = { viewModel.startRun() },
                        onPause = { viewModel.pauseRun() },
                        onResume = { viewModel.resumeRun() },
                        onStop = { showStopConfirmation = true }
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            title = { Text("Finish Run?") },
            text = { Text("Are you sure you want to finish this run?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopConfirmation = false
                        viewModel.stopRun()
                        onFinish(null)
                    }
                ) {
                    Text("Finish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmation = false }) {
                    Text("Continue")
                }
            }
        )
    }
}

@Composable
fun PermissionRequest(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Location required",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Location Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "RunTracker needs access to your location to track your runs accurately.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun MainStatsDisplay(trackingState: com.runtracker.app.service.TrackingState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Distance
        Text(
            text = String.format("%.2f", trackingState.distanceKm),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "kilometers",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Duration
        Text(
            text = trackingState.durationFormatted,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SecondaryStatsRow(trackingState: com.runtracker.app.service.TrackingState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatBox(
            label = "Pace",
            value = trackingState.paceFormatted,
            unit = "/km"
        )
        StatBox(
            label = "Heart Rate",
            value = trackingState.currentHeartRate?.toString() ?: "--",
            unit = "bpm"
        )
    }
}

@Composable
fun StatBox(label: String, value: String, unit: String) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RunControls(
    isTracking: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isTracking) {
            // Start button
            FloatingActionButton(
                onClick = onStart,
                modifier = Modifier.size(80.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(40.dp)
                )
            }
        } else {
            // Stop button
            FloatingActionButton(
                onClick = onStop,
                modifier = Modifier.size(64.dp),
                containerColor = MaterialTheme.colorScheme.error,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Pause/Resume button
            FloatingActionButton(
                onClick = if (isPaused) onResume else onPause,
                modifier = Modifier.size(80.dp),
                containerColor = if (isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
