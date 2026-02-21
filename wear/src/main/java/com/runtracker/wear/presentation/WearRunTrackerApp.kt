package com.runtracker.wear.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.runtracker.wear.service.WearTrackingState

enum class ActivityType {
    RUNNING, SWIMMING, CYCLING
}

@Composable
fun WearRunTrackerApp(
    trackingState: WearTrackingState,
    isAmbient: Boolean = false,
    pendingWorkout: com.runtracker.shared.data.model.ScheduledWorkout? = null,
    onStartRun: () -> Unit,
    onStartWorkoutRun: (com.runtracker.shared.data.model.WorkoutType) -> Unit,
    onStartSwim: (String, Int) -> Unit = { _, _ -> },  // swimType, poolLength
    onStartSwimWorkout: (SwimWorkoutTypeWatch, String, Int) -> Unit = { _, _, _ -> },  // workoutType, swimType, poolLength
    onStartCycling: (String) -> Unit = {},
    onStartCyclingWorkout: (CycleWorkoutTypeWatch, String) -> Unit = { _, _ -> },
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
    onStopRun: () -> Unit,
    onClearPendingWorkout: () -> Unit
) {
    val navController = rememberSwipeDismissableNavController()

    // Consolidated navigation effect — single source of truth prevents race conditions
    // between tracking state and pending workout navigation
    LaunchedEffect(trackingState.isTracking, pendingWorkout) {
        when {
            // Tracking takes highest priority
            trackingState.isTracking -> {
                if (navController.currentDestination?.route != "tracking") {
                    navController.navigate("tracking") {
                        popUpTo("activity_select") { inclusive = false }
                    }
                }
            }
            // Pending workout from phone
            pendingWorkout != null -> {
                if (navController.currentDestination?.route != "workout_preview") {
                    navController.navigate("workout_preview") {
                        popUpTo("activity_select") { inclusive = false }
                    }
                }
            }
            // Return to activity select when tracking stops
            navController.currentDestination?.route == "tracking" -> {
                navController.navigate("activity_select") {
                    popUpTo("activity_select") { inclusive = true }
                }
            }
        }
    }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "activity_select"
    ) {
        // New activity selection home screen
        composable("activity_select") {
            ActivitySelectScreen(
                onSelectRunning = { navController.navigate("home") },
                onSelectSwimming = { navController.navigate("swimming_home") },
                onSelectCycling = { navController.navigate("cycling_home") },
                hasPendingWorkout = pendingWorkout != null,
                onViewPendingWorkout = { navController.navigate("workout_preview") },
                onMusicControl = { navController.navigate("music_control") }
            )
        }
        
        // Running home
        composable("home") {
            HomeScreen(
                onStartRun = onStartRun,
                onSelectWorkout = { navController.navigate("workout_select") },
                hasPendingWorkout = pendingWorkout != null,
                onViewPendingWorkout = { navController.navigate("workout_preview") },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Swimming home
        composable("swimming_home") {
            SwimmingHomeScreen(
                onStartSwim = { swimType, poolLength -> onStartSwim(swimType, poolLength) },
                onStartSwimWorkout = { workoutType, swimType, poolLength -> 
                    onStartSwimWorkout(workoutType, swimType, poolLength)
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Cycling home
        composable("cycling_home") {
            CyclingHomeScreen(
                onStartCycling = { cyclingType -> onStartCycling(cyclingType) },
                onStartCyclingWorkout = { workoutType, cyclingType ->
                    onStartCyclingWorkout(workoutType, cyclingType)
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("workout_select") {
            WorkoutSelectScreen(
                onWorkoutSelected = { workoutType ->
                    navController.navigate("workout_preview_type/$workoutType")
                },
                onCustomWorkouts = { navController.navigate("custom_workouts") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("custom_workouts") {
            CustomWorkoutsScreen(
                onWorkoutSelected = { workoutJson ->
                    // Navigate to custom workout preview
                    navController.navigate("custom_workout_preview")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("custom_workout_preview") {
            // TODO: Show custom workout preview from synced data
            // For now, navigate back
            navController.popBackStack()
        }
        composable("workout_preview_type/{workoutType}") { backStackEntry ->
            val workoutTypeName = backStackEntry.arguments?.getString("workoutType") ?: "EASY_RUN"
            val workoutType = try {
                com.runtracker.shared.data.model.WorkoutType.valueOf(workoutTypeName)
            } catch (e: Exception) {
                com.runtracker.shared.data.model.WorkoutType.EASY_RUN
            }
            WorkoutTypePreviewScreen(
                workoutType = workoutType,
                onStartRun = { 
                    onStartWorkoutRun(workoutType)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("workout_preview") {
            pendingWorkout?.let { workout ->
                WorkoutPreviewScreen(
                    workout = workout,
                    onStartRun = {
                        onClearPendingWorkout()
                        onStartRun()
                    },
                    onBack = { 
                        onClearPendingWorkout()
                        navController.popBackStack() 
                    }
                )
            }
        }
        composable("tracking") {
            TrackingPagerScreen(
                trackingState = trackingState,
                isAmbient = isAmbient,
                onPause = onPauseRun,
                onResume = onResumeRun,
                onStop = onStopRun
            )
        }
        
        // Music Controls
        composable("music_control") {
            MusicControlScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun ActivitySelectScreen(
    onSelectRunning: () -> Unit,
    onSelectSwimming: () -> Unit,
    onSelectCycling: () -> Unit,
    hasPendingWorkout: Boolean,
    onViewPendingWorkout: () -> Unit,
    onMusicControl: () -> Unit = {}
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            autoCentering = AutoCenteringParams(itemIndex = 0),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            // Snap fling gives native Wear OS scroll feel (like Fitbit)
            flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState)
        ) {
            item {
                Text(
                    text = "Start Workout",
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Pending workout from phone
            if (hasPendingWorkout) {
                item {
                    Chip(
                        onClick = onViewPendingWorkout,
                        label = {
                            Column {
                                Text(
                                    text = "📱 From Phone",
                                    style = MaterialTheme.typography.body1,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "View synced workout",
                                    style = MaterialTheme.typography.caption2,
                                    color = MaterialTheme.colors.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = ChipDefaults.primaryChipColors(
                            backgroundColor = Color(0xFF4CAF50)
                        )
                    )
                }
            }
            
            // Running
            item {
                Chip(
                    onClick = onSelectRunning,
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🏃", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = "Running",
                                    style = MaterialTheme.typography.body1,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Outdoor & treadmill",
                                    style = MaterialTheme.typography.caption2,
                                    color = MaterialTheme.colors.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
            
            // Swimming
            item {
                Chip(
                    onClick = onSelectSwimming,
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🏊", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = "Swimming",
                                    style = MaterialTheme.typography.body1,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pool & open water",
                                    style = MaterialTheme.typography.caption2,
                                    color = MaterialTheme.colors.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF0288D1).copy(alpha = 0.3f)
                    )
                )
            }
            
            // Cycling
            item {
                Chip(
                    onClick = onSelectCycling,
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🚴", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = "Cycling",
                                    style = MaterialTheme.typography.body1,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Outdoor & indoor",
                                    style = MaterialTheme.typography.caption2,
                                    color = MaterialTheme.colors.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFFFF5722).copy(alpha = 0.3f)
                    )
                )
            }
            
            // Music Controls
            item {
                Chip(
                    onClick = onMusicControl,
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🎵", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = "Music",
                                    style = MaterialTheme.typography.body1,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Control playback",
                                    style = MaterialTheme.typography.caption2,
                                    color = MaterialTheme.colors.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF9C27B0).copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

// Swimming workout types with HR zones
enum class SwimWorkoutTypeWatch(
    val displayName: String,
    val description: String,
    val targetHrZone: Int,  // 1-5
    val durationMinutes: Int
) {
    EASY_SWIM("Easy Swim", "Zone 2 • Recovery pace", 2, 30),
    ENDURANCE_SWIM("Endurance Swim", "Zone 3 • Build aerobic base", 3, 45),
    THRESHOLD_SWIM("Threshold Swim", "Zone 4 • Push your limits", 4, 30),
    INTERVAL_SWIM("Interval Sets", "Zone 4-5 • Speed work", 4, 40),
    TECHNIQUE_DRILL("Technique Drills", "Zone 2 • Focus on form", 2, 30),
    OPEN_WATER("Open Water", "Zone 3 • Distance swim", 3, 60),
    SPRINT_SETS("Sprint Sets", "Zone 5 • Max effort", 5, 25),
    RECOVERY_SWIM("Recovery Swim", "Zone 1 • Active recovery", 1, 20)
}

// Cycling workout types with HR zones
enum class CycleWorkoutTypeWatch(
    val displayName: String,
    val description: String,
    val targetHrZone: Int,
    val durationMinutes: Int
) {
    EASY_RIDE("Easy Ride", "Zone 2 • Recovery spin", 2, 45),
    ENDURANCE_RIDE("Endurance Ride", "Zone 3 • Build base", 3, 90),
    TEMPO_RIDE("Tempo Ride", "Zone 3-4 • Sustained effort", 4, 60),
    THRESHOLD_RIDE("Threshold Ride", "Zone 4 • FTP work", 4, 45),
    INTERVAL_RIDE("Intervals", "Zone 4-5 • Power bursts", 5, 50),
    HILL_CLIMB("Hill Climb", "Zone 4-5 • Climbing focus", 4, 60),
    SPRINT_INTERVALS("Sprint Intervals", "Zone 5 • Max power", 5, 30),
    RECOVERY_RIDE("Recovery Ride", "Zone 1 • Easy spin", 1, 30)
}

@Composable
fun SwimmingHomeScreen(
    onStartSwim: (String, Int) -> Unit,  // swimType, poolLength
    onStartSwimWorkout: (SwimWorkoutTypeWatch, String, Int) -> Unit,  // workoutType, swimType, poolLength
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    var selectedSwimType by remember { mutableStateOf<String?>(null) }
    var showWorkouts by remember { mutableStateOf(false) }
    var showPoolLengthPicker by remember { mutableStateOf(false) }
    var selectedPoolLength by remember { mutableStateOf(25) } // Default 25m
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        onDismissed = {
            if (showWorkouts) {
                showWorkouts = false
            } else {
                onBack()
            }
        }
    ) { isBackground ->
        if (!isBackground) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                autoCentering = AutoCenteringParams(itemIndex = 0),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 26.dp),
                flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState)
            ) {
                if (!showWorkouts) {
                    // Swim type selection
                    item {
                        Text(
                            text = "🏊 Swimming",
                            style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0288D1),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    // Quick start options
                    item {
                        Text(
                            text = "Quick Start",
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    // Pool length selector
                    item {
                        Chip(
                            onClick = { showPoolLengthPicker = true },
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pool Length",
                                        style = MaterialTheme.typography.body1
                                    )
                                    Text(
                                        text = "${selectedPoolLength}m",
                                        style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0288D1)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ChipDefaults.secondaryChipColors()
                        )
                    }
                    
                    item {
                        Chip(
                            onClick = { onStartSwim("POOL", selectedPoolLength) },
                            label = {
                                Column {
                                    Text(
                                        text = "Pool Swim",
                                        style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Free swim • ${selectedPoolLength}m pool",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ChipDefaults.primaryChipColors(
                                backgroundColor = Color(0xFF0288D1)
                            )
                        )
                    }
                    
                    item {
                        Chip(
                            onClick = { onStartSwim("OPEN_WATER", 0) },
                            label = {
                                Column {
                                    Text(
                                        text = "Open Water",
                                        style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Free swim • GPS tracking",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ChipDefaults.secondaryChipColors()
                        )
                    }
                    
                    // Structured workouts
                    item {
                        Text(
                            text = "Workouts with HR Zones",
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    item {
                        Chip(
                            onClick = { 
                                selectedSwimType = "POOL"
                                showWorkouts = true 
                            },
                            label = {
                                Column {
                                    Text(
                                        text = "Pool Workouts",
                                        style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Structured with HR guidance",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ChipDefaults.chipColors(
                                backgroundColor = Color(0xFF0288D1).copy(alpha = 0.3f)
                            )
                        )
                    }
                    
                    item {
                        Chip(
                            onClick = { 
                                selectedSwimType = "OPEN_WATER"
                                showWorkouts = true 
                            },
                            label = {
                                Column {
                                    Text(
                                        text = "Open Water Workouts",
                                        style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Structured with HR guidance",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ChipDefaults.chipColors(
                                backgroundColor = Color(0xFF0288D1).copy(alpha = 0.3f)
                            )
                        )
                    }
                } else {
                    // Workout selection
                    item {
                        Text(
                            text = "Select Workout",
                            style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0288D1),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    SwimWorkoutTypeWatch.values().forEach { workout ->
                        item {
                            Chip(
                                onClick = { 
                                    onStartSwimWorkout(workout, selectedSwimType ?: "POOL", selectedPoolLength)
                                },
                                label = {
                                    Column {
                                        Text(
                                            text = workout.displayName,
                                            style = MaterialTheme.typography.body1,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = workout.description,
                                            style = MaterialTheme.typography.caption2,
                                            color = MaterialTheme.colors.onSurfaceVariant
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                colors = ChipDefaults.secondaryChipColors()
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Pool length picker dialog
    if (showPoolLengthPicker) {
        val poolLengths = listOf(25, 33, 50)
        
        androidx.wear.compose.material.dialog.Dialog(
            showDialog = showPoolLengthPicker,
            onDismissRequest = { showPoolLengthPicker = false }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = "Pool Length",
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0288D1),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                poolLengths.forEach { length ->
                    item {
                        Chip(
                            onClick = { 
                                selectedPoolLength = length
                                showPoolLengthPicker = false
                            },
                            label = {
                                Text(
                                    text = "${length}m",
                                    style = MaterialTheme.typography.body1,
                                    fontWeight = if (length == selectedPoolLength) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = if (length == selectedPoolLength) {
                                ChipDefaults.primaryChipColors(backgroundColor = Color(0xFF0288D1))
                            } else {
                                ChipDefaults.secondaryChipColors()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CyclingHomeScreen(
    onStartCycling: (String) -> Unit,
    onStartCyclingWorkout: (CycleWorkoutTypeWatch, String) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    var selectedCyclingType by remember { mutableStateOf<String?>(null) }
    var showWorkouts by remember { mutableStateOf(false) }
    
    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        onDismissed = { 
            if (showWorkouts) {
                showWorkouts = false
            } else {
                onBack() 
            }
        }
    ) { isBackground ->
        if (!isBackground) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                autoCentering = AutoCenteringParams(itemIndex = 0),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 26.dp),
                flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState)
            ) {
                if (!showWorkouts) {
                    item {
                        Text(
                            text = "🚴 Cycling",
                            style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    // Quick start options
                    item {
                        Text(
                            text = "Quick Start",
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    item {
                        Chip(
                            onClick = { onStartCycling("OUTDOOR") },
                            label = {
                                Column {
                                    Text(
                                        text = "Outdoor Ride",
                                        style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Free ride • GPS tracking",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ChipDefaults.primaryChipColors(
                                backgroundColor = Color(0xFFFF5722)
                            )
                        )
                    }
                    
                    item {
                        Chip(
                            onClick = { onStartCycling("INDOOR") },
                            label = {
                                Column {
                                    Text(
                                        text = "Indoor Cycling",
                                        style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Free ride • Stationary",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ChipDefaults.secondaryChipColors()
                        )
                    }
                    
                    // Structured workouts
                    item {
                        Text(
                            text = "Workouts with HR Zones",
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    item {
                        Chip(
                            onClick = { 
                                selectedCyclingType = "OUTDOOR"
                                showWorkouts = true 
                            },
                            label = {
                                Column {
                                    Text(
                                        text = "Outdoor Workouts",
                                        style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Structured with HR guidance",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ChipDefaults.chipColors(
                                backgroundColor = Color(0xFFFF5722).copy(alpha = 0.3f)
                            )
                        )
                    }
                    
                    item {
                        Chip(
                            onClick = { 
                                selectedCyclingType = "INDOOR"
                                showWorkouts = true 
                            },
                            label = {
                                Column {
                                    Text(
                                        text = "Indoor Workouts",
                                        style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Structured with HR guidance",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ChipDefaults.chipColors(
                                backgroundColor = Color(0xFFFF5722).copy(alpha = 0.3f)
                            )
                        )
                    }
                    
                    item {
                        Chip(
                            onClick = { 
                                selectedCyclingType = "SMART_TRAINER"
                                showWorkouts = true 
                            },
                            label = {
                                Column {
                                    Text(
                                        text = "Smart Trainer Workouts",
                                        style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Power + HR guidance",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ChipDefaults.chipColors(
                                backgroundColor = Color(0xFFFF5722).copy(alpha = 0.3f)
                            )
                        )
                    }
                } else {
                    // Workout selection
                    item {
                        Text(
                            text = "Select Workout",
                            style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    CycleWorkoutTypeWatch.values().forEach { workout ->
                        item {
                            Chip(
                                onClick = { 
                                    onStartCyclingWorkout(workout, selectedCyclingType ?: "OUTDOOR")
                                },
                                label = {
                                    Column {
                                        Text(
                                            text = workout.displayName,
                                            style = MaterialTheme.typography.body1,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = workout.description,
                                            style = MaterialTheme.typography.caption2,
                                            color = MaterialTheme.colors.onSurfaceVariant
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                colors = ChipDefaults.secondaryChipColors()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onStartRun: () -> Unit,
    onSelectWorkout: () -> Unit,
    hasPendingWorkout: Boolean,
    onViewPendingWorkout: () -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    // Load gamification data off composition path — SharedPreferences + Gson.fromJson
    // can block the main thread for 20-50ms on Wear OS hardware
    var currentStreak by remember { mutableStateOf(0) }
    var moveProgress by remember { mutableStateOf(0f) }
    var exerciseProgress by remember { mutableStateOf(0f) }
    var hasGamificationData by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val prefs = context.getSharedPreferences("gamification", Context.MODE_PRIVATE)
            val json = prefs.getString("gamification_data", null)
            if (json != null) {
                try {
                    val data = com.google.gson.Gson().fromJson(json, Map::class.java) as? Map<String, Any>
                    data?.let {
                        currentStreak = (it["currentStreak"] as? Double)?.toInt() ?: 0
                        moveProgress = (it["moveProgress"] as? Double)?.toFloat() ?: 0f
                        exerciseProgress = (it["exerciseProgress"] as? Double)?.toFloat() ?: 0f
                        hasGamificationData = true
                    }
                } catch (_: Exception) { }
            }
        }
    }
    
    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                // Mini activity rings at top
                if (hasGamificationData) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniActivityRing(progress = moveProgress, color = Color(0xFFFF2D55), size = 24)
                        MiniActivityRing(progress = exerciseProgress, color = Color(0xFF30D158), size = 24)
                        if (currentStreak > 0) {
                            Text(
                                text = "🔥$currentStreak",
                                fontSize = 12.sp,
                                color = Color(0xFFFF9500)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    text = "RunTracker",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show pending workout button if available
                if (hasPendingWorkout) {
                    Button(
                        onClick = onViewPendingWorkout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text(
                            text = "📱 View Workout",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                
                // Quick start button
                Button(
                    onClick = onStartRun,
                    modifier = Modifier.size(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
                ) {
                    Text(
                        text = "▶",
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
                
                Text(
                    text = "Quick Start",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Select workout type button
                Button(
                    onClick = onSelectWorkout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text(
                        text = "Choose Workout",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MiniActivityRing(
    progress: Float,
    color: Color,
    size: Int
) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val strokeWidth = 3.dp.toPx()
        val radius = (this.size.minDimension - strokeWidth) / 2
        val center = Offset(this.size.width / 2, this.size.height / 2)
        
        // Background ring
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Progress ring
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceAtMost(1f),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun TrackingScreen(
    trackingState: WearTrackingState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    var showStopConfirmation by remember { mutableStateOf(false) }
    
    // Cache activity-specific values to avoid recomputation
    val activityColor = remember(trackingState.activityType) {
        when (trackingState.activityType) {
            "SWIMMING" -> Color(0xFF0288D1)
            "CYCLING" -> Color(0xFFFF5722)
            else -> Color(0xFFBB86FC) // Primary color
        }
    }
    
    val activityIcon = remember(trackingState.activityType) {
        when (trackingState.activityType) {
            "SWIMMING" -> "🏊"
            "CYCLING" -> "🚴"
            else -> "🏃"
        }
    }

    Scaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Activity indicator and Duration
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (trackingState.activityType != "RUNNING") {
                        Text(
                            text = activityIcon,
                            fontSize = 20.sp
                        )
                    }
                    Text(
                        text = trackingState.durationFormatted,
                        style = MaterialTheme.typography.display1,
                        fontWeight = FontWeight.Bold,
                        color = activityColor,
                        textAlign = TextAlign.Center
                    )
                }

                // Distance
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.2f", trackingState.distanceKm),
                        style = MaterialTheme.typography.display3,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = "km",
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
                
                // HR Zone guidance for structured workouts (swim/bike with HR targets)
                if (trackingState.hasHrTarget && !trackingState.isIntervalWorkout) {
                    val hrZoneColor = when (trackingState.hrAlert) {
                        com.runtracker.wear.service.HrAlert.TOO_LOW -> Color(0xFF64B5F6)  // Blue
                        com.runtracker.wear.service.HrAlert.IN_ZONE -> Color(0xFF81C784)  // Green
                        com.runtracker.wear.service.HrAlert.TOO_HIGH -> Color(0xFFE57373) // Red
                    }
                    
                    val hrStatusText = when (trackingState.hrAlert) {
                        com.runtracker.wear.service.HrAlert.TOO_LOW -> "SPEED UP"
                        com.runtracker.wear.service.HrAlert.IN_ZONE -> "IN ZONE"
                        com.runtracker.wear.service.HrAlert.TOO_HIGH -> "SLOW DOWN"
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // HR Zone indicator
                        trackingState.targetHrZone?.let { zone ->
                            Text(
                                text = "Zone $zone",
                                style = MaterialTheme.typography.caption1,
                                fontWeight = FontWeight.Bold,
                                color = activityColor
                            )
                        }
                        
                        // Current HR with status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "❤️ ${trackingState.heartRate ?: "--"}",
                                style = MaterialTheme.typography.title3,
                                fontWeight = FontWeight.Bold,
                                color = hrZoneColor
                            )
                            Text(
                                text = hrStatusText,
                                style = MaterialTheme.typography.caption2,
                                fontWeight = FontWeight.Bold,
                                color = hrZoneColor
                            )
                        }
                        
                        // Target range
                        trackingState.targetHrMin?.let { min ->
                            trackingState.targetHrMax?.let { max ->
                                Text(
                                    text = "Target: $min-$max bpm",
                                    style = MaterialTheme.typography.caption2,
                                    color = MaterialTheme.colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Interval display (if interval workout)
                if (trackingState.isIntervalWorkout) {
                    val intervalColor = when (trackingState.currentIntervalType) {
                        "WARMUP" -> Color(0xFF64B5F6)  // Blue
                        "WORK" -> Color(0xFFE57373)    // Red
                        "RECOVERY" -> Color(0xFF81C784) // Green
                        "COOLDOWN" -> Color(0xFF64B5F6) // Blue
                        else -> MaterialTheme.colors.primary
                    }
                    
                    Text(
                        text = trackingState.currentIntervalType,
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = intervalColor,
                        textAlign = TextAlign.Center
                    )
                    
                    // Interval countdown
                    val remainingSecs = trackingState.intervalTimeRemaining / 1000
                    val mins = remainingSecs / 60
                    val secs = remainingSecs % 60
                    Text(
                        text = "${mins}:${String.format("%02d", secs)}",
                        style = MaterialTheme.typography.caption1,
                        color = intervalColor,
                        textAlign = TextAlign.Center
                    )
                    
                    // Repetition counter if applicable
                    if (trackingState.totalRepetitions > 1) {
                        Text(
                            text = "Rep ${trackingState.currentRepetition}/${trackingState.totalRepetitions}",
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                    }
                } else if (!trackingState.hasHrTarget) {
                    // Regular run without structured HR targets - show pace targets
                    trackingState.targetPaceFormatted?.let { paceTarget ->
                        Text(
                            text = "Target: $paceTarget /km",
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    trackingState.targetHrFormatted?.let { hrTarget ->
                        Text(
                            text = "HR: $hrTarget",
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Stats row with zone indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItemWithZone(
                        value = trackingState.paceFormatted,
                        label = "/km",
                        alert = trackingState.paceAlert
                    )
                    StatItemWithZone(
                        value = trackingState.heartRate?.toString() ?: "--",
                        label = "bpm",
                        alert = when (trackingState.hrAlert) {
                            com.runtracker.wear.service.HrAlert.TOO_LOW -> ZoneAlert.TOO_LOW
                            com.runtracker.wear.service.HrAlert.IN_ZONE -> ZoneAlert.IN_ZONE
                            com.runtracker.wear.service.HrAlert.TOO_HIGH -> ZoneAlert.TOO_HIGH
                        }
                    )
                }

                // Controls - sized for round watch faces
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    // Stop button
                    Button(
                        onClick = { showStopConfirmation = true },
                        modifier = Modifier.size(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFE57373)
                        )
                    ) {
                        Text("■", fontSize = 18.sp, color = Color.White)
                    }

                    // Pause/Resume button
                    Button(
                        onClick = if (trackingState.isPaused) onResume else onPause,
                        modifier = Modifier.size(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (trackingState.isPaused) 
                                MaterialTheme.colors.primary 
                            else 
                                MaterialTheme.colors.secondary
                        )
                    ) {
                        Text(
                            text = if (trackingState.isPaused) "▶" else "❚❚",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showStopConfirmation) {
        StopConfirmationDialog(
            onConfirm = {
                showStopConfirmation = false
                onStop()
            },
            onDismiss = { showStopConfirmation = false },
            activityType = trackingState.activityType
        )
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.title3,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant
        )
    }
}

enum class ZoneAlert { TOO_LOW, IN_ZONE, TOO_HIGH }

@Composable
fun StatItemWithZone(
    value: String, 
    label: String, 
    alert: Any
) {
    val zoneAlert = when (alert) {
        is com.runtracker.wear.service.PaceAlert -> when (alert) {
            com.runtracker.wear.service.PaceAlert.TOO_SLOW -> ZoneAlert.TOO_LOW
            com.runtracker.wear.service.PaceAlert.IN_ZONE -> ZoneAlert.IN_ZONE
            com.runtracker.wear.service.PaceAlert.TOO_FAST -> ZoneAlert.TOO_HIGH
        }
        is ZoneAlert -> alert
        else -> ZoneAlert.IN_ZONE
    }
    
    val color = when (zoneAlert) {
        ZoneAlert.TOO_LOW -> Color(0xFF64B5F6)  // Blue - too slow/low
        ZoneAlert.IN_ZONE -> Color(0xFF81C784)  // Green - in zone
        ZoneAlert.TOO_HIGH -> Color(0xFFE57373) // Red - too fast/high
    }
    
    val statusText = when (zoneAlert) {
        ZoneAlert.TOO_LOW -> "↓"
        ZoneAlert.IN_ZONE -> "✓"
        ZoneAlert.TOO_HIGH -> "↑"
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.title3,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.caption1,
                color = color,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
fun TrackingPagerScreen(
    trackingState: WearTrackingState,
    isAmbient: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    var previousPage by remember { mutableStateOf(0) }
    var showStopConfirmation by remember { mutableStateOf(false) }
    val pageCount = 6 // Main, HR Zone, Pace Zone, Compete Mode, Music, Safety
    
    // Cache formatted values to reduce string allocations during recomposition
    val formattedDistance = remember(trackingState.distanceMeters) {
        String.format("%.2f", trackingState.distanceMeters / 1000.0)
    }
    val formattedPace = remember(trackingState.currentPaceSecondsPerKm) {
        val paceMin = (trackingState.currentPaceSecondsPerKm / 60).toInt()
        val paceSec = (trackingState.currentPaceSecondsPerKm % 60).toInt()
        String.format("%d:%02d", paceMin, paceSec)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragEnd = {
                        previousPage = currentPage
                        // Lower threshold for easier swiping
                        if (totalDrag < -30 && currentPage < pageCount - 1) {
                            currentPage++
                        } else if (totalDrag > 30 && currentPage > 0) {
                            currentPage--
                        }
                        totalDrag = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    }
                )
            }
    ) {
        // Crossfade is much lighter than AnimatedContent on Wear OS:
        // - AnimatedContent keeps both old and new pages in memory during transition
        // - Crossfade only fades opacity — single composable at a time after transition
        Crossfade(
            targetState = currentPage,
            animationSpec = tween(durationMillis = 200),
            label = "page_transition"
        ) { page ->
            when (page) {
                0 -> MainTrackingPage(
                    trackingState = trackingState,
                    isAmbient = isAmbient,
                    onPause = onPause,
                    onResume = onResume,
                    onShowStopConfirmation = { showStopConfirmation = true },
                    onOpenSafetyMenu = { currentPage = 5 }
                )
                1 -> HeartRateZonePage(
                    trackingState = trackingState,
                    isAmbient = isAmbient
                )
                2 -> PaceZonePage(
                    trackingState = trackingState,
                    isAmbient = isAmbient
                )
                3 -> CompeteModePage(
                    trackingState = trackingState,
                    isAmbient = isAmbient
                )
                4 -> MusicControlPage(isAmbient = isAmbient)
                5 -> SafetyPage(isAmbient = isAmbient)
            }
        }
        
        // Page indicator dots on the right side
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = if (currentPage == index) 
                                MaterialTheme.colors.primary 
                            else 
                                MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.3f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
    }
    
    if (showStopConfirmation) {
        StopConfirmationDialog(
            onConfirm = {
                showStopConfirmation = false
                onStop()
            },
            onDismiss = { showStopConfirmation = false },
            activityType = trackingState.activityType
        )
    }
}

@Composable
fun MainTrackingPage(
    trackingState: WearTrackingState,
    isAmbient: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onShowStopConfirmation: () -> Unit,
    onOpenSafetyMenu: () -> Unit = {}
) {
    // Battery optimization: Simplified ambient mode with minimal rendering
    if (isAmbient) {
        // Ambient mode - minimal UI to save battery (OLED pixels off = power saved)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Duration - white outline text for burn-in protection
                Text(
                    text = trackingState.durationFormatted,
                    style = MaterialTheme.typography.display2,
                    fontWeight = FontWeight.Normal, // Thinner for burn-in protection
                    color = Color.White
                )
                // Distance
                Text(
                    text = "${String.format("%.2f", trackingState.distanceKm)} km",
                    style = MaterialTheme.typography.title2,
                    color = Color.Gray
                )
                // Pace only (simplified)
                Text(
                    text = "${trackingState.paceFormatted} /km",
                    style = MaterialTheme.typography.body1,
                    color = Color.DarkGray
                )
            }
        }
        return
    }
    
    // Active mode - full UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Duration
            Text(
                text = trackingState.durationFormatted,
                style = MaterialTheme.typography.display1,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center
            )

            // Distance
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2f", trackingState.distanceKm),
                    style = MaterialTheme.typography.display3,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    text = "km",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }

            // Interval display (if interval workout)
            if (trackingState.isIntervalWorkout) {
                val intervalColor = when (trackingState.currentIntervalType) {
                    "WARMUP" -> Color(0xFF64B5F6)
                    "WORK" -> Color(0xFFE57373)
                    "RECOVERY" -> Color(0xFF81C784)
                    "COOLDOWN" -> Color(0xFF64B5F6)
                    else -> MaterialTheme.colors.primary
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = trackingState.currentIntervalType,
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = intervalColor
                    )
                    
                    // Progress percentage
                    val progress = trackingState.currentPhaseProgress
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.caption1,
                        color = intervalColor
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItemWithZone(
                    value = trackingState.paceFormatted,
                    label = "/km",
                    alert = trackingState.paceAlert
                )
                StatItemWithZone(
                    value = trackingState.heartRate?.toString() ?: "--",
                    label = "bpm",
                    alert = when (trackingState.hrAlert) {
                        com.runtracker.wear.service.HrAlert.TOO_LOW -> ZoneAlert.TOO_LOW
                        com.runtracker.wear.service.HrAlert.IN_ZONE -> ZoneAlert.IN_ZONE
                        com.runtracker.wear.service.HrAlert.TOO_HIGH -> ZoneAlert.TOO_HIGH
                    }
                )
            }

            // Controls - sized for round watch faces
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // Safety button (SOS)
                Button(
                    onClick = onOpenSafetyMenu,
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE53935))
                ) {
                    Text("🛡", fontSize = 14.sp)
                }
                
                Button(
                    onClick = onShowStopConfirmation,
                    modifier = Modifier.size(42.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE57373))
                ) {
                    Text("■", fontSize = 18.sp, color = Color.White)
                }

                Button(
                    onClick = if (trackingState.isPaused) onResume else onPause,
                    modifier = Modifier.size(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (trackingState.isPaused) 
                            MaterialTheme.colors.primary 
                        else 
                            MaterialTheme.colors.secondary
                    )
                ) {
                    Text(
                        text = if (trackingState.isPaused) "▶" else "❚❚",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun HeartRateZonePage(
    trackingState: WearTrackingState,
    isAmbient: Boolean
) {
    val hrMin = trackingState.targetHrMin ?: 100
    val hrMax = trackingState.targetHrMax ?: 180
    val currentHr = trackingState.heartRate ?: 0

    // Cache derived values — only recompute when inputs change
    val rangeSize = remember(hrMin, hrMax) { (hrMax - hrMin).coerceAtLeast(1) }
    val position = remember(currentHr, hrMin, rangeSize) {
        ((currentHr - hrMin).toFloat() / rangeSize).coerceIn(-0.3f, 1.3f)
    }

    val zoneColor = remember(currentHr, hrMin, hrMax) {
        when {
            currentHr < hrMin -> Color(0xFF64B5F6)
            currentHr > hrMax -> Color(0xFFE57373)
            else -> Color(0xFF81C784)
        }
    }

    val statusText = remember(currentHr, hrMin, hrMax) {
        when {
            currentHr < hrMin -> "TOO LOW"
            currentHr > hrMax -> "TOO HIGH"
            else -> "IN ZONE"
        }
    }

    // Cache formatted strings to avoid allocation during recomposition
    val formattedDistance = remember(trackingState.distanceKm) {
        String.format("%.2f", trackingState.distanceKm)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAmbient) Color.Black else MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Title
            Text(
                text = "❤️ HEART RATE",
                style = MaterialTheme.typography.caption1,
                fontWeight = FontWeight.Bold,
                color = if (isAmbient) Color.White else MaterialTheme.colors.primary
            )
            
            // Zone visualization arc
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Draw the arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - arcSize) / 2, (size.height - arcSize) / 2)
                    
                    // Background arc (gray)
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.3f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Target zone arc (green area)
                    drawArc(
                        color = Color(0xFF81C784).copy(alpha = 0.5f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Current position indicator
                    val indicatorAngle = 135f + (position.coerceIn(0f, 1f) * 270f)
                    val indicatorRadius = arcSize / 2
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val indicatorX = centerX + indicatorRadius * kotlin.math.cos(Math.toRadians(indicatorAngle.toDouble())).toFloat()
                    val indicatorY = centerY + indicatorRadius * kotlin.math.sin(Math.toRadians(indicatorAngle.toDouble())).toFloat()
                    
                    drawCircle(
                        color = zoneColor,
                        radius = 10.dp.toPx(),
                        center = Offset(indicatorX, indicatorY)
                    )
                }
                
                // Current HR in center
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentHr.toString(),
                        style = MaterialTheme.typography.display2,
                        fontWeight = FontWeight.Bold,
                        color = if (isAmbient) Color.White else zoneColor
                    )
                    Text(
                        text = "bpm",
                        style = MaterialTheme.typography.caption2,
                        color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }
            
            // Status and target range
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.caption1,
                    fontWeight = FontWeight.Bold,
                    color = if (isAmbient) Color.White else zoneColor
                )
                Text(
                    text = "Target: $hrMin - $hrMax bpm",
                    style = MaterialTheme.typography.caption2,
                    color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                )
            }
            
            // Distance and phase progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formattedDistance,
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = if (isAmbient) Color.White else MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = "km",
                        style = MaterialTheme.typography.caption2,
                        color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                    )
                }

                if (trackingState.isIntervalWorkout) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(trackingState.currentPhaseProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold,
                            color = if (isAmbient) Color.White else MaterialTheme.colors.primary
                        )
                        Text(
                            text = trackingState.currentIntervalType.lowercase(),
                            style = MaterialTheme.typography.caption2,
                            color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaceZonePage(
    trackingState: WearTrackingState,
    isAmbient: Boolean
) {
    val paceMin = trackingState.targetPaceMin ?: 300.0 // 5:00/km
    val paceMax = trackingState.targetPaceMax ?: 420.0 // 7:00/km
    val currentPace = trackingState.currentPaceSecondsPerKm

    // Cache derived values — only recompute when inputs change
    val rangeSize = remember(paceMin, paceMax) { (paceMax - paceMin).coerceAtLeast(1.0) }
    val position = remember(currentPace, paceMax, rangeSize) {
        ((paceMax - currentPace) / rangeSize).toFloat().coerceIn(-0.3f, 1.3f)
    }

    val zoneColor = remember(currentPace, paceMin, paceMax) {
        when {
            currentPace <= 0 || currentPace.isInfinite() || currentPace.isNaN() -> Color.Gray
            currentPace < paceMin -> Color(0xFFE57373)
            currentPace > paceMax -> Color(0xFF64B5F6)
            else -> Color(0xFF81C784)
        }
    }

    val statusText = remember(currentPace, paceMin, paceMax) {
        when {
            currentPace <= 0 || currentPace.isInfinite() || currentPace.isNaN() -> "WAITING"
            currentPace < paceMin -> "TOO FAST"
            currentPace > paceMax -> "TOO SLOW"
            else -> "IN ZONE"
        }
    }

    val formattedDistance = remember(trackingState.distanceKm) {
        String.format("%.2f", trackingState.distanceKm)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAmbient) Color.Black else MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Title
            Text(
                text = "⚡ PACE",
                style = MaterialTheme.typography.caption1,
                fontWeight = FontWeight.Bold,
                color = if (isAmbient) Color.White else MaterialTheme.colors.primary
            )
            
            // Zone visualization arc
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - arcSize) / 2, (size.height - arcSize) / 2)
                    
                    // Background arc
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.3f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Target zone arc
                    drawArc(
                        color = Color(0xFF81C784).copy(alpha = 0.5f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Current position indicator
                    if (currentPace > 0 && !currentPace.isInfinite() && !currentPace.isNaN()) {
                        val indicatorAngle = 135f + (position.coerceIn(0f, 1f) * 270f)
                        val indicatorRadius = arcSize / 2
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val indicatorX = centerX + indicatorRadius * kotlin.math.cos(Math.toRadians(indicatorAngle.toDouble())).toFloat()
                        val indicatorY = centerY + indicatorRadius * kotlin.math.sin(Math.toRadians(indicatorAngle.toDouble())).toFloat()
                        
                        drawCircle(
                            color = zoneColor,
                            radius = 10.dp.toPx(),
                            center = Offset(indicatorX, indicatorY)
                        )
                    }
                }
                
                // Current pace in center
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = trackingState.paceFormatted,
                        style = MaterialTheme.typography.display2,
                        fontWeight = FontWeight.Bold,
                        color = if (isAmbient) Color.White else zoneColor
                    )
                    Text(
                        text = "/km",
                        style = MaterialTheme.typography.caption2,
                        color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }
            
            // Status and target range
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.caption1,
                    fontWeight = FontWeight.Bold,
                    color = if (isAmbient) Color.White else zoneColor
                )
                Text(
                    text = "Target: ${formatPace(paceMin)} - ${formatPace(paceMax)}",
                    style = MaterialTheme.typography.caption2,
                    color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                )
            }
            
            // Distance and phase progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formattedDistance,
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = if (isAmbient) Color.White else MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = "km",
                        style = MaterialTheme.typography.caption2,
                        color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                    )
                }
                
                if (trackingState.isIntervalWorkout) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(trackingState.currentPhaseProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold,
                            color = if (isAmbient) Color.White else MaterialTheme.colors.primary
                        )
                        Text(
                            text = trackingState.currentIntervalType.lowercase(),
                            style = MaterialTheme.typography.caption2,
                            color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatPace(paceSeconds: Double): String {
    if (paceSeconds <= 0 || paceSeconds.isInfinite() || paceSeconds.isNaN()) return "--:--"
    val minutes = (paceSeconds / 60).toInt()
    val seconds = (paceSeconds % 60).toInt()
    return "$minutes:${String.format("%02d", seconds)}"
}

@Composable
fun CompeteModePage(
    trackingState: WearTrackingState,
    isAmbient: Boolean
) {
    val pbTimeMillis = trackingState.competePbTimeMillis
    val targetDistance = trackingState.competeTargetDistance
    val distanceName = when (targetDistance) {
        1000 -> "1K"
        5000 -> "5K"
        10000 -> "10K"
        21097 -> "Half"
        42195 -> "Marathon"
        else -> "${targetDistance / 1000}K"
    }
    
    // Calculate positions based on time and pace
    val currentProgress = (trackingState.distanceMeters / targetDistance).coerceIn(0.0, 1.0).toFloat()
    val pbProgress = if (pbTimeMillis != null && pbTimeMillis > 0) {
        val pbPaceSecondsPerKm = pbTimeMillis / 1000.0 / (targetDistance / 1000.0)
        val pbDistanceAtCurrentTime = (trackingState.durationMillis / 1000.0) / pbPaceSecondsPerKm * 1000
        (pbDistanceAtCurrentTime / targetDistance).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }
    
    val isAhead = currentProgress > pbProgress
    val timeDiff = if (pbTimeMillis != null && trackingState.distanceMeters > 0) {
        val expectedTimeAtDistance = (trackingState.distanceMeters / targetDistance) * pbTimeMillis
        trackingState.durationMillis - expectedTimeAtDistance.toLong()
    } else 0L
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAmbient) Color.Black else MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title
            Text(
                text = "🏁 COMPETE",
                style = MaterialTheme.typography.caption1,
                fontWeight = FontWeight.Bold,
                color = if (isAmbient) Color.Gray else MaterialTheme.colors.primary
            )
            
            // Target distance
            Text(
                text = "vs $distanceName PB",
                style = MaterialTheme.typography.caption2,
                color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
            )
            
            // Animated track with runners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val trackWidth = size.width - 40f
                    val trackY = size.height / 2
                    val trackStartX = 20f
                    
                    // Draw track background
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(trackStartX, trackY),
                        end = Offset(trackStartX + trackWidth, trackY),
                        strokeWidth = 12f,
                        cap = StrokeCap.Round
                    )
                    
                    // Draw finish line
                    drawLine(
                        color = Color.White,
                        start = Offset(trackStartX + trackWidth, trackY - 20),
                        end = Offset(trackStartX + trackWidth, trackY + 20),
                        strokeWidth = 3f
                    )
                    
                    // Draw PB runner (ghost - blue)
                    if (pbTimeMillis != null) {
                        val pbX = trackStartX + (trackWidth * pbProgress)
                        drawCircle(
                            color = Color(0xFF64B5F6).copy(alpha = 0.7f),
                            radius = 14f,
                            center = Offset(pbX, trackY - 12)
                        )
                    }
                    
                    // Draw current runner (you - green or red based on position)
                    val currentX = trackStartX + (trackWidth * currentProgress)
                    val runnerColor = if (pbTimeMillis == null) {
                        Color(0xFF4CAF50)
                    } else if (isAhead) {
                        Color(0xFF4CAF50) // Green - ahead
                    } else {
                        Color(0xFFE57373) // Red - behind
                    }
                    drawCircle(
                        color = runnerColor,
                        radius = 16f,
                        center = Offset(currentX, trackY + 12)
                    )
                }
                
                // Runner labels
                if (pbTimeMillis != null) {
                    Text(
                        text = "PB",
                        style = MaterialTheme.typography.caption2,
                        color = Color(0xFF64B5F6),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 4.dp)
                    )
                }
                Text(
                    text = "YOU",
                    style = MaterialTheme.typography.caption2,
                    color = if (isAhead) Color(0xFF4CAF50) else Color(0xFFE57373),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 4.dp)
                )
            }
            
            // Time difference
            if (pbTimeMillis != null && trackingState.distanceMeters > 100) {
                val absDiff = kotlin.math.abs(timeDiff)
                val diffSeconds = absDiff / 1000
                val diffMins = diffSeconds / 60
                val diffSecs = diffSeconds % 60
                val sign = if (timeDiff < 0) "-" else "+"
                val diffText = if (diffMins > 0) {
                    "$sign${diffMins}:${String.format("%02d", diffSecs)}"
                } else {
                    "$sign${diffSecs}s"
                }
                
                Text(
                    text = diffText,
                    style = MaterialTheme.typography.display3,
                    fontWeight = FontWeight.Bold,
                    color = if (isAhead) Color(0xFF4CAF50) else Color(0xFFE57373)
                )
                
                Text(
                    text = if (isAhead) "AHEAD" else "BEHIND",
                    style = MaterialTheme.typography.caption1,
                    fontWeight = FontWeight.Bold,
                    color = if (isAhead) Color(0xFF4CAF50) else Color(0xFFE57373)
                )
            } else if (pbTimeMillis == null) {
                Text(
                    text = "No PB yet",
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
                Text(
                    text = "Complete a $distanceName to set one!",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Starting...",
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
            
            // Current progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.2f", trackingState.distanceKm),
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = if (isAmbient) Color.White else MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = "km",
                        style = MaterialTheme.typography.caption2,
                        color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(currentProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = if (isAmbient) Color.White else MaterialTheme.colors.primary
                    )
                    Text(
                        text = "of $distanceName",
                        style = MaterialTheme.typography.caption2,
                        color = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class WorkoutTypeInfo(
    val emoji: String,
    val name: String,
    val type: String,
    val description: String,
    val category: String
)

@Composable
fun WorkoutSelectScreen(
    onWorkoutSelected: (String) -> Unit,
    onCustomWorkouts: () -> Unit,
    onBack: () -> Unit
) {
    // Organized by category
    val workoutCategories = mapOf(
        "Basic Runs" to listOf(
            WorkoutTypeInfo("🏃", "Easy Run", "EASY_RUN", "Recovery pace", "Basic"),
            WorkoutTypeInfo("🏃‍♂️", "Long Run", "LONG_RUN", "Build endurance", "Basic"),
            WorkoutTypeInfo("💚", "Recovery Run", "RECOVERY_RUN", "Very easy effort", "Basic")
        ),
        "Speed Work" to listOf(
            WorkoutTypeInfo("⚡", "Tempo Run", "TEMPO_RUN", "Threshold pace", "Speed"),
            WorkoutTypeInfo("🔥", "Intervals", "INTERVAL_TRAINING", "Speed work", "Speed"),
            WorkoutTypeInfo("🎲", "Fartlek", "FARTLEK", "Variable pace", "Speed"),
            WorkoutTypeInfo("🎯", "Race Pace", "RACE_PACE", "Goal race speed", "Speed"),
            WorkoutTypeInfo("💨", "VO2 Max", "VO2_MAX_INTERVALS", "Short hard efforts", "Speed"),
            WorkoutTypeInfo("🏁", "Mile Repeats", "MILE_REPEATS", "Classic speed", "Speed"),
            WorkoutTypeInfo("📊", "Ladder", "LADDER_WORKOUT", "Increasing intervals", "Speed"),
            WorkoutTypeInfo("🔺", "Pyramid", "PYRAMID_WORKOUT", "Up then down", "Speed"),
            WorkoutTypeInfo("8️⃣", "Yasso 800s", "YASSO_800S", "Marathon predictor", "Speed")
        ),
        "Hills & Strength" to listOf(
            WorkoutTypeInfo("⛰️", "Hill Repeats", "HILL_REPEATS", "Strength & power", "Hills"),
            WorkoutTypeInfo("🚀", "Hill Sprints", "HILL_SPRINTS", "Short explosive", "Hills"),
            WorkoutTypeInfo("🪜", "Stair Workout", "STAIR_WORKOUT", "Stair climbing", "Hills")
        ),
        "Specialized" to listOf(
            WorkoutTypeInfo("📈", "Progression", "PROGRESSION_RUN", "Start easy, finish fast", "Special"),
            WorkoutTypeInfo("⚖️", "Negative Split", "NEGATIVE_SPLIT", "2nd half faster", "Special"),
            WorkoutTypeInfo("🧱", "Threshold", "THRESHOLD_RUN", "Lactate threshold", "Special"),
            WorkoutTypeInfo("🏎️", "Time Trial", "TIME_TRIAL", "All-out effort", "Special"),
            WorkoutTypeInfo("🏃‍♀️", "Parkrun Prep", "PARKRUN_PREP", "5K race prep", "Special"),
            WorkoutTypeInfo("🎭", "Race Simulation", "RACE_SIMULATION", "Practice race day", "Special")
        ),
        "Recovery" to listOf(
            WorkoutTypeInfo("🌱", "Shake Out", "SHAKE_OUT_RUN", "Pre/post race", "Recovery"),
            WorkoutTypeInfo("🏗️", "Base Building", "BASE_BUILDING", "Aerobic foundation", "Recovery"),
            WorkoutTypeInfo("🔄", "Active Recovery", "ACTIVE_RECOVERY", "Very light activity", "Recovery")
        )
    )
    
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    val listState = rememberScalingLazyListState()
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    
    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        onDismissed = {
            if (selectedCategory != null) {
                selectedCategory = null
            } else {
                onBack()
            }
        }
    ) { isBackground ->
        if (!isBackground) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                autoCentering = AutoCenteringParams(itemIndex = 0),
                flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState)
            ) {
                if (selectedCategory == null) {
                    // Show categories
                    item {
                        Text(
                            text = "Select Workout",
                            style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    // Custom workouts button
                    item {
                        Chip(
                            onClick = onCustomWorkouts,
                            label = {
                                Column {
                                    Text(
                                        text = "⭐ My Workouts",
                                        style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Custom built workouts",
                                        style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            colors = ChipDefaults.primaryChipColors()
                        )
                    }
                    
                    // Category chips
                    workoutCategories.keys.forEach { category ->
                        item {
                            Chip(
                                onClick = { selectedCategory = category },
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.body1,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${workoutCategories[category]?.size ?: 0}",
                                            style = MaterialTheme.typography.caption1,
                                            color = MaterialTheme.colors.onSurfaceVariant
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                colors = ChipDefaults.secondaryChipColors()
                            )
                        }
                    }
                } else {
                    // Show workouts in selected category
                    item {
                        Text(
                            text = selectedCategory!!,
                            style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    workoutCategories[selectedCategory]?.forEach { workout ->
                        item {
                            Chip(
                                onClick = { onWorkoutSelected(workout.type) },
                                label = {
                                    Column {
                                        Text(
                                            text = "${workout.emoji} ${workout.name}",
                                            style = MaterialTheme.typography.body1,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = workout.description,
                                            style = MaterialTheme.typography.caption2,
                                            color = MaterialTheme.colors.onSurfaceVariant
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                colors = ChipDefaults.secondaryChipColors()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutTypePreviewScreen(
    workoutType: com.runtracker.shared.data.model.WorkoutType,
    onStartRun: () -> Unit,
    onBack: () -> Unit
) {
    val workoutInfo = when (workoutType) {
        // Basic runs
        com.runtracker.shared.data.model.WorkoutType.EASY_RUN -> Triple("🏃 Easy Run", "30-45 min at conversational pace", listOf("5 min warm-up", "20-35 min easy", "5 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.LONG_RUN -> Triple("🏃‍♂️ Long Run", "60-90 min building endurance", listOf("10 min warm-up", "40-70 min steady", "10 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.RECOVERY_RUN -> Triple("💚 Recovery Run", "20-30 min very easy", listOf("5 min walk", "15-20 min easy jog", "5 min walk"))
        
        // Speed work
        com.runtracker.shared.data.model.WorkoutType.TEMPO_RUN -> Triple("⚡ Tempo Run", "Comfortably hard sustained effort", listOf("10 min warm-up", "20-30 min tempo", "10 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.INTERVAL_TRAINING -> Triple("🔥 Intervals", "High intensity speed work", listOf("10 min warm-up", "6x800m @ 5K pace", "400m recovery jogs", "10 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.FARTLEK -> Triple("🎲 Fartlek", "Unstructured speed play", listOf("10 min warm-up", "20 min varied pace", "10 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.RACE_PACE -> Triple("🎯 Race Pace", "Practice goal race speed", listOf("10 min warm-up", "15-20 min @ race pace", "10 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.VO2_MAX_INTERVALS -> Triple("💨 VO2 Max", "Short very hard intervals", listOf("10 min warm-up", "5x3 min hard", "2 min recovery", "10 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.MILE_REPEATS -> Triple("🏁 Mile Repeats", "Classic speed workout", listOf("10 min warm-up", "4x1 mile @ threshold", "3 min rest", "10 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.LADDER_WORKOUT -> Triple("📊 Ladder", "Increasing/decreasing intervals", listOf("Warm-up", "200-400-600-800-600-400-200m", "Recovery between", "Cool-down"))
        com.runtracker.shared.data.model.WorkoutType.PYRAMID_WORKOUT -> Triple("🔺 Pyramid", "Build up then back down", listOf("Warm-up", "1-2-3-4-3-2-1 min hard", "Equal recovery", "Cool-down"))
        com.runtracker.shared.data.model.WorkoutType.YASSO_800S -> Triple("8️⃣ Yasso 800s", "Marathon predictor workout", listOf("10 min warm-up", "10x800m @ goal time", "400m jog recovery", "10 min cool-down"))
        
        // Hills
        com.runtracker.shared.data.model.WorkoutType.HILL_REPEATS -> Triple("⛰️ Hill Repeats", "Build strength and power", listOf("10 min warm-up", "8x60s hill sprints", "Jog down recovery", "10 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.HILL_SPRINTS -> Triple("🚀 Hill Sprints", "Short explosive efforts", listOf("10 min warm-up", "10x20s all-out uphill", "Full recovery", "10 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.STAIR_WORKOUT -> Triple("🪜 Stair Workout", "Stair climbing intervals", listOf("5 min warm-up", "10x stair climb", "Walk down recovery", "5 min cool-down"))
        
        // Specialized
        com.runtracker.shared.data.model.WorkoutType.PROGRESSION_RUN -> Triple("📈 Progression", "Start easy, finish fast", listOf("10 min easy", "10 min moderate", "10 min tempo", "5 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.NEGATIVE_SPLIT -> Triple("⚖️ Negative Split", "Second half faster", listOf("First half easy pace", "Second half faster", "Finish strong"))
        com.runtracker.shared.data.model.WorkoutType.THRESHOLD_RUN -> Triple("🧱 Threshold", "Lactate threshold effort", listOf("10 min warm-up", "20 min @ threshold", "10 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.TIME_TRIAL -> Triple("🏎️ Time Trial", "All-out effort for distance", listOf("10 min warm-up", "Race effort", "10 min cool-down"))
        com.runtracker.shared.data.model.WorkoutType.PARKRUN_PREP -> Triple("🏃‍♀️ Parkrun Prep", "5K race preparation", listOf("10 min warm-up", "Strides", "5K effort", "Cool-down"))
        com.runtracker.shared.data.model.WorkoutType.RACE_SIMULATION -> Triple("🎭 Race Simulation", "Practice race conditions", listOf("Pre-race routine", "Race pace effort", "Practice fueling"))
        
        // Recovery
        com.runtracker.shared.data.model.WorkoutType.SHAKE_OUT_RUN -> Triple("🌱 Shake Out", "Short easy run pre/post race", listOf("10-15 min very easy", "4x20s strides", "5 min easy"))
        com.runtracker.shared.data.model.WorkoutType.BASE_BUILDING -> Triple("🏗️ Base Building", "Aerobic foundation work", listOf("Easy effort throughout", "Build duration gradually", "Stay conversational"))
        com.runtracker.shared.data.model.WorkoutType.ACTIVE_RECOVERY -> Triple("🔄 Active Recovery", "Very light activity", listOf("20-30 min", "Very easy effort", "Walking OK"))
        com.runtracker.shared.data.model.WorkoutType.AEROBIC_MAINTENANCE -> Triple("🔄 Aerobic", "Maintain fitness", listOf("30-40 min easy", "Steady effort", "Comfortable pace"))
        
        else -> Triple("🏃 Run", "General run", listOf("Warm-up", "Main run", "Cool-down"))
    }
    
    val (title, description, phases) = workoutInfo
    val listState = rememberScalingLazyListState()
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    
    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        onDismissed = onBack
    ) { isBackground ->
        if (!isBackground) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                autoCentering = AutoCenteringParams(itemIndex = 0),
                flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState)
            ) {
                item {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.title2,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                item {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                
                item {
                    Text(
                        text = "BREAKDOWN",
                        style = MaterialTheme.typography.caption2,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                items(phases.size) { index ->
                    val phase = phases[index]
                    val phaseColor = when {
                        phase.contains("warm", ignoreCase = true) -> Color(0xFFFF9800)
                        phase.contains("cool", ignoreCase = true) -> Color(0xFF2196F3)
                        phase.contains("recovery", ignoreCase = true) -> Color(0xFF81C784)
                        else -> MaterialTheme.colors.primary
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(phaseColor, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = phase,
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.onSurface
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onStartRun,
                        modifier = Modifier.size(64.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
                    ) {
                        Text("▶", fontSize = 24.sp, color = Color.White)
                    }
                }
                
                item {
                    Text(
                        text = "Start Run",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutPreviewScreen(
    workout: com.runtracker.shared.data.model.ScheduledWorkout,
    onStartRun: () -> Unit,
    onBack: () -> Unit
) {
    val intervals = workout.intervals ?: emptyList()
    val listState = rememberScalingLazyListState()
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    
    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        onDismissed = onBack
    ) { isBackground ->
        if (!isBackground) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                autoCentering = AutoCenteringParams(itemIndex = 0),
                flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState)
            ) {
                item {
                    Text(
                        text = "📱 From Phone",
                        style = MaterialTheme.typography.caption2,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                item {
                    Text(
                        text = formatWorkoutTypeName(workout.workoutType),
                        style = MaterialTheme.typography.title2,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                }
                
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        workout.targetDistanceMeters?.let { distance ->
                            Text(
                                text = "%.1f km".format(distance / 1000),
                                style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        workout.targetDurationMinutes?.let { duration ->
                            Text(
                                text = "$duration min",
                                style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                if (intervals.isNotEmpty()) {
                    item {
                        Text(
                            text = "BREAKDOWN",
                            style = MaterialTheme.typography.caption2,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    items(intervals.size.coerceAtMost(8)) { index ->
                        val interval = intervals[index]
                        val phaseColor = when (interval.type) {
                            com.runtracker.shared.data.model.IntervalType.WARMUP -> Color(0xFFFF9800)
                            com.runtracker.shared.data.model.IntervalType.COOLDOWN -> Color(0xFF2196F3)
                            com.runtracker.shared.data.model.IntervalType.RECOVERY -> Color(0xFF81C784)
                            com.runtracker.shared.data.model.IntervalType.WORK -> MaterialTheme.colors.primary
                        }
                        
                        val phaseName = when (interval.type) {
                            com.runtracker.shared.data.model.IntervalType.WARMUP -> "Warm-up"
                            com.runtracker.shared.data.model.IntervalType.COOLDOWN -> "Cool-down"
                            com.runtracker.shared.data.model.IntervalType.RECOVERY -> "Recovery"
                            com.runtracker.shared.data.model.IntervalType.WORK -> "Work"
                        }
                        
                        val durationText = interval.durationSeconds?.let { "${it / 60}min" }
                            ?: interval.distanceMeters?.let { "${it.toInt()}m" }
                            ?: ""
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(phaseColor, shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$phaseName $durationText",
                                style = MaterialTheme.typography.caption2,
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                    }
                    
                    if (intervals.size > 8) {
                        item {
                            Text(
                                text = "... +${intervals.size - 8} more",
                                style = MaterialTheme.typography.caption2,
                                color = MaterialTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onStartRun,
                        modifier = Modifier.size(64.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
                    ) {
                        Text("▶", fontSize = 24.sp, color = Color.White)
                    }
                }
                
                item {
                    Text(
                        text = "Start Run",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatWorkoutTypeName(workoutType: com.runtracker.shared.data.model.WorkoutType): String {
    return when (workoutType) {
        com.runtracker.shared.data.model.WorkoutType.EASY_RUN -> "🏃 Easy Run"
        com.runtracker.shared.data.model.WorkoutType.LONG_RUN -> "🏃‍♂️ Long Run"
        com.runtracker.shared.data.model.WorkoutType.TEMPO_RUN -> "⚡ Tempo Run"
        com.runtracker.shared.data.model.WorkoutType.INTERVAL_TRAINING -> "🔥 Intervals"
        com.runtracker.shared.data.model.WorkoutType.HILL_REPEATS -> "⛰️ Hill Repeats"
        com.runtracker.shared.data.model.WorkoutType.FARTLEK -> "🎲 Fartlek"
        com.runtracker.shared.data.model.WorkoutType.RECOVERY_RUN -> "💤 Recovery"
        com.runtracker.shared.data.model.WorkoutType.RACE_PACE -> "🏁 Race Pace"
        else -> "🏃 Run"
    }
}

@Composable
fun StopConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    activityType: String = "RUNNING"
) {
    val (title, subtitle) = when (activityType) {
        "SWIMMING" -> "Finish Swim?" to "Save this swim?"
        "CYCLING" -> "Finish Ride?" to "Save this ride?"
        else -> "Finish Run?" to "Save this run?"
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(
                    MaterialTheme.colors.surface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("X", fontSize = 16.sp)
                }
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.primaryButtonColors(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("OK", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CustomWorkoutsScreen(
    onWorkoutSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    // Get synced custom workouts from shared preferences or data layer
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("custom_workouts", Context.MODE_PRIVATE)
    val workoutsJson = sharedPrefs.getString("workouts_list", null)
    
    val customWorkouts = remember(workoutsJson) {
        if (workoutsJson != null) {
            try {
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<SyncedCustomWorkout>>() {}.type
                gson.fromJson<List<SyncedCustomWorkout>>(workoutsJson, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    val listState = rememberScalingLazyListState()
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    
    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        onDismissed = onBack
    ) { isBackground ->
        if (!isBackground) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                autoCentering = AutoCenteringParams(itemIndex = 0),
                flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState)
            ) {
                item {
                    Text(
                        text = "⭐ My Workouts",
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                if (customWorkouts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No custom workouts",
                                style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create workouts in the phone app and they'll sync here",
                                style = MaterialTheme.typography.caption2,
                                color = MaterialTheme.colors.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    customWorkouts.forEach { workout ->
                        item {
                            Chip(
                                onClick = { onWorkoutSelected(workout.id.toString()) },
                                label = {
                                    Column {
                                        Text(
                                            text = workout.name,
                                            style = MaterialTheme.typography.body1,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${workout.phasesCount} phases • ${workout.formattedDuration}",
                                            style = MaterialTheme.typography.caption2,
                                            color = MaterialTheme.colors.onSurfaceVariant
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                colors = ChipDefaults.secondaryChipColors()
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sync from phone app",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

data class SyncedCustomWorkout(
    val id: Long,
    val name: String,
    val description: String,
    val phasesCount: Int,
    val totalDurationSeconds: Int,
    val difficulty: String,
    val phasesJson: String
) {
    val formattedDuration: String
        get() {
            val mins = totalDurationSeconds / 60
            return if (mins >= 60) {
                "${mins / 60}h ${mins % 60}m"
            } else {
                "${mins}m"
            }
        }
}

@Composable
fun MusicControlPage(isAmbient: Boolean) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    var isPlaying by remember { mutableStateOf(audioManager.isMusicActive) }
    
    fun sendMediaKey(keyCode: Int) {
        val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }
    
    if (isAmbient) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎵 MUSIC",
                style = MaterialTheme.typography.title2,
                color = Color.White
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🎵 Music",
                style = MaterialTheme.typography.title3,
                color = Color(0xFF9C27B0)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Main controls
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Previous
                Button(
                    onClick = { sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
                    modifier = Modifier.size(40.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("⏮")
                }
                
                // Play/Pause
                Button(
                    onClick = { 
                        sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        isPlaying = !isPlaying
                    },
                    modifier = Modifier.size(52.dp),
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text(if (isPlaying) "⏸" else "▶", style = MaterialTheme.typography.title2)
                }
                
                // Next
                Button(
                    onClick = { sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT) },
                    modifier = Modifier.size(40.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("⏭")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Volume controls
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Button(
                    onClick = { 
                        audioManager.adjustStreamVolume(
                            android.media.AudioManager.STREAM_MUSIC,
                            android.media.AudioManager.ADJUST_LOWER,
                            0
                        )
                    },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("🔉")
                }
                
                Button(
                    onClick = { 
                        audioManager.adjustStreamVolume(
                            android.media.AudioManager.STREAM_MUSIC,
                            android.media.AudioManager.ADJUST_RAISE,
                            0
                        )
                    },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("🔊")
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Swipe ↕ for more",
                style = MaterialTheme.typography.caption3,
                color = MaterialTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SafetyPage(isAmbient: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSosConfirmation by remember { mutableStateOf(false) }
    var sosSent by remember { mutableStateOf(false) }
    var panicSent by remember { mutableStateOf(false) }
    var fakeCallSent by remember { mutableStateOf(false) }
    
    if (isAmbient) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🛡️ SAFETY",
                style = MaterialTheme.typography.title2,
                color = Color.White
            )
        }
        return
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "🛡️ Safety",
                style = MaterialTheme.typography.title2,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            
            // SOS Button - Big and prominent
            Button(
                onClick = { showSosConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFE53935)
                ),
                enabled = !sosSent
            ) {
                Text(
                    text = if (sosSent) "✓ SOS SENT" else "🆘 SOS",
                    style = MaterialTheme.typography.button,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // Panic Alarm Button
            Button(
                onClick = {
                    scope.launch {
                        com.runtracker.wear.safety.WatchSafetyService.triggerPanicAlarm(context)
                        panicSent = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFFF9800)
                ),
                enabled = !panicSent
            ) {
                Text(
                    text = if (panicSent) "✓ ALARM ON" else "🚨 Alarm",
                    style = MaterialTheme.typography.button,
                    color = Color.White
                )
            }
            
            // Fake Call Button
            Button(
                onClick = {
                    scope.launch {
                        com.runtracker.wear.safety.WatchSafetyService.triggerFakeCall(context)
                        fakeCallSent = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF4CAF50)
                ),
                enabled = !fakeCallSent
            ) {
                Text(
                    text = if (fakeCallSent) "✓ CALLING" else "📞 Fake Call",
                    style = MaterialTheme.typography.button,
                    color = Color.White
                )
            }
        }
    }
    
    // SOS Confirmation Dialog
    if (showSosConfirmation) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSosConfirmation = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colors.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🆘 Send SOS?",
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    )
                    
                    Text(
                        text = "This will send an emergency SMS with your location to your contacts",
                        style = MaterialTheme.typography.caption1,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showSosConfirmation = false },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.surface
                            )
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    com.runtracker.wear.safety.WatchSafetyService.triggerSos(context)
                                    sosSent = true
                                    showSosConfirmation = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFE53935)
                            )
                        ) {
                            Text("SEND SOS", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareButtonSosDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(Color(0xFFE53935), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🆘 SOS ACTIVATED",
                    style = MaterialTheme.typography.title2,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "Send emergency alert to your contacts?",
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.White
                        )
                    ) {
                        Text("SEND SOS", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
