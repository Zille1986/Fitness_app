package com.runtracker.app.ui.screens.body

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import coil.compose.AsyncImage
import com.runtracker.app.ui.screens.form.CameraPreviewWithPoseOverlay
import com.runtracker.app.ui.screens.form.PoseOverlay
import com.runtracker.shared.ai.PoseData
import com.runtracker.shared.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyAnalysisScreen(
    viewModel: BodyAnalysisViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onCreatePlan: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Debug logging
    LaunchedEffect(uiState.analysisPhase) {
        android.util.Log.d("BodyAnalysis", "UI Phase changed to: ${uiState.analysisPhase}")
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        android.util.Log.d("BodyAnalysis", "Gallery result: uri=$uri")
        uri?.let { viewModel.setPhotoUri(it) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body Analyzer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.scanHistory.isNotEmpty()) {
                        IconButton(onClick = { viewModel.toggleHistorySheet() }) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState.analysisPhase) {
                AnalysisPhase.GOAL_SELECTION -> {
                    GoalSelectionContent(
                        selectedGoal = uiState.selectedGoal,
                        onGoalSelect = { viewModel.setSelectedGoal(it) },
                        onContinue = { viewModel.startLiveCamera() },
                        onSelectFromGallery = { galleryLauncher.launch("image/*") },
                        scanHistory = uiState.scanHistory,
                        onViewHistory = { viewModel.toggleHistorySheet() }
                    )
                }
                
                AnalysisPhase.PHOTO_CAPTURE, AnalysisPhase.PHOTO_SELECTED -> {
                    PhotoCaptureContent(
                        photoUri = uiState.photoUri,
                        selectedGoal = uiState.selectedGoal,
                        onSelectPhoto = { galleryLauncher.launch("image/*") },
                        onStartAnalysis = { viewModel.startAnalysis() },
                        onBack = { viewModel.resetToCapture() }
                    )
                }
                
                AnalysisPhase.LIVE_CAMERA, AnalysisPhase.POSITIONING, AnalysisPhase.COLLECTING_DATA -> {
                    LiveCameraContent(
                        positionStatus = uiState.positionStatus,
                        positionProgress = uiState.positionProgress,
                        framesCollected = uiState.framesCollected,
                        requiredFrames = 30,
                        currentPose = uiState.currentPose,
                        isCollecting = uiState.analysisPhase == AnalysisPhase.COLLECTING_DATA,
                        onPoseDetected = { viewModel.onPoseDetected(it) },
                        onCancel = { viewModel.stopLiveCamera() }
                    )
                }
                
                AnalysisPhase.ANALYZING -> {
                    AnalyzingContent(
                        progress = uiState.analysisProgress,
                        status = uiState.analysisStatus
                    )
                }
                
                AnalysisPhase.RESULTS -> {
                    uiState.analysisResult?.let { result ->
                        ResultsContent(
                            result = result,
                            comparison = uiState.scanComparison,
                            progressSummary = uiState.progressSummary,
                            onNewScan = { viewModel.resetToCapture() },
                            onViewHistory = { viewModel.toggleHistorySheet() },
                            onCreatePlan = onCreatePlan
                        )
                    }
                }
            }
        }
    }
    
    // History Bottom Sheet
    if (uiState.showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleHistorySheet() }
        ) {
            ScanHistorySheet(
                scanHistory = uiState.scanHistory,
                scoreHistory = uiState.scoreHistory,
                progressSummary = uiState.progressSummary,
                onScanClick = { scanId -> 
                    viewModel.viewScanDetails(scanId)
                    viewModel.toggleHistorySheet()
                },
                onDismiss = { viewModel.toggleHistorySheet() }
            )
        }
    }
}

