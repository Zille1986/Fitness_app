package com.runtracker.app.ui.screens.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.runtracker.app.ui.components.*
import com.runtracker.shared.data.model.GymWorkout
import com.runtracker.shared.data.model.MuscleGroup
import com.runtracker.shared.data.model.WorkoutTemplate
import com.runtracker.shared.data.model.displayName
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymDashboardScreen(
    onStartWorkout: (Long) -> Unit,
    onViewWorkout: (Long) -> Unit,
    onViewExercises: () -> Unit,
    onViewHistory: () -> Unit,
    onViewAnalysis: () -> Unit = {},
    onCreateTemplate: () -> Unit,
    onEditTemplate: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: GymDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTemplateSelector by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.activeWorkout) {
        uiState.activeWorkout?.let { workout ->
            if (!workout.isCompleted && workout.id > 0) {
                onStartWorkout(workout.id)
                viewModel.clearActiveWorkout()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Gym",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Featured Gym Workouts (Strava-style stacked cards)
                item {
                    GymFeaturedWorkoutsSection(
                        templates = uiState.templates,
                        onTemplateClick = { template ->
                            viewModel.startWorkoutFromTemplate(template)
                        }
                    )
                }
                
                // Weekly Stats
                item {
                    SectionHeader(
                        title = "This Week",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                item {
                    uiState.weeklyStats?.let { stats ->
                        ModernWeeklyGymStatsCard(
                            stats = stats,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Muscle Heatmap
                item {
                    MuscleGroupHeatmap(
                        muscleGroupVolume = uiState.muscleGroupVolume,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Quick Actions
                item {
                    SectionHeader(
                        title = "Quick Actions",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                item {
                    ModernGymQuickActions(
                        onViewExercises = onViewExercises,
                        onViewHistory = onViewHistory,
                        onViewAnalysis = onViewAnalysis,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Recent Workouts - Carousel
                if (uiState.recentWorkouts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recent Workouts",
                            action = "See All",
                            onActionClick = onViewHistory,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        RecentActivitiesCarousel(
                            activities = uiState.recentWorkouts.take(5).map { workout ->
                                ActivityCardData(
                                    id = workout.id,
                                    title = workout.name.ifEmpty { "Gym Workout" },
                                    subtitle = "${workout.exercises.size} exercises",
                                    date = formatDate(workout.startTime),
                                    mainStat = formatVolumeDisplay(workout.totalVolume),
                                    mainStatLabel = "volume",
                                    secondaryStat = workout.durationFormatted,
                                    secondaryStatLabel = "time",
                                    type = com.runtracker.app.ui.components.WorkoutType.GYM
                                )
                            },
                            onActivityClick = { activity -> onViewWorkout(activity.id) }
                        )
                    }
                } else {
                    item {
                        ModernEmptyGymState(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
            
            // Modern Start Workout Button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.BottomCenter
            ) {
                ModernGymStartButton(
                    onClick = { showTemplateSelector = true },
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }

    if (showTemplateSelector) {
        TemplateSelectionDialog(
            templates = uiState.templates,
            onTemplateSelected = { template ->
                showTemplateSelector = false
                viewModel.startWorkoutFromTemplate(template)
            },
            onStartEmpty = {
                showTemplateSelector = false
                viewModel.startEmptyWorkout()
            },
            onCreateTemplate = {
                showTemplateSelector = false
                onCreateTemplate()
            },
            onEditTemplate = { templateId ->
                showTemplateSelector = false
                onEditTemplate(templateId)
            },
            onDismiss = { showTemplateSelector = false }
        )
    }
}

@Composable
fun WeeklyGymStatsCard(stats: com.runtracker.shared.data.repository.GymWeeklyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "This Week",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GymStatItem(
                    value = stats.workoutCount.toString(),
                    label = "Workouts"
                )
                GymStatItem(
                    value = stats.totalVolumeFormatted,
                    label = "Volume"
                )
            }
        }
    }
}

@Composable
fun GymStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun QuickActionsRow(
    onViewExercises: () -> Unit,
    onViewHistory: () -> Unit,
    onViewAnalysis: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onViewExercises)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "View exercises list",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Exercises",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onViewHistory)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "View workout history",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "History",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onViewAnalysis)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "View analytics",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Analysis",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun TemplateCard(
    template: WorkoutTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${template.exercises.size} exercises",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "~${template.estimatedDurationMinutes} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (template.targetMuscleGroups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = template.targetMuscleGroups.take(2).joinToString { it.displayName },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun GymWorkoutCard(
    workout: GymWorkout,
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
                    text = formatDate(workout.startTime),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = workout.name.ifEmpty { "Workout" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${workout.exercises.size} exercises • ${workout.totalSets} sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = workout.durationFormatted,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (workout.totalVolume > 0) {
                    Text(
                        text = "${String.format("%.0f", workout.totalVolume)} kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyGymState() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = "Fitness",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No workouts yet",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Start your first gym workout to track your progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TemplateSelectionDialog(
    templates: List<WorkoutTemplate>,
    onTemplateSelected: (WorkoutTemplate) -> Unit,
    onStartEmpty: () -> Unit,
    onCreateTemplate: () -> Unit,
    onEditTemplate: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Workout") },
        text = {
            LazyColumn {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onStartEmpty)
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Start workout",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Empty Workout",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }

                if (templates.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Or choose a template:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(templates) { template ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTemplateSelected(template) }
                                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = template.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${template.exercises.size} exercises",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onEditTemplate(template.id) }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit template",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Navigate to workout",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onCreateTemplate)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit template",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Create New Template",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatVolumeDisplay(volume: Double): String {
    return if (volume >= 1000) {
        String.format("%.1fk kg", volume / 1000)
    } else {
        String.format("%.0f kg", volume)
    }
}

// ==================== MODERN GYM UI COMPONENTS ====================

@Composable
fun GymFeaturedWorkoutsSection(
    templates: List<WorkoutTemplate>,
    onTemplateClick: (WorkoutTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val workoutCards = if (templates.isNotEmpty()) {
        templates.take(4).map { template ->
            WorkoutCardData(
                id = template.id.toString(),
                title = template.name,
                description = template.description.ifEmpty { 
                    "${template.exercises.size} exercises targeting ${template.targetMuscleGroups.take(2).joinToString { it.displayName }}"
                },
                duration = "${template.estimatedDurationMinutes} min",
                difficulty = when {
                    template.exercises.size > 6 -> WorkoutDifficulty.HARD
                    template.exercises.size > 4 -> WorkoutDifficulty.MODERATE
                    else -> WorkoutDifficulty.EASY
                },
                type = com.runtracker.app.ui.components.WorkoutType.GYM,
                icon = Icons.Default.FitnessCenter
            )
        }
    } else {
        listOf(
            WorkoutCardData(
                id = "push",
                title = "Push Day",
                description = "Chest, shoulders, and triceps workout to build upper body pushing strength.",
                duration = "45 min",
                difficulty = WorkoutDifficulty.MODERATE,
                type = com.runtracker.app.ui.components.WorkoutType.GYM,
                icon = Icons.Default.FitnessCenter
            ),
            WorkoutCardData(
                id = "pull",
                title = "Pull Day",
                description = "Back and biceps focused workout for a strong posterior chain.",
                duration = "50 min",
                difficulty = WorkoutDifficulty.MODERATE,
                type = com.runtracker.app.ui.components.WorkoutType.GYM,
                icon = Icons.Default.FitnessCenter
            ),
            WorkoutCardData(
                id = "legs",
                title = "Leg Day",
                description = "Complete lower body workout targeting quads, hamstrings, and glutes.",
                duration = "55 min",
                difficulty = WorkoutDifficulty.HARD,
                type = com.runtracker.app.ui.components.WorkoutType.GYM,
                icon = Icons.Default.FitnessCenter
            )
        )
    }

    StackedWorkoutCards(
        cards = workoutCards,
        onCardClick = { card ->
            templates.find { it.id.toString() == card.id }?.let { onTemplateClick(it) }
        },
        modifier = modifier
    )
}

@Composable
fun ModernWeeklyGymStatsCard(
    stats: com.runtracker.shared.data.repository.GymWeeklyStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stats.totalVolumeFormatted,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "volume this week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = com.runtracker.app.ui.theme.GradientColors.PurpleBlue
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = "Fitness",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GymStatItem(
                    icon = Icons.Default.FitnessCenter,
                    value = stats.workoutCount.toString(),
                    label = "Workouts"
                )
                GymStatItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${(stats.totalVolume * 0.05).toInt()}",
                    label = "Calories"
                )
            }
        }
    }
}

@Composable
fun GymStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ModernGymQuickActions(
    onViewExercises: () -> Unit,
    onViewHistory: () -> Unit,
    onViewAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GymQuickActionChip(
                icon = Icons.Default.List,
                label = "Exercises",
                onClick = onViewExercises
            )
        }
        item {
            GymQuickActionChip(
                icon = Icons.Default.History,
                label = "History",
                onClick = onViewHistory
            )
        }
        item {
            GymQuickActionChip(
                icon = Icons.Default.Analytics,
                label = "Analysis",
                onClick = onViewAnalysis
            )
        }
    }
}

@Composable
fun GymQuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ModernGymWorkoutCard(
    workout: GymWorkout,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RecentActivityCard(
        title = workout.name.ifEmpty { "Workout" },
        subtitle = "${workout.exercises.size} exercises • ${workout.totalSets} sets",
        date = formatDate(workout.startTime),
        stats = listOf(
            "Volume" to formatVolumeDisplay(workout.totalVolume),
            "Duration" to workout.durationFormatted
        ),
        type = com.runtracker.app.ui.components.WorkoutType.GYM,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun ModernEmptyGymState(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = com.runtracker.app.ui.theme.GradientColors.PurpleBlue
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "Fitness",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Ready to lift?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start your first gym workout to track your progress",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ModernGymStartButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0xFF7C4DFF).copy(alpha = 0.5f),
                spotColor = Color(0xFF7C4DFF).copy(alpha = 0.5f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF667EEA)
        ),
        contentPadding = PaddingValues(horizontal = 32.dp)
    ) {
        Icon(
            Icons.Default.FitnessCenter,
            contentDescription = "Fitness",
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Start Workout",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
