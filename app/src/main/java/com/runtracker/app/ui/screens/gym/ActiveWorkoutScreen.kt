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
import kotlin.math.roundToInt

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
                                    progressionSuggestion = uiState.exerciseSuggestions[exercise.exerciseId],
                                    videoFileName = uiState.exerciseVideoFileNames[exercise.exerciseId],
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
    progressionSuggestion: ProgressionSuggestion? = null,
    videoFileName: String? = null,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onUpdateSet: (Int, Double?, Int?) -> Unit,
    onCompleteSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showChart by remember { mutableStateOf(false) }
    var showDemo by remember { mutableStateOf(false) }
    var showPlateCalculator by remember { mutableStateOf(false) }
    var plateCalculatorWeight by remember { mutableStateOf(0.0) }
    val chartViewModel: ExerciseProgressChartViewModel = hiltViewModel(
        key = "progress_${exercise.exerciseId}"
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

                if (videoFileName != null) {
                    IconButton(onClick = { showDemo = true }) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = "Watch Demo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = { showChart = !showChart }) {
                    Icon(
                        Icons.Default.ShowChart,
                        contentDescription = "Progress Chart",
                        tint = if (showChart) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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

            // PB, Last Workout, and 1RM Info
            if (exercisePB != null || lastWorkout != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                    // 1RM Badge
                    exercisePB?.let { pb ->
                        if (pb.bestOneRepMax > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "1RM: ${pb.bestOneRepMaxFormatted}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Progressive Overload Suggestion
            progressionSuggestion?.let { suggestion ->
                if (suggestion.suggestionType != SuggestionType.MAINTAIN || suggestion.confidence > 0.6f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressionSuggestionChip(suggestion = suggestion)
                }
            }

            // Progress Chart (expandable)
            AnimatedVisibility(
                visible = showChart,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
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
                    oneRepMax = exercisePB?.bestOneRepMax,
                    onWeightChange = { weight -> onUpdateSet(setIndex, weight, null) },
                    onRepsChange = { reps -> onUpdateSet(setIndex, null, reps) },
                    onComplete = { onCompleteSet(setIndex) },
                    onRemove = { onRemoveSet(setIndex) },
                    onPlateCalculator = { weight ->
                        plateCalculatorWeight = weight
                        showPlateCalculator = true
                    }
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

    // Plate Calculator Dialog
    if (showPlateCalculator) {
        PlateCalculatorDialog(
            initialWeight = plateCalculatorWeight,
            onDismiss = { showPlateCalculator = false }
        )
    }

    // Exercise Demo Video Dialog
    if (showDemo && videoFileName != null) {
        ExerciseDemoDialog(
            exerciseName = exercise.exerciseName,
            videoFileName = videoFileName,
            onDismiss = { showDemo = false }
        )
    }
}

@Composable
fun SetRow(
    set: WorkoutSet,
    setIndex: Int,
    previousWeight: Double? = null,
    previousReps: Int? = null,
    oneRepMax: Double? = null,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onComplete: () -> Unit,
    onRemove: () -> Unit,
    onPlateCalculator: (Double) -> Unit = {}
) {
    var weightText by remember(set.weight) { mutableStateOf(if (set.weight > 0) set.weight.toString() else "") }
    var repsText by remember(set.reps) { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }

    val backgroundColor = if (set.isCompleted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    // Calculate 1RM percentage
    val oneRmPercent = if (oneRepMax != null && oneRepMax > 0 && set.weight > 0) {
        ((set.weight / oneRepMax) * 100).roundToInt()
    } else null

    Column {
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

            // Weight input with plate calculator
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                // 1RM % label under weight
                if (oneRmPercent != null) {
                    Text(
                        text = "${oneRmPercent}% 1RM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }

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

            // Complete button + Plate calculator
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onComplete,
                    enabled = !set.isCompleted && weightText.isNotEmpty() && repsText.isNotEmpty(),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (set.isCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                        contentDescription = "Complete set",
                        tint = if (set.isCompleted) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Plate calculator icon
                if (set.weight > 0 && !set.isCompleted) {
                    IconButton(
                        onClick = { onPlateCalculator(set.weight) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = "Plate Calculator",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressionSuggestionChip(
    suggestion: ProgressionSuggestion,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor, icon, text) = when (suggestion.suggestionType) {
        SuggestionType.INCREASE_WEIGHT -> {
            val weight = if (suggestion.suggestedWeight == suggestion.suggestedWeight.toLong().toDouble()) {
                "${suggestion.suggestedWeight.toLong()}kg"
            } else {
                "${"%.1f".format(suggestion.suggestedWeight)}kg"
            }
            Tuple4(
                Color(0xFF4CAF50).copy(alpha = 0.12f),
                Color(0xFF2E7D32),
                Icons.Default.TrendingUp,
                "Increase to $weight × ${suggestion.suggestedReps}"
            )
        }
        SuggestionType.INCREASE_REPS -> Tuple4(
            Color(0xFF4CAF50).copy(alpha = 0.12f),
            Color(0xFF2E7D32),
            Icons.Default.TrendingUp,
            "Try ${suggestion.suggestedReps} reps this session"
        )
        SuggestionType.DELOAD -> Tuple4(
            Color(0xFFFF9800).copy(alpha = 0.12f),
            Color(0xFFE65100),
            Icons.Default.TrendingDown,
            "Deload: ${suggestion.suggestedWeight.toLong()}kg × ${suggestion.suggestedReps}"
        )
        SuggestionType.MAINTAIN -> Tuple4(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.TrendingFlat,
            "Stay at current weight"
        )
        SuggestionType.TRY_NEW_VARIATION -> Tuple4(
            Color(0xFF2196F3).copy(alpha = 0.12f),
            Color(0xFF1565C0),
            Icons.Default.Refresh,
            "Try a new variation"
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = suggestion.reasoning,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
