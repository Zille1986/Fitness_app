package com.runtracker.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

private val RunningColor = Color(0xFF4CAF50)
private val GymColor = Color(0xFF2196F3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onCreatePlan: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val workoutsForMonth = remember(uiState.currentYear, uiState.currentMonth, uiState.runningPlan, uiState.scheduledGymWorkouts) {
        viewModel.getWorkoutsForMonth()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") }
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Calendar Header
                item {
                    CalendarHeader(
                        year = uiState.currentYear,
                        month = uiState.currentMonth,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth
                    )
                }
                
                // Calendar Grid
                item {
                    CalendarGrid(
                        year = uiState.currentYear,
                        month = uiState.currentMonth,
                        selectedDate = uiState.selectedDate,
                        workoutsForMonth = workoutsForMonth,
                        onDateSelect = viewModel::selectDate
                    )
                }
                
                // Legend
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(RunningColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Running", style = MaterialTheme.typography.labelSmall)
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(GymColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gym", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                // Selected Day Details
                item {
                    SelectedDayCard(
                        selectedDate = uiState.selectedDate,
                        runningWorkout = viewModel.getRunningWorkoutForDate(uiState.selectedDate),
                        gymWorkout = viewModel.getGymWorkoutForDate(uiState.selectedDate)
                    )
                }
                
                // Upcoming Workouts Section
                item {
                    Text(
                        text = "Upcoming Workouts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                val upcomingWorkouts = workoutsForMonth.filter { it.date >= getStartOfDay(System.currentTimeMillis()) }.take(5)
                
                if (upcomingWorkouts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Upcoming Workouts",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Schedule gym workouts or create a running plan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(upcomingWorkouts) { workout ->
                        UpcomingWorkoutCard(workout = workout)
                    }
                }
                
                // AI Analysis Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AIAnalysisCard(
                        isAnalyzing = uiState.isAnalyzing,
                        analysis = uiState.aiAnalysis,
                        suggestions = uiState.suggestions,
                        hasBodyScan = uiState.hasBodyScan,
                        hasWorkouts = workoutsForMonth.isNotEmpty(),
                        onAnalyze = viewModel::analyzeWorkouts,
                        onClear = viewModel::clearAnalysis,
                        onApplySuggestion = viewModel::applySuggestion,
                        onDismissSuggestion = viewModel::dismissSuggestion
                    )
                }
                
                // Show snackbar message for applied suggestions
                uiState.appliedSuggestionMessage?.let { message ->
                    item {
                        LaunchedEffect(message) {
                            kotlinx.coroutines.delay(3000)
                            viewModel.clearAppliedMessage()
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AIAnalysisCard(
    isAnalyzing: Boolean,
    analysis: String?,
    suggestions: List<WorkoutSuggestion>,
    hasBodyScan: Boolean,
    hasWorkouts: Boolean,
    onAnalyze: () -> Unit,
    onClear: () -> Unit,
    onApplySuggestion: (WorkoutSuggestion) -> Unit,
    onDismissSuggestion: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Workout Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Get personalized feedback on your workout schedule based on your body scan and fitness goals.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (hasBodyScan) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Body scan data available",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (analysis != null) {
                // Summary Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Summary",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = analysis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Actionable Suggestions
                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Suggested Changes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap Apply to automatically update your workouts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    suggestions.forEach { suggestion ->
                        SuggestionCard(
                            suggestion = suggestion,
                            onApply = { onApplySuggestion(suggestion) },
                            onDismiss = { onDismissSuggestion(suggestion.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onAnalyze,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAnalyzing && hasWorkouts
                ) {
                    Text("Analyze Again")
                }
            } else {
                Button(
                    onClick = onAnalyze,
                    enabled = !isAnalyzing && hasWorkouts,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing...")
                    } else {
                        Icon(Icons.Default.Psychology, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Schedule")
                    }
                }
                
                if (!hasWorkouts) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Schedule some workouts first to get analysis",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    year: Int,
    month: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(
        Calendar.getInstance().apply { set(year, month, 1) }.time
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
        }
        
        Text(
            text = monthName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    selectedDate: Long,
    workoutsForMonth: List<CalendarDayWorkout>,
    onDateSelect: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val offset = (firstDayOfWeek - Calendar.SUNDAY + 7) % 7
    
    val today = getStartOfDay(System.currentTimeMillis())
    val selectedDayStart = getStartOfDay(selectedDate)
    
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // Day headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Calendar days
        var dayCounter = 1
        for (week in 0..5) {
            if (dayCounter > daysInMonth) break
            
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 0..6) {
                    val cellIndex = week * 7 + dayOfWeek
                    
                    if (cellIndex < offset || dayCounter > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val day = dayCounter
                        calendar.set(Calendar.DAY_OF_MONTH, day)
                        val dayTimestamp = getStartOfDay(calendar.timeInMillis)
                        val isToday = dayTimestamp == today
                        val isSelected = dayTimestamp == selectedDayStart
                        
                        val dayWorkout = workoutsForMonth.find { it.dayOfMonth == day }
                        
                        CalendarDayCell(
                            day = day,
                            isToday = isToday,
                            isSelected = isSelected,
                            hasRunning = dayWorkout?.hasRunning == true,
                            hasGym = dayWorkout?.hasGym == true,
                            onClick = { onDateSelect(dayTimestamp) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasRunning: Boolean,
    hasGym: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            if (hasRunning || hasGym) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (hasRunning) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(RunningColor)
                        )
                    }
                    if (hasGym) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GymColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayCard(
    selectedDate: Long,
    runningWorkout: com.runtracker.shared.data.model.ScheduledWorkout?,
    gymWorkout: com.runtracker.shared.data.model.ScheduledGymWorkout?
) {
    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = dateFormat.format(Date(selectedDate)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (runningWorkout == null && gymWorkout == null) {
                Text(
                    text = "No workouts scheduled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (runningWorkout != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RunningColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DirectionsRun,
                                contentDescription = null,
                                tint = RunningColor
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = runningWorkout.workoutType.name.replace("_", " "),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = runningWorkout.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                if (runningWorkout != null && gymWorkout != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                if (gymWorkout != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GymColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = GymColor
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = gymWorkout.templateName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (gymWorkout.isCompleted) "Completed" else "Scheduled",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (gymWorkout.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingWorkoutCard(workout: CalendarDayWorkout) {
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(workout.date)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                
                if (workout.hasRunning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DirectionsRun,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = RunningColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = workout.runningDescription ?: "Running",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                }
                
                if (workout.hasGym) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = GymColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = workout.gymTemplateName ?: "Gym",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun getStartOfDay(timestamp: Long): Long {
    return com.runtracker.shared.util.TimeUtils.getStartOfDay(timestamp)
}

@Composable
private fun SuggestionCard(
    suggestion: WorkoutSuggestion,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (suggestion) {
                                is WorkoutSuggestion.SwapRunningWorkout -> Icons.Default.SwapHoriz
                                is WorkoutSuggestion.AddExerciseToTemplate -> Icons.Default.Add
                                is WorkoutSuggestion.RemoveExerciseFromTemplate -> Icons.Default.Remove
                                is WorkoutSuggestion.AdjustRunDuration -> Icons.Default.Timer
                                is WorkoutSuggestion.AddRestDay -> Icons.Default.Hotel
                                is WorkoutSuggestion.AddGymDay -> Icons.Default.FitnessCenter
                                is WorkoutSuggestion.RemoveGymDay -> Icons.Default.EventBusy
                                is WorkoutSuggestion.SwapExercise -> Icons.Default.SwapHoriz
                                is WorkoutSuggestion.SwapGymTemplate -> Icons.Default.SwapHoriz
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = suggestion.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = suggestion.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = suggestion.impact,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply Change")
            }
        }
    }
}
