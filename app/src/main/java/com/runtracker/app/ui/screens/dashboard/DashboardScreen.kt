package com.runtracker.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.runtracker.app.ui.components.*
import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.model.ScheduledWorkout
import com.runtracker.shared.data.model.WorkoutType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onStartRun: () -> Unit,
    onViewHistory: () -> Unit,
    onViewTrainingPlans: () -> Unit,
    onViewAnalytics: () -> Unit,
    onViewProfile: () -> Unit,
    onViewGym: () -> Unit,
    onViewNutrition: () -> Unit,
    onViewGamification: () -> Unit = {},
    onViewMindfulness: () -> Unit = {},
    onViewFormAnalysis: () -> Unit = {},
    onViewAiCoach: () -> Unit = {},
    onViewBodyAnalysis: () -> Unit = {},
    onRunClick: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "RunTracker",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    // Weekly streak badge
                    WeeklyStreakBadge(streakCount = uiState.weeklyRunCount)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onViewProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Category chips (Strava-style)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        CategoryChip(
                            label = "Run",
                            icon = Icons.Default.DirectionsRun,
                            selected = selectedCategory == 0,
                            onClick = { selectedCategory = 0 }
                        )
                    }
                    item {
                        CategoryChip(
                            label = "Gym",
                            icon = Icons.Default.FitnessCenter,
                            selected = selectedCategory == 1,
                            onClick = { selectedCategory = 1; onViewGym() }
                        )
                    }
                    item {
                        CategoryChip(
                            label = "Mind",
                            icon = Icons.Default.SelfImprovement,
                            selected = selectedCategory == 2,
                            onClick = { selectedCategory = 2; onViewMindfulness() }
                        )
                    }
                    item {
                        CategoryChip(
                            label = "Coach",
                            icon = Icons.Default.Psychology,
                            selected = selectedCategory == 3,
                            onClick = { selectedCategory = 3; onViewAiCoach() }
                        )
                    }
                }
            }
            
            // Suggested workout hint
            item {
                SuggestedWorkoutHint(
                    message = "Go a little longer or faster than usual.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Featured workout cards (Strava-style stacked cards)
            item {
                FeaturedWorkoutsSection(
                    onStartRun = onStartRun,
                    onViewPlans = onViewTrainingPlans,
                    nextWorkout = uiState.nextWorkout
                )
            }
            
            // Weekly Progress
            item {
                SectionHeader(
                    title = "This Week",
                    action = "Details",
                    onActionClick = onViewAnalytics,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            item {
                ModernWeeklyProgressCard(
                    uiState = uiState,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Quick Actions Grid
            item {
                SectionHeader(
                    title = "Quick Actions",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            item {
                ModernQuickActionsGrid(
                    onViewHistory = onViewHistory,
                    onViewTrainingPlans = onViewTrainingPlans,
                    onViewAnalytics = onViewAnalytics,
                    onViewGym = onViewGym,
                    onViewNutrition = onViewNutrition,
                    onViewGamification = onViewGamification,
                    onViewFormAnalysis = onViewFormAnalysis,
                    onViewBodyAnalysis = onViewBodyAnalysis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Recent Activity - Carousel
            if (uiState.recentRuns.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Recent Activity",
                        action = "See All",
                        onActionClick = onViewHistory,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    RecentActivitiesCarousel(
                        activities = uiState.recentRuns.take(5).map { run ->
                            ActivityCardData(
                                id = run.id,
                                title = "${String.format("%.2f", run.distanceKm)} km Run",
                                subtitle = run.avgPaceFormatted + "/km",
                                date = formatDate(run.startTime),
                                mainStat = String.format("%.2f", run.distanceKm),
                                mainStatLabel = "km",
                                secondaryStat = run.durationFormatted,
                                secondaryStatLabel = "time",
                                type = com.runtracker.app.ui.components.WorkoutType.RUNNING
                            )
                        },
                        onActivityClick = { activity -> onRunClick(activity.id) }
                    )
                }
            } else {
                item {
                    ModernEmptyStateCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // Floating action button
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        ModernStartButton(
            onClick = onStartRun,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
fun WeeklyProgressCard(uiState: DashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "This Week",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    value = String.format("%.1f", uiState.weeklyDistanceKm),
                    unit = "km",
                    label = "Distance"
                )
                StatItem(
                    value = uiState.weeklyDurationFormatted,
                    unit = "",
                    label = "Time"
                )
                StatItem(
                    value = uiState.weeklyRunCount.toString(),
                    unit = "",
                    label = "Runs"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val goalKm = uiState.userProfile?.weeklyGoalKm ?: 20.0
            Text(
                text = "Goal: ${String.format("%.1f", uiState.weeklyDistanceKm)} / ${goalKm.toInt()} km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = uiState.weeklyGoalProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun StatItem(value: String, unit: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun QuickActionsRow(
    onViewHistory: () -> Unit,
    onViewTrainingPlans: () -> Unit,
    onViewAnalytics: () -> Unit,
    onViewGym: () -> Unit,
    onViewNutrition: () -> Unit,
    onViewGamification: () -> Unit = {},
    onViewMindfulness: () -> Unit = {},
    onViewFormAnalysis: () -> Unit = {},
    onViewAiCoach: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.History,
                label = "History",
                onClick = onViewHistory
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarMonth,
                label = "Plans",
                onClick = onViewTrainingPlans
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Analytics,
                label = "Analytics",
                onClick = onViewAnalytics
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.FitnessCenter,
                label = "Gym",
                onClick = onViewGym
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Restaurant,
                label = "Nutrition",
                onClick = onViewNutrition
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                label = "Progress",
                onClick = onViewGamification
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.SelfImprovement,
                label = "Mindfulness",
                onClick = onViewMindfulness
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DirectionsRun,
                label = "Form Coach",
                onClick = onViewFormAnalysis
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Psychology,
                label = "AI Coach",
                onClick = onViewAiCoach
            )
        }
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun NextWorkoutCard(workout: ScheduledWorkout, onStartWorkout: () -> Unit) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.secondary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getWorkoutIcon(workout.workoutType),
                    contentDescription = "Workout type",
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Next Workout",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = workout.workoutType.name.replace("_", " "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                workout.targetDistanceMeters?.let { distance ->
                    Text(
                        text = "${String.format("%.1f", distance / 1000)} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            FilledTonalButton(onClick = onStartWorkout) {
                Text("Start")
            }
        }
    }
}

@Composable
fun RunCard(run: Run, onClick: () -> Unit) {
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
                    text = formatDate(run.startTime),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format("%.2f", run.distanceKm)} km",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = run.durationFormatted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${run.avgPaceFormatted} /km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View run details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsRun,
                contentDescription = "Running",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No runs yet",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Start your first run to see your progress here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getWorkoutIcon(workoutType: WorkoutType): ImageVector {
    return when (workoutType) {
        WorkoutType.EASY_RUN -> Icons.Default.DirectionsRun
        WorkoutType.LONG_RUN -> Icons.Default.Hiking
        WorkoutType.TEMPO_RUN -> Icons.Default.Speed
        WorkoutType.INTERVAL_TRAINING -> Icons.Default.Timer
        WorkoutType.HILL_REPEATS -> Icons.Default.Terrain
        WorkoutType.RECOVERY_RUN -> Icons.Default.SelfImprovement
        WorkoutType.RACE_PACE -> Icons.Default.EmojiEvents
        WorkoutType.FARTLEK -> Icons.Default.Shuffle
        WorkoutType.REST_DAY -> Icons.Default.Hotel
        WorkoutType.CROSS_TRAINING -> Icons.Default.FitnessCenter
        else -> Icons.Default.DirectionsRun
    }
}

// ==================== MODERN UI COMPONENTS ====================

@Composable
fun SuggestedWorkoutHint(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = "Tip",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FeaturedWorkoutsSection(
    onStartRun: () -> Unit,
    onViewPlans: () -> Unit,
    nextWorkout: ScheduledWorkout?,
    modifier: Modifier = Modifier
) {
    val workoutCards = listOf(
        WorkoutCardData(
            id = "tempo",
            title = "Tempo 5 km",
            description = "Improve your 5K-specific endurance with sustained tempo running. Build the speed and stamina needed for faster race performances.",
            duration = "35 min",
            difficulty = WorkoutDifficulty.HARD,
            type = com.runtracker.app.ui.components.WorkoutType.RUNNING,
            icon = Icons.Default.DirectionsRun
        ),
        WorkoutCardData(
            id = "easy",
            title = "Easy Run",
            description = "A relaxed pace run to build your aerobic base and aid recovery.",
            duration = "30 min",
            difficulty = WorkoutDifficulty.EASY,
            type = com.runtracker.app.ui.components.WorkoutType.RUNNING,
            icon = Icons.Default.DirectionsRun
        ),
        WorkoutCardData(
            id = "intervals",
            title = "Speed Intervals",
            description = "High-intensity intervals to boost your VO2 max and running economy.",
            duration = "45 min",
            difficulty = WorkoutDifficulty.HARD,
            type = com.runtracker.app.ui.components.WorkoutType.RUNNING,
            icon = Icons.Default.Speed
        ),
        WorkoutCardData(
            id = "long",
            title = "Long Run",
            description = "Build endurance with this extended distance run at a comfortable pace.",
            duration = "1h 15m",
            difficulty = WorkoutDifficulty.MODERATE,
            type = com.runtracker.app.ui.components.WorkoutType.RUNNING,
            icon = Icons.Default.Hiking
        )
    )

    StackedWorkoutCards(
        cards = workoutCards,
        onCardClick = { onStartRun() },
        modifier = modifier
    )
}

@Composable
fun ModernWeeklyProgressCard(
    uiState: DashboardUiState,
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
                // Distance
                Column {
                    Text(
                        text = String.format("%.1f", uiState.weeklyDistanceKm),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "km this week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Goal progress ring
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = uiState.weeklyGoalProgress.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${(uiState.weeklyGoalProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ModernStatItem(
                    icon = Icons.Default.Timer,
                    value = uiState.weeklyDurationFormatted,
                    label = "Time"
                )
                ModernStatItem(
                    icon = Icons.Default.DirectionsRun,
                    value = uiState.weeklyRunCount.toString(),
                    label = "Runs"
                )
                ModernStatItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${(uiState.weeklyDistanceKm * 60).toInt()}",
                    label = "Calories"
                )
            }
        }
    }
}

@Composable
fun ModernStatItem(
    icon: ImageVector,
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
fun ModernQuickActionsGrid(
    onViewHistory: () -> Unit,
    onViewTrainingPlans: () -> Unit,
    onViewAnalytics: () -> Unit,
    onViewGym: () -> Unit,
    onViewNutrition: () -> Unit,
    onViewGamification: () -> Unit,
    onViewFormAnalysis: () -> Unit,
    onViewBodyAnalysis: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        Triple(Icons.Default.FitnessCenter, "Gym", onViewGym),
        Triple(Icons.Default.CalendarMonth, "Plans", onViewTrainingPlans),
        Triple(Icons.Default.Analytics, "Stats", onViewAnalytics),
        Triple(Icons.Default.Restaurant, "Nutrition", onViewNutrition),
        Triple(Icons.Default.EmojiEvents, "Progress", onViewGamification),
        Triple(Icons.Default.Videocam, "Form", onViewFormAnalysis),
        Triple(Icons.Default.Person, "Body", onViewBodyAnalysis)
    )
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(actions) { (icon, label, onClick) ->
            ModernQuickActionChip(
                icon = icon,
                label = label,
                onClick = onClick
            )
        }
    }
}

@Composable
fun ModernQuickActionChip(
    icon: ImageVector,
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
fun ModernRunCard(
    run: Run,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RecentActivityCard(
        title = "${String.format("%.2f", run.distanceKm)} km Run",
        subtitle = "${run.durationFormatted} • ${run.avgPaceFormatted}/km",
        date = formatDate(run.startTime),
        stats = listOf(
            "Pace" to run.avgPaceFormatted,
            "Time" to run.durationFormatted
        ),
        type = com.runtracker.app.ui.components.WorkoutType.RUNNING,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun ModernEmptyStateCard(modifier: Modifier = Modifier) {
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
                            colors = listOf(Color(0xFFFF6B35), Color(0xFFE84118))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsRun,
                    contentDescription = "Running",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Ready to run?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start your first run to see your progress here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ModernStartButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0xFFFF6B35).copy(alpha = 0.4f),
                spotColor = Color(0xFFFF6B35).copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF6B35)
        ),
        contentPadding = PaddingValues(horizontal = 32.dp)
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "Start run",
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Start Run",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
