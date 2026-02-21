package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

/**
 * Represents a saved form analysis review for tracking progress over time
 */
@Entity(tableName = "form_reviews")
@TypeConverters(Converters::class)
data class FormReview(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: FormReviewMode,
    val exerciseType: String? = null, // For gym exercises (e.g., "SQUAT", "DEADLIFT")
    val overallScore: Int,
    val issueTypes: List<String>, // Serialized list of FormIssueType names
    val issueSeverities: List<String>, // Serialized list of IssueSeverity names
    val metrics: Map<String, Float>, // Key metrics from the analysis
    val repsAnalyzed: Int = 0,
    val notes: String? = null
)

enum class FormReviewMode {
    RUNNING, GYM
}

/**
 * Summary of form progress over time
 */
data class FormProgressSummary(
    val mode: FormReviewMode,
    val exerciseType: String? = null,
    val totalReviews: Int,
    val averageScore: Float,
    val bestScore: Int,
    val latestScore: Int,
    val scoreImprovement: Int, // Difference between latest and first review
    val mostCommonIssues: List<String>,
    val resolvedIssues: List<String>, // Issues that appeared before but not in recent reviews
    val trend: FormProgressTrend
)

enum class FormProgressTrend {
    IMPROVING, STABLE, DECLINING
}

/**
 * Comparison between two form reviews
 */
data class FormReviewComparison(
    val currentReview: FormReview,
    val previousReview: FormReview?,
    val scoreDifference: Int,
    val newIssues: List<String>, // Issues in current but not in previous
    val resolvedIssues: List<String>, // Issues in previous but not in current
    val persistentIssues: List<String>, // Issues in both
    val metricChanges: Map<String, Float> // Difference in metric values
)

/**
 * Form review with additional display information
 */
data class FormReviewWithDetails(
    val review: FormReview,
    val formattedDate: String,
    val exerciseDisplayName: String?,
    val issueCount: Int,
    val highSeverityCount: Int
)
