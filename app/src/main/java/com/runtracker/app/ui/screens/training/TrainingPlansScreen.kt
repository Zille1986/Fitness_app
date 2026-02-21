@file:OptIn(ExperimentalMaterial3Api::class)

package com.runtracker.app.ui.screens.training

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runtracker.shared.data.model.GoalType
import com.runtracker.shared.data.model.TrainingPlan
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlansScreen(
    onPlanClick: (Long) -> Unit,
    onBack: () -> Unit,
    onCustomWorkouts: () -> Unit = {},
    onCustomPlans: () -> Unit = {},
    viewModel: TrainingPlansViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Plans") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Plan") }
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
        } else if (uiState.plans.isEmpty()) {
            EmptyPlansState(
                modifier = Modifier.padding(padding),
                onCreatePlan = { showCreateDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick access cards for custom workouts and plans
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onCustomWorkouts() },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Custom Workouts",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Design your own",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onCustomPlans() },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Custom Plans",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Build your schedule",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                
                item {
                    Text(
                        "Your Plans",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                items(uiState.plans) { plan ->
                    TrainingPlanCard(
                        plan = plan,
                        isActive = plan.id == uiState.activePlan?.id,
                        onClick = { onPlanClick(plan.id) },
                        onSetActive = { viewModel.setActivePlan(plan) },
                        onDelete = { viewModel.deletePlan(plan) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlanDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { goalType, days ->
                viewModel.createPlan(goalType, daysPerWeek = days)
                showCreateDialog = false
            },
            onCreateAdvanced = { goalType, selectedDays, weeklyKm, longRunKm ->
                viewModel.createAdvancedPlan(goalType, selectedDays, weeklyKm, longRunKm)
                showCreateDialog = false
            }
        )
    }
    
    // Show generating dialog
    if (uiState.isGenerating) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generating your personalized plan with AI...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyPlansState(modifier: Modifier = Modifier, onCreatePlan: () -> Unit) {
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
            text = "No training plans yet",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Create a plan to reach your running goals",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreatePlan) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Plan")
        }
    }
}

@Composable
fun TrainingPlanCard(
    plan: TrainingPlan,
    isActive: Boolean,
    onClick: () -> Unit,
    onSetActive: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isActive) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isActive) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
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
                        if (!isActive) {
                            DropdownMenuItem(
                                text = { Text("Set as Active") },
                                onClick = {
                                    showMenu = false
                                    onSetActive()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            )
                        }
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlanInfoChip(
                    icon = Icons.Default.CalendarToday,
                    text = "${calculateWeeks(plan.startDate, plan.endDate)} weeks"
                )
                PlanInfoChip(
                    icon = Icons.Default.Flag,
                    text = formatGoalType(plan.goalType)
                )
                plan.targetDistance?.let { distance ->
                    PlanInfoChip(
                        icon = Icons.Default.DirectionsRun,
                        text = "${(distance / 1000).toInt()}K"
                    )
                }
            }
        }
    }
}

@Composable
fun PlanInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanDialog(
    onDismiss: () -> Unit,
    onCreate: (GoalType, Int) -> Unit,
    onCreateAdvanced: (GoalType, List<Int>, Int, Int) -> Unit
) {
    var selectedGoal by remember { mutableStateOf(GoalType.IMPROVE_10K) }
    var selectedDays by remember { mutableStateOf(setOf(Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SATURDAY, Calendar.SUNDAY)) }
    var currentWeeklyKm by remember { mutableStateOf("30") }
    var currentLongRunKm by remember { mutableStateOf("12") }
    
    val dayNames = listOf(
        Calendar.MONDAY to "M",
        Calendar.TUESDAY to "T", 
        Calendar.WEDNESDAY to "W",
        Calendar.THURSDAY to "T",
        Calendar.FRIDAY to "F",
        Calendar.SATURDAY to "S",
        Calendar.SUNDAY to "S"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Create Training Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Goal selection - compact
                Text(
                    text = "Goal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Goals in a more compact format
                Column {
                    listOf(
                        GoalType.FIRST_5K to "First 5K",
                        GoalType.IMPROVE_5K to "Improve 5K",
                        GoalType.FIRST_10K to "First 10K",
                        GoalType.IMPROVE_10K to "Improve 10K",
                        GoalType.HALF_MARATHON to "Half Marathon",
                        GoalType.MARATHON to "Marathon"
                    ).forEach { (goal, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGoal = goal }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedGoal == goal,
                                onClick = { selectedGoal = goal },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                // Day selection - always visible
                Text(
                    text = "Training Days",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Calendar.MONDAY to "M",
                        Calendar.TUESDAY to "T", 
                        Calendar.WEDNESDAY to "W",
                        Calendar.THURSDAY to "T",
                        Calendar.FRIDAY to "F",
                        Calendar.SATURDAY to "S",
                        Calendar.SUNDAY to "S"
                    ).forEach { (dayValue, dayName) ->
                        FilterChip(
                            selected = selectedDays.contains(dayValue),
                            onClick = {
                                selectedDays = if (selectedDays.contains(dayValue)) {
                                    selectedDays - dayValue
                                } else {
                                    selectedDays + dayValue
                                }
                            },
                            label = { Text(dayName) },
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Fitness inputs
                Text(
                    text = "Current Fitness",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = currentWeeklyKm,
                        onValueChange = { if (it.all { c -> c.isDigit() }) currentWeeklyKm = it },
                        label = { Text("Weekly km") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = currentLongRunKm,
                        onValueChange = { if (it.all { c -> c.isDigit() }) currentLongRunKm = it },
                        label = { Text("Long run km") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onCreateAdvanced(
                                selectedGoal,
                                selectedDays.toList().sorted(),
                                currentWeeklyKm.toIntOrNull() ?: 30,
                                currentLongRunKm.toIntOrNull() ?: 12
                            )
                        },
                        enabled = selectedDays.size >= 3
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

private fun calculateWeeks(startDate: Long, endDate: Long): Int {
    val diff = endDate - startDate
    return (diff / com.runtracker.shared.util.TimeUtils.ONE_WEEK_MS).toInt()
}

private fun formatGoalType(goalType: GoalType): String {
    return when (goalType) {
        GoalType.FIRST_5K -> "First 5K"
        GoalType.IMPROVE_5K -> "Improve 5K"
        GoalType.FIRST_10K -> "First 10K"
        GoalType.IMPROVE_10K -> "Improve 10K"
        GoalType.HALF_MARATHON -> "Half Marathon"
        GoalType.MARATHON -> "Marathon"
        GoalType.GENERAL_FITNESS -> "General Fitness"
        GoalType.WEIGHT_LOSS -> "Weight Loss"
        GoalType.CUSTOM -> "Custom"
    }
}
