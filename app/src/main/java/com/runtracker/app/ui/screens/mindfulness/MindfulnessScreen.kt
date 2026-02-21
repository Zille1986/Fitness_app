package com.runtracker.app.ui.screens.mindfulness

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindfulnessScreen(
    viewModel: MindfulnessViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val breathingState by viewModel.breathingState.collectAsState()
    val guidedSessionState by viewModel.guidedSessionState.collectAsState()
    
    var showMoodDialog by remember { mutableStateOf(false) }
    var showCheckinDialog by remember { mutableStateOf(false) }
    var selectedBreathingPattern by remember { mutableStateOf<BreathingPattern?>(null) }
    var selectedGuidedSession by remember { mutableStateOf<MindfulnessContent?>(null) }
    
    // Show guided session screen if active
    guidedSessionState?.let { state ->
        GuidedMindfulnessScreen(
            state = state,
            onUpdateState = { viewModel.updateGuidedSessionState(it) },
            onComplete = { rating -> viewModel.completeGuidedSession(rating) },
            onCancel = { viewModel.cancelGuidedSession() },
            onPause = { viewModel.pauseGuidedSession() },
            onResume = { viewModel.resumeGuidedSession() }
        )
        return
    }
    
    // Show breathing exercise screen if active
    breathingState?.let { state ->
        BreathingExerciseScreen(
            state = state,
            onUpdateState = { viewModel.updateBreathingState(it) },
            onComplete = { viewModel.completeBreathingSession(state.pattern) },
            onCancel = { viewModel.cancelBreathingSession() }
        )
        return
    }
    
    // Show breathing pattern selection
    selectedBreathingPattern?.let { pattern ->
        BreathingPatternPreviewDialog(
            pattern = pattern,
            onStart = {
                viewModel.startBreathingSession(pattern)
                selectedBreathingPattern = null
            },
            onDismiss = { selectedBreathingPattern = null }
        )
    }
    
    // Show guided session preview
    selectedGuidedSession?.let { content ->
        GuidedSessionPreviewDialog(
            content = content,
            onStart = {
                viewModel.startGuidedSession(content)
                selectedGuidedSession = null
            },
            onDismiss = { selectedGuidedSession = null }
        )
    }
    
    if (showMoodDialog) {
        MoodLogDialog(
            onDismiss = { showMoodDialog = false },
            onSave = { mood, energy, stress, notes ->
                viewModel.logMood(mood, energy, stress, notes)
                showMoodDialog = false
            }
        )
    }
    
    if (showCheckinDialog) {
        WellnessCheckinDialog(
            existingCheckin = uiState.todayCheckin,
            onDismiss = { showCheckinDialog = false },
            onSave = { sleepHours, sleepQuality, mood, energy, stress, soreness, hydration, notes ->
                viewModel.saveWellnessCheckin(sleepHours, sleepQuality, mood, energy, stress, soreness, hydration, notes)
                showCheckinDialog = false
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mindfulness") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weekly Stats Card
            item {
                WeeklyMindfulnessCard(
                    minutesThisWeek = uiState.mindfulnessMinutesThisWeek,
                    sessionsThisWeek = uiState.sessionsThisWeek
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
                        icon = "😊",
                        title = "Log Mood",
                        subtitle = "How are you feeling?",
                        onClick = { showMoodDialog = true }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = "📋",
                        title = "Daily Check-in",
                        subtitle = uiState.todayCheckin?.let { "Score: ${it.readinessScore}" } ?: "Not done",
                        onClick = { showCheckinDialog = true }
                    )
                }
            }
            
            // Breathing Exercises Section
            item {
                Text(
                    text = "Breathing Exercises",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(BreathingPatterns.getAll()) { pattern ->
                        BreathingPatternCard(
                            pattern = pattern,
                            onClick = { selectedBreathingPattern = pattern }
                        )
                    }
                }
            }
            
            // Mindfulness Sessions Section
            item {
                Text(
                    text = "Guided Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(MindfulnessSessions.getAll()) { session ->
                        MindfulnessSessionCard(
                            session = session,
                            onClick = { selectedGuidedSession = session }
                        )
                    }
                }
            }
            
            // Recent Sessions
            if (uiState.recentSessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(uiState.recentSessions.take(5)) { session ->
                    RecentSessionItem(session = session)
                }
            }
        }
    }
}

