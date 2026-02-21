package com.runtracker.app.ui.screens.form

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.ai.FormAnalysisAIResult
import com.runtracker.app.ai.GeminiFormAnalyzer
import com.runtracker.shared.ai.*
import com.runtracker.shared.data.model.FormProgressSummary
import com.runtracker.shared.data.model.FormReview
import com.runtracker.shared.data.model.FormReviewComparison
import com.runtracker.shared.data.model.FormReviewMode
import com.runtracker.shared.data.model.FormReviewWithDetails
import com.runtracker.shared.data.repository.FormReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class FormAnalysisViewModel @Inject constructor(
    private val formReviewRepository: FormReviewRepository,
    private val geminiFormAnalyzer: GeminiFormAnalyzer
) : ViewModel() {

    private val formAnalysisEngine = FormAnalysisEngine()
    
    // Frame capture for AI analysis
    private val capturedFrames = mutableListOf<Bitmap>()
    private val maxCapturedFrames = 5 // Capture up to 5 frames for AI analysis
    
    private val poseDataBuffer = mutableListOf<PoseData>()
    private val maxBufferSize = 100 // Store more frames for movement detection
    
    // Movement detection
    private var movementHistory = mutableListOf<Float>() // Track vertical movement
    private var repsDetected = 0
    private var lastRepTime = 0L
    private var isInDownPhase = false
    
    // Countdown
    private var countdownJob: Job? = null
    
    // Minimum requirements for valid analysis
    private val minFramesForAnalysis = 30 // At least 3 seconds of data
    private val minRepsForGymAnalysis = 3 // At least 3 reps for reliable form analysis
    private val minRepsForRunningAnalysis = 50 // At least 5 seconds of running data
    
    // Auto-complete thresholds
    private val autoCompleteReps = 5 // Auto-analyze after 5 reps
    private val autoCompleteFrames = 80 // Or after 8 seconds of good data
    
    // Pose validity tracking
    private var consecutiveInvalidFrames = 0
    private val maxInvalidFramesBeforePause = 15 // ~1.5 seconds of no valid pose

    private val _uiState = MutableStateFlow(FormAnalysisUiState())
    val uiState: StateFlow<FormAnalysisUiState> = _uiState.asStateFlow()

    fun setMode(mode: FormAnalysisMode) {
        _uiState.update { 
            it.copy(
                mode = mode,
                lastRunningAnalysis = null,
                lastGymAnalysis = null,
                analysisPhase = AnalysisPhase.IDLE,
                framesCollected = 0,
                repsDetected = 0,
                dataQuality = DataQuality.INSUFFICIENT
            ) 
        }
        resetAnalysisState()
    }

    fun setGymExercise(exercise: GymExerciseType) {
        _uiState.update { it.copy(selectedGymExercise = exercise) }
    }

    fun startAnalysis() {
        resetAnalysisState()
        _uiState.update { 
            it.copy(
                isCameraActive = true,
                poseDetected = false,
                analysisPhase = AnalysisPhase.COUNTDOWN,
                countdownSeconds = 5,
                framesCollected = 0,
                repsDetected = 0,
                dataQuality = DataQuality.INSUFFICIENT,
                lastRunningAnalysis = null,
                lastGymAnalysis = null
            ) 
        }
        
        // Start countdown
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 5 downTo 1) {
                _uiState.update { it.copy(countdownSeconds = i) }
                delay(1000)
            }
            // Countdown finished - start collecting data
            _uiState.update { 
                it.copy(
                    analysisPhase = AnalysisPhase.COLLECTING,
                    isAnalyzing = true,
                    countdownSeconds = 0
                ) 
            }
        }
    }

    fun stopAnalysis() {
        countdownJob?.cancel()
        _uiState.update { 
            it.copy(
                isAnalyzing = false,
                isCameraActive = false,
                analysisPhase = AnalysisPhase.IDLE
            ) 
        }
    }
    
    private fun resetAnalysisState() {
        poseDataBuffer.clear()
        movementHistory.clear()
        capturedFrames.forEach { it.recycle() }
        capturedFrames.clear()
        repsDetected = 0
        lastRepTime = 0L
        isInDownPhase = false
        consecutiveInvalidFrames = 0
    }
    
    fun captureFrame(bitmap: Bitmap) {
        // Capture frames periodically for AI analysis
        if (capturedFrames.size < maxCapturedFrames && _uiState.value.analysisPhase == AnalysisPhase.COLLECTING) {
            // Only capture every ~20 frames to get variety
            if (poseDataBuffer.size % 20 == 0 || capturedFrames.isEmpty()) {
                capturedFrames.add(bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false))
                android.util.Log.d("FormAnalysis", "Captured frame ${capturedFrames.size}/$maxCapturedFrames")
            }
        }
    }
    
    fun onPoseDetected(poseData: PoseData) {
        val currentPhase = _uiState.value.analysisPhase
        
        // Always update pose for overlay during countdown or collecting
        if (currentPhase == AnalysisPhase.COUNTDOWN || currentPhase == AnalysisPhase.COLLECTING) {
            _uiState.update { it.copy(currentPose = poseData, poseDetected = true) }
        }
        
        // Only collect data during COLLECTING phase
        if (currentPhase != AnalysisPhase.COLLECTING) return
        
        // Check if pose is valid (has key landmarks)
        val isValidPose = isPoseValidForExercise(poseData)
        
        if (!isValidPose) {
            consecutiveInvalidFrames++
            // If too many invalid frames, user may have walked away - auto-complete if we have enough data
            if (consecutiveInvalidFrames >= maxInvalidFramesBeforePause && hasEnoughDataForAnalysis()) {
                android.util.Log.i("FormAnalysis", "User left frame with sufficient data - auto-completing analysis")
                autoCompleteAnalysis()
                return
            }
            return // Don't add invalid frames to buffer
        }
        
        // Reset invalid frame counter on valid pose
        consecutiveInvalidFrames = 0
        
        // Add to buffer
        poseDataBuffer.add(poseData)
        if (poseDataBuffer.size > maxBufferSize) {
            poseDataBuffer.removeAt(0)
        }
        
        val framesCollected = poseDataBuffer.size
        
        // Detect movement/reps for gym exercises
        if (_uiState.value.mode == FormAnalysisMode.GYM) {
            detectRepMovement(poseData)
        }
        
        // Calculate data quality
        val quality = calculateDataQuality(framesCollected)
        
        _uiState.update { 
            it.copy(
                framesCollected = framesCollected,
                repsDetected = repsDetected,
                dataQuality = quality
            ) 
        }
        
        // Auto-complete analysis when we have excellent data
        if (shouldAutoComplete()) {
            android.util.Log.i("FormAnalysis", "Auto-completing: reps=$repsDetected, frames=$framesCollected")
            autoCompleteAnalysis()
            return
        }
        
        // Analyze periodically (every 10 frames) but only show if quality is sufficient
        if (framesCollected >= minFramesForAnalysis && framesCollected % 10 == 0) {
            analyzeCurrentPose()
        }
    }
    
    private fun isPoseValidForExercise(poseData: PoseData): Boolean {
        // Check that key landmarks are present based on exercise type
        val hasBasicLandmarks = poseData.landmarks.containsKey(PoseLandmark.LEFT_SHOULDER) &&
                poseData.landmarks.containsKey(PoseLandmark.RIGHT_SHOULDER) &&
                poseData.landmarks.containsKey(PoseLandmark.LEFT_HIP) &&
                poseData.landmarks.containsKey(PoseLandmark.RIGHT_HIP)
        
        if (!hasBasicLandmarks) return false
        
        // For gym exercises, check exercise-specific landmarks
        return when (_uiState.value.selectedGymExercise) {
            GymExerciseType.PUSH_UP, GymExerciseType.PLANK -> {
                poseData.landmarks.containsKey(PoseLandmark.LEFT_ELBOW) &&
                poseData.landmarks.containsKey(PoseLandmark.LEFT_WRIST)
            }
            GymExerciseType.SQUAT, GymExerciseType.LUNGE, GymExerciseType.DEADLIFT -> {
                poseData.landmarks.containsKey(PoseLandmark.LEFT_KNEE) &&
                poseData.landmarks.containsKey(PoseLandmark.LEFT_ANKLE)
            }
            else -> true
        }
    }
    
    private fun hasEnoughDataForAnalysis(): Boolean {
        val exercise = _uiState.value.selectedGymExercise
        val isStaticExercise = exercise == GymExerciseType.PLANK
        val isRunning = _uiState.value.mode == FormAnalysisMode.RUNNING
        
        return when {
            isRunning -> poseDataBuffer.size >= minFramesForAnalysis
            isStaticExercise -> poseDataBuffer.size >= minFramesForAnalysis
            else -> repsDetected >= minRepsForGymAnalysis && poseDataBuffer.size >= minFramesForAnalysis
        }
    }
    
    private fun shouldAutoComplete(): Boolean {
        val exercise = _uiState.value.selectedGymExercise
        val isStaticExercise = exercise == GymExerciseType.PLANK
        val isRunning = _uiState.value.mode == FormAnalysisMode.RUNNING
        val framesCollected = poseDataBuffer.size
        
        return when {
            isRunning -> framesCollected >= autoCompleteFrames
            isStaticExercise -> framesCollected >= autoCompleteFrames
            else -> repsDetected >= autoCompleteReps || (repsDetected >= minRepsForGymAnalysis && framesCollected >= autoCompleteFrames)
        }
    }
    
    private fun autoCompleteAnalysis() {
        // Stop collecting and trigger final analysis
        captureAndAnalyze()
    }
    
    private fun detectRepMovement(poseData: PoseData) {
        // Track vertical position of key landmarks based on exercise
        val exercise = _uiState.value.selectedGymExercise
        val verticalPosition = when (exercise) {
            GymExerciseType.SQUAT, GymExerciseType.LUNGE -> {
                // Track hip height
                val leftHip = poseData.landmarks[PoseLandmark.LEFT_HIP]?.y ?: return
                val rightHip = poseData.landmarks[PoseLandmark.RIGHT_HIP]?.y ?: return
                (leftHip + rightHip) / 2
            }
            GymExerciseType.PUSH_UP, GymExerciseType.BENCH_PRESS -> {
                // Track shoulder height
                val leftShoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER]?.y ?: return
                val rightShoulder = poseData.landmarks[PoseLandmark.RIGHT_SHOULDER]?.y ?: return
                (leftShoulder + rightShoulder) / 2
            }
            GymExerciseType.DEADLIFT, GymExerciseType.BARBELL_ROW -> {
                // Track shoulder height
                val leftShoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER]?.y ?: return
                val rightShoulder = poseData.landmarks[PoseLandmark.RIGHT_SHOULDER]?.y ?: return
                (leftShoulder + rightShoulder) / 2
            }
            GymExerciseType.OVERHEAD_PRESS -> {
                // Track wrist height
                val leftWrist = poseData.landmarks[PoseLandmark.LEFT_WRIST]?.y ?: return
                val rightWrist = poseData.landmarks[PoseLandmark.RIGHT_WRIST]?.y ?: return
                (leftWrist + rightWrist) / 2
            }
            GymExerciseType.PLANK -> {
                // Plank is static - no rep detection needed
                return
            }
        }
        
        movementHistory.add(verticalPosition)
        if (movementHistory.size > 30) {
            movementHistory.removeAt(0)
        }
        
        // Need at least 10 frames to detect movement
        if (movementHistory.size < 10) return
        
        // Calculate movement range
        val recentMovement = movementHistory.takeLast(10)
        val minPos = recentMovement.minOrNull() ?: return
        val maxPos = recentMovement.maxOrNull() ?: return
        val range = maxPos - minPos
        
        // Detect rep based on movement threshold (varies by exercise)
        val movementThreshold = when (exercise) {
            GymExerciseType.SQUAT -> 0.15f
            GymExerciseType.PUSH_UP -> 0.08f
            GymExerciseType.DEADLIFT -> 0.12f
            GymExerciseType.BENCH_PRESS -> 0.06f
            GymExerciseType.OVERHEAD_PRESS -> 0.10f
            GymExerciseType.BARBELL_ROW -> 0.08f
            GymExerciseType.LUNGE -> 0.12f
            GymExerciseType.PLANK -> 0f
        }
        
        val currentPos = verticalPosition
        val avgPos = movementHistory.average().toFloat()
        
        // Simple rep detection: going down then up
        val currentTime = System.currentTimeMillis()
        if (currentPos > avgPos + movementThreshold * 0.5f && !isInDownPhase) {
            isInDownPhase = true
        } else if (currentPos < avgPos - movementThreshold * 0.3f && isInDownPhase) {
            // Completed a rep (went down and came back up)
            if (currentTime - lastRepTime > 500) { // Min 500ms between reps
                repsDetected++
                lastRepTime = currentTime
                isInDownPhase = false
            }
        }
    }
    
    private fun calculateDataQuality(framesCollected: Int): DataQuality {
        val isGym = _uiState.value.mode == FormAnalysisMode.GYM
        val exercise = _uiState.value.selectedGymExercise
        val isStaticExercise = exercise == GymExerciseType.PLANK
        
        return when {
            framesCollected < 10 -> DataQuality.INSUFFICIENT
            framesCollected < minFramesForAnalysis -> DataQuality.LOW
            isGym && !isStaticExercise && repsDetected < 1 -> DataQuality.LOW // Need at least 1 rep to start
            isGym && !isStaticExercise && repsDetected < minRepsForGymAnalysis -> DataQuality.MODERATE // Need 3 reps for good analysis
            framesCollected < 50 -> DataQuality.MODERATE
            isGym && !isStaticExercise && repsDetected < autoCompleteReps -> DataQuality.GOOD // 3-4 reps
            framesCollected < autoCompleteFrames -> DataQuality.GOOD
            else -> DataQuality.EXCELLENT // 5+ reps or 80+ frames
        }
    }
    
    private fun analyzeCurrentPose() {
        val latestPose = poseDataBuffer.lastOrNull() ?: return
        
        when (_uiState.value.mode) {
            FormAnalysisMode.AUTO_DETECT -> {
                // Auto-detect mode doesn't use real-time pose analysis
                // It waits for captureAndAnalyze to use Gemini AI
            }
            FormAnalysisMode.RUNNING -> {
                val analysis = formAnalysisEngine.analyzeForm(latestPose)
                _uiState.update { it.copy(lastRunningAnalysis = analysis) }
            }
            FormAnalysisMode.GYM -> {
                val exercise = _uiState.value.selectedGymExercise
                val analysis = formAnalysisEngine.analyzeGymForm(latestPose, exercise)
                _uiState.update { it.copy(lastGymAnalysis = analysis) }
            }
        }
    }
    
    fun captureAndAnalyze() {
        val latestPose = poseDataBuffer.lastOrNull()
        
        // If no pose data collected, show detailed error
        if (latestPose == null) {
            val errorCode = "ERR_POSE_001"
            val errorMessage = buildString {
                append("[$errorCode] Pose detection failed.\n\n")
                append("Debug info:\n")
                append("- Buffer size: ${poseDataBuffer.size}\n")
                append("- Phase: ${_uiState.value.analysisPhase}\n")
                append("- Camera active: ${_uiState.value.isCameraActive}\n")
                append("- Pose detected flag: ${_uiState.value.poseDetected}\n")
                append("- Frames collected: ${_uiState.value.framesCollected}\n")
                append("\nPossible causes:\n")
                append("- Camera permission not granted\n")
                append("- ML Kit pose detection failed to initialize\n")
                append("- Body not visible in camera frame\n")
                append("- Poor lighting conditions")
            }
            android.util.Log.e("FormAnalysis", errorMessage)
            _uiState.update { it.copy(error = errorMessage) }
            return
        }
        
        val quality = _uiState.value.dataQuality
        
        if (quality == DataQuality.INSUFFICIENT || quality == DataQuality.LOW) {
            val exercise = _uiState.value.selectedGymExercise
            val isStaticExercise = exercise == GymExerciseType.PLANK
            val errorCode = when {
                poseDataBuffer.size < minFramesForAnalysis -> "ERR_POSE_002"
                _uiState.value.mode == FormAnalysisMode.GYM && !isStaticExercise && repsDetected < 1 -> "ERR_POSE_003"
                else -> "ERR_POSE_004"
            }
            val message = when {
                poseDataBuffer.size < minFramesForAnalysis -> 
                    "[$errorCode] Insufficient frames. Collected: ${poseDataBuffer.size}, Required: $minFramesForAnalysis. Keep recording."
                _uiState.value.mode == FormAnalysisMode.GYM && !isStaticExercise && repsDetected < 1 ->
                    "[$errorCode] No reps detected yet. Perform at least $minRepsForGymAnalysis ${exercise.displayName}s."
                else -> "[$errorCode] Need more reps. Detected: $repsDetected, Required: $minRepsForGymAnalysis. Keep going!"
            }
            android.util.Log.w("FormAnalysis", message)
            _uiState.update { it.copy(error = message) }
            return
        }
        
        _uiState.update { it.copy(analysisPhase = AnalysisPhase.ANALYZING) }
        
        viewModelScope.launch {
            delay(500) // Brief delay to show "analyzing" state
            
            when (_uiState.value.mode) {
                FormAnalysisMode.AUTO_DETECT -> {
                    // Use AI to auto-detect exercise and analyze form
                    if (capturedFrames.isEmpty()) {
                        _uiState.update { 
                            it.copy(
                                error = "No frames captured for AI analysis. Try again with better lighting.",
                                isAnalyzing = false,
                                analysisPhase = AnalysisPhase.IDLE
                            ) 
                        }
                        return@launch
                    }
                    
                    val aiResult = geminiFormAnalyzer.analyzeExerciseFormAutoDetect(capturedFrames)
                    
                    when (aiResult) {
                        is FormAnalysisAIResult.Success -> {
                            val result = AutoDetectAnalysisResult(
                                exerciseDetected = aiResult.exerciseDetected,
                                exerciseCategory = aiResult.exerciseCategory,
                                overallScore = aiResult.overallScore,
                                formQuality = aiResult.formQuality,
                                issues = aiResult.issues,
                                positiveFeedback = aiResult.positiveFeedback,
                                keyTips = aiResult.keyTips,
                                confidence = aiResult.confidence,
                                repCount = aiResult.repCount
                            )
                            
                            _uiState.update { 
                                it.copy(
                                    autoDetectResult = result,
                                    detectedExerciseName = aiResult.exerciseDetected,
                                    isAnalyzing = false,
                                    isCameraActive = false,
                                    analysisPhase = AnalysisPhase.COMPLETE
                                ) 
                            }
                        }
                        is FormAnalysisAIResult.Error -> {
                            _uiState.update { 
                                it.copy(
                                    error = "AI analysis failed: ${aiResult.message}",
                                    isAnalyzing = false,
                                    analysisPhase = AnalysisPhase.IDLE
                                ) 
                            }
                        }
                    }
                }
                FormAnalysisMode.RUNNING -> {
                    val analysis = formAnalysisEngine.analyzeForm(latestPose)
                    // Save review and get comparison
                    val savedReview = formReviewRepository.saveRunningReview(analysis)
                    val comparison = formReviewRepository.compareWithPrevious(savedReview)
                    val progressSummary = formReviewRepository.getProgressSummary(FormReviewMode.RUNNING)
                    val reviewHistory = formReviewRepository.getReviewsWithDetails(FormReviewMode.RUNNING)
                    
                    _uiState.update { 
                        it.copy(
                            lastRunningAnalysis = analysis,
                            isAnalyzing = false,
                            isCameraActive = false,
                            analysisPhase = AnalysisPhase.COMPLETE,
                            lastSavedReview = savedReview,
                            reviewComparison = comparison,
                            progressSummary = progressSummary,
                            reviewHistory = reviewHistory
                        ) 
                    }
                }
                FormAnalysisMode.GYM -> {
                    val exercise = _uiState.value.selectedGymExercise
                    
                    // Try AI analysis first if we have captured frames
                    val analysis = if (capturedFrames.isNotEmpty()) {
                        analyzeWithGemini(exercise)
                    } else {
                        // Fallback to pose-based analysis
                        formAnalysisEngine.analyzeGymForm(latestPose, exercise)
                    }
                    
                    // Save review and get comparison
                    val savedReview = formReviewRepository.saveGymReview(analysis, repsDetected)
                    val comparison = formReviewRepository.compareWithPrevious(savedReview)
                    val progressSummary = formReviewRepository.getProgressSummary(FormReviewMode.GYM, exercise.name)
                    val reviewHistory = formReviewRepository.getReviewsWithDetails(FormReviewMode.GYM, exercise.name)
                    
                    // Calculate average pose across all collected frames for comparison display
                    val avgPose = calculateAveragePose(poseDataBuffer)
                    
                    _uiState.update { 
                        it.copy(
                            lastGymAnalysis = analysis,
                            averagePose = avgPose,
                            isAnalyzing = false,
                            isCameraActive = false,
                            analysisPhase = AnalysisPhase.COMPLETE,
                            lastSavedReview = savedReview,
                            reviewComparison = comparison,
                            progressSummary = progressSummary,
                            reviewHistory = reviewHistory
                        ) 
                    }
                }
            }
        }
    }
    
    private suspend fun analyzeWithGemini(exercise: GymExerciseType): GymFormAnalysisResult {
        android.util.Log.d("FormAnalysis", "Using Gemini AI for ${exercise.displayName} analysis")
        
        val aiResult = geminiFormAnalyzer.analyzeExerciseForm(
            frames = capturedFrames,
            exerciseName = exercise.displayName
        )
        
        return when (aiResult) {
            is FormAnalysisAIResult.Success -> {
                android.util.Log.d("FormAnalysis", "Gemini analysis successful: score=${aiResult.overallScore}")
                GymFormAnalysisResult(
                    exerciseType = exercise,
                    overallScore = aiResult.overallScore,
                    issues = aiResult.issues,
                    metrics = mapOf(
                        "repCount" to aiResult.repCount.toFloat(),
                        "formQuality" to when (aiResult.formQuality) {
                            "EXCELLENT" -> 95f
                            "GOOD" -> 80f
                            "FAIR" -> 65f
                            else -> 50f
                        }
                    ),
                    repQuality = when (aiResult.formQuality) {
                        "EXCELLENT" -> RepQuality.GOOD
                        "GOOD" -> RepQuality.GOOD
                        "FAIR" -> RepQuality.FAIR
                        else -> RepQuality.POOR
                    },
                    tips = aiResult.keyTips + aiResult.positiveFeedback.map { "✓ $it" }
                )
            }
            is FormAnalysisAIResult.Error -> {
                android.util.Log.e("FormAnalysis", "Gemini analysis failed: ${aiResult.message}")
                // Fallback to pose-based analysis
                val latestPose = poseDataBuffer.lastOrNull()
                if (latestPose != null) {
                    formAnalysisEngine.analyzeGymForm(latestPose, exercise)
                } else {
                    // Return a basic analysis if no pose data
                    GymFormAnalysisResult(
                        exerciseType = exercise,
                        overallScore = 70,
                        issues = emptyList(),
                        metrics = emptyMap(),
                        repQuality = RepQuality.FAIR,
                        tips = listOf("Unable to analyze form - try again with better lighting")
                    )
                }
            }
        }
    }
    
    private fun calculateAveragePose(poses: List<PoseData>): PoseData? {
        if (poses.isEmpty()) return null
        
        // Collect all landmarks across all poses
        val landmarkSums = mutableMapOf<PoseLandmark, Triple<Float, Float, Float>>()
        val landmarkCounts = mutableMapOf<PoseLandmark, Int>()
        
        for (pose in poses) {
            for ((landmark, point) in pose.landmarks) {
                val current = landmarkSums[landmark] ?: Triple(0f, 0f, 0f)
                landmarkSums[landmark] = Triple(
                    current.first + point.x,
                    current.second + point.y,
                    current.third + point.z
                )
                landmarkCounts[landmark] = (landmarkCounts[landmark] ?: 0) + 1
            }
        }
        
        // Calculate averages
        val averageLandmarks = mutableMapOf<PoseLandmark, Point3D>()
        for ((landmark, sum) in landmarkSums) {
            val count = landmarkCounts[landmark] ?: 1
            averageLandmarks[landmark] = Point3D(
                sum.first / count,
                sum.second / count,
                sum.third / count
            )
        }
        
        return PoseData(landmarks = averageLandmarks)
    }
    
    fun onCameraError(error: String) {
        _uiState.update { 
            it.copy(
                error = error,
                isAnalyzing = false,
                isCameraActive = false
            ) 
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun toggleHistorySheet() {
        _uiState.update { it.copy(showHistorySheet = !it.showHistorySheet) }
    }
    
    fun loadReviewHistory() {
        val mode = when (_uiState.value.mode) {
            FormAnalysisMode.AUTO_DETECT -> FormReviewMode.GYM // Auto-detect reviews stored as gym
            FormAnalysisMode.RUNNING -> FormReviewMode.RUNNING
            FormAnalysisMode.GYM -> FormReviewMode.GYM
        }
        val exerciseType = when (_uiState.value.mode) {
            FormAnalysisMode.GYM -> _uiState.value.selectedGymExercise.name
            FormAnalysisMode.AUTO_DETECT -> _uiState.value.detectedExerciseName
            else -> null
        }
        
        val history = formReviewRepository.getReviewsWithDetails(mode, exerciseType)
        val summary = formReviewRepository.getProgressSummary(mode, exerciseType)
        
        _uiState.update { 
            it.copy(
                reviewHistory = history,
                progressSummary = summary
            ) 
        }
    }
    
    fun getScoreHistory(): List<Pair<Long, Int>> {
        val mode = when (_uiState.value.mode) {
            FormAnalysisMode.AUTO_DETECT -> FormReviewMode.GYM
            FormAnalysisMode.RUNNING -> FormReviewMode.RUNNING
            FormAnalysisMode.GYM -> FormReviewMode.GYM
        }
        val exerciseType = when (_uiState.value.mode) {
            FormAnalysisMode.GYM -> _uiState.value.selectedGymExercise.name
            FormAnalysisMode.AUTO_DETECT -> _uiState.value.detectedExerciseName
            else -> null
        }
        
        return formReviewRepository.getScoreHistory(mode, exerciseType)
    }
    
    // Demo mode for testing without camera
    fun runDemoAnalysis() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, isDemoMode = true) }
            
            delay(2000)
            
            val demoPoseData = createDemoPoseData()
            
            when (_uiState.value.mode) {
                FormAnalysisMode.AUTO_DETECT -> {
                    // Demo mode for auto-detect - create a sample result
                    val demoResult = AutoDetectAnalysisResult(
                        exerciseDetected = "Squat",
                        exerciseCategory = "STRENGTH",
                        overallScore = 78,
                        formQuality = "GOOD",
                        issues = emptyList(),
                        positiveFeedback = listOf("Good depth", "Knees tracking well"),
                        keyTips = listOf("Keep chest up", "Drive through heels"),
                        confidence = "HIGH",
                        repCount = 5
                    )
                    _uiState.update { 
                        it.copy(
                            isAnalyzing = false,
                            autoDetectResult = demoResult,
                            detectedExerciseName = "Squat",
                            isDemoMode = false,
                            analysisPhase = AnalysisPhase.COMPLETE
                        ) 
                    }
                }
                FormAnalysisMode.RUNNING -> {
                    val analysis = formAnalysisEngine.analyzeForm(demoPoseData)
                    _uiState.update { 
                        it.copy(
                            isAnalyzing = false,
                            lastRunningAnalysis = analysis,
                            isDemoMode = false
                        ) 
                    }
                }
                FormAnalysisMode.GYM -> {
                    val exercise = _uiState.value.selectedGymExercise
                    val analysis = formAnalysisEngine.analyzeGymForm(demoPoseData, exercise)
                    _uiState.update { 
                        it.copy(
                            isAnalyzing = false,
                            lastGymAnalysis = analysis,
                            averagePose = demoPoseData,  // Use demo pose for comparison display
                            currentPose = demoPoseData,
                            isDemoMode = false,
                            analysisPhase = AnalysisPhase.COMPLETE
                        ) 
                    }
                }
            }
        }
    }

    private fun createDemoPoseData(): PoseData {
        // Create demo pose based on selected exercise (side view)
        val exercise = _uiState.value.selectedGymExercise
        val landmarks = when (exercise) {
            GymExerciseType.PUSH_UP -> mapOf(
                // Side view push-up with slight form issues (elbows flared, hips slightly sagging)
                // Matches ideal pose structure but with deviations
                PoseLandmark.NOSE to Point3D(0.14f, 0.42f),
                PoseLandmark.LEFT_SHOULDER to Point3D(0.24f, 0.46f),
                PoseLandmark.RIGHT_SHOULDER to Point3D(0.24f, 0.46f),
                PoseLandmark.LEFT_ELBOW to Point3D(0.18f, 0.58f),      // Elbows flared out (lower X)
                PoseLandmark.RIGHT_ELBOW to Point3D(0.18f, 0.58f),
                PoseLandmark.LEFT_WRIST to Point3D(0.24f, 0.72f),
                PoseLandmark.RIGHT_WRIST to Point3D(0.24f, 0.72f),
                PoseLandmark.LEFT_HIP to Point3D(0.54f, 0.54f),        // Hips sagging (higher Y than shoulders)
                PoseLandmark.RIGHT_HIP to Point3D(0.54f, 0.54f),
                PoseLandmark.LEFT_KNEE to Point3D(0.74f, 0.52f),
                PoseLandmark.RIGHT_KNEE to Point3D(0.74f, 0.52f),
                PoseLandmark.LEFT_ANKLE to Point3D(0.90f, 0.50f),
                PoseLandmark.RIGHT_ANKLE to Point3D(0.90f, 0.50f)
            )
            GymExerciseType.SQUAT -> mapOf(
                // Side view squat with slight form issues
                PoseLandmark.NOSE to Point3D(0.38f, 0.18f),
                PoseLandmark.LEFT_SHOULDER to Point3D(0.42f, 0.28f),
                PoseLandmark.RIGHT_SHOULDER to Point3D(0.42f, 0.28f),
                PoseLandmark.LEFT_ELBOW to Point3D(0.38f, 0.38f),
                PoseLandmark.RIGHT_ELBOW to Point3D(0.38f, 0.38f),
                PoseLandmark.LEFT_WRIST to Point3D(0.42f, 0.42f),
                PoseLandmark.RIGHT_WRIST to Point3D(0.42f, 0.42f),
                PoseLandmark.LEFT_HIP to Point3D(0.52f, 0.52f),
                PoseLandmark.RIGHT_HIP to Point3D(0.52f, 0.52f),
                PoseLandmark.LEFT_KNEE to Point3D(0.38f, 0.68f),
                PoseLandmark.RIGHT_KNEE to Point3D(0.38f, 0.68f),
                PoseLandmark.LEFT_ANKLE to Point3D(0.48f, 0.88f),
                PoseLandmark.RIGHT_ANKLE to Point3D(0.48f, 0.88f)
            )
            GymExerciseType.PLANK -> mapOf(
                // Side view plank with slight hip pike
                PoseLandmark.NOSE to Point3D(0.14f, 0.38f),
                PoseLandmark.LEFT_SHOULDER to Point3D(0.22f, 0.42f),
                PoseLandmark.RIGHT_SHOULDER to Point3D(0.22f, 0.42f),
                PoseLandmark.LEFT_ELBOW to Point3D(0.22f, 0.56f),
                PoseLandmark.RIGHT_ELBOW to Point3D(0.22f, 0.56f),
                PoseLandmark.LEFT_HIP to Point3D(0.50f, 0.36f),        // Hips piked up
                PoseLandmark.RIGHT_HIP to Point3D(0.50f, 0.36f),
                PoseLandmark.LEFT_KNEE to Point3D(0.72f, 0.42f),
                PoseLandmark.RIGHT_KNEE to Point3D(0.72f, 0.42f),
                PoseLandmark.LEFT_ANKLE to Point3D(0.88f, 0.46f),
                PoseLandmark.RIGHT_ANKLE to Point3D(0.88f, 0.46f)
            )
            else -> mapOf(
                // Generic standing pose for other exercises
                PoseLandmark.NOSE to Point3D(0.50f, 0.10f),
                PoseLandmark.LEFT_SHOULDER to Point3D(0.50f, 0.20f),
                PoseLandmark.RIGHT_SHOULDER to Point3D(0.50f, 0.20f),
                PoseLandmark.LEFT_ELBOW to Point3D(0.50f, 0.35f),
                PoseLandmark.RIGHT_ELBOW to Point3D(0.50f, 0.35f),
                PoseLandmark.LEFT_WRIST to Point3D(0.50f, 0.48f),
                PoseLandmark.RIGHT_WRIST to Point3D(0.50f, 0.48f),
                PoseLandmark.LEFT_HIP to Point3D(0.50f, 0.52f),
                PoseLandmark.RIGHT_HIP to Point3D(0.50f, 0.52f),
                PoseLandmark.LEFT_KNEE to Point3D(0.50f, 0.72f),
                PoseLandmark.RIGHT_KNEE to Point3D(0.50f, 0.72f),
                PoseLandmark.LEFT_ANKLE to Point3D(0.50f, 0.90f),
                PoseLandmark.RIGHT_ANKLE to Point3D(0.50f, 0.90f)
            )
        }
        
        return PoseData(
            landmarks = landmarks,
            estimatedCadence = 168,
            estimatedVerticalOscillation = 8.5f,
            estimatedStrideLength = 1.15f,
            estimatedGroundContactTime = 265,
            estimatedFlightTime = 95
        )
    }
}

