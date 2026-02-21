package com.runtracker.app.ui.screens.gym

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditTemplateViewModel @Inject constructor(
    private val gymRepository: GymRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val templateId: Long = savedStateHandle.get<Long>("templateId") ?: 0L

    private val _uiState = MutableStateFlow(EditTemplateUiState())
    val uiState: StateFlow<EditTemplateUiState> = _uiState.asStateFlow()

    init {
        loadTemplate()
    }

    private fun loadTemplate() {
        viewModelScope.launch {
            gymRepository.getTemplateByIdFlow(templateId).collect { template ->
                template?.let {
                    _uiState.update { state ->
                        state.copy(
                            templateId = it.id,
                            name = it.name,
                            description = it.description,
                            exercises = it.exercises,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun addExerciseById(exerciseId: Long) {
        viewModelScope.launch {
            val exercise = gymRepository.getExerciseById(exerciseId) ?: return@launch
            val templateExercise = TemplateExercise(
                id = UUID.randomUUID().toString(),
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                sets = 3,
                targetRepsMin = 8,
                targetRepsMax = 12,
                restSeconds = 90,
                orderIndex = _uiState.value.exercises.size
            )
            _uiState.update { it.copy(exercises = it.exercises + templateExercise) }
        }
    }

    fun removeExercise(index: Int) {
        _uiState.update { state ->
            val updatedExercises = state.exercises.toMutableList().apply {
                removeAt(index)
            }.mapIndexed { i, ex -> ex.copy(orderIndex = i) }
            state.copy(exercises = updatedExercises)
        }
    }

    fun updateExerciseSets(index: Int, sets: Int) {
        _uiState.update { state ->
            val updatedExercises = state.exercises.toMutableList().apply {
                set(index, get(index).copy(sets = sets.coerceIn(1, 10)))
            }
            state.copy(exercises = updatedExercises)
        }
    }

    fun updateExerciseReps(index: Int, minReps: Int, maxReps: Int) {
        _uiState.update { state ->
            val updatedExercises = state.exercises.toMutableList().apply {
                set(index, get(index).copy(
                    targetRepsMin = minReps.coerceIn(1, 50),
                    targetRepsMax = maxReps.coerceIn(minReps, 50)
                ))
            }
            state.copy(exercises = updatedExercises)
        }
    }

    fun updateExerciseRest(index: Int, restSeconds: Int) {
        _uiState.update { state ->
            val updatedExercises = state.exercises.toMutableList().apply {
                set(index, get(index).copy(restSeconds = restSeconds.coerceIn(0, 300)))
            }
            state.copy(exercises = updatedExercises)
        }
    }

    fun moveExerciseUp(index: Int) {
        if (index <= 0) return
        _uiState.update { state ->
            val exercises = state.exercises.toMutableList()
            val temp = exercises[index]
            exercises[index] = exercises[index - 1].copy(orderIndex = index)
            exercises[index - 1] = temp.copy(orderIndex = index - 1)
            state.copy(exercises = exercises)
        }
    }

    fun moveExerciseDown(index: Int) {
        val exercises = _uiState.value.exercises
        if (index >= exercises.size - 1) return
        _uiState.update { state ->
            val list = state.exercises.toMutableList()
            val temp = list[index]
            list[index] = list[index + 1].copy(orderIndex = index)
            list[index + 1] = temp.copy(orderIndex = index + 1)
            state.copy(exercises = list)
        }
    }

    fun saveTemplate() {
        val state = _uiState.value
        if (state.name.isBlank()) return

        viewModelScope.launch {
            val existingTemplate = gymRepository.getTemplateById(state.templateId)
            val template = existingTemplate?.copy(
                name = state.name.trim(),
                description = state.description.trim(),
                exercises = state.exercises,
                estimatedDurationMinutes = estimateDuration(state.exercises)
            ) ?: return@launch
            
            gymRepository.updateTemplate(template)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun deleteTemplate() {
        viewModelScope.launch {
            val template = gymRepository.getTemplateById(_uiState.value.templateId)
            template?.let {
                gymRepository.deleteTemplate(it)
                _uiState.update { state -> state.copy(isDeleted = true) }
            }
        }
    }

    private fun estimateDuration(exercises: List<TemplateExercise>): Int {
        if (exercises.isEmpty()) return 15
        val totalSets = exercises.sumOf { it.sets }
        val avgRestSeconds = exercises.map { it.restSeconds }.average().toInt()
        val timePerSet = 45
        return ((totalSets * (timePerSet + avgRestSeconds)) / 60).coerceAtLeast(15)
    }
}

data class EditTemplateUiState(
    val templateId: Long = 0,
    val name: String = "",
    val description: String = "",
    val exercises: List<TemplateExercise> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false
) {
    val isValid: Boolean get() = name.isNotBlank()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTemplateScreen(
    templateId: Long,
    selectedExerciseId: Long? = null,
    onExerciseAdded: () -> Unit = {},
    onAddExercise: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditTemplateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedExerciseId) {
        selectedExerciseId?.let { exerciseId ->
            viewModel.addExerciseById(exerciseId)
            onExerciseAdded()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onDeleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Template") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(
                        onClick = { viewModel.saveTemplate() },
                        enabled = uiState.isValid
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.updateName(it) },
                        label = { Text("Template Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exercises",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.exercises.isNotEmpty()) {
                            Text(
                                text = "${uiState.exercises.size} exercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (uiState.exercises.isEmpty()) {
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
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No exercises yet",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Tap + to add exercises",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(uiState.exercises) { index, exercise ->
                        TemplateExerciseCard(
                            exercise = exercise,
                            index = index,
                            totalCount = uiState.exercises.size,
                            onSetsChange = { viewModel.updateExerciseSets(index, it) },
                            onRepsChange = { min, max -> viewModel.updateExerciseReps(index, min, max) },
                            onRestChange = { viewModel.updateExerciseRest(index, it) },
                            onMoveUp = { viewModel.moveExerciseUp(index) },
                            onMoveDown = { viewModel.moveExerciseDown(index) },
                            onRemove = { viewModel.removeExercise(index) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Template?") },
            text = { Text("This will permanently delete \"${uiState.name}\". This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTemplate()
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
