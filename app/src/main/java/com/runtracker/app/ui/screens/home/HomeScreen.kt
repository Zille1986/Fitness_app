package com.runtracker.app.ui.screens.home

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.runtracker.app.ui.components.*
import com.runtracker.app.ui.theme.GradientColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRunning: () -> Unit,
    onNavigateToSwimming: () -> Unit = {},
    onNavigateToCycling: () -> Unit = {},
    onNavigateToGym: () -> Unit,
    onNavigateToNutrition: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToMindfulness: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = getGreeting(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.userName.ifEmpty { "Athlete" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        val quote = remember { getRandomQuote() }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (com.runtracker.app.ui.theme.LocalIsDarkTheme.current)
                            GradientColors.ScreenBackground
                        else GradientColors.ScreenBackgroundLight
                    )
                )
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today's Exercises first (most actionable)
            item {
                TodaysExercisesCard(
                    todayWorkouts = uiState.todayWorkouts,
                    onStartWorkout = { workout ->
                        when (workout.type) {
                            "running" -> onNavigateToRunning()
                            "gym" -> onNavigateToGym()
                            else -> {}
                        }
                    }
                )
            }

            // Main Session — editorial hero card with background photo
            item {
                val nextWorkout = uiState.todayWorkouts.firstOrNull { !it.completed }
                    ?: uiState.todayWorkouts.firstOrNull()
                MainSessionCard(
                    workout = nextWorkout,
                    onStart = {
                        when (nextWorkout?.type) {
                            "running" -> onNavigateToRunning()
                            "gym" -> onNavigateToGym()
                            else -> onNavigateToRunning()
                        }
                    }
                )
            }

            // Streak Counter
            if (uiState.currentStreak > 0 || uiState.longestStreak > 0) {
                item {
                    StreakCard(
                        currentStreak = uiState.currentStreak,
                        longestStreak = uiState.longestStreak,
                        message = uiState.streakMessage
                    )
                }
            }

            // Weekly Activity Overview
            item {
                WeeklyActivityCard(
                    weeklyStats = uiState.weeklyStats,
                    onViewRunning = onNavigateToRunning,
                    onViewSwimming = onNavigateToSwimming,
                    onViewCycling = onNavigateToCycling,
                    onViewGym = onNavigateToGym
                )
            }
            
            // AI Readiness Score with animated ring
            item {
                ReadinessScoreCard(
                    readinessScore = uiState.readinessScore,
                    readinessFactors = uiState.readinessFactors,
                    recommendation = uiState.aiRecommendation
                )
            }
            
            // Nutrition Overview with animated rings
            item {
                NutritionOverviewCard(
                    caloriesConsumed = uiState.caloriesConsumed,
                    caloriesGoal = uiState.caloriesGoal,
                    proteinConsumed = uiState.proteinConsumed,
                    proteinGoal = uiState.proteinGoal,
                    hasData = uiState.hasNutritionData,
                    onViewNutrition = onNavigateToNutrition
                )
            }

            // Motivational Quote (subtle, at the bottom)
            item {
                MotivationalQuoteCard(
                    quote = quote.first,
                    author = quote.second
                )
            }
        }
    }
}

@Composable
private fun TodaysExercisesCard(
    todayWorkouts: List<TodayWorkout>,
    onStartWorkout: (TodayWorkout) -> Unit
) {
    val view = LocalView.current
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TODAY'S PLAN",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                text = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (todayWorkouts.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rest day - recover and recharge",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            todayWorkouts.forEach { workout ->
                WorkoutItem(
                    workout = workout,
                    onStart = { 
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onStartWorkout(workout) 
                    }
                )
                if (workout != todayWorkouts.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WorkoutItem(
    workout: TodayWorkout,
    onStart: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (workout.type == "running") Icons.Default.DirectionsRun else Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = if (workout.completed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = workout.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (workout.completed) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color(0xFF4CAF50)
                )
            } else {
                FilledTonalButton(onClick = onStart) {
                    Text("Start")
                }
            }
        }
    }
}

