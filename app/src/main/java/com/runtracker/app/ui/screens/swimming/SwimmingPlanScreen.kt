package com.runtracker.app.ui.screens.swimming

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
import com.runtracker.shared.data.repository.SwimmingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.runtracker.shared.util.TimeUtils
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SwimmingPlanViewModel @Inject constructor(
    private val swimmingRepository: SwimmingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SwimmingPlanUiState())
    val uiState: StateFlow<SwimmingPlanUiState> = _uiState.asStateFlow()
    
    fun updatePlanName(name: String) {
        _uiState.update { it.copy(planName = name) }
    }
    
    fun updateGoalType(goalType: SwimGoalType) {
        _uiState.update { it.copy(goalType = goalType) }
    }
    
    fun updateSwimType(swimType: SwimType) {
        _uiState.update { it.copy(defaultSwimType = swimType) }
    }
    
    fun updateWeeksCount(weeks: Int) {
        _uiState.update { it.copy(weeksCount = weeks) }
    }
    
    fun updateSwimDays(days: List<Int>) {
        _uiState.update { it.copy(swimDays = days) }
    }
    
    fun createPlan(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            
            // Generate weekly schedule based on settings
            val schedule = generateSchedule(state)
            
            val plan = SwimmingTrainingPlan(
                name = state.planName.ifEmpty { "${state.goalType.displayName} Plan" },
                description = "A ${state.weeksCount}-week swimming plan",
                goalType = state.goalType,
                startDate = System.currentTimeMillis(),
                endDate = System.currentTimeMillis() + (state.weeksCount * TimeUtils.ONE_WEEK_MS),
                weeklySchedule = schedule,
                isActive = true
            )
            
            swimmingRepository.insertPlan(plan)
            onSuccess()
        }
    }
    
    private fun generateSchedule(state: SwimmingPlanUiState): List<ScheduledSwimWorkout> {
        val schedule = mutableListOf<ScheduledSwimWorkout>()
        
        for (week in 1..state.weeksCount) {
            for (day in state.swimDays) {
                val workoutType = when {
                    day == state.swimDays.first() -> SwimWorkoutType.ENDURANCE_SWIM
                    day == state.swimDays.last() -> SwimWorkoutType.EASY_SWIM
                    else -> SwimWorkoutType.TECHNIQUE_DRILLS
                }
                
                schedule.add(
                    ScheduledSwimWorkout(
                        id = UUID.randomUUID().toString(),
                        dayOfWeek = day,
                        weekNumber = week,
                        workoutType = workoutType,
                        swimType = state.defaultSwimType,
                        targetDistanceMeters = (1000 + (week * 100)).toDouble(),
                        targetDurationMinutes = 30 + (week * 5),
                        description = "${workoutType.displayName} - Week $week"
                    )
                )
            }
        }
        
        return schedule
    }
}

data class SwimmingPlanUiState(
    val planName: String = "",
    val goalType: SwimGoalType = SwimGoalType.GENERAL_FITNESS,
    val defaultSwimType: SwimType = SwimType.POOL,
    val weeksCount: Int = 8,
    val swimDays: List<Int> = listOf(2, 4, 6) // Mon, Wed, Fri (Calendar.MONDAY = 2)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSwimmingPlanScreen(
    onBack: () -> Unit,
    onPlanCreated: () -> Unit,
    viewModel: SwimmingPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showGoalDialog by remember { mutableStateOf(false) }
    var showSwimTypeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Swimming Plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0288D1)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.planName,
                    onValueChange = { viewModel.updatePlanName(it) },
                    label = { Text("Plan Name") },
                    placeholder = { Text("My Swimming Plan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            item {
                Text(
                    text = "Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Card(
                    onClick = { showGoalDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(uiState.goalType.displayName)
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
            
            item {
                Text(
                    text = "Default Swim Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Card(
                    onClick = { showSwimTypeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(uiState.defaultSwimType.displayName)
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
            
            item {
                Text(
                    text = "Duration: ${uiState.weeksCount} weeks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Slider(
                    value = uiState.weeksCount.toFloat(),
                    onValueChange = { viewModel.updateWeeksCount(it.toInt()) },
                    valueRange = 4f..16f,
                    steps = 11
                )
            }
            
            item {
                Text(
                    text = "Swim Days",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val days = listOf("S", "M", "T", "W", "T", "F", "S")
                    val dayValues = listOf(1, 2, 3, 4, 5, 6, 7)
                    
                    days.forEachIndexed { index, day ->
                        val dayValue = dayValues[index]
                        val isSelected = dayValue in uiState.swimDays
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newDays = if (isSelected) {
                                    uiState.swimDays - dayValue
                                } else {
                                    uiState.swimDays + dayValue
                                }
                                viewModel.updateSwimDays(newDays.sorted())
                            },
                            label = { Text(day) }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.createPlan(onPlanCreated) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0288D1)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Plan")
                }
            }
        }
    }
    
    // Goal Selection Dialog
    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Select Goal") },
            text = {
                LazyColumn {
                    items(SwimGoalType.values().toList()) { goal ->
                        TextButton(
                            onClick = {
                                viewModel.updateGoalType(goal)
                                showGoalDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(goal.displayName)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
    
    // Swim Type Selection Dialog
    if (showSwimTypeDialog) {
        AlertDialog(
            onDismissRequest = { showSwimTypeDialog = false },
            title = { Text("Select Swim Type") },
            text = {
                LazyColumn {
                    items(SwimType.values().toList()) { type ->
                        TextButton(
                            onClick = {
                                viewModel.updateSwimType(type)
                                showSwimTypeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(type.displayName)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
