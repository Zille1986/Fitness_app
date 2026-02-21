package com.runtracker.shared.data.repository

import com.runtracker.shared.ai.FormAnalysisResult
import com.runtracker.shared.ai.FormIssue
import com.runtracker.shared.ai.GymExerciseType
import com.runtracker.shared.ai.GymFormAnalysisResult
import com.runtracker.shared.ai.IssueSeverity
import com.runtracker.shared.data.model.FormProgressSummary
import com.runtracker.shared.data.model.FormReview
import com.runtracker.shared.data.model.FormReviewComparison
import com.runtracker.shared.data.model.FormReviewMode
import com.runtracker.shared.data.model.FormReviewWithDetails
import com.runtracker.shared.data.model.FormProgressTrend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormReviewRepository {
    
    private val _reviews = MutableStateFlow<List<FormReview>>(emptyList())
    
    fun getAllReviews(): Flow<List<FormReview>> = _reviews
    
    fun getReviewsByMode(mode: FormReviewMode): Flow<List<FormReview>> = 
        _reviews.map { reviews -> reviews.filter { it.mode == mode } }
    
    fun getReviewsByExercise(exerciseType: String): Flow<List<FormReview>> =
        _reviews.map { reviews -> reviews.filter { it.exerciseType == exerciseType } }
    
    fun getRunningReviews(): Flow<List<FormReview>> = getReviewsByMode(FormReviewMode.RUNNING)
    
    fun getGymReviews(): Flow<List<FormReview>> = getReviewsByMode(FormReviewMode.GYM)
    
    suspend fun saveRunningReview(analysis: FormAnalysisResult, notes: String? = null): FormReview {
        val review = FormReview(
            id = System.currentTimeMillis(),
            mode = FormReviewMode.RUNNING,
            exerciseType = null,
            overallScore = analysis.overallScore,
            issueTypes = analysis.issues.map { it.type.name },
            issueSeverities = analysis.issues.map { it.severity.name },
            metrics = analysis.metrics,
            notes = notes
        )
        
        _reviews.value = _reviews.value + review
        return review
    }
    
    suspend fun saveGymReview(
        analysis: GymFormAnalysisResult, 
        repsAnalyzed: Int = 0,
        notes: String? = null
    ): FormReview {
        val review = FormReview(
            id = System.currentTimeMillis(),
            mode = FormReviewMode.GYM,
            exerciseType = analysis.exerciseType.name,
            overallScore = analysis.overallScore,
            issueTypes = analysis.issues.map { it.type.name },
            issueSeverities = analysis.issues.map { it.severity.name },
            metrics = analysis.metrics,
            repsAnalyzed = repsAnalyzed,
            notes = notes
        )
        
        _reviews.value = _reviews.value + review
        return review
    }
    
    fun getLatestReview(mode: FormReviewMode, exerciseType: String? = null): FormReview? {
        return _reviews.value
            .filter { it.mode == mode && (exerciseType == null || it.exerciseType == exerciseType) }
            .maxByOrNull { it.timestamp }
    }
    
    fun getPreviousReview(mode: FormReviewMode, exerciseType: String? = null): FormReview? {
        val reviews = _reviews.value
            .filter { it.mode == mode && (exerciseType == null || it.exerciseType == exerciseType) }
            .sortedByDescending { it.timestamp }
        
        return if (reviews.size >= 2) reviews[1] else null
    }
    
    fun compareWithPrevious(currentReview: FormReview): FormReviewComparison {
        val previousReview = _reviews.value
            .filter { 
                it.mode == currentReview.mode && 
                it.exerciseType == currentReview.exerciseType &&
                it.timestamp < currentReview.timestamp 
            }
            .maxByOrNull { it.timestamp }
        
        val currentIssues = currentReview.issueTypes.toSet()
        val previousIssues = previousReview?.issueTypes?.toSet() ?: emptySet()
        
        val metricChanges = if (previousReview != null) {
            currentReview.metrics.mapValues { (key, value) ->
                value - (previousReview.metrics[key] ?: value)
            }
        } else {
            emptyMap()
        }
        
        return FormReviewComparison(
            currentReview = currentReview,
            previousReview = previousReview,
            scoreDifference = currentReview.overallScore - (previousReview?.overallScore ?: currentReview.overallScore),
            newIssues = (currentIssues - previousIssues).toList(),
            resolvedIssues = (previousIssues - currentIssues).toList(),
            persistentIssues = (currentIssues.intersect(previousIssues)).toList(),
            metricChanges = metricChanges
        )
    }
    
    fun getProgressSummary(mode: FormReviewMode, exerciseType: String? = null): FormProgressSummary? {
        val reviews = _reviews.value
            .filter { it.mode == mode && (exerciseType == null || it.exerciseType == exerciseType) }
            .sortedBy { it.timestamp }
        
        if (reviews.isEmpty()) return null
        
        val scores = reviews.map { it.overallScore }
        val latestReview = reviews.last()
        val firstReview = reviews.first()
        
        // Calculate most common issues
        val allIssues = reviews.flatMap { it.issueTypes }
        val issueFrequency = allIssues.groupingBy { it }.eachCount()
        val mostCommonIssues = issueFrequency.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        // Find resolved issues (appeared in older reviews but not in recent ones)
        val recentReviews = reviews.takeLast(3)
        val olderReviews = reviews.dropLast(3)
        val recentIssues = recentReviews.flatMap { it.issueTypes }.toSet()
        val olderIssues = olderReviews.flatMap { it.issueTypes }.toSet()
        val resolvedIssues = (olderIssues - recentIssues).toList()
        
        // Calculate trend
        val trend = when {
            reviews.size < 3 -> FormProgressTrend.STABLE
            else -> {
                val recentAvg = reviews.takeLast(3).map { it.overallScore }.average()
                val olderAvg = reviews.dropLast(3).takeLast(3).map { it.overallScore }.average()
                when {
                    recentAvg > olderAvg + 5 -> FormProgressTrend.IMPROVING
                    recentAvg < olderAvg - 5 -> FormProgressTrend.DECLINING
                    else -> FormProgressTrend.STABLE
                }
            }
        }
        
        return FormProgressSummary(
            mode = mode,
            exerciseType = exerciseType,
            totalReviews = reviews.size,
            averageScore = scores.average().toFloat(),
            bestScore = scores.maxOrNull() ?: 0,
            latestScore = latestReview.overallScore,
            scoreImprovement = latestReview.overallScore - firstReview.overallScore,
            mostCommonIssues = mostCommonIssues,
            resolvedIssues = resolvedIssues,
            trend = trend
        )
    }
    
    fun getReviewsWithDetails(mode: FormReviewMode, exerciseType: String? = null): List<FormReviewWithDetails> {
        val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        
        return _reviews.value
            .filter { it.mode == mode && (exerciseType == null || it.exerciseType == exerciseType) }
            .sortedByDescending { it.timestamp }
            .map { review ->
                FormReviewWithDetails(
                    review = review,
                    formattedDate = dateFormat.format(Date(review.timestamp)),
                    exerciseDisplayName = review.exerciseType?.let { 
                        try {
                            GymExerciseType.valueOf(it).displayName
                        } catch (e: Exception) {
                            it
                        }
                    },
                    issueCount = review.issueTypes.size,
                    highSeverityCount = review.issueSeverities.count { it == IssueSeverity.HIGH.name }
                )
            }
    }
    
    fun getScoreHistory(mode: FormReviewMode, exerciseType: String? = null, limit: Int = 10): List<Pair<Long, Int>> {
        return _reviews.value
            .filter { it.mode == mode && (exerciseType == null || it.exerciseType == exerciseType) }
            .sortedByDescending { it.timestamp }
            .take(limit)
            .map { it.timestamp to it.overallScore }
            .reversed()
    }
    
    suspend fun deleteReview(reviewId: Long) {
        _reviews.value = _reviews.value.filter { it.id != reviewId }
    }
    
    suspend fun updateReviewNotes(reviewId: Long, notes: String) {
        _reviews.value = _reviews.value.map { review ->
            if (review.id == reviewId) review.copy(notes = notes) else review
        }
    }
}
