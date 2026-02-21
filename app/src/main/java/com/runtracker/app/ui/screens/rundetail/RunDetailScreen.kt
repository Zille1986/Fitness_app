package com.runtracker.app.ui.screens.rundetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runtracker.app.ui.theme.DarkPrimary
import com.runtracker.app.ui.theme.DarkSurface
import androidx.hilt.navigation.compose.hiltViewModel
import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.model.Split
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailScreen(
    runId: Long,
    onBack: () -> Unit,
    viewModel: RunDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.stravaError) {
        uiState.stravaError?.let { error ->
            snackbarHostState.showSnackbar("Strava upload failed: $error")
            viewModel.clearStravaError()
        }
    }
    
    LaunchedEffect(uiState.stravaUploadSuccess) {
        if (uiState.stravaUploadSuccess) {
            snackbarHostState.showSnackbar("Successfully uploaded to Strava!")
            viewModel.clearStravaSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Run Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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
            uiState.run?.let { run ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        RunHeaderCard(run)
                    }

                    item {
                        RunStatsGrid(run)
                    }

                    if (run.splits.isNotEmpty()) {
                        item {
                            Text(
                                text = "Splits",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            SplitsCard(splits = run.splits)
                        }
                    }

                    if (run.notes?.isNotEmpty() == true) {
                        item {
                            NotesCard(notes = run.notes!!)
                        }
                    }

                    // Strava upload card - always shown at the bottom
                    item {
                        StravaCard(
                            run = run,
                            isStravaConnected = uiState.isStravaConnected,
                            isUploading = uiState.isUploadingToStrava,
                            onUpload = { viewModel.uploadToStrava() }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Run?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRun()
                        showDeleteDialog = false
                        onBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RunHeaderCard(run: Run) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DarkPrimary.copy(alpha = 0.15f),
                            DarkSurface
                        )
                    )
                )
        ) {
            // Radial glow behind distance number
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DarkPrimary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatDate(run.startTime),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = String.format("%.2f", run.distanceKm),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkPrimary
                )
                Text(
                    text = "kilometers",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HeaderStatItem(
                        value = run.durationFormatted,
                        label = "Duration"
                    )
                    HeaderStatItem(
                        value = "${run.avgPaceFormatted} /km",
                        label = "Avg Pace"
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun RunStatsGrid(run: Run) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatGridItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Terrain,
                    value = "${run.elevationGainMeters.toInt()}m",
                    label = "Elevation Gain"
                )
                StatGridItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${run.caloriesBurned}",
                    label = "Calories"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                StatGridItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Favorite,
                    value = run.avgHeartRate?.toString() ?: "--",
                    label = "Avg HR"
                )
                StatGridItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FavoriteBorder,
                    value = run.maxHeartRate?.toString() ?: "--",
                    label = "Max HR"
                )
            }
            
            if (run.avgCadence != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatGridItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.DirectionsRun,
                        value = "${run.avgCadence} spm",
                        label = "Avg Cadence"
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StatGridItem(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SplitsCard(splits: List<Split>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "KM",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp)
                )
                Text(
                    text = "Pace",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp)
                )
                Text(
                    text = "Elev",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(60.dp)
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            val avgPace = splits.map { it.paceSecondsPerKm }.average()
            val paceThresholdFast = avgPace * 0.95  // 5% faster = green
            val paceThresholdSlow = avgPace * 1.05  // 5% slower = orange

            splits.forEach { split ->
                // 3-tier pace coloring: green (fast), teal (on pace), orange (slow)
                val paceColor = when {
                    split.paceSecondsPerKm < paceThresholdFast -> Color(0xFF7EE787)   // Green - fast
                    split.paceSecondsPerKm > paceThresholdSlow -> Color(0xFFFFA657)   // Orange - slow
                    else -> DarkPrimary                                                // Teal - on pace
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = split.kilometer.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(
                        text = split.durationFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(
                        text = split.paceFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = paceColor,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(
                        text = "${if (split.elevationChange >= 0) "+" else ""}${split.elevationChange.toInt()}m",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(60.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotesCard(notes: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun StravaCard(
    run: Run,
    isStravaConnected: Boolean,
    isUploading: Boolean,
    onUpload: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = if (run.stravaId != null) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Strava",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                run.stravaId != null -> "Uploaded to Strava"
                                !isStravaConnected -> "Not connected"
                                else -> "Not uploaded"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                when {
                    run.stravaId != null -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Uploaded",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    isUploading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    isStravaConnected -> {
                        Button(
                            onClick = onUpload,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Upload")
                        }
                    }
                }
            }
        }
    }
}
