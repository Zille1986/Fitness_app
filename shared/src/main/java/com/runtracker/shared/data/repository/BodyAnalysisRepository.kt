package com.runtracker.shared.data.repository

import com.runtracker.shared.data.db.BodyScanDao
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class BodyAnalysisRepository(
    private val bodyScanDao: BodyScanDao? = null
) {
    
    // In-memory cache for when DAO is not available
    private val _scans = MutableStateFlow<List<BodyScan>>(emptyList())
    
    fun getAllScans(): Flow<List<BodyScan>> = bodyScanDao?.getAllScans() ?: _scans
    
    fun getScansFlow(): Flow<List<BodyScan>> = bodyScanDao?.getAllScans() ?: _scans
    
    suspend fun saveScan(scan: BodyScan): BodyScan {
        return if (bodyScanDao != null) {
            val id = bodyScanDao.insertScan(scan)
            scan.copy(id = id)
        } else {
            val newScan = scan.copy(id = System.currentTimeMillis())
            _scans.value = _scans.value + newScan
            newScan
        }
    }
    
    suspend fun getLatestScan(): BodyScan? {
        return bodyScanDao?.getLatestScan() ?: _scans.value.maxByOrNull { it.timestamp }
    }
    
    suspend fun getPreviousScan(): BodyScan? {
        val scans = bodyScanDao?.getAllScans()?.first() ?: _scans.value
        val sorted = scans.sortedByDescending { it.timestamp }
        return if (sorted.size >= 2) sorted[1] else null
    }
    
    suspend fun getScanById(id: Long): BodyScan? {
        return bodyScanDao?.getScanById(id) ?: _scans.value.find { it.id == id }
    }
    
    fun compareToPrevious(currentScan: BodyScan): BodyScanComparison {
        val previousScan = _scans.value
            .filter { it.timestamp < currentScan.timestamp }
            .maxByOrNull { it.timestamp }
        
        val daysBetween = if (previousScan != null) {
            ((currentScan.timestamp - previousScan.timestamp) / (1000 * 60 * 60 * 24)).toInt()
        } else 0
        
        val scoreChange = if (previousScan != null) {
            currentScan.overallScore - previousScan.overallScore
        } else 0
        
        val bodyFatChange = if (previousScan?.estimatedBodyFatPercentage != null && 
            currentScan.estimatedBodyFatPercentage != null) {
            currentScan.estimatedBodyFatPercentage - previousScan.estimatedBodyFatPercentage
        } else null
        
        val previousFocusZones = previousScan?.focusZones?.toSet() ?: emptySet()
        val currentFocusZones = currentScan.focusZones.toSet()
        
        val improvedZones = (previousFocusZones - currentFocusZones).toList()
        val needsMoreWorkZones = (currentFocusZones - previousFocusZones).toList()
        
        val postureImprovement = if (previousScan != null) {
            currentScan.postureAssessment.issues.size < previousScan.postureAssessment.issues.size
        } else false
        
        val progress = when {
            previousScan == null -> ProgressAssessment.FIRST_SCAN
            scoreChange >= 10 -> ProgressAssessment.EXCELLENT
            scoreChange >= 5 -> ProgressAssessment.GOOD
            scoreChange > 0 -> ProgressAssessment.STEADY
            scoreChange == 0 -> ProgressAssessment.PLATEAU
            else -> ProgressAssessment.SLOW
        }
        
        // Generate detailed improvement analysis
        val improvements = mutableListOf<ImprovementItem>()
        val declines = mutableListOf<ImprovementItem>()
        val unchanged = mutableListOf<ImprovementItem>()
        
        if (previousScan != null) {
            // Overall Score
            when {
                scoreChange > 0 -> improvements.add(ImprovementItem(
                    category = ImprovementCategory.OVERALL_SCORE,
                    title = "Overall Score Improved",
                    description = "Your overall body score increased by $scoreChange points",
                    previousValue = "${previousScan.overallScore}",
                    currentValue = "${currentScan.overallScore}",
                    changePercent = (scoreChange.toFloat() / previousScan.overallScore) * 100,
                    isPositive = true
                ))
                scoreChange < 0 -> declines.add(ImprovementItem(
                    category = ImprovementCategory.OVERALL_SCORE,
                    title = "Overall Score Decreased",
                    description = "Your overall body score decreased by ${-scoreChange} points",
                    previousValue = "${previousScan.overallScore}",
                    currentValue = "${currentScan.overallScore}",
                    changePercent = (scoreChange.toFloat() / previousScan.overallScore) * 100,
                    isPositive = false
                ))
                else -> unchanged.add(ImprovementItem(
                    category = ImprovementCategory.OVERALL_SCORE,
                    title = "Overall Score Stable",
                    description = "Your overall body score remained the same",
                    previousValue = "${previousScan.overallScore}",
                    currentValue = "${currentScan.overallScore}",
                    isPositive = true
                ))
            }
            
            // Body Fat
            bodyFatChange?.let { change ->
                val formattedPrev = String.format("%.1f%%", previousScan.estimatedBodyFatPercentage)
                val formattedCurr = String.format("%.1f%%", currentScan.estimatedBodyFatPercentage)
                when {
                    change < -0.5f -> improvements.add(ImprovementItem(
                        category = ImprovementCategory.BODY_FAT,
                        title = "Body Fat Reduced",
                        description = "Estimated body fat decreased by ${String.format("%.1f", -change)}%",
                        previousValue = formattedPrev,
                        currentValue = formattedCurr,
                        changePercent = change,
                        isPositive = true
                    ))
                    change > 0.5f -> declines.add(ImprovementItem(
                        category = ImprovementCategory.BODY_FAT,
                        title = "Body Fat Increased",
                        description = "Estimated body fat increased by ${String.format("%.1f", change)}%",
                        previousValue = formattedPrev,
                        currentValue = formattedCurr,
                        changePercent = change,
                        isPositive = false
                    ))
                    else -> unchanged.add(ImprovementItem(
                        category = ImprovementCategory.BODY_FAT,
                        title = "Body Fat Stable",
                        description = "Body composition remained relatively stable",
                        previousValue = formattedPrev,
                        currentValue = formattedCurr,
                        isPositive = true
                    ))
                }
            }
            
            // Posture
            val prevPostureIssues = previousScan.postureAssessment.issues.size
            val currPostureIssues = currentScan.postureAssessment.issues.size
            when {
                currPostureIssues < prevPostureIssues -> improvements.add(ImprovementItem(
                    category = ImprovementCategory.POSTURE,
                    title = "Posture Improved",
                    description = "Resolved ${prevPostureIssues - currPostureIssues} posture issue(s)",
                    previousValue = "$prevPostureIssues issues",
                    currentValue = "$currPostureIssues issues",
                    isPositive = true
                ))
                currPostureIssues > prevPostureIssues -> declines.add(ImprovementItem(
                    category = ImprovementCategory.POSTURE,
                    title = "New Posture Issues",
                    description = "Detected ${currPostureIssues - prevPostureIssues} new posture concern(s)",
                    previousValue = "$prevPostureIssues issues",
                    currentValue = "$currPostureIssues issues",
                    isPositive = false
                ))
                else -> unchanged.add(ImprovementItem(
                    category = ImprovementCategory.POSTURE,
                    title = "Posture Unchanged",
                    description = if (currPostureIssues == 0) "Maintaining good posture" else "Same posture concerns remain",
                    previousValue = "$prevPostureIssues issues",
                    currentValue = "$currPostureIssues issues",
                    isPositive = currPostureIssues == 0
                ))
            }
            
            // Muscle Balance / Symmetry
            val prevSymmetry = previousScan.muscleBalance.leftRightSymmetry
            val currSymmetry = currentScan.muscleBalance.leftRightSymmetry
            if (currSymmetry.ordinal < prevSymmetry.ordinal) {
                improvements.add(ImprovementItem(
                    category = ImprovementCategory.SYMMETRY,
                    title = "Symmetry Improved",
                    description = "Left-right balance has improved",
                    previousValue = prevSymmetry.displayName,
                    currentValue = currSymmetry.displayName,
                    isPositive = true
                ))
            } else if (currSymmetry.ordinal > prevSymmetry.ordinal) {
                declines.add(ImprovementItem(
                    category = ImprovementCategory.SYMMETRY,
                    title = "Symmetry Declined",
                    description = "Left-right balance needs attention",
                    previousValue = prevSymmetry.displayName,
                    currentValue = currSymmetry.displayName,
                    isPositive = false
                ))
            }
            
            // Focus Zones
            if (improvedZones.isNotEmpty()) {
                improvements.add(ImprovementItem(
                    category = ImprovementCategory.FOCUS_ZONES,
                    title = "Areas No Longer Need Focus",
                    description = "${improvedZones.joinToString(", ") { it.displayName }} have improved",
                    previousValue = "${previousFocusZones.size} focus areas",
                    currentValue = "${currentFocusZones.size} focus areas",
                    isPositive = true
                ))
            }
            if (needsMoreWorkZones.isNotEmpty()) {
                declines.add(ImprovementItem(
                    category = ImprovementCategory.FOCUS_ZONES,
                    title = "New Focus Areas Identified",
                    description = "${needsMoreWorkZones.joinToString(", ") { it.displayName }} need attention",
                    previousValue = "${previousFocusZones.size} focus areas",
                    currentValue = "${currentFocusZones.size} focus areas",
                    isPositive = false
                ))
            }
        }
        
        // Generate summary message
        val summaryMessage = generateSummaryMessage(
            previousScan = previousScan,
            scoreChange = scoreChange,
            daysBetween = daysBetween,
            improvements = improvements,
            declines = declines
        )
        
        // Generate recommendations
        val recommendations = generateRecommendations(
            currentScan = currentScan,
            declines = declines,
            needsMoreWorkZones = needsMoreWorkZones
        )
        
        return BodyScanComparison(
            currentScan = currentScan,
            previousScan = previousScan,
            daysBetween = daysBetween,
            scoreChange = scoreChange,
            bodyFatChange = bodyFatChange,
            improvedZones = improvedZones,
            needsMoreWorkZones = needsMoreWorkZones,
            postureImprovement = postureImprovement,
            overallProgress = progress,
            improvements = improvements,
            declines = declines,
            unchanged = unchanged,
            summaryMessage = summaryMessage,
            recommendations = recommendations
        )
    }
    
    private fun generateSummaryMessage(
        previousScan: BodyScan?,
        scoreChange: Int,
        daysBetween: Int,
        improvements: List<ImprovementItem>,
        declines: List<ImprovementItem>
    ): String {
        if (previousScan == null) {
            return "Welcome to your fitness journey! This is your baseline scan. We'll track your progress from here."
        }
        
        val timeFrame = when {
            daysBetween == 0 -> "today"
            daysBetween == 1 -> "since yesterday"
            daysBetween < 7 -> "in the past $daysBetween days"
            daysBetween < 14 -> "in the past week"
            daysBetween < 30 -> "in the past ${daysBetween / 7} weeks"
            else -> "in the past ${daysBetween / 30} month(s)"
        }
        
        return when {
            improvements.size > declines.size && scoreChange > 0 ->
                "Great progress $timeFrame! You've improved in ${improvements.size} area(s) with your score up by $scoreChange points. Keep up the excellent work!"
            improvements.size > declines.size ->
                "You're making progress $timeFrame! ${improvements.size} area(s) have improved. Stay consistent to see even better results."
            declines.size > improvements.size && scoreChange < 0 ->
                "Your scan shows some areas need attention $timeFrame. Focus on the ${declines.size} declining area(s) to get back on track."
            declines.size > improvements.size ->
                "Mixed results $timeFrame. While some areas declined, keep working on your routine. Consistency is key!"
            improvements.isEmpty() && declines.isEmpty() ->
                "Your body composition is stable $timeFrame. Consider intensifying your workouts for more progress."
            else ->
                "Balanced progress $timeFrame with improvements and areas to work on. Review the details below for specific insights."
        }
    }
    
    private fun generateRecommendations(
        currentScan: BodyScan,
        declines: List<ImprovementItem>,
        needsMoreWorkZones: List<BodyZone>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Based on declines
        declines.forEach { decline ->
            when (decline.category) {
                ImprovementCategory.BODY_FAT -> {
                    recommendations.add("Consider adding more cardio or adjusting your nutrition to manage body composition")
                }
                ImprovementCategory.POSTURE -> {
                    recommendations.add("Include posture-focused exercises like planks, wall angels, and stretching in your routine")
                }
                ImprovementCategory.SYMMETRY -> {
                    recommendations.add("Add unilateral exercises (single-arm/leg) to address muscle imbalances")
                }
                ImprovementCategory.FOCUS_ZONES -> {
                    // Handled below with specific zones
                }
                else -> {}
            }
        }
        
        // Based on focus zones
        needsMoreWorkZones.forEach { zone ->
            val exercise = when (zone) {
                BodyZone.UPPER_CHEST, BodyZone.LOWER_CHEST -> "bench press, push-ups, and chest flyes"
                BodyZone.UPPER_BACK, BodyZone.LATS -> "rows, pull-ups, and lat pulldowns"
                BodyZone.LOWER_BACK -> "deadlifts, back extensions, and good mornings"
                BodyZone.SHOULDERS -> "overhead press, lateral raises, and face pulls"
                BodyZone.BICEPS -> "bicep curls, hammer curls, and chin-ups"
                BodyZone.TRICEPS -> "tricep dips, pushdowns, and close-grip bench"
                BodyZone.FOREARMS -> "wrist curls, farmer's walks, and dead hangs"
                BodyZone.ABS, BodyZone.OBLIQUES, BodyZone.CORE -> "planks, crunches, and leg raises"
                BodyZone.GLUTES -> "hip thrusts, squats, and lunges"
                BodyZone.QUADS -> "squats, leg press, and lunges"
                BodyZone.HAMSTRINGS -> "deadlifts, leg curls, and Romanian deadlifts"
                BodyZone.CALVES -> "calf raises and jump rope"
            }
            recommendations.add("Focus on ${zone.displayName.lowercase()} with exercises like $exercise")
        }
        
        // General recommendations based on posture
        currentScan.postureAssessment.issues.forEach { issue ->
            issue.exercises.firstOrNull()?.let { exercise ->
                if (recommendations.none { it.contains(exercise) }) {
                    recommendations.add("For ${issue.type.displayName.lowercase()}: try $exercise")
                }
            }
        }
        
        // Limit to top 5 recommendations
        return recommendations.take(5)
    }
    
    suspend fun getProgressSummary(): BodyProgressSummary? {
        val scans = (bodyScanDao?.getAllScans()?.first() ?: _scans.value).sortedBy { it.timestamp }
        if (scans.isEmpty()) return null
        
        val scores = scans.map { it.overallScore }
        val firstScan = scans.first()
        val latestScan = scans.last()
        
        // Find zones that have improved (were in focus but no longer are)
        val allFocusZones = scans.flatMap { it.focusZones }
        val zoneFrequency = allFocusZones.groupingBy { it }.eachCount()
        
        // Zones that appeared early but not in recent scans
        val recentScans = scans.takeLast(3)
        val olderScans = scans.dropLast(3)
        val recentFocusZones = recentScans.flatMap { it.focusZones }.toSet()
        val olderFocusZones = olderScans.flatMap { it.focusZones }.toSet()
        val improvedZones = (olderFocusZones - recentFocusZones).toList()
        
        // Zones that persist across scans
        val persistentZones = zoneFrequency.entries
            .filter { it.value >= scans.size / 2 }
            .map { it.key }
        
        // Goal achievement (simplified - based on score improvement)
        val goalProgress = when {
            scans.size < 2 -> 0f
            else -> ((latestScan.overallScore - firstScan.overallScore).coerceIn(0, 30) / 30f * 100)
        }
        
        return BodyProgressSummary(
            totalScans = scans.size,
            firstScanDate = firstScan.timestamp,
            latestScanDate = latestScan.timestamp,
            averageScore = scores.average().toFloat(),
            bestScore = scores.maxOrNull() ?: 0,
            scoreImprovement = latestScan.overallScore - firstScan.overallScore,
            consistentlyImprovedZones = improvedZones,
            persistentFocusZones = persistentZones,
            goalAchievementProgress = goalProgress
        )
    }
    
    suspend fun getScansWithDetails(): List<BodyScanWithDetails> {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val scans = bodyScanDao?.getAllScans()?.first() ?: _scans.value
        
        return scans
            .sortedByDescending { it.timestamp }
            .map { scan ->
                BodyScanWithDetails(
                    scan = scan,
                    formattedDate = dateFormat.format(Date(scan.timestamp)),
                    goalDisplayName = scan.userGoal.displayName,
                    focusZoneCount = scan.focusZones.size,
                    hasPostureIssues = scan.postureAssessment.issues.isNotEmpty()
                )
            }
    }
    
    suspend fun getScoreHistory(limit: Int = 10): List<Pair<Long, Int>> {
        val scans = bodyScanDao?.getAllScans()?.first() ?: _scans.value
        return scans
            .sortedByDescending { it.timestamp }
            .take(limit)
            .map { it.timestamp to it.overallScore }
            .reversed()
    }
    
    suspend fun deleteScan(scanId: Long) {
        bodyScanDao?.let { dao ->
            dao.getScanById(scanId)?.let { scan -> dao.deleteScan(scan) }
        } ?: run {
            _scans.value = _scans.value.filter { it.id != scanId }
        }
    }
    
    /**
     * Generate analysis result with recommendations based on scan data
     */
    fun generateAnalysisResult(scan: BodyScan): BodyAnalysisResult {
        val zoneRecommendations = generateZoneRecommendations(scan)
        val workoutRecommendations = generateWorkoutRecommendations(scan)
        val nutritionRecommendations = generateNutritionRecommendations(scan)
        val weeklyPlan = generateWeeklyPlan(scan)
        
        return BodyAnalysisResult(
            scan = scan,
            zoneRecommendations = zoneRecommendations,
            workoutRecommendations = workoutRecommendations,
            nutritionRecommendations = nutritionRecommendations,
            weeklyPlanSuggestion = weeklyPlan,
            progressNotes = generateProgressNotes(scan)
        )
    }
    
    private fun generateZoneRecommendations(scan: BodyScan): List<ZoneRecommendation> {
        return scan.focusZones.mapIndexed { index, zone ->
            val priority = when {
                index < 2 -> ZonePriority.HIGH
                index < 4 -> ZonePriority.MEDIUM
                else -> ZonePriority.LOW
            }
            
            ZoneRecommendation(
                zone = zone,
                priority = priority,
                currentAssessment = when (priority) {
                    ZonePriority.HIGH -> "Needs Development"
                    ZonePriority.MEDIUM -> "Could Be Stronger"
                    ZonePriority.LOW -> "Minor Focus"
                },
                recommendedExercises = getExercisesForZone(zone, scan.userGoal),
                weeklyFrequency = when (priority) {
                    ZonePriority.HIGH -> 3
                    ZonePriority.MEDIUM -> 2
                    ZonePriority.LOW -> 1
                },
                notes = getZoneNotes(zone, scan.userGoal)
            )
        }
    }
    
    private fun getExercisesForZone(zone: BodyZone, goal: FitnessGoal): List<String> {
        val baseExercises = when (zone) {
            BodyZone.UPPER_CHEST -> listOf("Incline Bench Press", "Incline Dumbbell Flyes", "Low-to-High Cable Flyes")
            BodyZone.LOWER_CHEST -> listOf("Decline Bench Press", "Dips", "High-to-Low Cable Flyes")
            BodyZone.SHOULDERS -> listOf("Overhead Press", "Lateral Raises", "Face Pulls", "Rear Delt Flyes")
            BodyZone.UPPER_BACK -> listOf("Barbell Rows", "Face Pulls", "Shrugs", "Reverse Flyes")
            BodyZone.LATS -> listOf("Pull-Ups", "Lat Pulldowns", "Single-Arm Rows", "Straight-Arm Pulldowns")
            BodyZone.LOWER_BACK -> listOf("Deadlifts", "Good Mornings", "Back Extensions", "Bird Dogs")
            BodyZone.BICEPS -> listOf("Barbell Curls", "Hammer Curls", "Incline Dumbbell Curls", "Preacher Curls")
            BodyZone.TRICEPS -> listOf("Close-Grip Bench Press", "Tricep Dips", "Skull Crushers", "Rope Pushdowns")
            BodyZone.FOREARMS -> listOf("Wrist Curls", "Reverse Curls", "Farmer's Walks", "Dead Hangs")
            BodyZone.ABS -> listOf("Hanging Leg Raises", "Cable Crunches", "Ab Wheel Rollouts", "Planks")
            BodyZone.OBLIQUES -> listOf("Russian Twists", "Side Planks", "Woodchops", "Bicycle Crunches")
            BodyZone.QUADS -> listOf("Squats", "Leg Press", "Lunges", "Leg Extensions")
            BodyZone.HAMSTRINGS -> listOf("Romanian Deadlifts", "Leg Curls", "Good Mornings", "Nordic Curls")
            BodyZone.GLUTES -> listOf("Hip Thrusts", "Bulgarian Split Squats", "Glute Bridges", "Cable Kickbacks")
            BodyZone.CALVES -> listOf("Standing Calf Raises", "Seated Calf Raises", "Donkey Calf Raises")
            BodyZone.CORE -> listOf("Dead Bugs", "Pallof Press", "Planks", "Bird Dogs")
        }
        
        return when (goal) {
            FitnessGoal.LOSE_WEIGHT -> baseExercises.take(2) + listOf("Add HIIT circuits")
            FitnessGoal.BUILD_MUSCLE -> baseExercises
            FitnessGoal.TONE_UP -> baseExercises.take(3)
            else -> baseExercises.take(3)
        }
    }
    
    private fun getZoneNotes(zone: BodyZone, goal: FitnessGoal): String {
        return when (goal) {
            FitnessGoal.LOSE_WEIGHT -> "Focus on higher reps (12-15) with shorter rest periods"
            FitnessGoal.BUILD_MUSCLE -> "Use progressive overload with 8-12 reps per set"
            FitnessGoal.INCREASE_STRENGTH -> "Focus on heavy compound movements with 4-6 reps"
            FitnessGoal.TONE_UP -> "Moderate weight with 10-15 reps, emphasize mind-muscle connection"
            else -> "Train with proper form and progressive overload"
        }
    }
    
    private fun generateWorkoutRecommendations(scan: BodyScan): List<WorkoutRecommendation> {
        val recommendations = mutableListOf<WorkoutRecommendation>()
        
        // Primary workout based on goal
        when (scan.userGoal) {
            FitnessGoal.LOSE_WEIGHT -> {
                recommendations.add(WorkoutRecommendation(
                    title = "Fat-Burning Circuit Training",
                    description = "High-intensity circuits to maximize calorie burn and boost metabolism",
                    type = WorkoutRecommendationType.HIIT,
                    frequency = "4x per week",
                    duration = "30-40 min",
                    exercises = listOf(
                        RecommendedExercise("Burpees", "3", "12", "Full body explosive movement"),
                        RecommendedExercise("Mountain Climbers", "3", "30 sec", "Core and cardio"),
                        RecommendedExercise("Jump Squats", "3", "15", "Lower body power"),
                        RecommendedExercise("Push-Up to Renegade Row", "3", "10 each", "Upper body compound")
                    ),
                    priority = 1
                ))
                recommendations.add(WorkoutRecommendation(
                    title = "Steady-State Cardio",
                    description = "Low-intensity cardio for active recovery and additional calorie burn",
                    type = WorkoutRecommendationType.CARDIO,
                    frequency = "2-3x per week",
                    duration = "30-45 min",
                    exercises = listOf(
                        RecommendedExercise("Brisk Walking", "1", "30-45 min", "Keep heart rate at 60-70% max"),
                        RecommendedExercise("Cycling", "1", "30 min", "Low impact option"),
                        RecommendedExercise("Swimming", "1", "30 min", "Full body, joint-friendly")
                    ),
                    priority = 2
                ))
            }
            FitnessGoal.BUILD_MUSCLE -> {
                recommendations.add(WorkoutRecommendation(
                    title = "Hypertrophy Training",
                    description = "Volume-focused training to maximize muscle growth",
                    type = WorkoutRecommendationType.HYPERTROPHY,
                    frequency = "4-5x per week",
                    duration = "60-75 min",
                    exercises = listOf(
                        RecommendedExercise("Compound Lifts", "4", "8-12", "Focus on progressive overload"),
                        RecommendedExercise("Isolation Exercises", "3", "10-15", "Target weak points"),
                        RecommendedExercise("Drop Sets", "2", "To failure", "On final sets for intensity")
                    ),
                    priority = 1
                ))
            }
            FitnessGoal.INCREASE_STRENGTH -> {
                recommendations.add(WorkoutRecommendation(
                    title = "Strength Training",
                    description = "Heavy compound movements to build maximal strength",
                    type = WorkoutRecommendationType.STRENGTH,
                    frequency = "4x per week",
                    duration = "60-90 min",
                    exercises = listOf(
                        RecommendedExercise("Squats", "5", "5", "Work up to heavy singles"),
                        RecommendedExercise("Deadlifts", "5", "3-5", "Focus on form under load"),
                        RecommendedExercise("Bench Press", "5", "5", "Pause reps for strength"),
                        RecommendedExercise("Overhead Press", "4", "5", "Strict form")
                    ),
                    priority = 1
                ))
            }
            else -> {
                recommendations.add(WorkoutRecommendation(
                    title = "Balanced Fitness Program",
                    description = "Well-rounded training for overall health and fitness",
                    type = WorkoutRecommendationType.STRENGTH,
                    frequency = "3-4x per week",
                    duration = "45-60 min",
                    exercises = listOf(
                        RecommendedExercise("Full Body Circuits", "3", "12-15", "Compound movements"),
                        RecommendedExercise("Core Work", "3", "15-20", "Stability focused"),
                        RecommendedExercise("Cardio Finisher", "1", "10-15 min", "Moderate intensity")
                    ),
                    priority = 1
                ))
            }
        }
        
        // Add posture correction if needed
        if (scan.postureAssessment.issues.isNotEmpty()) {
            recommendations.add(WorkoutRecommendation(
                title = "Posture Correction",
                description = "Targeted exercises to improve posture and reduce imbalances",
                type = WorkoutRecommendationType.POSTURE_CORRECTION,
                frequency = "Daily",
                duration = "10-15 min",
                exercises = scan.postureAssessment.issues.flatMap { issue ->
                    issue.exercises.map { exercise ->
                        RecommendedExercise(exercise, "2-3", "10-15", "Focus on form")
                    }
                }.take(4),
                priority = 2
            ))
        }
        
        return recommendations
    }
    
    private fun generateNutritionRecommendations(scan: BodyScan): List<NutritionRecommendation> {
        val recommendations = mutableListOf<NutritionRecommendation>()
        
        when (scan.userGoal) {
            FitnessGoal.LOSE_WEIGHT -> {
                recommendations.add(NutritionRecommendation(
                    category = NutritionCategory.CALORIES,
                    title = "Caloric Deficit",
                    description = "Create a moderate calorie deficit of 300-500 calories below maintenance",
                    tips = listOf(
                        "Track your food intake for accuracy",
                        "Focus on volume eating with vegetables",
                        "Avoid liquid calories",
                        "Plan meals ahead to avoid impulsive eating"
                    ),
                    priority = 1
                ))
                recommendations.add(NutritionRecommendation(
                    category = NutritionCategory.PROTEIN,
                    title = "High Protein Intake",
                    description = "Maintain muscle mass while losing fat with adequate protein",
                    tips = listOf(
                        "Aim for 1.6-2.2g protein per kg bodyweight",
                        "Include protein in every meal",
                        "Lean sources: chicken, fish, Greek yogurt, eggs",
                        "Consider a protein shake post-workout"
                    ),
                    priority = 2
                ))
            }
            FitnessGoal.BUILD_MUSCLE -> {
                recommendations.add(NutritionRecommendation(
                    category = NutritionCategory.CALORIES,
                    title = "Caloric Surplus",
                    description = "Eat in a slight surplus of 200-400 calories to fuel muscle growth",
                    tips = listOf(
                        "Increase calories gradually",
                        "Focus on nutrient-dense foods",
                        "Time carbs around workouts",
                        "Don't fear healthy fats"
                    ),
                    priority = 1
                ))
                recommendations.add(NutritionRecommendation(
                    category = NutritionCategory.PROTEIN,
                    title = "Maximize Protein Synthesis",
                    description = "Optimize protein intake for muscle building",
                    tips = listOf(
                        "Aim for 1.8-2.4g protein per kg bodyweight",
                        "Spread protein across 4-5 meals",
                        "Include leucine-rich sources",
                        "Post-workout protein within 2 hours"
                    ),
                    priority = 2
                ))
                recommendations.add(NutritionRecommendation(
                    category = NutritionCategory.CARBS,
                    title = "Fuel Your Training",
                    description = "Adequate carbohydrates for energy and recovery",
                    tips = listOf(
                        "Prioritize complex carbs: oats, rice, potatoes",
                        "Pre-workout carbs 1-2 hours before training",
                        "Post-workout carbs to replenish glycogen",
                        "Adjust based on training intensity"
                    ),
                    priority = 3
                ))
            }
            else -> {
                recommendations.add(NutritionRecommendation(
                    category = NutritionCategory.PROTEIN,
                    title = "Adequate Protein",
                    description = "Support your fitness goals with sufficient protein",
                    tips = listOf(
                        "Aim for 1.4-1.8g protein per kg bodyweight",
                        "Include protein in every meal",
                        "Variety of sources for complete amino acids"
                    ),
                    priority = 1
                ))
            }
        }
        
        // Always add hydration
        recommendations.add(NutritionRecommendation(
            category = NutritionCategory.HYDRATION,
            title = "Stay Hydrated",
            description = "Proper hydration is essential for performance and recovery",
            tips = listOf(
                "Drink at least 2-3 liters of water daily",
                "Increase intake on training days",
                "Monitor urine color (pale yellow is ideal)",
                "Electrolytes for intense or long workouts"
            ),
            priority = 4
        ))
        
        return recommendations.sortedBy { it.priority }
    }
    
    private fun generateWeeklyPlan(scan: BodyScan): WeeklyPlanSuggestion {
        val (trainingDays, cardio, strength, split) = when (scan.userGoal) {
            FitnessGoal.LOSE_WEIGHT -> listOf(5, 3, 2, TrainingSplit.CARDIO_STRENGTH)
            FitnessGoal.BUILD_MUSCLE -> listOf(5, 1, 4, TrainingSplit.PUSH_PULL_LEGS)
            FitnessGoal.INCREASE_STRENGTH -> listOf(4, 0, 4, TrainingSplit.UPPER_LOWER)
            FitnessGoal.TONE_UP -> listOf(4, 2, 2, TrainingSplit.FULL_BODY)
            FitnessGoal.IMPROVE_ENDURANCE -> listOf(5, 4, 1, TrainingSplit.CARDIO_STRENGTH)
            else -> listOf(4, 1, 3, TrainingSplit.UPPER_LOWER)
        }
        
        val dailyBreakdown = when (split as TrainingSplit) {
            TrainingSplit.PUSH_PULL_LEGS -> listOf(
                DayPlan("Monday", "Push (Chest, Shoulders, Triceps)", "60 min"),
                DayPlan("Tuesday", "Pull (Back, Biceps)", "60 min"),
                DayPlan("Wednesday", "Legs & Core", "60 min"),
                DayPlan("Thursday", "Rest", "", true),
                DayPlan("Friday", "Push", "60 min"),
                DayPlan("Saturday", "Pull", "60 min"),
                DayPlan("Sunday", "Rest", "", true)
            )
            TrainingSplit.UPPER_LOWER -> listOf(
                DayPlan("Monday", "Upper Body", "60 min"),
                DayPlan("Tuesday", "Lower Body", "60 min"),
                DayPlan("Wednesday", "Rest or Light Cardio", "30 min", true),
                DayPlan("Thursday", "Upper Body", "60 min"),
                DayPlan("Friday", "Lower Body", "60 min"),
                DayPlan("Saturday", "Active Recovery", "30 min"),
                DayPlan("Sunday", "Rest", "", true)
            )
            TrainingSplit.CARDIO_STRENGTH -> listOf(
                DayPlan("Monday", "HIIT Cardio", "30 min"),
                DayPlan("Tuesday", "Full Body Strength", "45 min"),
                DayPlan("Wednesday", "Steady Cardio", "40 min"),
                DayPlan("Thursday", "Rest", "", true),
                DayPlan("Friday", "HIIT Cardio", "30 min"),
                DayPlan("Saturday", "Full Body Strength", "45 min"),
                DayPlan("Sunday", "Active Recovery", "30 min")
            )
            else -> listOf(
                DayPlan("Monday", "Full Body", "50 min"),
                DayPlan("Tuesday", "Cardio", "30 min"),
                DayPlan("Wednesday", "Rest", "", true),
                DayPlan("Thursday", "Full Body", "50 min"),
                DayPlan("Friday", "Cardio", "30 min"),
                DayPlan("Saturday", "Full Body", "50 min"),
                DayPlan("Sunday", "Rest", "", true)
            )
        }
        
        return WeeklyPlanSuggestion(
            totalTrainingDays = trainingDays as Int,
            cardioSessions = cardio as Int,
            strengthSessions = strength as Int,
            restDays = 7 - trainingDays,
            splitType = split,
            dailyBreakdown = dailyBreakdown
        )
    }
    
    private fun generateProgressNotes(scan: BodyScan): String {
        val notes = StringBuilder()
        
        if (scan.focusZones.isNotEmpty()) {
            notes.append("Focus on developing your ${scan.focusZones.take(2).joinToString(" and ") { it.displayName }}. ")
        }
        
        if (scan.postureAssessment.issues.isNotEmpty()) {
            notes.append("Address posture issues to prevent injury and improve performance. ")
        }
        
        notes.append("Stay consistent with your ${scan.userGoal.displayName.lowercase()} goal!")
        
        return notes.toString()
    }
}
