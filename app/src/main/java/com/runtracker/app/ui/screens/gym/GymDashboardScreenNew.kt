package com.runtracker.app.ui.screens.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runtracker.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymDashboardScreenNew(
    onStartWorkout: (Long) -> Unit,
    onViewWorkouts: () -> Unit,
    onViewFormAnalysis: () -> Unit,
    onViewWorkoutDetail: (Long) -> Unit,
    onCreateTemplate: () -> Unit = {},
    onEditTemplate: (Long) -> Unit = {},
    onScheduleWorkouts: () -> Unit = {},
    onViewHIIT: () -> Unit = {},
    viewModel: GymDashboardViewModelNew = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gym",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    uiState.nextWorkout?.let { onStartWorkout(it.templateId) }
                        ?: onViewWorkouts()
                },
                icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                text = { Text("Start Workout") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Next Workout
            item {
                NextWorkoutCard(
                    nextWorkout = uiState.nextWorkout,
                    onStartWorkout = { uiState.nextWorkout?.let { onStartWorkout(it.templateId) } },
                    onViewWorkouts = onViewWorkouts
                )
            }

            // Recent Workouts
            item {
                Text(
                    text = "Recent Workouts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.recentWorkouts.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.FitnessCenter,
                        title = "No Workouts Yet",
                        description = "Start your first gym workout to track your progress",
                        actionLabel = "Start Workout",
                        onAction = onViewWorkouts,
                        accentColor = AppColors.GymPrimary
                    )
                }
            } else {
                items(uiState.recentWorkouts.take(3)) { workout ->
                    RecentWorkoutItem(
                        workout = workout,
                        onClick = { onViewWorkoutDetail(workout.id) }
                    )
                }
            }

            // Gym Stats Chart
            item {
                GymChartCard(
                    weeklyData = uiState.weeklyVolume,
                    totalVolume = uiState.totalWeeklyVolume,
                    totalSets = uiState.totalWeeklySets
                )
            }

            // Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarMonth,
                        title = "Schedule",
                        subtitle = "Plan workouts",
                        onClick = onScheduleWorkouts
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.List,
                        title = "History",
                        subtitle = "Past workouts",
                        onClick = onViewWorkouts
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Analytics,
                        title = "Form Analysis",
                        subtitle = "Check your form",
                        onClick = onViewFormAnalysis
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalFireDepartment,
                        title = "HIIT",
                        subtitle = "Interval training",
                        onClick = onViewHIIT
                    )
                }
            }

            // Workout Templates Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Workout Templates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onCreateTemplate) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New")
                    }
                }
            }

            if (uiState.templates.isEmpty()) {
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
                                .padding(24.dp),
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
                                text = "No Templates Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create a workout template to get started",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onCreateTemplate) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Template")
                            }
                        }
                    }
                }
            } else {
                items(uiState.templates) { template ->
                    TemplateCard(
                        template = template,
                        onStart = { onStartWorkout(template.id) },
                        onEdit = { onEditTemplate(template.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: TemplateInfo,
    onStart: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${template.exerciseCount} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit template",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                FilledTonalButton(onClick = onStart) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start")
                }
            }
        }
    }
}

@Composable
private fun NextWorkoutCard(
    nextWorkout: NextWorkoutInfo?,
    onStartWorkout: () -> Unit,
    onViewWorkouts: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = "NEXT WORKOUT",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (nextWorkout != null) {
            Column {
                if (nextWorkout.scheduledDate != null) {
                    Text(
                        text = nextWorkout.scheduledDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = nextWorkout.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${nextWorkout.exerciseCount} exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (nextWorkout.lastPerformed != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last: ${nextWorkout.lastPerformed}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column {
                Text(
                    text = "No workout template selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onViewWorkouts,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Create Workout")
                }
            }
        }
    }
}

@Composable
private fun RecentWorkoutItem(
    workout: RecentWorkoutInfo,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = workout.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${workout.exerciseCount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${workout.totalSets}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GymChartCard(
    weeklyData: List<Float>,
    totalVolume: Float,
    totalSets: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Volume",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = String.format("%.0f kg", totalVolume),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.GymPrimary
                    )
                    Text(
                        text = "Total volume",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$totalSets",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Animated bar chart
            EnhancedBarChart(
                data = weeklyData,
                labels = listOf("M", "T", "W", "T", "F", "S", "S"),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                barColor = AppColors.GymPrimary
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFFFF5722)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Data classes
data class NextWorkoutInfo(
    val templateId: Long,
    val name: String,
    val exerciseCount: Int,
    val lastPerformed: String?,
    val isFromPlan: Boolean = false,
    val plannedWorkoutId: String? = null,
    val estimatedDuration: Int? = null,
    val scheduledDate: String? = null
)

data class RecentWorkoutInfo(
    val id: Long,
    val name: String,
    val date: String,
    val exerciseCount: Int,
    val totalSets: Int
)
