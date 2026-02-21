package com.runtracker.wear.presentation.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    onStartRun: () -> Unit,
    onSelectWorkout: () -> Unit,
    hasPendingWorkout: Boolean,
    onViewPendingWorkout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()

    // Load gamification data off main thread
    var currentStreak by remember { mutableIntStateOf(0) }
    var moveProgress by remember { mutableFloatStateOf(0f) }
    var exerciseProgress by remember { mutableFloatStateOf(0f) }
    var hasGamificationData by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
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
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 26.dp),
                flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState)
            ) {
                item {
                    Text("🏃 Running", style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(vertical = 8.dp))
                }

                // Activity rings and streak
                if (hasGamificationData) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiniActivityRing(progress = moveProgress, color = WearColors.ZoneAbove)
                            MiniActivityRing(progress = exerciseProgress, color = WearColors.ZoneIn)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔥", fontSize = 16.sp)
                                Text("$currentStreak", style = MaterialTheme.typography.title3,
                                    fontWeight = FontWeight.Bold)
                                Text("streak", style = MaterialTheme.typography.caption3,
                                    color = MaterialTheme.colors.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (hasPendingWorkout) {
                    item {
                        Chip(
                            onClick = onViewPendingWorkout,
                            label = {
                                Column {
                                    Text("📱 Synced Workout", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                    Text("Tap to view", style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ChipDefaults.primaryChipColors(backgroundColor = WearColors.Success)
                        )
                    }
                }

                item {
                    Button(
                        onClick = onStartRun,
                        modifier = Modifier.size(64.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = WearColors.Success)
                    ) {
                        Text("▶", fontSize = 24.sp, color = Color.White)
                    }
                }

                item {
                    Text("Quick Start", style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant)
                }

                item {
                    Chip(
                        onClick = onSelectWorkout,
                        label = {
                            Column {
                                Text("📋 Workouts", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                Text("Structured training plans",
                                    style = MaterialTheme.typography.caption2,
                                    color = MaterialTheme.colors.onSurfaceVariant)
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

@Composable
private fun MiniActivityRing(
    progress: Float,
    color: Color,
    size: Int = 36
) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size.dp)) {
        val strokeWidth = 4.dp.toPx()
        val arcSize = this.size.minDimension - strokeWidth
        val topLeft = androidx.compose.ui.geometry.Offset((this.size.width - arcSize) / 2, (this.size.height - arcSize) / 2)

        drawArc(
            color = color.copy(alpha = 0.2f), startAngle = -90f, sweepAngle = 360f,
            useCenter = false, topLeft = topLeft,
            size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        drawArc(
            color = color, startAngle = -90f,
            sweepAngle = (progress.coerceIn(0f, 1f) * 360f),
            useCenter = false, topLeft = topLeft,
            size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

@Composable
fun WorkoutSelectScreen(
    onWorkoutSelected: (String) -> Unit,
    onCustomWorkouts: () -> Unit,
    onBack: () -> Unit
) {
    val workoutCategories = remember {
        mapOf(
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
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val listState = rememberScalingLazyListState()
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        onDismissed = {
            if (selectedCategory != null) selectedCategory = null else onBack()
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
                    item {
                        Text("Select Workout", style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }
                    item {
                        Chip(
                            onClick = onCustomWorkouts,
                            label = {
                                Column {
                                    Text("⭐ My Workouts", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                    Text("Custom built workouts", style = MaterialTheme.typography.caption2,
                                        color = MaterialTheme.colors.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = ChipDefaults.primaryChipColors()
                        )
                    }

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
                                        Text(category, style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                        Text("${workoutCategories[category]?.size ?: 0}",
                                            style = MaterialTheme.typography.caption1,
                                            color = MaterialTheme.colors.onSurfaceVariant)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                colors = ChipDefaults.secondaryChipColors()
                            )
                        }
                    }
                } else {
                    item {
                        Text(selectedCategory!!, style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }

                    workoutCategories[selectedCategory]?.forEach { workout ->
                        item {
                            Chip(
                                onClick = { onWorkoutSelected(workout.type) },
                                label = {
                                    Column {
                                        Text("${workout.emoji} ${workout.name}",
                                            style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                                        Text(workout.description,
                                            style = MaterialTheme.typography.caption2,
                                            color = MaterialTheme.colors.onSurfaceVariant)
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
fun WorkoutTypePreviewScreen(
    workoutType: com.runtracker.shared.data.model.WorkoutType,
    onStartRun: () -> Unit,
    onBack: () -> Unit
) {
    val workoutInfo = remember(workoutType) {
        when (workoutType) {
            com.runtracker.shared.data.model.WorkoutType.EASY_RUN -> Triple("🏃 Easy Run", "30-45 min at conversational pace", listOf("5 min warm-up", "20-35 min easy", "5 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.LONG_RUN -> Triple("🏃‍♂️ Long Run", "60-90 min building endurance", listOf("10 min warm-up", "40-70 min steady", "10 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.RECOVERY_RUN -> Triple("💚 Recovery Run", "20-30 min very easy", listOf("5 min walk", "15-20 min easy jog", "5 min walk"))
            com.runtracker.shared.data.model.WorkoutType.TEMPO_RUN -> Triple("⚡ Tempo Run", "Comfortably hard sustained effort", listOf("10 min warm-up", "20-30 min tempo", "10 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.INTERVAL_TRAINING -> Triple("🔥 Intervals", "High intensity speed work", listOf("10 min warm-up", "6x800m @ 5K pace", "400m recovery jogs", "10 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.FARTLEK -> Triple("🎲 Fartlek", "Unstructured speed play", listOf("10 min warm-up", "20 min varied pace", "10 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.RACE_PACE -> Triple("🎯 Race Pace", "Practice goal race speed", listOf("10 min warm-up", "15-20 min @ race pace", "10 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.VO2_MAX_INTERVALS -> Triple("💨 VO2 Max", "Short very hard intervals", listOf("10 min warm-up", "5x3 min hard", "2 min recovery", "10 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.MILE_REPEATS -> Triple("🏁 Mile Repeats", "Classic speed workout", listOf("10 min warm-up", "4x1 mile @ threshold", "3 min rest", "10 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.LADDER_WORKOUT -> Triple("📊 Ladder", "Increasing/decreasing intervals", listOf("Warm-up", "200-400-600-800-600-400-200m", "Recovery between", "Cool-down"))
            com.runtracker.shared.data.model.WorkoutType.PYRAMID_WORKOUT -> Triple("🔺 Pyramid", "Build up then back down", listOf("Warm-up", "1-2-3-4-3-2-1 min hard", "Equal recovery", "Cool-down"))
            com.runtracker.shared.data.model.WorkoutType.YASSO_800S -> Triple("8️⃣ Yasso 800s", "Marathon predictor workout", listOf("10 min warm-up", "10x800m @ goal time", "400m jog recovery", "10 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.HILL_REPEATS -> Triple("⛰️ Hill Repeats", "Build strength and power", listOf("10 min warm-up", "8x60s hill sprints", "Jog down recovery", "10 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.HILL_SPRINTS -> Triple("🚀 Hill Sprints", "Short explosive efforts", listOf("10 min warm-up", "10x20s all-out uphill", "Full recovery", "10 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.STAIR_WORKOUT -> Triple("🪜 Stair Workout", "Stair climbing intervals", listOf("5 min warm-up", "10x stair climb", "Walk down recovery", "5 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.PROGRESSION_RUN -> Triple("📈 Progression", "Start easy, finish fast", listOf("10 min easy", "10 min moderate", "10 min tempo", "5 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.NEGATIVE_SPLIT -> Triple("⚖️ Negative Split", "Second half faster", listOf("First half easy pace", "Second half faster", "Finish strong"))
            com.runtracker.shared.data.model.WorkoutType.THRESHOLD_RUN -> Triple("🧱 Threshold", "Lactate threshold effort", listOf("10 min warm-up", "20 min @ threshold", "10 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.TIME_TRIAL -> Triple("🏎️ Time Trial", "All-out effort for distance", listOf("10 min warm-up", "Race effort", "10 min cool-down"))
            com.runtracker.shared.data.model.WorkoutType.PARKRUN_PREP -> Triple("🏃‍♀️ Parkrun Prep", "5K race preparation", listOf("10 min warm-up", "Strides", "5K effort", "Cool-down"))
            com.runtracker.shared.data.model.WorkoutType.RACE_SIMULATION -> Triple("🎭 Race Simulation", "Practice race conditions", listOf("Pre-race routine", "Race pace effort", "Practice fueling"))
            com.runtracker.shared.data.model.WorkoutType.SHAKE_OUT_RUN -> Triple("🌱 Shake Out", "Short easy run pre/post race", listOf("10-15 min very easy", "4x20s strides", "5 min easy"))
            com.runtracker.shared.data.model.WorkoutType.BASE_BUILDING -> Triple("🏗️ Base Building", "Aerobic foundation work", listOf("Easy effort throughout", "Build duration gradually", "Stay conversational"))
            com.runtracker.shared.data.model.WorkoutType.ACTIVE_RECOVERY -> Triple("🔄 Active Recovery", "Very light activity", listOf("20-30 min", "Very easy effort", "Walking OK"))
            com.runtracker.shared.data.model.WorkoutType.AEROBIC_MAINTENANCE -> Triple("🔄 Aerobic", "Maintain fitness", listOf("30-40 min easy", "Steady effort", "Comfortable pace"))
            else -> Triple("🏃 Run", "General run", listOf("Warm-up", "Main run", "Cool-down"))
        }
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
                    Text(title, style = MaterialTheme.typography.title2, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary, modifier = Modifier.padding(top = 8.dp))
                }
                item {
                    Text(description, style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                item {
                    Text("BREAKDOWN", style = MaterialTheme.typography.caption2, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp))
                }

                items(phases.size) { index ->
                    val phase = phases[index]
                    val phaseColor = when {
                        phase.contains("warm", ignoreCase = true) -> WearColors.Warning
                        phase.contains("cool", ignoreCase = true) -> WearColors.ZoneBelow
                        phase.contains("recovery", ignoreCase = true) -> WearColors.ZoneIn
                        else -> MaterialTheme.colors.primary
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(phaseColor, shape = CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(phase, style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.onSurface)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onStartRun, modifier = Modifier.size(64.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = WearColors.Success)) {
                        Text("▶", fontSize = 24.sp, color = Color.White)
                    }
                }
                item {
                    Text("Start Run", style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant)
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
                    Text("📱 From Phone", style = MaterialTheme.typography.caption2,
                        color = WearColors.Success, modifier = Modifier.padding(top = 4.dp))
                }
                item {
                    Text(formatWorkoutTypeName(workout.workoutType),
                        style = MaterialTheme.typography.title2, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 4.dp)) {
                        workout.targetDistanceMeters?.let { distance ->
                            Text("%.1f km".format(distance / 1000),
                                style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                        }
                        workout.targetDurationMinutes?.let { duration ->
                            Text("$duration min", style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (intervals.isNotEmpty()) {
                    item {
                        Text("BREAKDOWN", style = MaterialTheme.typography.caption2,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp))
                    }

                    items(intervals.size.coerceAtMost(8)) { index ->
                        val interval = intervals[index]
                        val phaseColor = when (interval.type) {
                            com.runtracker.shared.data.model.IntervalType.WARMUP -> WearColors.Warning
                            com.runtracker.shared.data.model.IntervalType.COOLDOWN -> WearColors.ZoneBelow
                            com.runtracker.shared.data.model.IntervalType.RECOVERY -> WearColors.ZoneIn
                            com.runtracker.shared.data.model.IntervalType.WORK -> MaterialTheme.colors.primary
                        }
                        val phaseName = when (interval.type) {
                            com.runtracker.shared.data.model.IntervalType.WARMUP -> "Warm-up"
                            com.runtracker.shared.data.model.IntervalType.COOLDOWN -> "Cool-down"
                            com.runtracker.shared.data.model.IntervalType.RECOVERY -> "Recovery"
                            com.runtracker.shared.data.model.IntervalType.WORK -> "Work"
                        }
                        val durationText = interval.durationSeconds?.let { "${it / 60}min" }
                            ?: interval.distanceMeters?.let { "${it.toInt()}m" } ?: ""

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(phaseColor, shape = CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$phaseName $durationText", style = MaterialTheme.typography.caption2,
                                color = MaterialTheme.colors.onSurface)
                        }
                    }

                    if (intervals.size > 8) {
                        item {
                            Text("... +${intervals.size - 8} more",
                                style = MaterialTheme.typography.caption2,
                                color = MaterialTheme.colors.onSurfaceVariant)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onStartRun, modifier = Modifier.size(64.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = WearColors.Success)) {
                        Text("▶", fontSize = 24.sp, color = Color.White)
                    }
                }
                item {
                    Text("Start Run", style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant)
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
    val context = LocalContext.current

    // Load custom workouts off main thread
    var customWorkouts by remember { mutableStateOf<List<SyncedCustomWorkout>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("custom_workouts", Context.MODE_PRIVATE)
            val json = prefs.getString("workouts_list", null)
            if (json != null) {
                try {
                    val gson = com.google.gson.Gson()
                    val type = object : com.google.gson.reflect.TypeToken<List<SyncedCustomWorkout>>() {}.type
                    customWorkouts = gson.fromJson<List<SyncedCustomWorkout>>(json, type)
                } catch (_: Exception) { }
            }
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
                    Text("⭐ My Workouts", style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(vertical = 8.dp))
                }

                if (customWorkouts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No custom workouts", style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Create workouts in the phone app and they'll sync here",
                                style = MaterialTheme.typography.caption2,
                                color = MaterialTheme.colors.onSurfaceVariant,
                                textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    customWorkouts.forEach { workout ->
                        item {
                            Chip(
                                onClick = { onWorkoutSelected(workout.id.toString()) },
                                label = {
                                    Column {
                                        Text(workout.name, style = MaterialTheme.typography.body1,
                                            fontWeight = FontWeight.Bold)
                                        Text("${workout.phasesCount} phases • ${workout.formattedDuration}",
                                            style = MaterialTheme.typography.caption2,
                                            color = MaterialTheme.colors.onSurfaceVariant)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                colors = ChipDefaults.secondaryChipColors()
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sync from phone app", style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}
