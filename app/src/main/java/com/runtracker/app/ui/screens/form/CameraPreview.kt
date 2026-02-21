package com.runtracker.app.ui.screens.form

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.runtracker.shared.ai.FormIssue
import com.runtracker.shared.ai.FormIssueType
import com.runtracker.shared.ai.GymExerciseType
import com.runtracker.shared.ai.IssueSeverity
import com.runtracker.shared.ai.Point3D
import com.runtracker.shared.ai.PoseData
import com.runtracker.shared.ai.PoseLandmark
import java.util.concurrent.Executors

@Composable
fun CameraPreviewWithPoseOverlay(
    modifier: Modifier = Modifier,
    onPoseDetected: (PoseData) -> Unit,
    onFrameCaptured: ((android.graphics.Bitmap) -> Unit)? = null,
    onError: (String) -> Unit,
    isAnalyzing: Boolean,
    useFrontCamera: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var currentPoseData by remember { mutableStateOf<PoseData?>(null) }
    
    // Use rememberUpdatedState to avoid recreating the helper when callbacks change
    val currentOnPoseDetected by rememberUpdatedState(onPoseDetected)
    val currentOnFrameCaptured by rememberUpdatedState(onFrameCaptured)
    val currentOnError by rememberUpdatedState(onError)
    
    // Create the helper only once and keep it stable
    val poseDetectorHelper = remember(context) {
        PoseDetectorHelper(
            context = context,
            onPoseDetected = { poseData ->
                currentPoseData = poseData
                currentOnPoseDetected(poseData)
            },
            onError = { error ->
                Log.e("CameraPreview", "Pose detection error: $error")
                currentOnError(error)
            },
            onFrameCaptured = { bitmap ->
                currentOnFrameCaptured?.invoke(bitmap)
            }
        )
    }
    
    // Only dispose when the composable is truly removed from composition
    DisposableEffect(poseDetectorHelper) {
        Log.d("CameraPreview", "PoseDetectorHelper created")
        onDispose {
            Log.d("CameraPreview", "PoseDetectorHelper disposing")
            poseDetectorHelper.close()
        }
    }
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                    
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(
                                Executors.newSingleThreadExecutor(),
                                poseDetectorHelper.imageAnalyzer
                            )
                        }
                    
                    val cameraSelector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Camera binding failed", e)
                        onError("Camera binding failed: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Pose overlay
        if (isAnalyzing) {
            currentPoseData?.let { poseData ->
                PoseOverlay(
                    poseData = poseData,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun PoseOverlay(
    poseData: PoseData,
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    showPoints: Boolean = true
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        drawPoseSkeleton(poseData, width, height, color, showPoints)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPoseSkeleton(
    poseData: PoseData,
    width: Float,
    height: Float,
    lineColor: Color,
    showPoints: Boolean
) {
    // Draw skeleton connections - simplified for side view
    val connections = listOf(
        // Head to shoulders (use midpoint of shoulders conceptually)
        Pair(PoseLandmark.NOSE, PoseLandmark.LEFT_SHOULDER),
        // Torso - for side view, left and right overlap so just draw one side
        Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
        // Left arm
        Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW),
        Pair(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
        // Left leg
        Pair(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE),
        Pair(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
    )
    
    // Draw connections
    connections.forEach { (start, end) ->
        val startPoint = poseData.landmarks[start]
        val endPoint = poseData.landmarks[end]
        
        if (startPoint != null && endPoint != null) {
            drawLine(
                color = lineColor,
                start = Offset(startPoint.x * width, startPoint.y * height),
                end = Offset(endPoint.x * width, endPoint.y * height),
                strokeWidth = 4f
            )
        }
    }
    
    // Draw landmark points
    if (showPoints) {
        poseData.landmarks.forEach { (_, point) ->
            drawCircle(
                color = lineColor,
                radius = 8f,
                center = Offset(point.x * width, point.y * height)
            )
            drawCircle(
                color = Color.White,
                radius = 8f,
                center = Offset(point.x * width, point.y * height),
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
fun FormComparisonOverlay(
    userPose: PoseData,
    exerciseType: GymExerciseType,
    issues: List<FormIssue>,
    modifier: Modifier = Modifier
) {
    val idealPose = remember(exerciseType) { getIdealPoseForExercise(exerciseType) }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Draw ideal pose in cyan (semi-transparent)
        drawPoseSkeleton(idealPose, width, height, Color.Cyan.copy(alpha = 0.6f), showPoints = false)
        
        // Draw user pose - color based on issues
        val userColor = when {
            issues.isEmpty() -> Color.Green
            issues.any { it.severity == IssueSeverity.HIGH } -> Color.Red
            issues.any { it.severity == IssueSeverity.MEDIUM } -> Color.Yellow
            else -> Color.Green
        }
        drawPoseSkeleton(userPose, width, height, userColor, showPoints = true)
        
        // Highlight problem areas with circles
        issues.forEach { issue ->
            val problemLandmarks = getProblemLandmarksForIssue(issue.type)
            problemLandmarks.forEach { landmark ->
                userPose.landmarks[landmark]?.let { point ->
                    // Draw attention circle around problem area
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.3f),
                        radius = 30f,
                        center = Offset(point.x * width, point.y * height)
                    )
                    drawCircle(
                        color = Color.Red,
                        radius = 30f,
                        center = Offset(point.x * width, point.y * height),
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
    }
}

private fun getIdealPoseForExercise(exerciseType: GymExerciseType): PoseData {
    // Return ideal pose landmarks for each exercise (normalized 0-1 coordinates)
    // All poses are designed for SIDE VIEW (user films from the side)
    // X = horizontal position (0=left, 1=right), Y = vertical position (0=top, 1=bottom)
    val landmarks = when (exerciseType) {
        GymExerciseType.PUSH_UP -> mapOf(
            // Side view of push-up bottom position
            // Person facing right, body nearly horizontal, hands on ground
            // Y increases downward, so ground is at higher Y values
            PoseLandmark.NOSE to Point3D(0.15f, 0.45f, 0f),           // Head
            PoseLandmark.LEFT_SHOULDER to Point3D(0.25f, 0.48f, 0f),  // Shoulder near head
            PoseLandmark.RIGHT_SHOULDER to Point3D(0.25f, 0.48f, 0f),
            PoseLandmark.LEFT_ELBOW to Point3D(0.22f, 0.62f, 0f),     // Elbow below shoulder
            PoseLandmark.RIGHT_ELBOW to Point3D(0.22f, 0.62f, 0f),
            PoseLandmark.LEFT_WRIST to Point3D(0.25f, 0.75f, 0f),     // Hands on ground (high Y)
            PoseLandmark.RIGHT_WRIST to Point3D(0.25f, 0.75f, 0f),
            PoseLandmark.LEFT_HIP to Point3D(0.55f, 0.48f, 0f),       // Hip in line with shoulders
            PoseLandmark.RIGHT_HIP to Point3D(0.55f, 0.48f, 0f),
            PoseLandmark.LEFT_KNEE to Point3D(0.75f, 0.50f, 0f),      // Knees straight
            PoseLandmark.RIGHT_KNEE to Point3D(0.75f, 0.50f, 0f),
            PoseLandmark.LEFT_ANKLE to Point3D(0.92f, 0.52f, 0f),     // Feet at end
            PoseLandmark.RIGHT_ANKLE to Point3D(0.92f, 0.52f, 0f)
        )
        GymExerciseType.SQUAT -> mapOf(
            // Side view of squat bottom position - thighs parallel, back straight
            PoseLandmark.NOSE to Point3D(0.35f, 0.15f, 0f),
            PoseLandmark.LEFT_SHOULDER to Point3D(0.40f, 0.25f, 0f),
            PoseLandmark.RIGHT_SHOULDER to Point3D(0.40f, 0.25f, 0f),
            PoseLandmark.LEFT_ELBOW to Point3D(0.35f, 0.35f, 0f),
            PoseLandmark.RIGHT_ELBOW to Point3D(0.35f, 0.35f, 0f),
            PoseLandmark.LEFT_WRIST to Point3D(0.40f, 0.40f, 0f),
            PoseLandmark.RIGHT_WRIST to Point3D(0.40f, 0.40f, 0f),
            PoseLandmark.LEFT_HIP to Point3D(0.55f, 0.50f, 0f),
            PoseLandmark.RIGHT_HIP to Point3D(0.55f, 0.50f, 0f),
            PoseLandmark.LEFT_KNEE to Point3D(0.40f, 0.65f, 0f),
            PoseLandmark.RIGHT_KNEE to Point3D(0.40f, 0.65f, 0f),
            PoseLandmark.LEFT_ANKLE to Point3D(0.50f, 0.90f, 0f),
            PoseLandmark.RIGHT_ANKLE to Point3D(0.50f, 0.90f, 0f)
        )
        GymExerciseType.DEADLIFT -> mapOf(
            // Side view of deadlift mid-pull - hip hinge, flat back
            PoseLandmark.NOSE to Point3D(0.30f, 0.20f, 0f),
            PoseLandmark.LEFT_SHOULDER to Point3D(0.35f, 0.30f, 0f),
            PoseLandmark.RIGHT_SHOULDER to Point3D(0.35f, 0.30f, 0f),
            PoseLandmark.LEFT_ELBOW to Point3D(0.40f, 0.45f, 0f),
            PoseLandmark.RIGHT_ELBOW to Point3D(0.40f, 0.45f, 0f),
            PoseLandmark.LEFT_WRIST to Point3D(0.42f, 0.60f, 0f),
            PoseLandmark.RIGHT_WRIST to Point3D(0.42f, 0.60f, 0f),
            PoseLandmark.LEFT_HIP to Point3D(0.55f, 0.45f, 0f),
            PoseLandmark.RIGHT_HIP to Point3D(0.55f, 0.45f, 0f),
            PoseLandmark.LEFT_KNEE to Point3D(0.50f, 0.65f, 0f),
            PoseLandmark.RIGHT_KNEE to Point3D(0.50f, 0.65f, 0f),
            PoseLandmark.LEFT_ANKLE to Point3D(0.55f, 0.90f, 0f),
            PoseLandmark.RIGHT_ANKLE to Point3D(0.55f, 0.90f, 0f)
        )
        GymExerciseType.PLANK -> mapOf(
            // Side view of plank - straight line from head to heels
            PoseLandmark.NOSE to Point3D(0.12f, 0.35f, 0f),
            PoseLandmark.LEFT_SHOULDER to Point3D(0.20f, 0.40f, 0f),
            PoseLandmark.RIGHT_SHOULDER to Point3D(0.20f, 0.40f, 0f),
            PoseLandmark.LEFT_ELBOW to Point3D(0.20f, 0.55f, 0f),
            PoseLandmark.RIGHT_ELBOW to Point3D(0.20f, 0.55f, 0f),
            PoseLandmark.LEFT_HIP to Point3D(0.50f, 0.40f, 0f),
            PoseLandmark.RIGHT_HIP to Point3D(0.50f, 0.40f, 0f),
            PoseLandmark.LEFT_KNEE to Point3D(0.72f, 0.42f, 0f),
            PoseLandmark.RIGHT_KNEE to Point3D(0.72f, 0.42f, 0f),
            PoseLandmark.LEFT_ANKLE to Point3D(0.90f, 0.45f, 0f),
            PoseLandmark.RIGHT_ANKLE to Point3D(0.90f, 0.45f, 0f)
        )
        GymExerciseType.LUNGE -> mapOf(
            // Side view of lunge - front knee at 90 degrees
            PoseLandmark.NOSE to Point3D(0.45f, 0.10f, 0f),
            PoseLandmark.LEFT_SHOULDER to Point3D(0.45f, 0.20f, 0f),
            PoseLandmark.RIGHT_SHOULDER to Point3D(0.45f, 0.20f, 0f),
            PoseLandmark.LEFT_ELBOW to Point3D(0.45f, 0.35f, 0f),
            PoseLandmark.RIGHT_ELBOW to Point3D(0.45f, 0.35f, 0f),
            PoseLandmark.LEFT_WRIST to Point3D(0.45f, 0.45f, 0f),
            PoseLandmark.RIGHT_WRIST to Point3D(0.45f, 0.45f, 0f),
            PoseLandmark.LEFT_HIP to Point3D(0.50f, 0.50f, 0f),
            PoseLandmark.RIGHT_HIP to Point3D(0.50f, 0.50f, 0f),
            PoseLandmark.LEFT_KNEE to Point3D(0.35f, 0.70f, 0f),
            PoseLandmark.RIGHT_KNEE to Point3D(0.65f, 0.70f, 0f),
            PoseLandmark.LEFT_ANKLE to Point3D(0.30f, 0.90f, 0f),
            PoseLandmark.RIGHT_ANKLE to Point3D(0.70f, 0.90f, 0f)
        )
        else -> mapOf(
            // Side view of standing pose for other exercises
            PoseLandmark.NOSE to Point3D(0.50f, 0.08f, 0f),
            PoseLandmark.LEFT_SHOULDER to Point3D(0.50f, 0.18f, 0f),
            PoseLandmark.RIGHT_SHOULDER to Point3D(0.50f, 0.18f, 0f),
            PoseLandmark.LEFT_ELBOW to Point3D(0.50f, 0.35f, 0f),
            PoseLandmark.RIGHT_ELBOW to Point3D(0.50f, 0.35f, 0f),
            PoseLandmark.LEFT_WRIST to Point3D(0.50f, 0.48f, 0f),
            PoseLandmark.RIGHT_WRIST to Point3D(0.50f, 0.48f, 0f),
            PoseLandmark.LEFT_HIP to Point3D(0.50f, 0.50f, 0f),
            PoseLandmark.RIGHT_HIP to Point3D(0.50f, 0.50f, 0f),
            PoseLandmark.LEFT_KNEE to Point3D(0.50f, 0.72f, 0f),
            PoseLandmark.RIGHT_KNEE to Point3D(0.50f, 0.72f, 0f),
            PoseLandmark.LEFT_ANKLE to Point3D(0.50f, 0.92f, 0f),
            PoseLandmark.RIGHT_ANKLE to Point3D(0.50f, 0.92f, 0f)
        )
    }
    return PoseData(landmarks = landmarks)
}

private fun getProblemLandmarksForIssue(issueType: FormIssueType): List<PoseLandmark> {
    return when (issueType) {
        FormIssueType.ELBOW_POSITION -> listOf(PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW)
        FormIssueType.HIP_POSITION -> listOf(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        FormIssueType.KNEE_TRACKING -> listOf(PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE)
        FormIssueType.BACK_POSITION -> listOf(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        FormIssueType.SHOULDER_POSITION -> listOf(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        FormIssueType.HEAD_POSITION -> listOf(PoseLandmark.NOSE)
        FormIssueType.FOOT_POSITION -> listOf(PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE)
        FormIssueType.WRIST_POSITION -> listOf(PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST)
        FormIssueType.HAND_POSITION -> listOf(PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST)
        FormIssueType.DEPTH -> listOf(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        FormIssueType.LOCKOUT -> listOf(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        FormIssueType.BAR_PATH -> listOf(PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST)
        else -> emptyList()
    }
}
