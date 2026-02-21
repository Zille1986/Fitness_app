package com.runtracker.app.ui.screens.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.repository.BodyAnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AnalysisDashboardViewModel @Inject constructor(
    private val bodyAnalysisRepository: BodyAnalysisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisDashboardUiState())
    val uiState: StateFlow<AnalysisDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            loadLatestScan()
            loadComparison()
            loadScanHistory()
        }
    }

    private fun loadLatestScan() {
        viewModelScope.launch {
            val scans = bodyAnalysisRepository.getScansWithDetails()
        if (scans.isNotEmpty()) {
            val latest = scans.first()
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            
            _uiState.update {
                it.copy(
                    latestScan = LatestScanInfo(
                        score = latest.scan.overallScore,
                        date = dateFormat.format(Date(latest.scan.timestamp)),
                        bodyType = latest.scan.bodyType.displayName,
                        bodyFatPercentage = latest.scan.estimatedBodyFatPercentage ?: 0f,
                        focusZoneCount = latest.scan.focusZones.size
                    )
                )
            }
            
            // Load posture issues
            val postureIssues = latest.scan.postureAssessment.issues.map { issue ->
                PostureIssueInfo(
                    name = issue.type.displayName,
                    description = issue.description,
                    severity = issue.severity.name,
                    exercises = issue.exercises
                )
            }
            _uiState.update { it.copy(postureIssues = postureIssues) }
            
            // Load focus areas
            val areasNeedingWork = latest.scan.focusZones.map { zone ->
                AreaNeedingWorkInfo(
                    zoneName = zone.displayName,
                    recommendation = "Focus on ${zone.displayName.lowercase()} exercises"
                )
            }
            _uiState.update { it.copy(areasNeedingWork = areasNeedingWork) }
            }
        }
    }

    private fun loadComparison() {
        viewModelScope.launch {
            val scans = bodyAnalysisRepository.getScansWithDetails()
        if (scans.size >= 2) {
            val current = scans[0].scan
            val previous = scans[1].scan
            
            val comparison = bodyAnalysisRepository.compareToPrevious(current)
            
            val scoreChange = (current.overallScore - previous.overallScore).toFloat()
            val bodyFatChange = if (current.estimatedBodyFatPercentage != null && previous.estimatedBodyFatPercentage != null) {
                current.estimatedBodyFatPercentage!! - previous.estimatedBodyFatPercentage!!
            } else 0f
            
            val overallImproved = scoreChange > 0 || bodyFatChange < 0
            
            val summaryMessage = when {
                scoreChange > 5 -> "Great progress! Your score improved significantly."
                scoreChange > 0 -> "You're making steady progress. Keep it up!"
                scoreChange == 0f -> "Maintaining your current level. Try to push a bit harder."
                else -> "Score decreased slightly. Review your training and nutrition."
            }
            
            _uiState.update {
                it.copy(
                    comparison = ScanComparison(
                        scoreChange = scoreChange,
                        bodyFatChange = bodyFatChange,
                        overallImproved = overallImproved,
                        summaryMessage = summaryMessage
                    )
                )
            }
            
            // Generate improvements list
            val improvements = mutableListOf<ImprovementInfo>()
            if (scoreChange > 0) {
                improvements.add(ImprovementInfo(
                    title = "Overall Score Improved",
                    description = "Your fitness score went up by ${scoreChange.toInt()} points"
                ))
            }
            if (bodyFatChange < 0) {
                improvements.add(ImprovementInfo(
                    title = "Body Fat Reduced",
                    description = "You lost ${String.format("%.1f", -bodyFatChange)}% body fat"
                ))
            }
            if (comparison?.postureImprovement == true) {
                improvements.add(ImprovementInfo(
                    title = "Posture Improved",
                    description = "Your posture assessment shows improvement"
                ))
            }
            
            _uiState.update { it.copy(improvements = improvements) }
            }
        }
    }

    private fun loadScanHistory() {
        viewModelScope.launch {
            val scans = bodyAnalysisRepository.getScansWithDetails()
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        
        val history = scans.map { scanWithDetails ->
            ScanHistoryInfo(
                id = scanWithDetails.scan.id,
                date = dateFormat.format(Date(scanWithDetails.scan.timestamp)),
                score = scanWithDetails.scan.overallScore,
                goalName = scanWithDetails.scan.userGoal.displayName
            )
        }
        
        _uiState.update { it.copy(scanHistory = history) }
        }
    }

    fun refresh() {
        loadData()
    }
}

data class AnalysisDashboardUiState(
    val latestScan: LatestScanInfo? = null,
    val comparison: ScanComparison? = null,
    val improvements: List<ImprovementInfo> = emptyList(),
    val areasNeedingWork: List<AreaNeedingWorkInfo> = emptyList(),
    val postureIssues: List<PostureIssueInfo> = emptyList(),
    val scanHistory: List<ScanHistoryInfo> = emptyList()
)
