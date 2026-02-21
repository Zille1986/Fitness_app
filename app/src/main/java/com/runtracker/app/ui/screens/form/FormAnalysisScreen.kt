package com.runtracker.app.ui.screens.form

import android.Manifest
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.runtracker.shared.ai.*
import com.runtracker.shared.data.model.FormProgressSummary
import com.runtracker.shared.data.model.FormReviewComparison
import com.runtracker.shared.data.model.FormReviewWithDetails
import com.runtracker.shared.data.model.FormProgressTrend

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormAnalysisScreen(
    viewModel: FormAnalysisViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (uiState.mode) {
                            FormAnalysisMode.AUTO_DETECT -> "AI Form Coach"
                            FormAnalysisMode.RUNNING -> "Running Form Coach"
                            FormAnalysisMode.GYM -> "Gym Form Coach"
                        }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Auto-Detect Info Card (shown when in auto-detect mode and no analysis yet)
            if (uiState.mode == FormAnalysisMode.AUTO_DETECT && uiState.autoDetectResult == null && !uiState.isCameraActive) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "AI Form Coach",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Just start exercising - the AI will automatically recognize what you're doing and provide form feedback",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            // Detected Exercise (shown after auto-detect analysis)
            if (uiState.mode == FormAnalysisMode.AUTO_DETECT && uiState.detectedExerciseName != null) {
                item {
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
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Exercise Detected",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = uiState.detectedExerciseName ?: "Unknown",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            // Form Score Card
            item {
                val score = when (uiState.mode) {
                    FormAnalysisMode.AUTO_DETECT -> uiState.autoDetectResult?.overallScore ?: 0
                    FormAnalysisMode.RUNNING -> uiState.lastRunningAnalysis?.overallScore ?: 0
                    FormAnalysisMode.GYM -> uiState.lastGymAnalysis?.overallScore ?: 0
                }
                val hasAnalysis = when (uiState.mode) {
                    FormAnalysisMode.AUTO_DETECT -> uiState.autoDetectResult != null
                    FormAnalysisMode.RUNNING -> uiState.lastRunningAnalysis != null
                    FormAnalysisMode.GYM -> uiState.lastGymAnalysis != null
                }
                FormScoreCard(score = score, hasAnalysis = hasAnalysis)
            }
            
            // Camera Preview / Start Analysis
            item {
                CameraCard(
                    analysisPhase = uiState.analysisPhase,
                    countdownSeconds = uiState.countdownSeconds,
                    framesCollected = uiState.framesCollected,
                    repsDetected = uiState.repsDetected,
                    dataQuality = uiState.dataQuality,
                    isAnalyzing = uiState.isAnalyzing,
                    isCameraActive = uiState.isCameraActive,
                    poseDetected = uiState.poseDetected,
                    isGymMode = uiState.mode == FormAnalysisMode.GYM,
                    onStartAnalysis = { viewModel.startAnalysis() },
                    onStopAnalysis = { viewModel.stopAnalysis() },
                    onCaptureAnalysis = { viewModel.captureAndAnalyze() },
                    onDemoAnalysis = { viewModel.runDemoAnalysis() },
                    onPoseDetected = { viewModel.onPoseDetected(it) },
                    onFrameCaptured = { viewModel.captureFrame(it) },
                    onCameraError = { viewModel.onCameraError(it) },
                    exerciseName = when (uiState.mode) {
                        FormAnalysisMode.AUTO_DETECT -> uiState.detectedExerciseName ?: "Any Exercise"
                        FormAnalysisMode.GYM -> uiState.selectedGymExercise.displayName
                        FormAnalysisMode.RUNNING -> "Running"
                    }
                )
            }
            
            // Advanced Mode Toggle (collapsed by default)
            item {
                var showAdvancedOptions by remember { mutableStateOf(false) }
                
                Column {
                    TextButton(
                        onClick = { showAdvancedOptions = !showAdvancedOptions },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (showAdvancedOptions) "Hide Advanced Options" else "Advanced Options",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Icon(
                            if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    
                    if (showAdvancedOptions) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModeSelector(
                            currentMode = uiState.mode,
                            onModeChange = { viewModel.setMode(it) }
                        )
                        
                        // Gym Exercise Selector (only in gym mode)
                        if (uiState.mode == FormAnalysisMode.GYM) {
                            Spacer(modifier = Modifier.height(8.dp))
                            GymExerciseSelector(
                                selectedExercise = uiState.selectedGymExercise,
                                onExerciseSelect = { viewModel.setGymExercise(it) }
                            )
                        }
                    }
                }
            }
            
            // Error message
            uiState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearError() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
            
            // Auto-Detect Analysis Results
            if (uiState.mode == FormAnalysisMode.AUTO_DETECT) {
                uiState.autoDetectResult?.let { result ->
                    // Positive Feedback
                    if (result.positiveFeedback.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "What You're Doing Well",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    result.positiveFeedback.forEach { feedback ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.tertiary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = feedback,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Form Issues
                    if (result.issues.isNotEmpty()) {
                        item {
                            Text(
                                text = "Areas to Improve",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        items(result.issues) { issue ->
                            FormIssueCard(issue = issue)
                        }
                    }
                    
                    // Key Tips
                    if (result.keyTips.isNotEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Key Tips",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    result.keyTips.forEachIndexed { index, tip ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "${index + 1}.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = tip,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Rep Count & Confidence
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (result.repCount > 0) {
                                Card(modifier = Modifier.weight(1f)) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${result.repCount}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Reps Detected",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                            Card(modifier = Modifier.weight(1f)) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = result.confidence,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when (result.confidence) {
                                            "HIGH" -> MaterialTheme.colorScheme.primary
                                            "MEDIUM" -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                    Text(
                                        text = "AI Confidence",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Running Analysis Results
            if (uiState.mode == FormAnalysisMode.RUNNING) {
                uiState.lastRunningAnalysis?.let { analysis ->
                    item {
                        MetricsCard(
                            metrics = analysis.metrics,
                            cadence = analysis.cadenceEstimate,
                            stride = analysis.strideAnalysis
                        )
                    }
                    
                    if (analysis.issues.isNotEmpty()) {
                        item {
                            Text(
                                text = "Areas to Improve",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        items(analysis.issues) { issue ->
                            FormIssueCard(issue = issue)
                        }
                    }
                }
            }
            
            // Gym Analysis Results
            if (uiState.mode == FormAnalysisMode.GYM) {
                uiState.lastGymAnalysis?.let { analysis ->
                    // Form Comparison Visual - use average pose across all reps
                    val comparisonPose = uiState.averagePose ?: uiState.currentPose
                    comparisonPose?.let { pose ->
                        item {
                            FormComparisonCard(
                                userPose = pose,
                                exerciseType = analysis.exerciseType,
                                issues = analysis.issues
                            )
                        }
                    }
                    
                    item {
                        GymMetricsCard(
                            exerciseType = analysis.exerciseType,
                            metrics = analysis.metrics,
                            repQuality = analysis.repQuality
                        )
                    }
                    
                    if (analysis.issues.isNotEmpty()) {
                        item {
                            Text(
                                text = "Form Issues Detected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        items(analysis.issues) { issue ->
                            FormIssueCard(issue = issue)
                        }
                    }
                    
                    if (analysis.tips.isNotEmpty()) {
                        item {
                            GymTipsCard(tips = analysis.tips)
                        }
                    }
                }
            }
            
            // Form Tips Section (Running mode)
            if (uiState.mode == FormAnalysisMode.RUNNING) {
                item {
                    FormTipsCard()
                }
            }
            
            // Comparison with Previous Review
            uiState.reviewComparison?.let { comparison ->
                if (comparison.previousReview != null) {
                    item {
                        ComparisonCard(comparison = comparison)
                    }
                }
            }
            
            // Progress Summary
            uiState.progressSummary?.let { summary ->
                if (summary.totalReviews > 1) {
                    item {
                        ProgressSummaryCard(
                            summary = summary,
                            onViewHistory = { viewModel.toggleHistorySheet() }
                        )
                    }
                }
            }
            
            // Drills Section
            item {
                val issues = when (uiState.mode) {
                    FormAnalysisMode.AUTO_DETECT -> uiState.autoDetectResult?.issues?.map { it.type } ?: emptyList()
                    FormAnalysisMode.RUNNING -> uiState.lastRunningAnalysis?.issues?.map { it.type } ?: emptyList()
                    FormAnalysisMode.GYM -> uiState.lastGymAnalysis?.issues?.map { it.type } ?: emptyList()
                }
                DrillsCard(issues = issues)
            }
        }
    }
    
    // History Bottom Sheet
    if (uiState.showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleHistorySheet() }
        ) {
            FormHistorySheet(
                reviewHistory = uiState.reviewHistory,
                progressSummary = uiState.progressSummary,
                scoreHistory = viewModel.getScoreHistory(),
                onDismiss = { viewModel.toggleHistorySheet() }
            )
        }
    }
}

@Composable
fun FormScoreCard(
    score: Int,
    hasAnalysis: Boolean
) {
    val scoreColor = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFF8BC34A)
        score >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasAnalysis) scoreColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasAnalysis) {
                Text(
                    text = "Form Score",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(scoreColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = when {
                        score >= 80 -> "Excellent Form!"
                        score >= 60 -> "Good Form"
                        score >= 40 -> "Needs Improvement"
                        else -> "Focus on Basics"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scoreColor
                )
            } else {
                Icon(
                    Icons.Default.DirectionsRun,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start an analysis to see your form score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraCard(
    analysisPhase: AnalysisPhase = AnalysisPhase.IDLE,
    countdownSeconds: Int = 0,
    framesCollected: Int = 0,
    repsDetected: Int = 0,
    dataQuality: DataQuality = DataQuality.INSUFFICIENT,
    isAnalyzing: Boolean,
    isCameraActive: Boolean = false,
    poseDetected: Boolean = false,
    isGymMode: Boolean = false,
    onStartAnalysis: () -> Unit,
    onStopAnalysis: () -> Unit,
    onCaptureAnalysis: () -> Unit = {},
    onDemoAnalysis: () -> Unit = {},
    onPoseDetected: (PoseData) -> Unit = {},
    onFrameCaptured: (android.graphics.Bitmap) -> Unit = {},
    onCameraError: (String) -> Unit = {},
    exerciseName: String = "Running"
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isCameraActive && cameraPermissionState.status.isGranted) {
                    // Real camera preview with pose overlay
                    CameraPreviewWithPoseOverlay(
                        modifier = Modifier.fillMaxSize(),
                        onPoseDetected = onPoseDetected,
                        onFrameCaptured = onFrameCaptured,
                        onError = onCameraError,
                        isAnalyzing = isAnalyzing,
                        useFrontCamera = true
                    )
                    
                    // Overlay based on phase
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        when (analysisPhase) {
                            AnalysisPhase.COUNTDOWN -> {
                                // Large countdown number in center
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$countdownSeconds",
                                        style = MaterialTheme.typography.displayLarge,
                                        fontSize = 80.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Get in position!",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                                
                                // Pose status at top
                                Surface(
                                    color = if (poseDetected) Color(0xFF4CAF50).copy(alpha = 0.8f) 
                                           else Color(0xFFFF9800).copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.align(Alignment.TopCenter)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (poseDetected) Icons.Default.CheckCircle else Icons.Default.PersonSearch,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (poseDetected) "Pose detected" else "Looking for pose...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            
                            AnalysisPhase.COLLECTING -> {
                                // Data quality indicator at top
                                val qualityColor = when (dataQuality) {
                                    DataQuality.INSUFFICIENT -> Color(0xFFE53935)
                                    DataQuality.LOW -> Color(0xFFFF9800)
                                    DataQuality.MODERATE -> Color(0xFFFFC107)
                                    DataQuality.GOOD -> Color(0xFF8BC34A)
                                    DataQuality.EXCELLENT -> Color(0xFF4CAF50)
                                }
                                
                                Surface(
                                    color = qualityColor.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.align(Alignment.TopCenter)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            when (dataQuality) {
                                                DataQuality.INSUFFICIENT, DataQuality.LOW -> Icons.Default.Warning
                                                DataQuality.MODERATE -> Icons.Default.TrendingUp
                                                DataQuality.GOOD, DataQuality.EXCELLENT -> Icons.Default.CheckCircle
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when (dataQuality) {
                                                DataQuality.INSUFFICIENT -> "Collecting data..."
                                                DataQuality.LOW -> "Need more data"
                                                DataQuality.MODERATE -> "Moderate confidence"
                                                DataQuality.GOOD -> "Good data quality"
                                                DataQuality.EXCELLENT -> "Excellent data!"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                
                                // Stats at bottom
                                Surface(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${framesCollected / 10}s",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Recorded",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                        
                                        if (isGymMode && exerciseName != "Plank") {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$repsDetected",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = if (repsDetected > 0) Color(0xFF4CAF50) else Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Reps",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            AnalysisPhase.ANALYZING -> {
                                // Processing indicator
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = Color.White)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Analyzing your form...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                            }
                            
                            else -> {
                                // Default pose status
                                Surface(
                                    color = if (poseDetected) Color(0xFF4CAF50).copy(alpha = 0.8f) 
                                           else Color(0xFFFF9800).copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.align(Alignment.TopCenter)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (poseDetected) Icons.Default.CheckCircle else Icons.Default.PersonSearch,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (poseDetected) "Pose Detected - $exerciseName" 
                                                   else "Looking for pose...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (!cameraPermissionState.status.isGranted) {
                    // Permission not granted
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera permission required",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() }
                        ) {
                            Text("Grant Permission")
                        }
                        if (cameraPermissionState.status.shouldShowRationale) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Camera is needed to analyze your form in real-time",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Camera not active - show placeholder
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap 'Start Camera' to begin",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Position yourself so your full body is visible",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Control buttons based on phase
            when (analysisPhase) {
                AnalysisPhase.COUNTDOWN -> {
                    OutlinedButton(
                        onClick = onStopAnalysis,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }
                }
                
                AnalysisPhase.COLLECTING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onStopAnalysis,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop")
                        }
                        
                        Button(
                            onClick = onCaptureAnalysis,
                            modifier = Modifier.weight(1f),
                            enabled = dataQuality != DataQuality.INSUFFICIENT
                        ) {
                            Icon(Icons.Default.Camera, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Analyze")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Hint text based on data quality
                    Text(
                        text = when {
                            dataQuality == DataQuality.INSUFFICIENT -> 
                                "Keep recording to collect enough data..."
                            dataQuality == DataQuality.LOW && isGymMode && repsDetected < 1 ->
                                "Perform at least one rep of $exerciseName"
                            dataQuality == DataQuality.LOW ->
                                "A few more seconds for better accuracy"
                            dataQuality == DataQuality.MODERATE ->
                                "Good! More reps = better analysis"
                            else ->
                                "Great data! Tap Analyze when ready"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                
                AnalysisPhase.ANALYZING -> {
                    // Show progress, no buttons
                    Text(
                        text = "Processing your form data...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                else -> {
                    // IDLE or COMPLETE - show start buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onStartAnalysis,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = cameraPermissionState.status.isGranted
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Camera Analysis")
                        }
                        
                        OutlinedButton(
                            onClick = onDemoAnalysis,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Demo (No Camera)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricsCard(
    metrics: Map<String, Float>,
    cadence: Int,
    stride: StrideAnalysis
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Form Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    icon = "👟",
                    value = "$cadence",
                    label = "Cadence",
                    unit = "spm"
                )
                MetricItem(
                    icon = "📏",
                    value = "%.2f".format(stride.strideLength),
                    label = "Stride",
                    unit = "m"
                )
                MetricItem(
                    icon = "⏱️",
                    value = "${stride.groundContactTime}",
                    label = "GCT",
                    unit = "ms"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                metrics["forwardLean"]?.let { lean ->
                    MetricItem(
                        icon = "🏃",
                        value = "%.1f°".format(lean),
                        label = "Forward Lean",
                        unit = ""
                    )
                }
                metrics["verticalOscillation"]?.let { osc ->
                    MetricItem(
                        icon = "↕️",
                        value = "%.1f".format(osc),
                        label = "Bounce",
                        unit = "cm"
                    )
                }
                metrics["armAngle"]?.let { angle ->
                    MetricItem(
                        icon = "💪",
                        value = "%.0f°".format(angle),
                        label = "Arm Angle",
                        unit = ""
                    )
                }
            }
        }
    }
}

@Composable
fun MetricItem(
    icon: String,
    value: String,
    label: String,
    unit: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 20.sp)
        Text(
            text = value + unit,
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
fun FormIssueCard(issue: FormIssue) {
    val severityColor = when (issue.severity) {
        IssueSeverity.HIGH -> Color(0xFFF44336)
        IssueSeverity.MEDIUM -> Color(0xFFFF9800)
        IssueSeverity.LOW -> Color(0xFF4CAF50)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = severityColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = issue.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = issue.correction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun FormTipsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Form Tips",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val tips = listOf(
                "🏃 Run tall with a slight forward lean from ankles",
                "💪 Keep arms at 90° with relaxed shoulders",
                "👟 Aim for 170-180 steps per minute",
                "👀 Look ahead, not down at your feet",
                "🤲 Keep hands relaxed, not clenched"
            )
            
            tips.forEach { tip ->
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DrillsCard(issues: List<FormIssueType>) {
    val drills = issues.flatMap { FormDrills.getDrillsForIssue(it) }.distinctBy { it.name }.take(3)
    
    if (drills.isEmpty()) return
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Recommended Drills",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            drills.forEach { drill ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = drill.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = drill.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        Text(
                            text = "Duration: ${drill.duration}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = drill.frequency,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (drill != drills.last()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelector(
    currentMode: FormAnalysisMode,
    onModeChange: (FormAnalysisMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FormAnalysisMode.values().forEach { mode ->
                val isSelected = mode == currentMode
                FilterChip(
                    selected = isSelected,
                    onClick = { onModeChange(mode) },
                    label = { 
                        Text(
                            when (mode) {
                                FormAnalysisMode.AUTO_DETECT -> "Auto"
                                FormAnalysisMode.RUNNING -> "Running"
                                FormAnalysisMode.GYM -> "Gym"
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (mode) {
                                FormAnalysisMode.AUTO_DETECT -> Icons.Default.AutoAwesome
                                FormAnalysisMode.RUNNING -> Icons.Default.DirectionsRun
                                FormAnalysisMode.GYM -> Icons.Default.FitnessCenter
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymExerciseSelector(
    selectedExercise: GymExerciseType,
    onExerciseSelect: (GymExerciseType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Exercise",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedExercise.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    GymExerciseType.values().forEach { exercise ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(exercise.displayName)
                                    Text(
                                        text = exercise.muscleGroups.joinToString(", "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onExerciseSelect(exercise)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GymMetricsCard(
    exerciseType: GymExerciseType,
    metrics: Map<String, Float>,
    repQuality: RepQuality
) {
    val qualityColor = when (repQuality) {
        RepQuality.GOOD -> Color(0xFF4CAF50)
        RepQuality.FAIR -> Color(0xFFFF9800)
        RepQuality.POOR -> Color(0xFFF44336)
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exerciseType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    color = qualityColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (repQuality) {
                            RepQuality.GOOD -> "Good Form"
                            RepQuality.FAIR -> "Fair Form"
                            RepQuality.POOR -> "Needs Work"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = qualityColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Muscle Groups: ${exerciseType.muscleGroups.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Key metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                metrics.entries.take(3).forEach { (key, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatMetricValue(key, value),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatMetricName(key),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatMetricName(key: String): String {
    return key.replace(Regex("([A-Z])"), " $1")
        .trim()
        .replaceFirstChar { it.uppercase() }
}

private fun formatMetricValue(key: String, value: Float): String {
    return when {
        key.contains("angle", ignoreCase = true) -> "${value.toInt()}°"
        key.contains("lockout", ignoreCase = true) || 
        key.contains("heelRise", ignoreCase = true) -> if (value > 0.5f) "Yes" else "No"
        else -> String.format("%.1f", value)
    }
}

@Composable
fun GymTipsCard(tips: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFFFD700)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Form Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            tips.forEach { tip ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ComparisonCard(comparison: FormReviewComparison) {
    val scoreDiff = comparison.scoreDifference
    val isImproved = scoreDiff > 0
    val isDeclined = scoreDiff < 0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isImproved -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                isDeclined -> Color(0xFFF44336).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "vs Previous Review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            isImproved -> Icons.Default.TrendingUp
                            isDeclined -> Icons.Default.TrendingDown
                            else -> Icons.Default.TrendingFlat
                        },
                        contentDescription = null,
                        tint = when {
                            isImproved -> Color(0xFF4CAF50)
                            isDeclined -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            isImproved -> "+$scoreDiff pts"
                            isDeclined -> "$scoreDiff pts"
                            else -> "No change"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isImproved -> Color(0xFF4CAF50)
                            isDeclined -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Score comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${comparison.previousReview?.overallScore ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Previous",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${comparison.currentReview.overallScore}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isImproved -> Color(0xFF4CAF50)
                            isDeclined -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Issues comparison
            if (comparison.resolvedIssues.isNotEmpty() || comparison.newIssues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                if (comparison.resolvedIssues.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fixed: ${comparison.resolvedIssues.joinToString(", ") { formatIssueName(it) }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                
                if (comparison.newIssues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New: ${comparison.newIssues.joinToString(", ") { formatIssueName(it) }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressSummaryCard(
    summary: FormProgressSummary,
    onViewHistory: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                TextButton(onClick = onViewHistory) {
                    Text("View History")
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.totalReviews}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reviews",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.averageScore.toInt()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Avg Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.bestScore}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Best",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val improvementColor = when {
                        summary.scoreImprovement > 0 -> Color(0xFF4CAF50)
                        summary.scoreImprovement < 0 -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = if (summary.scoreImprovement >= 0) "+${summary.scoreImprovement}" else "${summary.scoreImprovement}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = improvementColor
                    )
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Trend indicator
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = when (summary.trend) {
                    FormProgressTrend.IMPROVING -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    FormProgressTrend.DECLINING -> Color(0xFFF44336).copy(alpha = 0.1f)
                    FormProgressTrend.STABLE -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (summary.trend) {
                            FormProgressTrend.IMPROVING -> Icons.Default.TrendingUp
                            FormProgressTrend.DECLINING -> Icons.Default.TrendingDown
                            FormProgressTrend.STABLE -> Icons.Default.TrendingFlat
                        },
                        contentDescription = null,
                        tint = when (summary.trend) {
                            FormProgressTrend.IMPROVING -> Color(0xFF4CAF50)
                            FormProgressTrend.DECLINING -> Color(0xFFF44336)
                            FormProgressTrend.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (summary.trend) {
                            FormProgressTrend.IMPROVING -> "Your form is improving!"
                            FormProgressTrend.DECLINING -> "Your form needs attention"
                            FormProgressTrend.STABLE -> "Your form is consistent"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Resolved issues
            if (summary.resolvedIssues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Issues you've fixed:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    summary.resolvedIssues.take(3).forEach { issue ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = formatIssueName(issue),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormHistorySheet(
    reviewHistory: List<FormReviewWithDetails>,
    progressSummary: FormProgressSummary?,
    scoreHistory: List<Pair<Long, Int>>,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Form Review History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Score trend mini chart
        if (scoreHistory.size >= 2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Score Trend",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Simple visual score trend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        scoreHistory.takeLast(7).forEach { (_, score) ->
                            val height = (score / 100f * 60).dp
                            val color = when {
                                score >= 80 -> Color(0xFF4CAF50)
                                score >= 60 -> Color(0xFF8BC34A)
                                score >= 40 -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(height)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$score",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Review list
        Text(
            text = "Past Reviews",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (reviewHistory.isEmpty()) {
            Text(
                text = "No previous reviews yet. Complete a form analysis to start tracking your progress!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            reviewHistory.take(10).forEach { reviewWithDetails ->
                ReviewHistoryItem(reviewWithDetails = reviewWithDetails)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ReviewHistoryItem(reviewWithDetails: FormReviewWithDetails) {
    val scoreColor = when {
        reviewWithDetails.review.overallScore >= 80 -> Color(0xFF4CAF50)
        reviewWithDetails.review.overallScore >= 60 -> Color(0xFF8BC34A)
        reviewWithDetails.review.overallScore >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${reviewWithDetails.review.overallScore}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reviewWithDetails.exerciseDisplayName ?: "Running Form",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = reviewWithDetails.formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if (reviewWithDetails.issueCount > 0) {
                    Text(
                        text = "${reviewWithDetails.issueCount} issues",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (reviewWithDetails.highSeverityCount > 0) {
                    Text(
                        text = "${reviewWithDetails.highSeverityCount} critical",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

private fun formatIssueName(issueType: String): String {
    return issueType.replace("_", " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}

@Composable
fun FormComparisonCard(
    userPose: PoseData,
    exerciseType: GymExerciseType,
    issues: List<FormIssue>
) {
    val overallColor = when {
        issues.isEmpty() -> Color(0xFF4CAF50)
        issues.any { it.severity == IssueSeverity.HIGH } -> Color(0xFFF44336)
        issues.any { it.severity == IssueSeverity.MEDIUM } -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Form Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Ideal form description
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ideal ${exerciseType.displayName} Form",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = getIdealFormDescription(exerciseType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Body part checklist
            Text(
                text = "Form Checklist",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val checklistItems = getFormChecklist(exerciseType, issues)
            checklistItems.forEach { item ->
                FormChecklistItem(
                    bodyPart = item.bodyPart,
                    status = item.status,
                    tip = item.tip
                )
            }
            
            if (issues.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Great form! Keep it up!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

private fun getIdealFormDescription(exerciseType: GymExerciseType): String {
    return when (exerciseType) {
        GymExerciseType.PUSH_UP -> "Keep your body in a straight line from head to heels. Elbows at 45° angle, core tight, lower until chest nearly touches the ground."
        GymExerciseType.SQUAT -> "Feet shoulder-width apart, chest up, knees tracking over toes. Lower until thighs are parallel to ground, weight in heels."
        GymExerciseType.DEADLIFT -> "Flat back, hips hinged, bar close to body. Drive through heels, keep core braced, shoulders back at the top."
        GymExerciseType.PLANK -> "Straight line from head to heels. Elbows under shoulders, core engaged, don't let hips sag or pike up."
        GymExerciseType.LUNGE -> "Front knee at 90°, back knee nearly touching ground. Torso upright, core engaged, front knee over ankle."
        GymExerciseType.BENCH_PRESS -> "Feet flat, back arched slightly, shoulder blades retracted. Lower bar to chest, press up in a slight arc."
        GymExerciseType.OVERHEAD_PRESS -> "Core braced, bar at shoulder height. Press straight up, lock out overhead, keep back neutral."
        GymExerciseType.BARBELL_ROW -> "Hip hinge position, back flat, pull bar to lower chest. Squeeze shoulder blades together at top."
    }
}

data class FormChecklistItemData(
    val bodyPart: String,
    val status: FormCheckStatus,
    val tip: String?
)

enum class FormCheckStatus { GOOD, WARNING, ERROR }

private fun getFormChecklist(exerciseType: GymExerciseType, issues: List<FormIssue>): List<FormChecklistItemData> {
    val issueTypes = issues.map { it.type }
    
    // Use actual FormIssueType enum values
    return when (exerciseType) {
        GymExerciseType.PUSH_UP -> listOf(
            FormChecklistItemData(
                "Elbow Position",
                if (issueTypes.contains(FormIssueType.ELBOW_POSITION)) FormCheckStatus.ERROR else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.ELBOW_POSITION)) "Keep elbows at 45° angle" else null
            ),
            FormChecklistItemData(
                "Hip Alignment",
                if (issueTypes.contains(FormIssueType.HIP_POSITION)) FormCheckStatus.ERROR else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.HIP_POSITION)) "Keep hips in line with shoulders" else null
            ),
            FormChecklistItemData(
                "Depth",
                if (issueTypes.contains(FormIssueType.DEPTH)) FormCheckStatus.WARNING else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.DEPTH)) "Lower chest closer to ground" else null
            )
        )
        GymExerciseType.SQUAT -> listOf(
            FormChecklistItemData(
                "Knee Tracking",
                if (issueTypes.contains(FormIssueType.KNEE_TRACKING)) FormCheckStatus.ERROR else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.KNEE_TRACKING)) "Push knees out over toes" else null
            ),
            FormChecklistItemData(
                "Back Position",
                if (issueTypes.contains(FormIssueType.BACK_POSITION)) FormCheckStatus.ERROR else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.BACK_POSITION)) "Keep chest up, back flat" else null
            ),
            FormChecklistItemData(
                "Depth",
                if (issueTypes.contains(FormIssueType.DEPTH)) FormCheckStatus.WARNING else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.DEPTH)) "Go deeper - thighs parallel" else null
            )
        )
        GymExerciseType.DEADLIFT -> listOf(
            FormChecklistItemData(
                "Back Position",
                if (issueTypes.contains(FormIssueType.BACK_POSITION)) FormCheckStatus.ERROR else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.BACK_POSITION)) "Keep back flat, chest up" else null
            ),
            FormChecklistItemData(
                "Hip Hinge",
                if (issueTypes.contains(FormIssueType.HIP_POSITION)) FormCheckStatus.WARNING else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.HIP_POSITION)) "Hinge more at hips" else null
            ),
            FormChecklistItemData(
                "Lockout",
                if (issueTypes.contains(FormIssueType.LOCKOUT)) FormCheckStatus.WARNING else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.LOCKOUT)) "Fully extend at top" else null
            )
        )
        GymExerciseType.PLANK -> listOf(
            FormChecklistItemData(
                "Hip Position",
                if (issueTypes.contains(FormIssueType.HIP_POSITION)) FormCheckStatus.ERROR else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.HIP_POSITION)) "Keep hips level with shoulders" else null
            ),
            FormChecklistItemData(
                "Core Engagement",
                FormCheckStatus.GOOD,
                null
            ),
            FormChecklistItemData(
                "Shoulder Position",
                if (issueTypes.contains(FormIssueType.SHOULDER_POSITION)) FormCheckStatus.WARNING else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.SHOULDER_POSITION)) "Elbows under shoulders" else null
            )
        )
        GymExerciseType.LUNGE -> listOf(
            FormChecklistItemData(
                "Front Knee",
                if (issueTypes.contains(FormIssueType.KNEE_TRACKING)) FormCheckStatus.ERROR else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.KNEE_TRACKING)) "Keep knee over ankle" else null
            ),
            FormChecklistItemData(
                "Torso Position",
                if (issueTypes.contains(FormIssueType.BACK_POSITION)) FormCheckStatus.WARNING else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.BACK_POSITION)) "Stay upright" else null
            ),
            FormChecklistItemData(
                "Depth",
                if (issueTypes.contains(FormIssueType.DEPTH)) FormCheckStatus.WARNING else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.DEPTH)) "Lower back knee more" else null
            )
        )
        GymExerciseType.BENCH_PRESS -> listOf(
            FormChecklistItemData(
                "Bar Path",
                if (issueTypes.contains(FormIssueType.BAR_PATH)) FormCheckStatus.ERROR else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.BAR_PATH)) "Keep bar over chest" else null
            ),
            FormChecklistItemData(
                "Elbow Position",
                if (issueTypes.contains(FormIssueType.ELBOW_POSITION)) FormCheckStatus.WARNING else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.ELBOW_POSITION)) "Tuck elbows at 45°" else null
            ),
            FormChecklistItemData(
                "Lockout",
                if (issueTypes.contains(FormIssueType.LOCKOUT)) FormCheckStatus.WARNING else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.LOCKOUT)) "Fully extend arms at top" else null
            )
        )
        GymExerciseType.OVERHEAD_PRESS -> listOf(
            FormChecklistItemData(
                "Bar Path",
                if (issueTypes.contains(FormIssueType.BAR_PATH)) FormCheckStatus.ERROR else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.BAR_PATH)) "Press straight up" else null
            ),
            FormChecklistItemData(
                "Back Position",
                if (issueTypes.contains(FormIssueType.BACK_POSITION)) FormCheckStatus.ERROR else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.BACK_POSITION)) "Don't lean back excessively" else null
            ),
            FormChecklistItemData(
                "Lockout",
                if (issueTypes.contains(FormIssueType.LOCKOUT)) FormCheckStatus.WARNING else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.LOCKOUT)) "Lock out overhead" else null
            )
        )
        GymExerciseType.BARBELL_ROW -> listOf(
            FormChecklistItemData(
                "Back Position",
                if (issueTypes.contains(FormIssueType.BACK_POSITION)) FormCheckStatus.ERROR else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.BACK_POSITION)) "Keep back flat" else null
            ),
            FormChecklistItemData(
                "Hip Hinge",
                if (issueTypes.contains(FormIssueType.HIP_POSITION)) FormCheckStatus.WARNING else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.HIP_POSITION)) "Maintain hip hinge" else null
            ),
            FormChecklistItemData(
                "Elbow Position",
                if (issueTypes.contains(FormIssueType.ELBOW_POSITION)) FormCheckStatus.WARNING else FormCheckStatus.GOOD,
                if (issueTypes.contains(FormIssueType.ELBOW_POSITION)) "Pull elbows back" else null
            )
        )
    }
}

@Composable
private fun FormChecklistItem(
    bodyPart: String,
    status: FormCheckStatus,
    tip: String?
) {
    val (icon, color) = when (status) {
        FormCheckStatus.GOOD -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        FormCheckStatus.WARNING -> Icons.Default.Warning to Color(0xFFFF9800)
        FormCheckStatus.ERROR -> Icons.Default.Cancel to Color(0xFFF44336)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bodyPart,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            tip?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