enum class FormAnalysisMode {
    RUNNING, GYM, AUTO_DETECT
}

enum class AnalysisPhase {
    IDLE,       // Not started
    COUNTDOWN,  // 5-second countdown to get in position
    COLLECTING, // Actively collecting pose data
    ANALYZING,  // Processing final analysis
    COMPLETE    // Analysis complete, showing results
}

enum class DataQuality {
    INSUFFICIENT, // Not enough data
    LOW,          // Minimal data, low confidence
    MODERATE,     // Some data, moderate confidence  
    GOOD,         // Good amount of data
    EXCELLENT     // Plenty of data, high confidence
}

data class FormAnalysisUiState(
    val mode: FormAnalysisMode = FormAnalysisMode.AUTO_DETECT,
    val selectedGymExercise: GymExerciseType = GymExerciseType.SQUAT,
    val isAnalyzing: Boolean = false,
    val isCameraActive: Boolean = false,
    val poseDetected: Boolean = false,
    val isDemoMode: Boolean = false,
    val currentPose: PoseData? = null,
    val averagePose: PoseData? = null, // Average pose across all collected frames
    val lastRunningAnalysis: FormAnalysisResult? = null,
    val lastGymAnalysis: GymFormAnalysisResult? = null,
    val autoDetectResult: AutoDetectAnalysisResult? = null, // New: result from auto-detect mode
    val detectedExerciseName: String? = null, // New: exercise name detected by AI
    val error: String? = null,
    // New fields for improved UX
    val analysisPhase: AnalysisPhase = AnalysisPhase.IDLE,
    val countdownSeconds: Int = 0,
    val framesCollected: Int = 0,
    val repsDetected: Int = 0,
    val dataQuality: DataQuality = DataQuality.INSUFFICIENT,
    // Form review history
    val lastSavedReview: FormReview? = null,
    val reviewComparison: FormReviewComparison? = null,
    val progressSummary: FormProgressSummary? = null,
    val reviewHistory: List<FormReviewWithDetails> = emptyList(),
    val showHistorySheet: Boolean = false
)

// Result for auto-detect mode
data class AutoDetectAnalysisResult(
    val exerciseDetected: String,
    val exerciseCategory: String,
    val overallScore: Int,
    val formQuality: String,
    val issues: List<FormIssue>,
    val positiveFeedback: List<String>,
    val keyTips: List<String>,
    val confidence: String,
    val repCount: Int
)
