package com.runtracker.app.ui.screens.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.runtracker.shared.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomWorkoutListScreen(
    viewModel: CustomWorkoutViewModel = hiltViewModel(),
    onNavigateToBuilder: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    onBack: () -> Unit
) {
    val workouts by viewModel.workouts.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle sync status
    LaunchedEffect(syncStatus) {
        when (syncStatus) {
            is SyncStatus.Success -> {
                snackbarHostState.showSnackbar("Synced to watch!")
                viewModel.clearSyncStatus()
            }
            is SyncStatus.Error -> {
                snackbarHostState.showSnackbar((syncStatus as SyncStatus.Error).message)
                viewModel.clearSyncStatus()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Workouts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Sync to watch button
                    IconButton(
                        onClick = { viewModel.syncToWatch() },
                        enabled = syncStatus !is SyncStatus.Syncing
                    ) {
                        if (syncStatus is SyncStatus.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Watch, contentDescription = "Sync to Watch")
                        }
                    }
                    IconButton(onClick = {
                        viewModel.startNewWorkout()
                        onNavigateToBuilder()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Workout")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text("All") }
                    )
                }
                items(WorkoutCategory.values().toList()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            
            val filteredWorkouts = viewModel.getFilteredWorkouts()
            
            if (filteredWorkouts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No workouts yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            viewModel.startNewWorkout()
                            onNavigateToBuilder()
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Your First Workout")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredWorkouts, key = { it.id }) { workout ->
                        WorkoutCard(
                            workout = workout,
                            onStart = { onStartWorkout(workout.id) },
                            onEdit = {
                                viewModel.editWorkout(workout)
                                onNavigateToBuilder()
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(workout.id) },
                            onDelete = { viewModel.deleteWorkout(workout.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutCard(
    workout: CustomRunWorkout,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (workout.description.isNotBlank()) {
                        Text(
                            text = workout.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (workout.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (workout.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Workout stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WorkoutStat(
                    icon = Icons.Default.Timer,
                    value = workout.formattedDuration,
                    label = "Duration"
                )
                WorkoutStat(
                    icon = Icons.Default.Route,
                    value = "${workout.phases.size}",
                    label = "Phases"
                )
                WorkoutStat(
                    icon = Icons.Default.Speed,
                    value = workout.difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                    label = "Difficulty"
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Phase preview
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(workout.phases.take(8)) { phase ->
                    PhaseChip(phase = phase)
                }
                if (workout.phases.size > 8) {
                    item {
                        AssistChip(
                            onClick = { },
                            label = { Text("+${workout.phases.size - 8}") }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Workout") },
            text = { Text("Are you sure you want to delete '${workout.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
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
fun WorkoutStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PhaseChip(phase: WorkoutPhase) {
    val color = when (phase.type) {
        PhaseType.WARMUP -> Color(0xFFFF9800)
        PhaseType.COOLDOWN -> Color(0xFF2196F3)
        PhaseType.RECOVERY, PhaseType.REST, PhaseType.FLOAT -> Color(0xFF81C784)
        PhaseType.WORK, PhaseType.TEMPO -> Color(0xFFE53935)
        PhaseType.STRIDE, PhaseType.SURGE -> Color(0xFF9C27B0)
        PhaseType.SPRINT -> Color(0xFFD32F2F)
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = phase.type.name.take(4),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
