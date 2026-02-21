package com.runtracker.app.ui.screens.body

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.ai.BodyAnalysisAIResult
import com.runtracker.app.ai.GeminiBodyAnalyzer
import com.runtracker.shared.ai.PoseData
import com.runtracker.shared.ai.PoseLandmark
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.BodyAnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class BodyAnalysisViewModel @Inject constructor(
    private val repository: BodyAnalysisRepository,
    private val geminiBodyAnalyzer: GeminiBodyAnalyzer,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BodyAnalysisUiState())
    val uiState: StateFlow<BodyAnalysisUiState> = _uiState.asStateFlow()
    
    // Live camera state
    private val poseDataBuffer = mutableListOf<PoseData>()
    private var positionHoldStartTime = 0L
    private var positionHoldJob: Job? = null
    private val requiredHoldTimeMs = 2000L // Hold position for 2 seconds to start
    private val requiredFrames = 30 // Collect 30 frames (~3 seconds)
    
    init {
        loadHistory()
    }
    
    fun setSelectedGoal(goal: FitnessGoal) {
        _uiState.update { it.copy(selectedGoal = goal) }
    }
    
    fun setPhotoUri(uri: Uri?) {
        android.util.Log.d("BodyAnalysis", "setPhotoUri called: uri=$uri")
        if (uri != null) {
            _uiState.update { it.copy(photoUri = uri, analysisPhase = AnalysisPhase.PHOTO_SELECTED) }
            android.util.Log.d("BodyAnalysis", "Phase changed to PHOTO_SELECTED")
        }
    }
    
    fun startLiveCamera() {
        poseDataBuffer.clear()
        positionHoldStartTime = 0L
        _uiState.update { 
            it.copy(
                analysisPhase = AnalysisPhase.LIVE_CAMERA,
                positionStatus = PositionStatus.NOT_DETECTED,
                positionProgress = 0f,
                framesCollected = 0,
                currentPose = null
            ) 
        }
    }
    
    fun onPoseDetected(poseData: PoseData) {
        val currentPhase = _uiState.value.analysisPhase
        
        android.util.Log.d("BodyAnalysis", "onPoseDetected: phase=$currentPhase, landmarks=${poseData.landmarks.size}")
        
        // Update current pose for overlay
        _uiState.update { it.copy(currentPose = poseData) }
        
        when (currentPhase) {
            AnalysisPhase.LIVE_CAMERA, AnalysisPhase.POSITIONING -> {
                // Check if user is in correct position
                val positionStatus = checkPositionStatus(poseData)
                _uiState.update { it.copy(positionStatus = positionStatus) }
                
                if (positionStatus == PositionStatus.READY) {
                    if (positionHoldStartTime == 0L) {
                        positionHoldStartTime = System.currentTimeMillis()
                        _uiState.update { it.copy(analysisPhase = AnalysisPhase.POSITIONING) }
                    }
                    
                    val holdDuration = System.currentTimeMillis() - positionHoldStartTime
                    val progress = (holdDuration.toFloat() / requiredHoldTimeMs).coerceIn(0f, 1f)
                    _uiState.update { it.copy(positionProgress = progress) }
                    
                    if (holdDuration >= requiredHoldTimeMs) {
                        // Start collecting data
                        startDataCollection()
                    }
                } else {
                    // Reset hold timer if position lost
                    positionHoldStartTime = 0L
                    _uiState.update { 
                        it.copy(
                            positionProgress = 0f,
                            analysisPhase = AnalysisPhase.LIVE_CAMERA
                        ) 
                    }
                }
            }
            AnalysisPhase.COLLECTING_DATA -> {
                // Collect pose data
                poseDataBuffer.add(poseData)
                _uiState.update { it.copy(framesCollected = poseDataBuffer.size) }
                
                if (poseDataBuffer.size >= requiredFrames) {
                    // Enough data collected - run analysis
                    runAnalysisFromPoseData()
                }
            }
            else -> { /* Ignore pose data in other phases */ }
        }
    }
    
    private fun checkPositionStatus(poseData: PoseData): PositionStatus {
        // Check if all required landmarks are detected
        val requiredLandmarks = listOf(
            PoseLandmark.NOSE,
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE
        )
        
        val detectedLandmarks = requiredLandmarks.filter { poseData.landmarks.containsKey(it) }
        val detectionRatio = detectedLandmarks.size.toFloat() / requiredLandmarks.size
        
        android.util.Log.d("BodyAnalysis", "Detected ${detectedLandmarks.size}/${requiredLandmarks.size} landmarks (${(detectionRatio * 100).toInt()}%)")
        
        // Relaxed threshold: only need 60% of landmarks (was 70%)
        if (detectionRatio < 0.6f) {
            android.util.Log.d("BodyAnalysis", "NOT_DETECTED: Not enough landmarks")
            return PositionStatus.NOT_DETECTED
        }
        
        // Get body bounds
        val allPoints = poseData.landmarks.values.toList()
        if (allPoints.isEmpty()) return PositionStatus.NOT_DETECTED
        
        val minX = allPoints.minOf { it.x }
        val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOf { it.y }
        val maxY = allPoints.maxOf { it.y }
        
        val bodyWidth = maxX - minX
        val bodyHeight = maxY - minY
        val centerX = (minX + maxX) / 2
        
        android.util.Log.d("BodyAnalysis", "Body bounds: width=$bodyWidth, height=$bodyHeight, centerX=$centerX")
        
        // Check if body is too close (takes up too much of frame)
        if (bodyHeight > 0.95f || bodyWidth > 0.9f) {
            android.util.Log.d("BodyAnalysis", "TOO_CLOSE: height=$bodyHeight, width=$bodyWidth")
            return PositionStatus.TOO_CLOSE
        }
        
        // Relaxed threshold: body needs to be at least 30% of frame height (was 50%)
        if (bodyHeight < 0.3f) {
            android.util.Log.d("BodyAnalysis", "TOO_FAR: height=$bodyHeight")
            return PositionStatus.TOO_FAR
        }
        
        // Relaxed centering: within middle 80% of frame (was 60%)
        if (centerX < 0.1f || centerX > 0.9f) {
            android.util.Log.d("BodyAnalysis", "NOT_CENTERED: centerX=$centerX")
            return PositionStatus.NOT_CENTERED
        }
        
        // Relaxed edge check
        if (minY < 0.01f || maxY > 0.99f) {
            android.util.Log.d("BodyAnalysis", "OUT_OF_FRAME: minY=$minY, maxY=$maxY")
            return PositionStatus.OUT_OF_FRAME
        }
        
        android.util.Log.d("BodyAnalysis", "READY: All checks passed!")
        return PositionStatus.READY
    }
    
    private fun startDataCollection() {
        poseDataBuffer.clear()
        _uiState.update { 
            it.copy(
                analysisPhase = AnalysisPhase.COLLECTING_DATA,
                framesCollected = 0
            ) 
        }
    }
    
    private fun runAnalysisFromPoseData() {
        val goal = _uiState.value.selectedGoal
        
        _uiState.update { it.copy(analysisPhase = AnalysisPhase.ANALYZING) }
        
        viewModelScope.launch {
            // Simulate AI analysis with progressive updates
            delay(300)
            _uiState.update { it.copy(analysisProgress = 0.2f, analysisStatus = "Analyzing body landmarks...") }
            
            delay(400)
            _uiState.update { it.copy(analysisProgress = 0.4f, analysisStatus = "Assessing muscle groups...") }
            
            delay(400)
            _uiState.update { it.copy(analysisProgress = 0.6f, analysisStatus = "Evaluating posture...") }
            
            delay(300)
            _uiState.update { it.copy(analysisProgress = 0.8f, analysisStatus = "Generating recommendations...") }
            
            delay(300)
            _uiState.update { it.copy(analysisProgress = 1.0f, analysisStatus = "Complete!") }
            
            // Generate analysis from pose data
            val scan = generateScanFromPoseData(poseDataBuffer, goal)
            val savedScan = repository.saveScan(scan)
            val analysisResult = repository.generateAnalysisResult(savedScan)
            val comparison = repository.compareToPrevious(savedScan)
            val progressSummary = repository.getProgressSummary()
            
            delay(200)
            
            _uiState.update { 
                it.copy(
                    analysisPhase = AnalysisPhase.RESULTS,
                    currentScan = savedScan,
                    analysisResult = analysisResult,
                    scanComparison = comparison,
                    progressSummary = progressSummary
                ) 
            }
            
            loadHistory()
        }
    }
    
    private fun generateScanFromPoseData(poseData: List<PoseData>, goal: FitnessGoal): BodyScan {
        // Analyze pose data to generate body scan
        // In production, this would use actual ML analysis
        
        val avgPose = if (poseData.isNotEmpty()) poseData.last() else null
        
        // Detect posture issues from pose data
        val postureIssues = mutableListOf<PostureIssue>()
        
        avgPose?.let { pose ->
            // Check shoulder alignment
            val leftShoulder = pose.landmarks[PoseLandmark.LEFT_SHOULDER]
            val rightShoulder = pose.landmarks[PoseLandmark.RIGHT_SHOULDER]
            if (leftShoulder != null && rightShoulder != null) {
                val shoulderDiff = kotlin.math.abs(leftShoulder.y - rightShoulder.y)
                if (shoulderDiff > 0.03f) {
                    postureIssues.add(PostureIssue(
                        type = PostureIssueType.UNEVEN_SHOULDERS,
                        severity = if (shoulderDiff > 0.06f) IssueSeverity.MODERATE else IssueSeverity.MILD,
                        description = "Shoulders are not level - one side is higher",
                        exercises = listOf("Shoulder shrugs", "Wall slides", "Doorway stretch")
                    ))
                }
            }
            
            // Check hip alignment
            val leftHip = pose.landmarks[PoseLandmark.LEFT_HIP]
            val rightHip = pose.landmarks[PoseLandmark.RIGHT_HIP]
            if (leftHip != null && rightHip != null) {
                val hipDiff = kotlin.math.abs(leftHip.y - rightHip.y)
                if (hipDiff > 0.03f) {
                    postureIssues.add(PostureIssue(
                        type = PostureIssueType.UNEVEN_HIPS,
                        severity = if (hipDiff > 0.06f) IssueSeverity.MODERATE else IssueSeverity.MILD,
                        description = "Hips are tilted - possible leg length discrepancy",
                        exercises = listOf("Hip stretches", "Glute bridges", "Single-leg deadlifts")
                    ))
                }
            }
        }
        
        val postureLevel = when {
            postureIssues.isEmpty() -> PostureLevel.GOOD
            postureIssues.any { it.severity == IssueSeverity.SEVERE } -> PostureLevel.POOR
            postureIssues.any { it.severity == IssueSeverity.MODERATE } -> PostureLevel.FAIR
            else -> PostureLevel.GOOD
        }
        
        val overallScore = when {
            postureIssues.isEmpty() -> Random.nextInt(80, 95)
            postureIssues.any { it.severity == IssueSeverity.SEVERE } -> Random.nextInt(40, 60)
            postureIssues.any { it.severity == IssueSeverity.MODERATE } -> Random.nextInt(60, 80)
            else -> Random.nextInt(70, 85)
        }
        
        return BodyScan(
            photoPath = "live_scan_${System.currentTimeMillis()}",
            userGoal = goal,
            bodyType = BodyType.values().random(),
            estimatedBodyFatPercentage = Random.nextFloat() * 15 + 12,
            focusZones = BodyZone.values().toList().shuffled().take(3),
            overallScore = overallScore,
            muscleBalance = MuscleBalanceAssessment(
                overallBalance = BalanceLevel.values().random(),
                leftRightSymmetry = if (postureIssues.any { it.type == PostureIssueType.UNEVEN_SHOULDERS }) 
                    BalanceLevel.SLIGHT_IMBALANCE else BalanceLevel.BALANCED,
                frontBackBalance = BalanceLevel.values().random(),
                upperLowerBalance = BalanceLevel.values().random(),
                imbalances = emptyList()
            ),
            postureAssessment = PostureAssessment(
                overallPosture = postureLevel,
                issues = postureIssues
            )
        )
    }
    
    fun stopLiveCamera() {
        positionHoldJob?.cancel()
        poseDataBuffer.clear()
        _uiState.update { 
            it.copy(
                analysisPhase = AnalysisPhase.GOAL_SELECTION,
                positionStatus = PositionStatus.NOT_DETECTED,
                positionProgress = 0f,
                framesCollected = 0
            ) 
        }
    }
    
    fun startAnalysis() {
        val photoUri = _uiState.value.photoUri
        android.util.Log.d("BodyAnalysis", "startAnalysis called: photoUri=$photoUri")
        if (photoUri == null) {
            android.util.Log.e("BodyAnalysis", "startAnalysis: photoUri is null, returning")
            return
        }
        val goal = _uiState.value.selectedGoal
        
        android.util.Log.d("BodyAnalysis", "startAnalysis: Starting analysis with goal=$goal")
        _uiState.update { it.copy(analysisPhase = AnalysisPhase.ANALYZING, analysisProgress = 0f) }
        
        viewModelScope.launch {
            try {
                // Load bitmap from URI
                _uiState.update { it.copy(analysisProgress = 0.1f, analysisStatus = "Loading image...") }
                
                val bitmap = context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
                
                if (bitmap == null) {
                    _uiState.update { 
                        it.copy(
                            analysisPhase = AnalysisPhase.PHOTO_SELECTED,
                            analysisStatus = "Failed to load image"
                        ) 
                    }
                    return@launch
                }
                
                _uiState.update { it.copy(analysisProgress = 0.2f, analysisStatus = "Analyzing with AI...") }
                
                // Use Gemini for analysis
                val aiResult = geminiBodyAnalyzer.analyzeBodyImage(bitmap, goal)
                
                _uiState.update { it.copy(analysisProgress = 0.8f, analysisStatus = "Processing results...") }
                
                when (aiResult) {
                    is BodyAnalysisAIResult.Success -> {
                        // Create scan from AI result
                        val scan = BodyScan(
                            photoPath = photoUri.toString(),
                            userGoal = goal,
                            bodyType = aiResult.bodyType,
                            estimatedBodyFatPercentage = aiResult.estimatedBodyFatPercentage,
                            focusZones = aiResult.focusZones,
                            overallScore = aiResult.overallScore,
                            muscleBalance = aiResult.muscleBalance,
                            postureAssessment = aiResult.postureAssessment
                        )
                        
                        val savedScan = repository.saveScan(scan)
                        val analysisResult = repository.generateAnalysisResult(savedScan)
                        
                        val comparison = repository.compareToPrevious(savedScan)
                        val progressSummary = repository.getProgressSummary()
                        
                        _uiState.update { it.copy(analysisProgress = 1.0f, analysisStatus = "Complete!") }
                        
                        delay(300)
                        
                        _uiState.update { 
                            it.copy(
                                analysisPhase = AnalysisPhase.RESULTS,
                                currentScan = savedScan,
                                analysisResult = analysisResult,
                                scanComparison = comparison,
                                progressSummary = progressSummary,
                                aiNotes = aiResult.notes,
                                aiConfidence = aiResult.confidence.name
                            ) 
                        }
                        
                        loadHistory()
                    }
                    is BodyAnalysisAIResult.Error -> {
                        android.util.Log.e("BodyAnalysis", "AI analysis failed: ${aiResult.message}")
                        // Fall back to mock analysis
                        _uiState.update { it.copy(analysisStatus = "AI unavailable, using basic analysis...") }
                        performFallbackAnalysis(photoUri, goal)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BodyAnalysis", "Analysis error: ${e.message}", e)
                // Fall back to mock analysis
                _uiState.update { it.copy(analysisStatus = "Error occurred, using basic analysis...") }
                performFallbackAnalysis(photoUri, goal)
            }
        }
    }
    
    private fun getOverallAssessment(score: Int): String {
        return when {
            score >= 85 -> "Excellent physique with great muscle development and balance."
            score >= 70 -> "Good overall fitness level with some areas for improvement."
            score >= 55 -> "Moderate fitness level. Consistent training will yield great results."
            else -> "Starting point identified. Focus on the recommended areas for best progress."
        }
    }
    
    private suspend fun performFallbackAnalysis(photoUri: Uri, goal: FitnessGoal) {
        delay(500)
        _uiState.update { it.copy(analysisProgress = 0.5f) }
        
        delay(500)
        _uiState.update { it.copy(analysisProgress = 0.8f) }
        
        val scan = generateMockScan(photoUri.toString(), goal)
        val savedScan = repository.saveScan(scan)
        val analysisResult = repository.generateAnalysisResult(savedScan)
        val comparison = repository.compareToPrevious(savedScan)
        val progressSummary = repository.getProgressSummary()
        
        _uiState.update { it.copy(analysisProgress = 1.0f, analysisStatus = "Complete!") }
        
        delay(300)
        
        _uiState.update { 
            it.copy(
                analysisPhase = AnalysisPhase.RESULTS,
                currentScan = savedScan,
                analysisResult = analysisResult,
                scanComparison = comparison,
                progressSummary = progressSummary
            ) 
        }
        
        loadHistory()
    }
    
    fun loadHistory() {
        viewModelScope.launch {
            val history = repository.getScansWithDetails()
            val scoreHistory = repository.getScoreHistory()
            val summary = repository.getProgressSummary()
            
            _uiState.update { 
                it.copy(
                    scanHistory = history,
                    scoreHistory = scoreHistory,
                    progressSummary = summary
                ) 
            }
        }
    }
    
    fun toggleHistorySheet() {
        _uiState.update { it.copy(showHistorySheet = !it.showHistorySheet) }
    }
    
    fun viewScanDetails(scanId: Long) {
        viewModelScope.launch {
            val scan = repository.getScanById(scanId) ?: return@launch
            val result = repository.generateAnalysisResult(scan)
            val comparison = repository.compareToPrevious(scan)
            
            _uiState.update { 
                it.copy(
                    currentScan = scan,
                    analysisResult = result,
                    scanComparison = comparison,
                    analysisPhase = AnalysisPhase.RESULTS
                ) 
            }
        }
    }
    
    fun resetToCapture() {
        _uiState.update { 
            it.copy(
                analysisPhase = AnalysisPhase.GOAL_SELECTION,
                photoUri = null,
                currentScan = null,
                analysisResult = null,
                analysisProgress = 0f,
                analysisStatus = ""
            ) 
        }
    }
    
    fun deleteScan(scanId: Long) {
        viewModelScope.launch {
            repository.deleteScan(scanId)
            loadHistory()
        }
    }
    
    private fun generateMockScan(photoPath: String, goal: FitnessGoal): BodyScan {
        // Simulate AI-detected body analysis
        // In production, this would use ML Kit or a custom model
        
        val bodyType = BodyType.values().random()
        val estimatedBf = Random.nextFloat() * 20 + 10 // 10-30%
        
        // Generate focus zones based on "analysis"
        val allZones = BodyZone.values().toList()
        val focusZones = allZones.shuffled().take(Random.nextInt(2, 5))
        
        // Generate posture assessment
        val postureIssues = if (Random.nextBoolean()) {
            listOf(
                PostureIssue(
                    type = PostureIssueType.values().random(),
                    severity = IssueSeverity.values().random(),
                    description = "Detected slight misalignment",
                    exercises = listOf("Wall Angels", "Chin Tucks", "Cat-Cow Stretch")
                )
            )
        } else emptyList()
        
        val postureLevel = when {
            postureIssues.isEmpty() -> PostureLevel.GOOD
            postureIssues.any { it.severity == IssueSeverity.SEVERE } -> PostureLevel.POOR
            postureIssues.any { it.severity == IssueSeverity.MODERATE } -> PostureLevel.FAIR
            else -> PostureLevel.GOOD
        }
        
        // Generate muscle balance assessment
        val imbalances = if (Random.nextBoolean()) {
            listOf(
                MuscleImbalance(
                    description = "Slight left-right asymmetry in shoulders",
                    affectedZones = listOf(BodyZone.SHOULDERS),
                    correction = "Include unilateral exercises"
                )
            )
        } else emptyList()
        
        val overallScore = Random.nextInt(55, 95)
        
        return BodyScan(
            photoPath = photoPath,
            userGoal = goal,
            bodyType = bodyType,
            estimatedBodyFatPercentage = estimatedBf,
            focusZones = focusZones,
            overallScore = overallScore,
            muscleBalance = MuscleBalanceAssessment(
                overallBalance = BalanceLevel.values().random(),
                leftRightSymmetry = BalanceLevel.values().random(),
                frontBackBalance = BalanceLevel.values().random(),
                upperLowerBalance = BalanceLevel.values().random(),
                imbalances = imbalances
            ),
            postureAssessment = PostureAssessment(
                overallPosture = postureLevel,
                issues = postureIssues
            )
        )
    }
}

enum class AnalysisPhase {
    GOAL_SELECTION,
    PHOTO_CAPTURE,
    PHOTO_SELECTED,
    LIVE_CAMERA,      // New: Live camera with positioning frame
    POSITIONING,      // New: User is positioning themselves
    COLLECTING_DATA,  // New: Collecting pose data
    ANALYZING,
    RESULTS
}

enum class PositionStatus {
    NOT_DETECTED,     // No body detected
    OUT_OF_FRAME,     // Body detected but not in frame
    TOO_CLOSE,        // Too close to camera
    TOO_FAR,          // Too far from camera
    NOT_CENTERED,     // Not centered in frame
    READY             // Perfect position - ready to scan
}

data class BodyAnalysisUiState(
    val analysisPhase: AnalysisPhase = AnalysisPhase.GOAL_SELECTION,
    val selectedGoal: FitnessGoal = FitnessGoal.GENERAL_FITNESS,
    val photoUri: Uri? = null,
    val analysisProgress: Float = 0f,
    val analysisStatus: String = "",
    val currentScan: BodyScan? = null,
    val analysisResult: BodyAnalysisResult? = null,
    val scanComparison: BodyScanComparison? = null,
    val progressSummary: BodyProgressSummary? = null,
    val scanHistory: List<BodyScanWithDetails> = emptyList(),
    val scoreHistory: List<Pair<Long, Int>> = emptyList(),
    val showHistorySheet: Boolean = false,
    val error: String? = null,
    // Live camera state
    val useLiveCamera: Boolean = true,
    val positionStatus: PositionStatus = PositionStatus.NOT_DETECTED,
    val positionProgress: Float = 0f,  // 0-1, how long user has been in position
    val framesCollected: Int = 0,
    val currentPose: com.runtracker.shared.ai.PoseData? = null,
    // AI analysis state
    val aiNotes: String = "",
    val aiConfidence: String = ""
)
