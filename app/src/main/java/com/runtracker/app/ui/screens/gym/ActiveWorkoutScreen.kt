package com.runtracker.app.ui.screens.gym

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runtracker.shared.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    workoutId: Long,
    selectedExerciseId: Long? = null,
    onExerciseAdded: () -> Unit = {},
    onFinish: () -> Unit,
    onAddExercise: () -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFinishDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    // Handle selected exercise from picker
    LaunchedEffect(selectedExerciseId) {
        selectedExerciseId?.let { exerciseId ->
            viewModel.addExerciseById(exerciseId)
            onExerciseAdded()
        }
    }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onFinish()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.workout?.name ?: "Workout",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = uiState.elapsedFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showCancelDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = { showFinishDialog = true }) {
                        Text("Finish", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExercise,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.workout?.exercises?.let { exercises ->
                        if (exercises.isEmpty()) {
                            item {
                                EmptyExercisesCard(onAddExercise = onAddExercise)
                            }
                        } else {
                            itemsIndexed(exercises) { exerciseIndex, exercise ->
                                ExerciseCard(
                                    exercise = exercise,
                                    exerciseIndex = exerciseIndex,
                                    exercisePB = uiState.exercisePBs[exercise.exerciseId],
                                    lastWorkout = uiState.exerciseLastWorkouts[exercise.exerciseId],
                                    onAddSet = { viewModel.addSet(exerciseIndex) },
                                    onRemoveSet = { setIndex -> 
                                        viewModel.removeSet(exerciseIndex, setIndex) 
                                    },
                                    onUpdateSet = { setIndex, weight, reps ->
                                        viewModel.updateSet(exerciseIndex, setIndex, weight, reps)
                                    },
                                    onCompleteSet = { setIndex ->
                                        viewModel.completeSet(exerciseIndex, setIndex)
                                    },
                                    onRemoveExercise = { viewModel.removeExercise(exerciseIndex) }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            // Rest Timer Overlay
            AnimatedVisibility(
                visible = uiState.isRestTimerActive,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                RestTimerBar(
                    timeRemaining = uiState.restTimeFormatted,
                    onSkip = { viewModel.skipRestTimer() }
                )
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finish Workout?") },
            text = {
                val completedSets = uiState.workout?.exercises?.sumOf { it.completedSets } ?: 0
                Text("You've completed $completedSets sets. Save this workout?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    viewModel.finishWorkout()
                }) {
                    Text("Finish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Continue")
                }
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Workout?") },
            text = { Text("This workout will be discarded and not saved.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelWorkout()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Going")
                }
            }
        )
    }
}

@Composable
fun EmptyExercisesCard(onAddExercise: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No exercises added",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAddExercise) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Exercise")
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: WorkoutExercise,
    exerciseIndex: Int,
    exercisePB: ExercisePB? = null,
    lastWorkout: ExerciseHistory? = null,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onUpdateSet: (Int, Double?, Int?) -> Unit,
    onCompleteSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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
                    fontWeight = FontWeight.Bold
                )
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Remove Exercise") },
                            onClick = {
                                showMenu = false
                                onRemoveExercise()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }
            
            // PB and Last Workout Info
            if (exercisePB != null || lastWorkout != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Personal Best
                    exercisePB?.let { pb ->
                        Surface(
                            color = Color(0xFFFFD700).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PB: ${pb.bestWeightFormatted}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB8860B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // Last Workout
                    lastWorkout?.let { last ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Last: ${last.bestWeight}kg × ${last.bestReps}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SET",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "PREVIOUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "KG",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(70.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            exercise.sets.forEachIndexed { setIndex, set ->
                SetRow(
                    set = set,
                    setIndex = setIndex,
                    previousWeight = lastWorkout?.bestWeight,
                    previousReps = lastWorkout?.bestReps,
                    onWeightChange = { weight -> onUpdateSet(setIndex, weight, null) },
                    onRepsChange = { reps -> onUpdateSet(setIndex, null, reps) },
                    onComplete = { onCompleteSet(setIndex) },
                    onRemove = { onRemoveSet(setIndex) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onAddSet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Set")
            }
        }
    }
}

@Composable
fun SetRow(
    set: WorkoutSet,
    setIndex: Int,
    previousWeight: Double? = null,
    previousReps: Int? = null,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onComplete: () -> Unit,
    onRemove: () -> Unit
) {
    var weightText by remember(set.weight) { mutableStateOf(if (set.weight > 0) set.weight.toString() else "") }
    var repsText by remember(set.reps) { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }

    val backgroundColor = if (set.isCompleted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (set.isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${setIndex + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = if (set.isCompleted) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Previous workout data
        Text(
            text = if (previousWeight != null && previousReps != null) {
                "${previousWeight}×${previousReps}"
            } else {
                "-"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.Center
        )

        // Weight input
        OutlinedTextField(
            value = weightText,
            onValueChange = { value ->
                weightText = value
                value.toDoubleOrNull()?.let { onWeightChange(it) }
            },
            modifier = Modifier.width(70.dp),
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            enabled = !set.isCompleted
        )

        // Reps input
        OutlinedTextField(
            value = repsText,
            onValueChange = { value ->
                repsText = value
                value.toIntOrNull()?.let { onRepsChange(it) }
            },
            modifier = Modifier.width(60.dp),
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = !set.isCompleted
        )

        // Complete button
        IconButton(
            onClick = onComplete,
            enabled = !set.isCompleted && weightText.isNotEmpty() && repsText.isNotEmpty()
        ) {
            Icon(
                imageVector = if (set.isCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                contentDescription = "Complete set",
                tint = if (set.isCompleted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RestTimerBar(
    timeRemaining: String,
    onSkip: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Rest Timer",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = timeRemaining,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Button(onClick = onSkip) {
                Text("Skip")
            }
        }
    }
}