@Composable
fun GoalSelectionContent(
    selectedGoal: FitnessGoal,
    onGoalSelect: (FitnessGoal) -> Unit,
    onContinue: () -> Unit,
    onSelectFromGallery: () -> Unit,
    scanHistory: List<BodyScanWithDetails>,
    onViewHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Body Analysis",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Get personalized workout and nutrition recommendations based on your body analysis",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        item {
            Text(
                text = "What's your primary goal?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(FitnessGoal.values().toList()) { goal ->
            GoalCard(
                goal = goal,
                isSelected = goal == selectedGoal,
                onClick = { onGoalSelect(goal) }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Live Scan", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onSelectFromGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select from Gallery", style = MaterialTheme.typography.bodyLarge)
            }
        }
        
        // Previous scans summary
        if (scanHistory.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewHistory() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Previous Scans",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${scanHistory.size} scans recorded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun LiveCameraContent(
    positionStatus: PositionStatus,
    positionProgress: Float,
    framesCollected: Int,
    requiredFrames: Int,
    currentPose: PoseData?,
    isCollecting: Boolean,
    onPoseDetected: (PoseData) -> Unit,
    onCancel: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview with pose overlay
        CameraPreviewWithPoseOverlay(
            modifier = Modifier.fillMaxSize(),
            onPoseDetected = onPoseDetected,
            onError = { /* Handle error */ },
            isAnalyzing = true,
            useFrontCamera = true
        )
        
        // Positioning frame overlay
        PositioningFrameOverlay(
            positionStatus = positionStatus,
            positionProgress = positionProgress,
            isCollecting = isCollecting,
            framesCollected = framesCollected,
            requiredFrames = requiredFrames,
            modifier = Modifier.fillMaxSize()
        )
        
        // Status text at top
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val statusText = when {
                    isCollecting -> "Hold still... Scanning"
                    positionStatus == PositionStatus.READY -> "Perfect! Hold position..."
                    positionStatus == PositionStatus.NOT_DETECTED -> "Step into the frame"
                    positionStatus == PositionStatus.TOO_CLOSE -> "Step back - too close"
                    positionStatus == PositionStatus.TOO_FAR -> "Step closer - too far"
                    positionStatus == PositionStatus.NOT_CENTERED -> "Move to center"
                    positionStatus == PositionStatus.OUT_OF_FRAME -> "Full body must be visible"
                    else -> "Position yourself in the frame"
                }
                
                val statusColor = when {
                    isCollecting -> Color.Cyan
                    positionStatus == PositionStatus.READY -> Color.Green
                    else -> Color.Yellow
                }
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                
                if (isCollecting) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = framesCollected.toFloat() / requiredFrames,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Color.Cyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$framesCollected / $requiredFrames frames",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                } else if (positionStatus == PositionStatus.READY && positionProgress > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = positionProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Color.Green
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Starting in ${((1 - positionProgress) * 2).toInt() + 1}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        // Cancel button at bottom
        Button(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancel")
        }
    }
}

