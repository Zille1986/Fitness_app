package com.runtracker.app.ui.screens.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runtracker.shared.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutBuilderScreen(
    viewModel: CustomWorkoutViewModel = hiltViewModel(),
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val builderState by viewModel.builderState.collectAsState()
    var showAddPhaseDialog by remember { mutableStateOf(false) }
    var editingPhaseIndex by remember { mutableStateOf<Int?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (builderState.isEditing) "Edit Workout" else "Create Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveWorkout(onSave) },
                        enabled = builderState.isValid
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPhaseDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Phase") }
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
            // Workout details section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Workout Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = builderState.name,
                            onValueChange = { viewModel.updateWorkoutName(it) },
                            label = { Text("Workout Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = builderState.description,
                            onValueChange = { viewModel.updateWorkoutDescription(it) },
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        
                        // Category dropdown
                        var categoryExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = builderState.category.name.replace("_", " ").lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                WorkoutCategory.values().forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            viewModel.updateWorkoutCategory(category)
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Difficulty dropdown
                        var difficultyExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = difficultyExpanded,
                            onExpandedChange = { difficultyExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = builderState.difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Difficulty") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = difficultyExpanded,
                                onDismissRequest = { difficultyExpanded = false }
                            ) {
                                RunDifficulty.values().forEach { difficulty ->
                                    DropdownMenuItem(
                                        text = { Text(difficulty.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            viewModel.updateWorkoutDifficulty(difficulty)
                                            difficultyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Summary
            if (builderState.phases.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    builderState.formattedDuration,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Total Duration", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${builderState.phases.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Phases", style = MaterialTheme.typography.labelSmall)
                            }
                            if (builderState.totalDistanceMeters > 0) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        builderState.formattedDistance,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Distance", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            
            // Phases section
            item {
                Text(
                    "Workout Phases",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (builderState.phases.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddPhaseDialog = true },
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
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Add your first phase")
                            Text(
                                "Start with a warm-up, add intervals, and finish with a cool-down",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            itemsIndexed(builderState.phases) { index, phase ->
                PhaseCard(
                    phase = phase,
                    index = index,
                    totalPhases = builderState.phases.size,
                    onEdit = { editingPhaseIndex = index },
                    onDelete = { viewModel.removePhase(index) },
                    onMoveUp = { viewModel.movePhaseUp(index) },
                    onMoveDown = { viewModel.movePhaseDown(index) },
                    onDuplicate = { viewModel.duplicatePhase(index) }
                )
            }
            
            // Bottom spacer for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // Add Phase Dialog
    if (showAddPhaseDialog) {
        AddPhaseDialog(
            onDismiss = { showAddPhaseDialog = false },
            onAdd = { phase ->
                viewModel.addPhase(phase)
                showAddPhaseDialog = false
            }
        )
    }
    
    // Edit Phase Dialog
    editingPhaseIndex?.let { index ->
        val phase = builderState.phases.getOrNull(index)
        if (phase != null) {
            EditPhaseDialog(
                phase = phase,
                onDismiss = { editingPhaseIndex = null },
                onSave = { updatedPhase ->
                    viewModel.updatePhase(index, updatedPhase)
                    editingPhaseIndex = null
                }
            )
        }
    }
}

@Composable
fun PhaseCard(
    phase: WorkoutPhase,
    index: Int,
    totalPhases: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit
) {
    val phaseColor = when (phase.type) {
        PhaseType.WARMUP -> Color(0xFFFF9800)
        PhaseType.COOLDOWN -> Color(0xFF2196F3)
        PhaseType.RECOVERY, PhaseType.REST, PhaseType.FLOAT -> Color(0xFF81C784)
        PhaseType.WORK, PhaseType.TEMPO -> Color(0xFFE53935)
        PhaseType.STRIDE, PhaseType.SURGE -> Color(0xFF9C27B0)
        PhaseType.SPRINT -> Color(0xFFD32F2F)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, phaseColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Phase indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(phaseColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Phase details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    phase.name.ifBlank { phase.type.name.lowercase().replaceFirstChar { it.uppercase() } },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    phase.durationSeconds?.let {
                        Text(
                            phase.durationFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    phase.distanceMeters?.let {
                        Text(
                            phase.distanceFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (phase.repetitions > 1) {
                        Text(
                            "×${phase.repetitions}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = phaseColor
                        )
                    }
                }
                Text(
                    phase.effort.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = phaseColor
                )
            }
            
            // Action buttons
            Column {
                Row {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalPhases - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(20.dp))
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPhaseDialog(
    onDismiss: () -> Unit,
    onAdd: (WorkoutPhase) -> Unit
) {
    var selectedType by remember { mutableStateOf(PhaseType.WORK) }
    var name by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("") }
    var durationSeconds by remember { mutableStateOf("") }
    var distanceMeters by remember { mutableStateOf("") }
    var repetitions by remember { mutableStateOf("1") }
    var selectedEffort by remember { mutableStateOf(EffortLevel.MODERATE) }
    var useDuration by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Phase") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Phase type selection
                    Text("Phase Type", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Quick select buttons for common types
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            PhaseType.WARMUP to "Warm",
                            PhaseType.WORK to "Work",
                            PhaseType.RECOVERY to "Rest",
                            PhaseType.COOLDOWN to "Cool"
                        ).forEach { (type, label) ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // More types dropdown
                    var typeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedType.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("All Types") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            PhaseType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        selectedType = type
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    // Duration or Distance toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = useDuration,
                            onClick = { useDuration = true },
                            label = { Text("Duration") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !useDuration,
                            onClick = { useDuration = false },
                            label = { Text("Distance") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                item {
                    if (useDuration) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = durationMinutes,
                                onValueChange = { durationMinutes = it.filter { c -> c.isDigit() } },
                                label = { Text("Minutes") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = durationSeconds,
                                onValueChange = { durationSeconds = it.filter { c -> c.isDigit() } },
                                label = { Text("Seconds") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = distanceMeters,
                            onValueChange = { distanceMeters = it.filter { c -> c.isDigit() } },
                            label = { Text("Distance (meters)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = repetitions,
                        onValueChange = { repetitions = it.filter { c -> c.isDigit() } },
                        label = { Text("Repetitions") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                
                item {
                    // Effort level
                    Text("Effort Level", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            EffortLevel.EASY to "Easy",
                            EffortLevel.MODERATE to "Mod",
                            EffortLevel.HARD to "Hard"
                        ).forEach { (effort, label) ->
                            FilterChip(
                                selected = selectedEffort == effort,
                                onClick = { selectedEffort = effort },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val totalSeconds = (durationMinutes.toIntOrNull() ?: 0) * 60 + (durationSeconds.toIntOrNull() ?: 0)
                    val phase = WorkoutPhase(
                        type = selectedType,
                        name = name,
                        durationSeconds = if (useDuration && totalSeconds > 0) totalSeconds else null,
                        distanceMeters = if (!useDuration) distanceMeters.toDoubleOrNull() else null,
                        repetitions = repetitions.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                        effort = selectedEffort
                    )
                    onAdd(phase)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhaseDialog(
    phase: WorkoutPhase,
    onDismiss: () -> Unit,
    onSave: (WorkoutPhase) -> Unit
) {
    var selectedType by remember { mutableStateOf(phase.type) }
    var name by remember { mutableStateOf(phase.name) }
    var durationMinutes by remember { mutableStateOf(((phase.durationSeconds ?: 0) / 60).toString()) }
    var durationSeconds by remember { mutableStateOf(((phase.durationSeconds ?: 0) % 60).toString()) }
    var distanceMeters by remember { mutableStateOf(phase.distanceMeters?.toInt()?.toString() ?: "") }
    var repetitions by remember { mutableStateOf(phase.repetitions.toString()) }
    var selectedEffort by remember { mutableStateOf(phase.effort) }
    var useDuration by remember { mutableStateOf(phase.durationSeconds != null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Phase") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    var typeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedType.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Phase Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            PhaseType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        selectedType = type
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = useDuration,
                            onClick = { useDuration = true },
                            label = { Text("Duration") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !useDuration,
                            onClick = { useDuration = false },
                            label = { Text("Distance") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                item {
                    if (useDuration) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = durationMinutes,
                                onValueChange = { durationMinutes = it.filter { c -> c.isDigit() } },
                                label = { Text("Minutes") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = durationSeconds,
                                onValueChange = { durationSeconds = it.filter { c -> c.isDigit() } },
                                label = { Text("Seconds") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = distanceMeters,
                            onValueChange = { distanceMeters = it.filter { c -> c.isDigit() } },
                            label = { Text("Distance (meters)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = repetitions,
                        onValueChange = { repetitions = it.filter { c -> c.isDigit() } },
                        label = { Text("Repetitions") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                
                item {
                    Text("Effort Level", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    var effortExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = effortExpanded,
                        onExpandedChange = { effortExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedEffort.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = effortExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = effortExpanded,
                            onDismissRequest = { effortExpanded = false }
                        ) {
                            EffortLevel.values().forEach { effort ->
                                DropdownMenuItem(
                                    text = { Text(effort.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        selectedEffort = effort
                                        effortExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val totalSeconds = (durationMinutes.toIntOrNull() ?: 0) * 60 + (durationSeconds.toIntOrNull() ?: 0)
                    val updatedPhase = phase.copy(
                        type = selectedType,
                        name = name,
                        durationSeconds = if (useDuration && totalSeconds > 0) totalSeconds else null,
                        distanceMeters = if (!useDuration) distanceMeters.toDoubleOrNull() else null,
                        repetitions = repetitions.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                        effort = selectedEffort
                    )
                    onSave(updatedPhase)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
