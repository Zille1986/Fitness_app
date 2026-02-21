package com.runtracker.app.ui.screens.cycling

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
import com.runtracker.shared.data.repository.CyclingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.runtracker.shared.util.TimeUtils
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CyclingPlanViewModel @Inject constructor(
    private val cyclingRepository: CyclingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CyclingPlanUiState())
    val uiState: StateFlow<CyclingPlanUiState> = _uiState.asStateFlow()
    
    fun updatePlanName(name: String) {
        _uiState.update { it.copy(planName = name) }
    }
    
    fun updateGoalType(goalType: CyclingGoalType) {
        _uiState.update { it.copy(goalType = goalType) }
    }
    
    fun updateCyclingType(cyclingType: CyclingType) {
        _uiState.update { it.copy(defaultCyclingType = cyclingType) }
    }
    
    fun updateWeeksCount(weeks: Int) {
        _uiState.update { it.copy(weeksCount = weeks) }
    }
    
    fun updateRideDays(days: List<Int>) {
        _uiState.update { it.copy(rideDays = days) }
    }
    
    fun updateFtp(ftp: Int?) {
        _uiState.update { it.copy(ftp = ftp) }
    }
    
    fun createPlan(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            
            val schedule = generateSchedule(state)
            
            val plan = CyclingTrainingPlan(
                name = state.planName.ifEmpty { "${state.goalType.displayName} Plan" },
                description = "A ${state.weeksCount}-week cycling plan",
                goalType = state.goalType,
                ftp = state.ftp,
                startDate = System.currentTimeMillis(),
                endDate = System.currentTimeMillis() + (state.weeksCount * TimeUtils.ONE_WEEK_MS),
                weeklySchedule = schedule,
                isActive = true
            )
            
            cyclingRepository.insertPlan(plan)
            onSuccess()
        }
    }
    
    private fun generateSchedule(state: CyclingPlanUiState): List<ScheduledCyclingWorkout> {
        val schedule = mutableListOf<ScheduledCyclingWorkout>()
        
        for (week in 1..state.weeksCount) {
            for ((index, day) in state.rideDays.withIndex()) {
                val workoutType = when {
                    index == 0 -> CyclingWorkoutType.EASY_RIDE
                    index == state.rideDays.lastIndex -> CyclingWorkoutType.LONG_RIDE
                    week % 4 == 0 -> CyclingWorkoutType.RECOVERY_RIDE
                    else -> CyclingWorkoutType.ENDURANCE_RIDE
                }
                
                val baseDistance = when (workoutType) {
                    CyclingWorkoutType.LONG_RIDE -> 40000.0 + (week * 5000)
                    CyclingWorkoutType.RECOVERY_RIDE -> 15000.0
                    CyclingWorkoutType.EASY_RIDE -> 20000.0
                    else -> 25000.0 + (week * 2000)
                }
                
                schedule.add(
                    ScheduledCyclingWorkout(
                        id = UUID.randomUUID().toString(),
                        dayOfWeek = day,
                        weekNumber = week,
                        workoutType = workoutType,
                        cyclingType = state.defaultCyclingType,
                        targetDistanceMeters = baseDistance,
                        targetDurationMinutes = (baseDistance / 25000 * 60).toInt(),
                        description = "${workoutType.displayName} - Week $week"
                    )
                )
            }
        }
        
        return schedule
    }
}

data class CyclingPlanUiState(
    val planName: String = "",
    val goalType: CyclingGoalType = CyclingGoalType.GENERAL_FITNESS,
    val defaultCyclingType: CyclingType = CyclingType.OUTDOOR,
    val weeksCount: Int = 8,
    val rideDays: List<Int> = listOf(2, 4, 6, 7), // Mon, Wed, Fri, Sat
    val ftp: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCyclingPlanScreen(
    onBack: () -> Unit,
    onPlanCreated: () -> Unit,
    viewModel: CyclingPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showGoalDialog by remember { mutableStateOf(false) }
    var showCyclingTypeDialog by remember { mutableStateOf(false) }
    var ftpText by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Cycling Plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF5722)
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
                    placeholder = { Text("My Cycling Plan") },
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
                    text = "Default Ride Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Card(
                    onClick = { showCyclingTypeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(uiState.defaultCyclingType.displayName)
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
            
            item {
                OutlinedTextField(
                    value = ftpText,
                    onValueChange = { 
                        ftpText = it
                        viewModel.updateFtp(it.toIntOrNull())
                    },
                    label = { Text("FTP (Functional Threshold Power)") },
                    placeholder = { Text("e.g., 200") },
                    suffix = { Text("watts") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = "Optional - used for power-based training zones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    text = "Ride Days",
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
                        val isSelected = dayValue in uiState.rideDays
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newDays = if (isSelected) {
                                    uiState.rideDays - dayValue
                                } else {
                                    uiState.rideDays + dayValue
                                }
                                viewModel.updateRideDays(newDays.sorted())
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
                        containerColor = Color(0xFFFF5722)
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
                    items(CyclingGoalType.values().toList()) { goal ->
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
    
    // Cycling Type Selection Dialog
    if (showCyclingTypeDialog) {
        AlertDialog(
            onDismissRequest = { showCyclingTypeDialog = false },
            title = { Text("Select Ride Type") },
            text = {
                LazyColumn {
                    items(CyclingType.values().toList()) { type ->
                        TextButton(
                            onClick = {
                                viewModel.updateCyclingType(type)
                                showCyclingTypeDialog = false
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