@Composable
fun PositioningFrameOverlay(
    positionStatus: PositionStatus,
    positionProgress: Float,
    isCollecting: Boolean,
    framesCollected: Int,
    requiredFrames: Int,
    modifier: Modifier = Modifier
) {
    val frameColor = when {
        isCollecting -> Color.Cyan
        positionStatus == PositionStatus.READY -> Color.Green
        else -> Color.White.copy(alpha = 0.6f)
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (positionStatus == PositionStatus.READY) positionProgress else 0f,
        label = "progress"
    )
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Define the target frame (where user should stand)
        val frameLeft = width * 0.15f
        val frameRight = width * 0.85f
        val frameTop = height * 0.05f
        val frameBottom = height * 0.95f
        val frameWidth = frameRight - frameLeft
        val frameHeight = frameBottom - frameTop
        
        val cornerLength = 40f
        val strokeWidth = 4f
        
        // Draw corner brackets
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        
        // Top-left corner
        drawLine(
            color = frameColor,
            start = Offset(frameLeft, frameTop),
            end = Offset(frameLeft + cornerLength, frameTop),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = frameColor,
            start = Offset(frameLeft, frameTop),
            end = Offset(frameLeft, frameTop + cornerLength),
            strokeWidth = strokeWidth
        )
        
        // Top-right corner
        drawLine(
            color = frameColor,
            start = Offset(frameRight, frameTop),
            end = Offset(frameRight - cornerLength, frameTop),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = frameColor,
            start = Offset(frameRight, frameTop),
            end = Offset(frameRight, frameTop + cornerLength),
            strokeWidth = strokeWidth
        )
        
        // Bottom-left corner
        drawLine(
            color = frameColor,
            start = Offset(frameLeft, frameBottom),
            end = Offset(frameLeft + cornerLength, frameBottom),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = frameColor,
            start = Offset(frameLeft, frameBottom),
            end = Offset(frameLeft, frameBottom - cornerLength),
            strokeWidth = strokeWidth
        )
        
        // Bottom-right corner
        drawLine(
            color = frameColor,
            start = Offset(frameRight, frameBottom),
            end = Offset(frameRight - cornerLength, frameBottom),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = frameColor,
            start = Offset(frameRight, frameBottom),
            end = Offset(frameRight, frameBottom - cornerLength),
            strokeWidth = strokeWidth
        )
        
        // Draw dashed lines connecting corners when not ready
        if (positionStatus != PositionStatus.READY && !isCollecting) {
            // Top line
            drawLine(
                color = frameColor.copy(alpha = 0.3f),
                start = Offset(frameLeft + cornerLength, frameTop),
                end = Offset(frameRight - cornerLength, frameTop),
                strokeWidth = 2f,
                pathEffect = dashEffect
            )
            // Bottom line
            drawLine(
                color = frameColor.copy(alpha = 0.3f),
                start = Offset(frameLeft + cornerLength, frameBottom),
                end = Offset(frameRight - cornerLength, frameBottom),
                strokeWidth = 2f,
                pathEffect = dashEffect
            )
            // Left line
            drawLine(
                color = frameColor.copy(alpha = 0.3f),
                start = Offset(frameLeft, frameTop + cornerLength),
                end = Offset(frameLeft, frameBottom - cornerLength),
                strokeWidth = 2f,
                pathEffect = dashEffect
            )
            // Right line
            drawLine(
                color = frameColor.copy(alpha = 0.3f),
                start = Offset(frameRight, frameTop + cornerLength),
                end = Offset(frameRight, frameBottom - cornerLength),
                strokeWidth = 2f,
                pathEffect = dashEffect
            )
        }
        
        // Draw progress ring when in position
        if (positionStatus == PositionStatus.READY && animatedProgress > 0 && !isCollecting) {
            val centerX = width / 2
            val centerY = height / 2
            val radius = 60f
            
            // Background circle
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 8f)
            )
            
            // Progress arc
            drawArc(
                color = Color.Green,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = 8f)
            )
        }
        
        // Draw scanning animation when collecting
        if (isCollecting) {
            val scanProgress = framesCollected.toFloat() / requiredFrames
            val scanLineY = frameTop + (frameHeight * scanProgress)
            
            drawLine(
                color = Color.Cyan,
                start = Offset(frameLeft, scanLineY),
                end = Offset(frameRight, scanLineY),
                strokeWidth = 3f
            )
        }
    }
}

