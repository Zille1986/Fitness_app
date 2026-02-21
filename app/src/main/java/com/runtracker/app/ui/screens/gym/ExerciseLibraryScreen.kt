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
import androidx.compose.ui.graphics.Color
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
class ExerciseLibraryViewModel @Inject constructor(
    private val gymRepository: GymRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseLibraryUiState())
    val uiState: StateFlow<ExerciseLibraryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        loadExercises()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            gymRepository.getAllExercises().collect { exercises ->
                _uiState.update { 
                    it.copy(
                        allExercises = exercises,
                        filteredExercises = filterExercises(exercises, it.selectedMuscleGroup, _searchQuery.value),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredExercises = filterExercises(state.allExercises, state.selectedMuscleGroup, query)
            )
        }
    }

    fun setMuscleGroupFilter(muscleGroup: MuscleGroup?) {
        _uiState.update { state ->
            state.copy(
                selectedMuscleGroup = muscleGroup,
                filteredExercises = filterExercises(state.allExercises, muscleGroup, state.searchQuery)
            )
        }
    }

    private fun filterExercises(
        exercises: List<Exercise>,
        muscleGroup: MuscleGroup?,
        query: String
    ): List<Exercise> {
        return exercises.filter { exercise ->
            val matchesMuscle = muscleGroup == null || 
                exercise.muscleGroup == muscleGroup || 
                muscleGroup in exercise.secondaryMuscleGroups
            
            val matchesQuery = query.isEmpty() || 
                exercise.name.contains(query, ignoreCase = true) ||
                exercise.muscleGroup.name.contains(query, ignoreCase = true)
            
            matchesMuscle && matchesQuery
        }
    }
}

data class ExerciseLibraryUiState(
    val allExercises: List<Exercise> = emptyList(),
    val filteredExercises: List<Exercise> = emptyList(),
    val selectedMuscleGroup: MuscleGroup? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    onExerciseSelected: ((Exercise) -> Unit)? = null,
    onCreateExercise: (() -> Unit)? = null,
    onBack: () -> Unit,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercises") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onCreateExercise != null) {
                        IconButton(onClick = onCreateExercise) {
                            Icon(Icons.Default.Add, contentDescription = "Create Exercise")
                        }
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Badge(
                            modifier = Modifier.offset(x = 8.dp, y = (-8).dp),
                            containerColor = if (uiState.selectedMuscleGroup != null) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                Color.Transparent
                        ) {
                            if (uiState.selectedMuscleGroup != null) {
                                Text("1")
                            }
                        }
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search exercises...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Active filter chip
            if (uiState.selectedMuscleGroup != null) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.setMuscleGroupFilter(null) },
                        label = { Text(uiState.selectedMuscleGroup!!.displayName) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove filter",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val groupedExercises = uiState.filteredExercises.groupBy { it.muscleGroup }
                    
                    groupedExercises.forEach { (muscleGroup, exercises) ->
                        item {
                            Text(
                                text = muscleGroup.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(exercises) { exercise ->
                            ExerciseListItem(
                                exercise = exercise,
                                onClick = { onExerciseSelected?.invoke(exercise) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        MuscleGroupFilterSheet(
            selectedMuscleGroup = uiState.selectedMuscleGroup,
            onMuscleGroupSelected = { 
                viewModel.setMuscleGroupFilter(it)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
fun ExerciseListItem(
    exercise: Exercise,
    onClick: () -> Unit
) {
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
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = exercise.equipment.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = exercise.exerciseType.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleGroupFilterSheet(
    selectedMuscleGroup: MuscleGroup?,
    onMuscleGroupSelected: (MuscleGroup?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Filter by Muscle Group",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // All option
            FilterOption(
                text = "All Muscles",
                isSelected = selectedMuscleGroup == null,
                onClick = { onMuscleGroupSelected(null) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            MuscleGroup.values().forEach { muscleGroup ->
                FilterOption(
                    text = muscleGroup.displayName,
                    isSelected = selectedMuscleGroup == muscleGroup,
                    onClick = { onMuscleGroupSelected(muscleGroup) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FilterOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