@Composable
private fun WeeklyActivityCard(
    weeklyStats: WeeklyStats,
    onViewRunning: () -> Unit,
    onViewSwimming: () -> Unit,
    onViewCycling: () -> Unit,
    onViewGym: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = MaterialTheme.colorScheme.secondary
    ) {
        Text(
            text = "THIS WEEK",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Activity grid - 2x2 with background photos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PhotoActivityItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DirectionsRun,
                label = "Run",
                value = "${weeklyStats.runCount}",
                subValue = String.format("%.1f km", weeklyStats.totalRunDistance / 1000),
                imageUrl = com.runtracker.app.ui.components.SportImages.RUNNING,
                onClick = onViewRunning
            )

            PhotoActivityItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Pool,
                label = "Swim",
                value = "${weeklyStats.swimCount}",
                subValue = String.format("%.1f km", weeklyStats.totalSwimDistance / 1000),
                imageUrl = com.runtracker.app.ui.components.SportImages.SWIMMING,
                onClick = onViewSwimming
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PhotoActivityItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DirectionsBike,
                label = "Bike",
                value = "${weeklyStats.bikeCount}",
                subValue = String.format("%.1f km", weeklyStats.totalBikeDistance / 1000),
                imageUrl = com.runtracker.app.ui.components.SportImages.CYCLING,
                onClick = onViewCycling
            )

            PhotoActivityItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.FitnessCenter,
                label = "Gym",
                value = "${weeklyStats.gymCount}",
                subValue = "${weeklyStats.totalSets} sets",
                imageUrl = com.runtracker.app.ui.components.SportImages.GYM,
                onClick = onViewGym
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val totalWorkouts = weeklyStats.runCount + weeklyStats.swimCount + weeklyStats.bikeCount + weeklyStats.gymCount
        val progress = (totalWorkouts / 7f).coerceIn(0f, 1f)
        
        // Modern progress bar with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
        
        Text(
            text = "$totalWorkouts of 7 workouts completed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun CompactActivityItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Radial glow behind icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subValue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoActivityItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        // Background photo
        AsyncImage(
            model = imageUrl,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subValue,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReadinessScoreCard(
    readinessScore: Int,
    readinessFactors: List<ReadinessFactor>,
    recommendation: String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = when {
            readinessScore >= 80 -> Color(0xFF7EE787)
            readinessScore >= 60 -> Color(0xFFFFA657)
            else -> Color(0xFFF85149)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "READINESS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "AI Coach Assessment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Animated score circle with glow
            Box(contentAlignment = Alignment.Center) {
                AnimatedScoreCircle(
                    score = readinessScore,
                    maxScore = 100,
                    modifier = Modifier,
                    size = 72.dp,
                    strokeWidth = 6.dp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Factors with modern styling
        readinessFactors.forEach { factor ->
            val factorColor = when {
                factor.status.startsWith("Good") -> Color(0xFF7EE787)
                factor.status.startsWith("Moderate") -> Color(0xFFFFA657)
                factor.status == "No data" -> MaterialTheme.colorScheme.onSurfaceVariant
                factor.status.startsWith("Low") -> Color(0xFFF85149)
                else -> Color(0xFFF85149)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = factor.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(factorColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = factor.status,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = factorColor
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // AI recommendation with subtle styling
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = recommendation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun NutritionOverviewCard(
    caloriesConsumed: Int,
    caloriesGoal: Int,
    proteinConsumed: Int,
    proteinGoal: Int,
    hasData: Boolean = true,
    onViewNutrition: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewNutrition),
        accentColor = Color(0xFF7EE787)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NUTRITION",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7EE787),
                letterSpacing = 1.sp
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasData) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No data — tap to log a meal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionCircle(
                    label = "Calories",
                    consumed = caloriesConsumed,
                    goal = caloriesGoal,
                    unit = "kcal",
                    color = Color(0xFFFFA657)
                )
                NutritionCircle(
                    label = "Protein",
                    consumed = proteinConsumed,
                    goal = proteinGoal,
                    unit = "g",
                    color = Color(0xFF00E5CC)
                )
            }
        }
    }
}

@Composable
private fun NutritionCircle(
    label: String,
    consumed: Int,
    goal: Int,
    unit: String,
    color: Color
) {
    val progress = (consumed.toFloat() / goal).coerceIn(0f, 1f)
    
    val animatedConsumed by animateIntAsState(
        targetValue = consumed,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "consumed"
    )
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedProgressRing(
                progress = progress,
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 8.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$animatedConsumed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "of $goal",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionsRow(
    onSwimming: () -> Unit,
    onCycling: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Pool,
            title = "Swimming",
            subtitle = "Pool, Ocean, Lake",
            color = Color(0xFF0288D1),
            onClick = onSwimming
        )
        QuickActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.DirectionsBike,
            title = "Cycling",
            subtitle = "Outdoor & Trainer",
            color = Color(0xFFFF5722),
            onClick = onCycling
        )
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
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
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
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

@Composable
private fun StreakCard(
    currentStreak: Int,
    longestStreak: Int,
    message: String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = Color(0xFFFFA657)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color(0xFFFFA657).copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔥",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$currentStreak day${if (currentStreak != 1) "s" else ""}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (longestStreak > currentStreak) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Best",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$longestStreak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFA657)
                    )
                }
            }
        }
    }
}

