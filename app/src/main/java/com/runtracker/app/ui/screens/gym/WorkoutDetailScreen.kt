package com.runtracker.app.ui.screens.gym

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.GymWorkout
import com.runtracker.shared.data.model.WorkoutExercise
import com.runtracker.shared.data.model.WorkoutSet
import com.runtracker.shared.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val gymRepository: GymRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workoutId: Long = savedStateHandle.get<Long>("workoutId") ?: 0L

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    init {
        loadWorkout()
    }

    private fun loadWorkout() {
        viewModelScope.launch {
            gymRepository.getWorkoutById(workoutId).collect { workout ->
                _uiState.update {
                    it.copy(
                        workout = workout,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteWorkout() {
        viewModelScope.launch {
            _uiState.value.workout?.let { workout ->
                gymRepository.deleteWorkout(workout)
            }
        }
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, newWeight: Double?, newReps: Int?) {
        viewModelScope.launch {
            val workout = _uiState.value.workout ?: return@launch
            val exercise = workout.exercises.getOrNull(exerciseIndex) ?: return@launch
            val set = exercise.sets.getOrNull(setIndex) ?: return@launch

            val updatedSet = set.copy(
                weight = newWeight ?: set.weight,
                reps = newReps ?: set.reps
            )

            val updatedSets = exercise.sets.toMutableList().apply { this[setIndex] = updatedSet }
            val updatedExercise = exercise.copy(sets = updatedSets)
            val updatedExercises = workout.exercises.toMutableList().apply { this[exerciseIndex] = updatedExercise }

            // Recalculate workout totals
            val totalVolume = updatedExercises.sumOf { it.totalVolume }
            val totalSets = updatedExercises.sumOf { it.completedSets }
            val totalReps = updatedExercises.sumOf { it.totalReps }

            val updatedWorkout = workout.copy(
                exercises = updatedExercises,
                totalVolume = totalVolume,
                totalSets = totalSets,
                totalReps = totalReps
            )

            gymRepository.updateWorkout(updatedWorkout)

            // Recalculate exercise history (fixes PR/stats)
            gymRepository.recalculateExerciseHistory(
                workoutId = workout.id,
                exerciseId = exercise.exerciseId,
                sets = updatedSets
            )
        }
    }
}

data class WorkoutDetailUiState(
    val workout: GymWorkout? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: Long,
    onBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Details") },
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
            uiState.workout?.let { workout ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        WorkoutSummaryCard(workout)
                    }

                    item {
                        Text(
                            text = "Exercises",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    itemsIndexed(workout.exercises) { exerciseIndex, exercise ->
                        ExerciseDetailCard(
                            exercise = exercise,
                            onUpdateSet = { setIndex, newWeight, newReps ->
                                viewModel.updateSet(exerciseIndex, setIndex, newWeight, newReps)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Workout?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWorkout()
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
fun WorkoutSummaryCard(workout: GymWorkout) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatDate(workout.startTime),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = workout.name.ifEmpty { "Workout" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WorkoutSummaryStatItem(
                    value = workout.durationFormatted,
                    label = "Duration"
                )
                WorkoutSummaryStatItem(
                    value = workout.exercises.size.toString(),
                    label = "Exercises"
                )
                WorkoutSummaryStatItem(
                    value = workout.totalSets.toString(),
                    label = "Sets"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WorkoutSummaryStatItem(
                    value = formatVolume(workout.totalVolume),
                    label = "Volume"
                )
                WorkoutSummaryStatItem(
                    value = workout.totalReps.toString(),
                    label = "Reps"
                )
            }
        }
    }
}

@Composable
fun WorkoutSummaryStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ExerciseDetailCard(
    exercise: WorkoutExercise,
    onUpdateSet: (setIndex: Int, newWeight: Double?, newReps: Int?) -> Unit = { _, _, _ -> }
) {
    var showChart by remember { mutableStateOf(false) }
    val chartViewModel: ExerciseProgressChartViewModel = hiltViewModel(
        key = "detail_progress_${exercise.exerciseId}"
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showChart = !showChart }) {
                    Icon(
                        Icons.Default.ShowChart,
                        contentDescription = "Progress Chart",
                        tint = if (showChart) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress Chart (expandable)
            AnimatedVisibility(
                visible = showChart,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    ExerciseProgressChart(
                        exerciseId = exercise.exerciseId,
                        exerciseName = exercise.exerciseName,
                        viewModel = chartViewModel
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SET",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "WEIGHT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "VOLUME",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            val completedSets = exercise.sets.filter { it.isCompleted }
            completedSets.forEachIndexed { _, set ->
                val actualSetIndex = exercise.sets.indexOf(set)
                EditableSetRow(
                    set = set,
                    onWeightChanged = { newWeight ->
                        onUpdateSet(actualSetIndex, newWeight, null)
                    },
                    onRepsChanged = { newReps ->
                        onUpdateSet(actualSetIndex, null, newReps)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${exercise.totalVolume.toInt()} kg",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EditableSetRow(
    set: WorkoutSet,
    onWeightChanged: (Double) -> Unit,
    onRepsChanged: (Int) -> Unit
) {
    var editingWeight by remember { mutableStateOf(false) }
    var editingReps by remember { mutableStateOf(false) }
    var weightText by remember(set.weight) { mutableStateOf(set.weight.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }) }
    var repsText by remember(set.reps) { mutableStateOf(set.reps.toString()) }
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number
        Text(
            text = set.setNumber.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Weight (editable)
        Box(modifier = Modifier.weight(1f)) {
            if (editingWeight) {
                val focusRequester = remember { FocusRequester() }
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    modifier = Modifier
                        .width(80.dp)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val newWeight = weightText.toDoubleOrNull()
                            if (newWeight != null && newWeight >= 0) {
                                onWeightChanged(newWeight)
                            } else {
                                weightText = set.weight.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }
                            }
                            editingWeight = false
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            } else {
                Text(
                    text = "${set.weight} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { editingWeight = true }
                )
            }
        }

        // Reps (editable)
        Box(modifier = Modifier.weight(1f)) {
            if (editingReps) {
                val focusRequester = remember { FocusRequester() }
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    modifier = Modifier
                        .width(60.dp)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val newReps = repsText.toIntOrNull()
                            if (newReps != null && newReps >= 0) {
                                onRepsChanged(newReps)
                            } else {
                                repsText = set.reps.toString()
                            }
                            editingReps = false
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            } else {
                Text(
                    text = set.reps.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { editingReps = true }
                )
            }
        }

        // Volume (calculated, not editable)
        Text(
            text = "${(set.weight * set.reps).toInt()} kg",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatVolume(volume: Double): String {
    return if (volume >= 1000) {
        String.format("%.1fk kg", volume / 1000)
    } else {
        String.format("%.0f kg", volume)
    }
}