@Composable
fun GoalCard(
    goal: FitnessGoal,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Goal icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (goal) {
                        FitnessGoal.LOSE_WEIGHT -> Icons.Default.TrendingDown
                        FitnessGoal.BUILD_MUSCLE -> Icons.Default.FitnessCenter
                        FitnessGoal.TONE_UP -> Icons.Default.AutoAwesome
                        FitnessGoal.IMPROVE_ENDURANCE -> Icons.Default.DirectionsRun
                        FitnessGoal.INCREASE_STRENGTH -> Icons.Default.Shield
                        FitnessGoal.GENERAL_FITNESS -> Icons.Default.Favorite
                        FitnessGoal.ATHLETIC_PERFORMANCE -> Icons.Default.EmojiEvents
                        FitnessGoal.BODY_RECOMPOSITION -> Icons.Default.SwapVert
                    },
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PhotoCaptureContent(
    photoUri: Uri?,
    selectedGoal: FitnessGoal,
    onSelectPhoto: () -> Unit,
    onStartAnalysis: () -> Unit,
    onBack: () -> Unit
) {
    // Debug: Log when this composable is rendered
    LaunchedEffect(photoUri) {
        android.util.Log.d("BodyAnalysis", "PhotoCaptureContent rendered: photoUri=$photoUri")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status banner when photo is selected
        if (photoUri != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Photo ready! Tap 'Analyze' below to start",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Goal reminder
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Goal: ${selectedGoal.displayName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Photo preview or placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clickable { onSelectPhoto() },
            shape = RoundedCornerShape(20.dp)
        ) {
            if (photoUri != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Selected photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Show a checkmark overlay to confirm photo is loaded
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(Color.Green, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tap to select a photo",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Only show tips if no photo selected yet
        if (photoUri == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tips for best results:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TipItem("Stand in a well-lit area")
                    TipItem("Wear form-fitting clothes")
                    TipItem("Stand straight with arms relaxed")
                    TipItem("Include your full body in the frame")
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back")
            }
            
            Button(
                onClick = onStartAnalysis,
                modifier = Modifier.weight(2f),
                enabled = photoUri != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze")
            }
        }
    }
}

@Composable
fun TipItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AnalyzingContent(
    progress: Float,
    status: String
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated scanning indicator
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.size(120.dp),
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Analyzing Your Body",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ResultsContent(
    result: BodyAnalysisResult,
    comparison: BodyScanComparison?,
    progressSummary: BodyProgressSummary?,
    onNewScan: () -> Unit,
    onViewHistory: () -> Unit,
    onCreatePlan: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall Score Card
        item {
            OverallScoreCard(
                score = result.scan.overallScore,
                bodyType = result.scan.bodyType,
                goal = result.scan.userGoal,
                bodyFat = result.scan.estimatedBodyFatPercentage
            )
        }
        
        // Comparison with previous (if available)
        comparison?.let { comp ->
            if (comp.previousScan != null) {
                item {
                    ComparisonCard(comparison = comp)
                }
            }
        }
        
        // Focus Zones
        if (result.zoneRecommendations.isNotEmpty()) {
            item {
                Text(
                    text = "Focus Zones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(result.zoneRecommendations) { zone ->
                        ZoneCard(recommendation = zone)
                    }
                }
            }
        }
        
        // Workout Recommendations
        item {
            Text(
                text = "Workout Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(result.workoutRecommendations) { workout ->
            WorkoutRecommendationCard(recommendation = workout)
        }
        
        // Nutrition Recommendations
        item {
            Text(
                text = "Nutrition Tips",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(result.nutritionRecommendations) { nutrition ->
            NutritionRecommendationCard(recommendation = nutrition)
        }
        
        // Weekly Plan
        item {
            WeeklyPlanCard(plan = result.weeklyPlanSuggestion)
        }
        
        // Posture Assessment
        if (result.scan.postureAssessment.issues.isNotEmpty()) {
            item {
                PostureAssessmentCard(assessment = result.scan.postureAssessment)
            }
        }
        
        // Create Plan Button
        item {
            Button(
                onClick = onCreatePlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create 3-Month Workout Plan", fontWeight = FontWeight.Bold)
            }
        }
        
        // Action buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("History")
                }
                
                OutlinedButton(
                    onClick = onNewScan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Scan")
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun OverallScoreCard(
    score: Int,
    bodyType: BodyType,
    goal: FitnessGoal,
    bodyFat: Float?
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
            containerColor = scoreColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Fitness Readiness Score",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.2f))
                    .border(4.dp, scoreColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Body info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = bodyType.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Body Type",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                bodyFat?.let {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${String.format("%.1f", it)}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Est. Body Fat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = goal.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Goal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ComparisonCard(comparison: BodyScanComparison) {
    val isImproved = comparison.scoreChange > 0
    val isDeclined = comparison.scoreChange < 0
    var isExpanded by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with score change
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Progress Comparison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "vs ${comparison.daysBetween} days ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isImproved -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    isDeclined -> Color(0xFFF44336).copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
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
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    isImproved -> "+${comparison.scoreChange}"
                                    isDeclined -> "${comparison.scoreChange}"
                                    else -> "±0"
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Summary message
            if (comparison.summaryMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = comparison.summaryMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    // Improvements section
                    if (comparison.improvements.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "✅ Improvements",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        comparison.improvements.forEach { item ->
                            ImprovementItemRow(item = item, isPositive = true)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    
                    // Declines section
                    if (comparison.declines.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "⚠️ Needs Attention",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        comparison.declines.forEach { item ->
                            ImprovementItemRow(item = item, isPositive = false)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    
                    // Unchanged section (collapsed by default)
                    if (comparison.unchanged.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "➖ Unchanged",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = comparison.unchanged.joinToString(", ") { it.title.replace(" Stable", "").replace(" Unchanged", "") },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Recommendations
                    if (comparison.recommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "💡 Recommendations",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        comparison.recommendations.forEachIndexed { index, rec ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = rec,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImprovementItemRow(item: ImprovementItem, isPositive: Boolean) {
    val backgroundColor = if (isPositive) {
        Color(0xFF4CAF50).copy(alpha = 0.08f)
    } else {
        Color(0xFFF44336).copy(alpha = 0.08f)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon
        Text(
            text = item.category.icon,
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Value change
        if (item.previousValue != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.currentValue,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text(
                    text = "was ${item.previousValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ZoneCard(recommendation: ZoneRecommendation) {
    val priorityColor = when (recommendation.priority) {
        ZonePriority.HIGH -> Color(0xFFF44336)
        ZonePriority.MEDIUM -> Color(0xFFFF9800)
        ZonePriority.LOW -> Color(0xFF4CAF50)
    }
    
    Card(
        modifier = Modifier.width(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = priorityColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recommendation.zone.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = priorityColor
                ) {
                    Text(
                        text = recommendation.priority.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = recommendation.currentAssessment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${recommendation.weeklyFrequency}x/week",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = priorityColor
            )
        }
    }
}

@Composable
fun WorkoutRecommendationCard(recommendation: WorkoutRecommendation) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${recommendation.frequency} • ${recommendation.duration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    recommendation.exercises.forEach { exercise ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${exercise.sets} x ${exercise.reps}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NutritionRecommendationCard(recommendation: NutritionRecommendation) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recommendation.category.icon,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = recommendation.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    recommendation.tips.forEach { tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyPlanCard(plan: WeeklyPlanSuggestion) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Weekly Plan: ${plan.splitType.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBadge(value = "${plan.totalTrainingDays}", label = "Training")
                StatBadge(value = "${plan.strengthSessions}", label = "Strength")
                StatBadge(value = "${plan.cardioSessions}", label = "Cardio")
                StatBadge(value = "${plan.restDays}", label = "Rest")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            plan.dailyBreakdown.forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = day.dayOfWeek,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(
                        text = day.focus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (day.isRestDay) MaterialTheme.colorScheme.onSurfaceVariant 
                               else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (day.duration.isNotEmpty()) {
                        Text(
                            text = day.duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatBadge(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PostureAssessmentCard(assessment: PostureAssessment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Straighten,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Posture Assessment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            assessment.issues.forEach { issue ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = when (issue.severity) {
                            IssueSeverity.SEVERE -> Color(0xFFF44336)
                            IssueSeverity.MODERATE -> Color(0xFFFF9800)
                            IssueSeverity.MILD -> Color(0xFFFFC107)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = issue.type.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Exercises: ${issue.exercises.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistorySheet(
    scanHistory: List<BodyScanWithDetails>,
    scoreHistory: List<Pair<Long, Int>>,
    progressSummary: BodyProgressSummary?,
    onScanClick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Scan History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress summary
        progressSummary?.let { summary ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${summary.totalScans}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Scans",
                            style = MaterialTheme.typography.labelSmall
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
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val color = if (summary.scoreImprovement >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        Text(
                            text = if (summary.scoreImprovement >= 0) "+${summary.scoreImprovement}" else "${summary.scoreImprovement}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Score trend chart
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
        
        // Scan list
        Text(
            text = "All Scans",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (scanHistory.isEmpty()) {
            Text(
                text = "No scans yet. Take your first body scan to start tracking progress!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            scanHistory.forEach { scanWithDetails ->
                ScanHistoryItem(
                    scanWithDetails = scanWithDetails,
                    onClick = { onScanClick(scanWithDetails.scan.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ScanHistoryItem(
    scanWithDetails: BodyScanWithDetails,
    onClick: () -> Unit
) {
    val scoreColor = when {
        scanWithDetails.scan.overallScore >= 80 -> Color(0xFF4CAF50)
        scanWithDetails.scan.overallScore >= 60 -> Color(0xFF8BC34A)
        scanWithDetails.scan.overallScore >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${scanWithDetails.scan.overallScore}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scanWithDetails.goalDisplayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = scanWithDetails.formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${scanWithDetails.focusZoneCount} zones",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (scanWithDetails.hasPostureIssues) {
                    Text(
                        text = "Posture issues",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9800)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