@Composable
fun WeeklyMindfulnessCard(
    minutesThisWeek: Int,
    sessionsThisWeek: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$minutesThisWeek",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Minutes",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$sessionsThisWeek",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sessions",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 32.sp)
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
fun BreathingPatternCard(
    pattern: BreathingPattern,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🌬️",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = pattern.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${pattern.totalDuration / 60} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pattern.benefits.first(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MindfulnessSessionCard(
    session: MindfulnessContent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = when (session.type) {
                    MindfulnessType.PRE_RUN_FOCUS -> "🏃"
                    MindfulnessType.POST_RUN_GRATITUDE -> "🙏"
                    MindfulnessType.BODY_SCAN -> "🧘"
                    MindfulnessType.STRESS_RELIEF -> "😌"
                    else -> "🧠"
                },
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            Text(
                text = "${session.durationSeconds / 60} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentSessionItem(session: MindfulnessSession) {
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (session.type) {
                        MindfulnessType.BREATHING_EXERCISE -> "🌬️"
                        MindfulnessType.PRE_RUN_FOCUS -> "🏃"
                        MindfulnessType.POST_RUN_GRATITUDE -> "🙏"
                        MindfulnessType.BODY_SCAN -> "🧘"
                        else -> "🧠"
                    },
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.type.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateFormat.format(Date(session.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${session.durationSeconds / 60}m",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BreathingPatternPreviewDialog(
    pattern: BreathingPattern,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pattern.name) },
        text = {
            Column {
                Text(pattern.description)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Pattern:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("• Inhale: ${pattern.inhaleSeconds}s")
                if (pattern.holdAfterInhale > 0) Text("• Hold: ${pattern.holdAfterInhale}s")
                Text("• Exhale: ${pattern.exhaleSeconds}s")
                if (pattern.holdAfterExhale > 0) Text("• Hold: ${pattern.holdAfterExhale}s")
                Text("• ${pattern.cycles} cycles")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Benefits:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                pattern.benefits.forEach { benefit ->
                    Text("• $benefit")
                }
            }
        },
        confirmButton = {
            Button(onClick = onStart) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BreathingExerciseScreen(
    state: BreathingSessionState,
    onUpdateState: (BreathingSessionState) -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val pattern = state.pattern
    
    // Animation for breathing circle
    val breathingProgress by animateFloatAsState(
        targetValue = when (state.phase) {
            BreathingPhase.INHALE -> 1f
            BreathingPhase.HOLD_IN -> 1f
            BreathingPhase.EXHALE -> 0.3f
            BreathingPhase.HOLD_OUT -> 0.3f
        },
        animationSpec = tween(
            durationMillis = when (state.phase) {
                BreathingPhase.INHALE -> pattern.inhaleSeconds * 1000
                BreathingPhase.HOLD_IN -> 100
                BreathingPhase.EXHALE -> pattern.exhaleSeconds * 1000
                BreathingPhase.HOLD_OUT -> 100
            },
            easing = LinearEasing
        ),
        label = "breathing"
    )
    
    // Timer effect
    LaunchedEffect(state) {
        if (!state.isActive) return@LaunchedEffect
        
        delay(1000)
        
        val newSecondsRemaining = state.secondsRemaining - 1
        
        if (newSecondsRemaining <= 0) {
            // Move to next phase
            val (nextPhase, nextDuration, nextCycle) = when (state.phase) {
                BreathingPhase.INHALE -> {
                    if (pattern.holdAfterInhale > 0) {
                        Triple(BreathingPhase.HOLD_IN, pattern.holdAfterInhale, state.currentCycle)
                    } else {
                        Triple(BreathingPhase.EXHALE, pattern.exhaleSeconds, state.currentCycle)
                    }
                }
                BreathingPhase.HOLD_IN -> Triple(BreathingPhase.EXHALE, pattern.exhaleSeconds, state.currentCycle)
                BreathingPhase.EXHALE -> {
                    if (pattern.holdAfterExhale > 0) {
                        Triple(BreathingPhase.HOLD_OUT, pattern.holdAfterExhale, state.currentCycle)
                    } else {
                        if (state.currentCycle >= pattern.cycles) {
                            onComplete()
                            return@LaunchedEffect
                        }
                        Triple(BreathingPhase.INHALE, pattern.inhaleSeconds, state.currentCycle + 1)
                    }
                }
                BreathingPhase.HOLD_OUT -> {
                    if (state.currentCycle >= pattern.cycles) {
                        onComplete()
                        return@LaunchedEffect
                    }
                    Triple(BreathingPhase.INHALE, pattern.inhaleSeconds, state.currentCycle + 1)
                }
            }
            
            onUpdateState(state.copy(
                phase = nextPhase,
                secondsRemaining = nextDuration,
                currentCycle = nextCycle
            ))
        } else {
            onUpdateState(state.copy(secondsRemaining = newSecondsRemaining))
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Pattern name
            Text(
                text = pattern.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cycle counter
            Text(
                text = "Cycle ${state.currentCycle} of ${pattern.cycles}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Breathing circle
            Box(
                modifier = Modifier.size(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2 * breathingProgress
                    drawCircle(
                        color = when (state.phase) {
                            BreathingPhase.INHALE -> Color(0xFF4CAF50)
                            BreathingPhase.HOLD_IN -> Color(0xFF2196F3)
                            BreathingPhase.EXHALE -> Color(0xFF9C27B0)
                            BreathingPhase.HOLD_OUT -> Color(0xFFFF9800)
                        }.copy(alpha = 0.3f),
                        radius = radius
                    )
                    drawCircle(
                        color = when (state.phase) {
                            BreathingPhase.INHALE -> Color(0xFF4CAF50)
                            BreathingPhase.HOLD_IN -> Color(0xFF2196F3)
                            BreathingPhase.EXHALE -> Color(0xFF9C27B0)
                            BreathingPhase.HOLD_OUT -> Color(0xFFFF9800)
                        },
                        radius = radius,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (state.phase) {
                            BreathingPhase.INHALE -> "Breathe In"
                            BreathingPhase.HOLD_IN -> "Hold"
                            BreathingPhase.EXHALE -> "Breathe Out"
                            BreathingPhase.HOLD_OUT -> "Hold"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${state.secondsRemaining}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Cancel button
            OutlinedButton(onClick = onCancel) {
                Text("End Session")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodLogDialog(
    onDismiss: () -> Unit,
    onSave: (MoodLevel, EnergyLevel, StressLevel, String) -> Unit
) {
    var selectedMood by remember { mutableStateOf(MoodLevel.NEUTRAL) }
    var selectedEnergy by remember { mutableStateOf(EnergyLevel.MODERATE) }
    var selectedStress by remember { mutableStateOf(StressLevel.MODERATE) }
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How are you feeling?") },
        text = {
            Column {
                // Mood selection
                Text("Mood", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MoodLevel.values().forEach { mood ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedMood = mood }
                                .background(
                                    if (selectedMood == mood) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        Color.Transparent
                                )
                                .padding(8.dp)
                        ) {
                            Text(mood.emoji, fontSize = 24.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Energy selection
                Text("Energy", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EnergyLevel.values().forEach { energy ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedEnergy = energy }
                                .background(
                                    if (selectedEnergy == energy) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        Color.Transparent
                                )
                                .padding(8.dp)
                        ) {
                            Text(energy.emoji, fontSize = 24.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stress selection
                Text("Stress", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StressLevel.values().forEach { stress ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedStress = stress }
                                .background(
                                    if (selectedStress == stress) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        Color.Transparent
                                )
                                .padding(8.dp)
                        ) {
                            Text(stress.emoji, fontSize = 24.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedMood, selectedEnergy, selectedStress, notes) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessCheckinDialog(
    existingCheckin: WellnessCheckin?,
    onDismiss: () -> Unit,
    onSave: (Float?, Int?, MoodLevel?, EnergyLevel?, StressLevel?, Int?, Int?, String) -> Unit
) {
    var sleepHours by remember { mutableStateOf(existingCheckin?.sleepHours?.toString() ?: "") }
    var sleepQuality by remember { mutableStateOf(existingCheckin?.sleepQuality ?: 3) }
    var selectedMood by remember { mutableStateOf(existingCheckin?.mood ?: MoodLevel.NEUTRAL) }
    var selectedEnergy by remember { mutableStateOf(existingCheckin?.energy ?: EnergyLevel.MODERATE) }
    var selectedStress by remember { mutableStateOf(existingCheckin?.stress ?: StressLevel.MODERATE) }
    var soreness by remember { mutableStateOf(existingCheckin?.soreness ?: 3) }
    var hydration by remember { mutableStateOf(existingCheckin?.hydration ?: 3) }
    var notes by remember { mutableStateOf(existingCheckin?.notes ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily Wellness Check-in") },
        text = {
            LazyColumn {
                item {
                    // Sleep hours
                    OutlinedTextField(
                        value = sleepHours,
                        onValueChange = { sleepHours = it },
                        label = { Text("Hours of sleep") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Sleep quality
                    Text("Sleep Quality", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = sleepQuality.toFloat(),
                        onValueChange = { sleepQuality = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Mood
                    Text("Mood", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MoodLevel.values().forEach { mood ->
                            Text(
                                text = mood.emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { selectedMood = mood }
                                    .background(
                                        if (selectedMood == mood)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            Color.Transparent
                                    )
                                    .padding(8.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Soreness
                    Text("Muscle Soreness (1=None, 5=Very Sore)", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = soreness.toFloat(),
                        onValueChange = { soreness = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Hydration
                    Text("Hydration Level", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = hydration.toFloat(),
                        onValueChange = { hydration = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    sleepHours.toFloatOrNull(),
                    sleepQuality,
                    selectedMood,
                    selectedEnergy,
                    selectedStress,
                    soreness,
                    hydration,
                    notes
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GuidedSessionPreviewDialog(
    content: MindfulnessContent,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(content.title) },
        text = {
            Column {
                Text(content.description)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Duration: ${content.durationSeconds / 60} minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "What you'll do:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                content.instructions.forEachIndexed { index, instruction ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = instruction,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onStart) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Begin Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GuidedMindfulnessScreen(
    state: GuidedSessionState,
    onUpdateState: (GuidedSessionState) -> Unit,
    onComplete: (Int?) -> Unit,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    var showRatingDialog by remember { mutableStateOf(false) }
    
    // Timer effect
    LaunchedEffect(state.isActive, state.isPaused, state.secondsRemaining) {
        if (!state.isActive || state.isPaused) return@LaunchedEffect
        
        delay(1000)
        
        val newSecondsRemaining = state.secondsRemaining - 1
        val newTotalElapsed = state.totalElapsedSeconds + 1
        
        if (newSecondsRemaining <= 0) {
            // Move to next step or complete
            if (state.isLastStep) {
                showRatingDialog = true
            } else {
                onUpdateState(state.copy(
                    currentStepIndex = state.currentStepIndex + 1,
                    secondsRemaining = state.secondsPerStep,
                    totalElapsedSeconds = newTotalElapsed
                ))
            }
        } else {
            onUpdateState(state.copy(
                secondsRemaining = newSecondsRemaining,
                totalElapsedSeconds = newTotalElapsed
            ))
        }
    }
    
    if (showRatingDialog) {
        SessionRatingDialog(
            onRate = { rating ->
                showRatingDialog = false
                onComplete(rating)
            },
            onSkip = {
                showRatingDialog = false
                onComplete(null)
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when (state.content.type) {
                    MindfulnessType.PRE_RUN_FOCUS -> Color(0xFF1A237E).copy(alpha = 0.9f)
                    MindfulnessType.POST_RUN_GRATITUDE -> Color(0xFF1B5E20).copy(alpha = 0.9f)
                    MindfulnessType.BODY_SCAN -> Color(0xFF4A148C).copy(alpha = 0.9f)
                    MindfulnessType.STRESS_RELIEF -> Color(0xFF0D47A1).copy(alpha = 0.9f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.White
                    )
                }
                Text(
                    text = state.content.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { if (state.isPaused) onResume() else onPause() }) {
                    Icon(
                        if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (state.isPaused) "Resume" else "Pause",
                        tint = Color.White
                    )
                }
            }
            
            // Progress indicator
            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = state.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            
            Text(
                text = "Step ${state.currentStepIndex + 1} of ${state.content.instructions.size}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Main instruction display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // Animated icon based on session type
                Text(
                    text = when (state.content.type) {
                        MindfulnessType.PRE_RUN_FOCUS -> "🏃"
                        MindfulnessType.POST_RUN_GRATITUDE -> "🙏"
                        MindfulnessType.BODY_SCAN -> "🧘"
                        MindfulnessType.STRESS_RELIEF -> "😌"
                        else -> "🧠"
                    },
                    fontSize = 64.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Current instruction
                Text(
                    text = state.currentInstruction,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 32.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Timer for current step
                Text(
                    text = "${state.secondsRemaining}s",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Light
                )
                
                if (state.isPaused) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Paused",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (state.currentStepIndex > 0) {
                    OutlinedButton(
                        onClick = {
                            onUpdateState(state.copy(
                                currentStepIndex = state.currentStepIndex - 1,
                                secondsRemaining = state.secondsPerStep
                            ))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }
                }
                
                Button(
                    onClick = {
                        if (state.isLastStep) {
                            showRatingDialog = true
                        } else {
                            onUpdateState(state.copy(
                                currentStepIndex = state.currentStepIndex + 1,
                                secondsRemaining = state.secondsPerStep,
                                totalElapsedSeconds = state.totalElapsedSeconds + (state.secondsPerStep - state.secondsRemaining)
                            ))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(if (state.isLastStep) "Complete" else "Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (state.isLastStep) Icons.Default.Check else Icons.Default.ArrowForward,
                        contentDescription = null
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SessionRatingDialog(
    onRate: (Int) -> Unit,
    onSkip: () -> Unit
) {
    var selectedRating by remember { mutableStateOf(0) }
    
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("How was your session?") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Rate your experience",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..5).forEach { rating ->
                        IconButton(
                            onClick = { selectedRating = rating }
                        ) {
                            Icon(
                                imageVector = if (rating <= selectedRating) 
                                    Icons.Default.Star 
                                else 
                                    Icons.Default.StarBorder,
                                contentDescription = "$rating stars",
                                tint = if (rating <= selectedRating) 
                                    Color(0xFFFFD700) 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRate(selectedRating) },
                enabled = selectedRating > 0
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        }
    )
}
