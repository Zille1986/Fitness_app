package com.runtracker.app.ui.screens.gym

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.GymWorkout
import com.runtracker.shared.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GymHistoryViewModel @Inject constructor(
    private val gymRepository: GymRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GymHistoryUiState())
    val uiState: StateFlow<GymHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            gymRepository.getAllWorkouts().collect { workouts ->
                val completedWorkouts = workouts.filter { it.isCompleted }
                val groupedWorkouts = groupWorkoutsByMonth(completedWorkouts)
                _uiState.update {
                    it.copy(
                        workouts = completedWorkouts,
                        groupedWorkouts = groupedWorkouts,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun groupWorkoutsByMonth(workouts: List<GymWorkout>): Map<String, List<GymWorkout>> {
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        return workouts.groupBy { workout ->
            calendar.timeInMillis = workout.startTime
            monthFormat.format(calendar.time)
        }
    }

    fun deleteWorkout(workout: GymWorkout) {
        viewModelScope.launch {
            gymRepository.deleteWorkout(workout)
        }
    }
}

data class GymHistoryUiState(
    val workouts: List<GymWorkout> = emptyList(),
    val groupedWorkouts: Map<String, List<GymWorkout>> = emptyMap(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymHistoryScreen(
    onWorkoutClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: GymHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var workoutToDelete by remember { mutableStateOf<GymWorkout?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        } else if (uiState.workouts.isEmpty()) {
            EmptyGymHistoryState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    GymHistorySummaryCard(workouts = uiState.workouts)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                uiState.groupedWorkouts.forEach { (month, workouts) ->
                    item {
                        Text(
                            text = month,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(workouts, key = { it.id }) { workout ->
                        GymHistoryWorkoutCard(
                            workout = workout,
                            onClick = { onWorkoutClick(workout.id) },
                            onDelete = { workoutToDelete = workout }
                        )
                    }
                }
            }
        }
    }

    workoutToDelete?.let { workout ->
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            title = { Text("Delete Workout?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWorkout(workout)
                        workoutToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GymHistorySummaryCard(workouts: List<GymWorkout>) {
    val totalVolume = workouts.sumOf { it.totalVolume }
    val totalSets = workouts.sumOf { it.totalSets }
    val totalWorkouts = workouts.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "All Time Stats",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HistorySummaryItem(
                    value = totalWorkouts.toString(),
                    label = "Workouts"
                )
                HistorySummaryItem(
                    value = formatVolume(totalVolume),
                    label = "Volume"
                )
                HistorySummaryItem(
                    value = totalSets.toString(),
                    label = "Sets"
                )
            }
        }
    }
}

@Composable
fun HistorySummaryItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun GymHistoryWorkoutCard(
    workout: GymWorkout,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDate(workout.startTime),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = workout.name.ifEmpty { "Workout" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${workout.exercises.size} exercises • ${workout.totalSets} sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = workout.durationFormatted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatVolume(workout.totalVolume),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyGymHistoryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No workouts recorded yet",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Complete your first gym workout to see it here",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMM d 'at' h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatVolume(volume: Double): String {
    return if (volume >= 1000) {
        String.format("%.1fk kg", volume / 1000)
    } else {
        String.format("%.0f kg", volume)
    }
}
