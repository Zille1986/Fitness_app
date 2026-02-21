package com.runtracker.app.ui.screens.gym

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateExerciseViewModel @Inject constructor(
    private val gymRepository: GymRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateExerciseUiState())
    val uiState: StateFlow<CreateExerciseUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateMuscleGroup(muscleGroup: MuscleGroup) {
        _uiState.update { it.copy(muscleGroup = muscleGroup) }
    }

    fun updateEquipment(equipment: Equipment) {
        _uiState.update { it.copy(equipment = equipment) }
    }

    fun updateExerciseType(exerciseType: ExerciseType) {
        _uiState.update { it.copy(exerciseType = exerciseType) }
    }

    fun saveExercise() {
        val state = _uiState.value
        if (state.name.isBlank() || state.muscleGroup == null || state.equipment == null) return

        viewModelScope.launch {
            val exercise = Exercise(
                name = state.name.trim(),
                description = state.description.trim(),
                muscleGroup = state.muscleGroup,
                equipment = state.equipment,
                exerciseType = state.exerciseType,
                isCustom = true
            )
            gymRepository.insertExercise(exercise)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}

data class CreateExerciseUiState(
    val name: String = "",
    val description: String = "",
    val muscleGroup: MuscleGroup? = null,
    val equipment: Equipment? = null,
    val exerciseType: ExerciseType = ExerciseType.COMPOUND,
    val isSaved: Boolean = false
) {
    val isValid: Boolean get() = name.isNotBlank() && muscleGroup != null && equipment != null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateExerciseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMuscleGroupPicker by remember { mutableStateOf(false) }
    var showEquipmentPicker by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveExercise() },
                        enabled = uiState.isValid
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
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
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Exercise Name *") },
                    placeholder = { Text("e.g., Incline Hammer Curl") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Brief description of the exercise") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

            item {
                Text(
                    text = "Muscle Group *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedCard(
                    onClick = { showMuscleGroupPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.muscleGroup?.displayName ?: "Select muscle group",
                            color = if (uiState.muscleGroup != null) 
                                MaterialTheme.colorScheme.onSurface 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Equipment *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedCard(
                    onClick = { showEquipmentPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.equipment?.displayName ?: "Select equipment",
                            color = if (uiState.equipment != null) 
                                MaterialTheme.colorScheme.onSurface 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Exercise Type",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ExerciseType.values().toList()) { type ->
                        FilterChip(
                            selected = uiState.exerciseType == type,
                            onClick = { viewModel.updateExerciseType(type) },
                            label = { 
                                Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) 
                            }
                        )
                    }
                }
            }
        }
    }

    if (showMuscleGroupPicker) {
        MuscleGroupPickerDialog(
            selectedMuscleGroup = uiState.muscleGroup,
            onSelect = { 
                viewModel.updateMuscleGroup(it)
                showMuscleGroupPicker = false
            },
            onDismiss = { showMuscleGroupPicker = false }
        )
    }

    if (showEquipmentPicker) {
        EquipmentPickerDialog(
            selectedEquipment = uiState.equipment,
            onSelect = { 
                viewModel.updateEquipment(it)
                showEquipmentPicker = false
            },
            onDismiss = { showEquipmentPicker = false }
        )
    }
}

@Composable
fun MuscleGroupPickerDialog(
    selectedMuscleGroup: MuscleGroup?,
    onSelect: (MuscleGroup) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Muscle Group") },
        text = {
            LazyColumn {
                items(MuscleGroup.values().toList()) { muscleGroup ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = muscleGroup == selectedMuscleGroup,
                            onClick = { onSelect(muscleGroup) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(muscleGroup.displayName)
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
fun EquipmentPickerDialog(
    selectedEquipment: Equipment?,
    onSelect: (Equipment) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Equipment") },
        text = {
            LazyColumn {
                items(Equipment.values().toList()) { equipment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = equipment == selectedEquipment,
                            onClick = { onSelect(equipment) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(equipment.displayName)
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