@Composable
private fun MainSessionCard(
    workout: TodayWorkout?,
    onStart: () -> Unit
) {
    val workoutType = workout?.type ?: "running"
    val workoutName = workout?.name ?: "Ready to Train"
    val workoutDescription = workout?.description ?: "Start a new session and push your limits."

    val imageUrl = remember(workoutType) {
        when (workoutType) {
            "running" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuCddTvozj6m5uomnWYVHwxZaAaK5AV2kxpSdILwp70J6ObXKKRQVYaTsBPS8Ykzhlrc5-cu4Ab6VOHAywPX1oLwEoxOZpjQUb48e2QARQHQjOHWiZ_pfiNXLX7GS6wrMcS6w9XpB0K_fJXLD0T4c7bkul5Xcbm4v18FW5qoqKPIHbsOiHk95HOht7ua3xZ2Viknx5lD4tN1ljD_3DHGo5sQnY5RvRlG21I8upPyDi69hyqL0-gVWoEYxzY6gXrekrRTR5-X3Pqyuo4"
            "gym" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuDo7bcdz_LXnmWIRlyKR5NciFFjr_pr53RZUmNcYOroD7PG-dfbIXC98hSFH8V-M5QPzrVVqAdomCskIfD8siA7ACuynB5u8w8uLtMJ_XnQ2l28eQqMVi-ORNkxi2e3e7c15m7r5EFlJzdNveO_NOl2yN5FFmNnOSXFXaru5JXSqJQnEsihbi9DWVsIENtnXlHQCZmL4_7EPzTH2pzjDFQ2kpXvw7BptPPvexhjW9ygA3DYqxlZf-sGEIPaFOg6cb-Y1UN3xRm42VA"
            else -> "https://lh3.googleusercontent.com/aida-public/AB6AXuCddTvozj6m5uomnWYVHwxZaAaK5AV2kxpSdILwp70J6ObXKKRQVYaTsBPS8Ykzhlrc5-cu4Ab6VOHAywPX1oLwEoxOZpjQUb48e2QARQHQjOHWiZ_pfiNXLX7GS6wrMcS6w9XpB0K_fJXLD0T4c7bkul5Xcbm4v18FW5qoqKPIHbsOiHk95HOht7ua3xZ2Viknx5lD4tN1ljD_3DHGo5sQnY5RvRlG21I8upPyDi69hyqL0-gVWoEYxzY6gXrekrRTR5-X3Pqyuo4"
        }
    }

    val categoryLabel = when (workoutType) {
        "running" -> "SPEED WORK"
        "gym" -> "STRENGTH"
        else -> "SESSION"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onStart)
    ) {
        // Background photo
        AsyncImage(
            model = imageUrl,
            contentDescription = workoutName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Category chip + duration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = categoryLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "45 Minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = workoutName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )

            // Description
            Text(
                text = workoutDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Start Session",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                OutlinedButton(
                    onClick = onStart,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "View Details",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

// Data classes
data class TodayWorkout(
    val id: Long,
    val name: String,
    val description: String,
    val type: String, // "running" or "gym"
    val completed: Boolean
)

data class WeeklyStats(
    val runCount: Int = 0,
    val totalRunDistance: Float = 0f,
    val gymCount: Int = 0,
    val totalSets: Int = 0,
    val swimCount: Int = 0,
    val totalSwimDistance: Float = 0f,
    val bikeCount: Int = 0,
    val totalBikeDistance: Float = 0f
)

data class ReadinessFactor(
    val name: String,
    val status: String // "Good", "Moderate", "Low"
)
