package com.runtracker.app.ui.screens.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
fun CustomPlanListScreen(
    viewModel: CustomPlanViewModel = hiltViewModel(),
    onNavigateToBuilder: () -> Unit,
    onBack: () -> Unit
) {
    val plans by viewModel.plans.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Plans") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.startNewPlan()
                        onNavigateToBuilder()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Plan")
                    }
                }
            )
        }
    ) { padding ->
        if (plans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No training plans yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        viewModel.startNewPlan()
                        onNavigateToBuilder()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Your First Plan")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plans, key = { it.id }) { plan ->
                    PlanCard(
                        plan = plan,
                        isActive = activePlan?.id == plan.id,
                        onActivate = { viewModel.setActivePlan(plan.id) },
                        onEdit = {
                            viewModel.editPlan(plan)
                            onNavigateToBuilder()
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(plan.id) },
                        onDelete = { viewModel.deletePlan(plan.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlanCard(
    plan: CustomTrainingPlan,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 2.dp),
        colors = if (isActive) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "ACTIVE",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    if (plan.description.isNotBlank()) {
                        Text(
                            text = plan.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (plan.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (plan.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Plan stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlanStat(value = "${plan.durationWeeks}", label = "Weeks")
                PlanStat(value = "${plan.totalWorkouts}", label = "Workouts")
                PlanStat(
                    value = plan.goalType.name.replace("_", " ").lowercase()
                        .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                    label = "Goal"
                )
            }
            
            if (isActive && plan.weeks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = plan.progressPercent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${plan.completedWorkouts}/${plan.totalWorkouts} workouts completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isActive) {
                    Button(
                        onClick = onActivate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start")
                    }
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
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
            title = { Text("Delete Plan") },
            text = { Text("Are you sure you want to delete '${plan.name}'?") },
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
fun PlanStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanBuilderScreen(
    viewModel: CustomPlanViewModel = hiltViewModel(),
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val builderState by viewModel.builderState.collectAsState()
    val customWorkouts by viewModel.customWorkouts.collectAsState()
    var selectedWeekIndex by remember { mutableStateOf<Int?>(null) }
    var showAddWorkoutDialog by remember { mutableStateOf(false) }
    var addWorkoutWeekIndex by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (builderState.isEditing) "Edit Plan" else "Create Plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.savePlan(onSave) },
                        enabled = builderState.isValid
                    ) {
                        Text("Save")
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
            // Plan details
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Plan Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = builderState.name,
                            onValueChange = { viewModel.updatePlanName(it) },
                            label = { Text("Plan Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = builderState.description,
                            onValueChange = { viewModel.updatePlanDescription(it) },
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        
                        // Goal type dropdown
                        var goalExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = goalExpanded,
                            onExpandedChange = { goalExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = builderState.goalType.name.replace("_", " ").lowercase()
                                    .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Goal") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = goalExpanded,
                                onDismissRequest = { goalExpanded = false }
                            ) {
                                GoalType.values().forEach { goal ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(goal.name.replace("_", " ").lowercase()
                                                .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
                                        },
                                        onClick = {
                                            viewModel.updateGoalType(goal)
                                            goalExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Duration weeks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Duration:", modifier = Modifier.width(80.dp))
                            IconButton(
                                onClick = { 
                                    if (builderState.durationWeeks > 1) {
                                        viewModel.updateDurationWeeks(builderState.durationWeeks - 1)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                "${builderState.durationWeeks} weeks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { viewModel.updateDurationWeeks(builderState.durationWeeks + 1) }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                        
                        // Auto-generate button
                        OutlinedButton(
                            onClick = { viewModel.generateBasicPlan() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-Generate Plan")
                        }
                    }
                }
            }
            
            // Summary
            if (builderState.weeks.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                                    "${builderState.durationWeeks}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Weeks", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${builderState.totalWorkouts}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Workouts", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            
            // Weeks section
            item {
                Text(
                    "Weekly Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (builderState.weeks.isEmpty()) {
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
                            Text("Set the duration above to add weeks")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Or use Auto-Generate to create a basic plan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            itemsIndexed(builderState.weeks) { index, week ->
                WeekCard(
                    week = week,
                    weekIndex = index,
                    isExpanded = selectedWeekIndex == index,
                    onToggleExpand = {
                        selectedWeekIndex = if (selectedWeekIndex == index) null else index
                    },
                    onWeekTypeChange = { viewModel.updateWeekType(index, it) },
                    onAddWorkout = {
                        addWorkoutWeekIndex = index
                        showAddWorkoutDialog = true
                    },
                    onRemoveWorkout = { workoutIndex ->
                        viewModel.removeWorkoutFromWeek(index, workoutIndex)
                    },
                    onCopyWeek = { toIndex ->
                        viewModel.copyWeek(index, toIndex)
                    },
                    totalWeeks = builderState.durationWeeks
                )
            }
        }
    }
    
    // Add Workout Dialog
    if (showAddWorkoutDialog) {
        AddPlanWorkoutDialog(
            customWorkouts = customWorkouts,
            onDismiss = { showAddWorkoutDialog = false },
            onAdd = { workout ->
                viewModel.addWorkoutToWeek(addWorkoutWeekIndex, workout)
                showAddWorkoutDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekCard(
    week: PlanWeek,
    weekIndex: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onWeekTypeChange: (WeekType) -> Unit,
    onAddWorkout: () -> Unit,
    onRemoveWorkout: (Int) -> Unit,
    onCopyWeek: (Int) -> Unit,
    totalWeeks: Int
) {
    val weekTypeColor = when (week.weekType) {
        WeekType.BASE -> Color(0xFF4CAF50)
        WeekType.BUILD -> Color(0xFF2196F3)
        WeekType.PEAK -> Color(0xFFE53935)
        WeekType.TAPER -> Color(0xFFFF9800)
        WeekType.RECOVERY -> Color(0xFF81C784)
        WeekType.RACE -> Color(0xFF9C27B0)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(weekTypeColor, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            week.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${week.weekType.name} • ${week.workouts.size} workouts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
            
            // Expanded content
            if (isExpanded) {
                Divider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Week type selector
                    Text("Week Type", style = MaterialTheme.typography.labelMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(WeekType.values().toList()) { type ->
                            FilterChip(
                                selected = week.weekType == type,
                                onClick = { onWeekTypeChange(type) },
                                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    
                    // Workouts
                    Text("Workouts", style = MaterialTheme.typography.labelMedium)
                    
                    week.workouts.forEachIndexed { index, workout ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    getDayName(workout.dayOfWeek),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    workout.name.ifBlank { workout.workoutType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() } },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                workout.targetDurationMinutes?.let {
                                    Text(
                                        "$it min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onRemoveWorkout(index) }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    // Add workout button
                    OutlinedButton(
                        onClick = onAddWorkout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Workout")
                    }
                    
                    // Copy week button
                    if (totalWeeks > 1) {
                        var showCopyMenu by remember { mutableStateOf(false) }
                        Box {
                            TextButton(
                                onClick = { showCopyMenu = true }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy to another week")
                            }
                            DropdownMenu(
                                expanded = showCopyMenu,
                                onDismissRequest = { showCopyMenu = false }
                            ) {
                                (0 until totalWeeks).filter { it != weekIndex }.forEach { targetIndex ->
                                    DropdownMenuItem(
                                        text = { Text("Week ${targetIndex + 1}") },
                                        onClick = {
                                            onCopyWeek(targetIndex)
                                            showCopyMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlanWorkoutDialog(
    customWorkouts: List<CustomRunWorkout>,
    onDismiss: () -> Unit,
    onAdd: (PlanWorkout) -> Unit
) {
    var selectedDay by remember { mutableStateOf(1) }
    var selectedType by remember { mutableStateOf(WorkoutType.EASY_RUN) }
    var name by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("") }
    var useCustomWorkout by remember { mutableStateOf(false) }
    var selectedCustomWorkoutId by remember { mutableStateOf<Long?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Workout") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Day selector
                    Text("Day of Week", style = MaterialTheme.typography.labelMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items((1..7).toList()) { day ->
                            FilterChip(
                                selected = selectedDay == day,
                                onClick = { selectedDay = day },
                                label = { Text(getDayName(day).take(3)) }
                            )
                        }
                    }
                }
                
                if (customWorkouts.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !useCustomWorkout,
                                onClick = { useCustomWorkout = false },
                                label = { Text("Standard") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = useCustomWorkout,
                                onClick = { useCustomWorkout = true },
                                label = { Text("Custom") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                if (useCustomWorkout && customWorkouts.isNotEmpty()) {
                    item {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = customWorkouts.find { it.id == selectedCustomWorkoutId }?.name ?: "Select workout",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Custom Workout") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                customWorkouts.forEach { workout ->
                                    DropdownMenuItem(
                                        text = { Text(workout.name) },
                                        onClick = {
                                            selectedCustomWorkoutId = workout.id
                                            name = workout.name
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        var typeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Workout Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                WorkoutType.values().forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
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
                        OutlinedTextField(
                            value = durationMinutes,
                            onValueChange = { durationMinutes = it.filter { c -> c.isDigit() } },
                            label = { Text("Duration (minutes)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val workout = PlanWorkout(
                        dayOfWeek = selectedDay,
                        workoutType = if (useCustomWorkout) WorkoutType.CUSTOM else selectedType,
                        customWorkoutId = if (useCustomWorkout) selectedCustomWorkoutId else null,
                        name = name,
                        targetDurationMinutes = durationMinutes.toIntOrNull()
                    )
                    onAdd(workout)
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

private fun getDayName(day: Int): String {
    return when (day) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> "Day $day"
    }
}
