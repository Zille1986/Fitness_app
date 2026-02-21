package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

/**
 * Represents a body analysis scan with photo and AI-generated recommendations
 */
@Entity(tableName = "body_scans")
@TypeConverters(Converters::class)
data class BodyScan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val photoPath: String, // Local path to the saved photo
    val userGoal: FitnessGoal,
    val bodyType: BodyType,
    val estimatedBodyFatPercentage: Float? = null,
    val focusZones: List<BodyZone>,
    val overallScore: Int, // 0-100 fitness readiness score
    val muscleBalance: MuscleBalanceAssessment,
    val postureAssessment: PostureAssessment,
    val notes: String? = null
)

/**
 * User's primary fitness goal
 */
enum class FitnessGoal(val displayName: String, val description: String) {
    LOSE_WEIGHT("Lose Weight", "Focus on fat loss and cardio"),
    BUILD_MUSCLE("Build Muscle", "Focus on strength training and muscle growth"),
    TONE_UP("Tone & Define", "Reduce body fat while maintaining muscle"),
    IMPROVE_ENDURANCE("Improve Endurance", "Build cardiovascular fitness"),
    INCREASE_STRENGTH("Increase Strength", "Maximize power and lifting capacity"),
    GENERAL_FITNESS("General Fitness", "Overall health and wellness"),
    ATHLETIC_PERFORMANCE("Athletic Performance", "Sport-specific training"),
    BODY_RECOMPOSITION("Body Recomposition", "Lose fat and gain muscle simultaneously")
}

/**
 * Body type classification
 */
enum class BodyType(val displayName: String, val characteristics: String) {
    ECTOMORPH("Ectomorph", "Lean build, fast metabolism, difficulty gaining weight"),
    MESOMORPH("Mesomorph", "Athletic build, gains muscle easily, moderate metabolism"),
    ENDOMORPH("Endomorph", "Wider build, slower metabolism, gains weight easily"),
    ECTO_MESOMORPH("Ecto-Mesomorph", "Lean but can build muscle with effort"),
    ENDO_MESOMORPH("Endo-Mesomorph", "Athletic but prone to weight gain")
}

/**
 * Body zones that can be targeted for improvement
 */
enum class BodyZone(val displayName: String, val muscleGroups: List<String>) {
    UPPER_CHEST("Upper Chest", listOf("Pectoralis Major (Clavicular)")),
    LOWER_CHEST("Lower Chest", listOf("Pectoralis Major (Sternal)")),
    SHOULDERS("Shoulders", listOf("Anterior Deltoid", "Lateral Deltoid", "Posterior Deltoid")),
    UPPER_BACK("Upper Back", listOf("Trapezius", "Rhomboids", "Rear Deltoids")),
    LATS("Lats", listOf("Latissimus Dorsi")),
    LOWER_BACK("Lower Back", listOf("Erector Spinae")),
    BICEPS("Biceps", listOf("Biceps Brachii", "Brachialis")),
    TRICEPS("Triceps", listOf("Triceps Brachii")),
    FOREARMS("Forearms", listOf("Forearm Flexors", "Forearm Extensors")),
    ABS("Abs", listOf("Rectus Abdominis", "Transverse Abdominis")),
    OBLIQUES("Obliques", listOf("Internal Obliques", "External Obliques")),
    QUADS("Quadriceps", listOf("Rectus Femoris", "Vastus Lateralis", "Vastus Medialis")),
    HAMSTRINGS("Hamstrings", listOf("Biceps Femoris", "Semitendinosus", "Semimembranosus")),
    GLUTES("Glutes", listOf("Gluteus Maximus", "Gluteus Medius")),
    CALVES("Calves", listOf("Gastrocnemius", "Soleus")),
    CORE("Core", listOf("Deep Core Stabilizers", "Hip Flexors"))
}

/**
 * Priority level for focusing on a body zone
 */
enum class ZonePriority {
    HIGH, MEDIUM, LOW
}

/**
 * Recommendation for a specific body zone
 */
data class ZoneRecommendation(
    val zone: BodyZone,
    val priority: ZonePriority,
    val currentAssessment: String, // e.g., "Underdeveloped", "Well-balanced", "Strong"
    val recommendedExercises: List<String>,
    val weeklyFrequency: Int, // Recommended training days per week
    val notes: String? = null
)

/**
 * Muscle balance assessment
 */
data class MuscleBalanceAssessment(
    val overallBalance: BalanceLevel,
    val leftRightSymmetry: BalanceLevel,
    val frontBackBalance: BalanceLevel,
    val upperLowerBalance: BalanceLevel,
    val imbalances: List<MuscleImbalance>
)

enum class BalanceLevel(val displayName: String) {
    BALANCED("Balanced"),
    SLIGHT_IMBALANCE("Slight Imbalance"),
    MODERATE_IMBALANCE("Moderate Imbalance"),
    SIGNIFICANT_IMBALANCE("Significant Imbalance")
}

data class MuscleImbalance(
    val description: String,
    val affectedZones: List<BodyZone>,
    val correction: String
)

/**
 * Posture assessment
 */
data class PostureAssessment(
    val overallPosture: PostureLevel,
    val issues: List<PostureIssue>
)

enum class PostureLevel {
    EXCELLENT, GOOD, FAIR, POOR
}

data class PostureIssue(
    val type: PostureIssueType,
    val severity: IssueSeverity,
    val description: String,
    val exercises: List<String>
)

enum class PostureIssueType(val displayName: String) {
    FORWARD_HEAD("Forward Head Posture"),
    ROUNDED_SHOULDERS("Rounded Shoulders"),
    KYPHOSIS("Upper Back Rounding"),
    LORDOSIS("Excessive Lower Back Curve"),
    ANTERIOR_PELVIC_TILT("Anterior Pelvic Tilt"),
    POSTERIOR_PELVIC_TILT("Posterior Pelvic Tilt"),
    SCOLIOSIS("Lateral Spine Curvature"),
    UNEVEN_SHOULDERS("Uneven Shoulders"),
    UNEVEN_HIPS("Uneven Hips")
}

enum class IssueSeverity {
    MILD, MODERATE, SEVERE
}

/**
 * Complete analysis result with all recommendations
 */
data class BodyAnalysisResult(
    val scan: BodyScan,
    val zoneRecommendations: List<ZoneRecommendation>,
    val workoutRecommendations: List<WorkoutRecommendation>,
    val nutritionRecommendations: List<NutritionRecommendation>,
    val weeklyPlanSuggestion: WeeklyPlanSuggestion,
    val progressNotes: String? = null
)

/**
 * Workout recommendation based on analysis
 */
data class WorkoutRecommendation(
    val title: String,
    val description: String,
    val type: WorkoutRecommendationType,
    val frequency: String, // e.g., "3x per week"
    val duration: String, // e.g., "45-60 min"
    val exercises: List<RecommendedExercise>,
    val priority: Int // 1 = highest priority
)

enum class WorkoutRecommendationType {
    STRENGTH, HYPERTROPHY, CARDIO, HIIT, FLEXIBILITY, RECOVERY, POSTURE_CORRECTION
}

data class RecommendedExercise(
    val name: String,
    val sets: String,
    val reps: String,
    val notes: String? = null
)

/**
 * Nutrition recommendation
 */
data class NutritionRecommendation(
    val category: NutritionCategory,
    val title: String,
    val description: String,
    val tips: List<String>,
    val priority: Int
)

enum class NutritionCategory(val displayName: String, val icon: String) {
    PROTEIN("Protein", "🥩"),
    CARBS("Carbohydrates", "🍚"),
    FATS("Healthy Fats", "🥑"),
    HYDRATION("Hydration", "💧"),
    TIMING("Meal Timing", "⏰"),
    SUPPLEMENTS("Supplements", "💊"),
    CALORIES("Calorie Balance", "🔥"),
    MICRONUTRIENTS("Vitamins & Minerals", "🥗")
}

/**
 * Weekly training plan suggestion
 */
data class WeeklyPlanSuggestion(
    val totalTrainingDays: Int,
    val cardioSessions: Int,
    val strengthSessions: Int,
    val restDays: Int,
    val splitType: TrainingSplit,
    val dailyBreakdown: List<DayPlan>
)

enum class TrainingSplit(val displayName: String) {
    FULL_BODY("Full Body"),
    UPPER_LOWER("Upper/Lower"),
    PUSH_PULL_LEGS("Push/Pull/Legs"),
    BRO_SPLIT("Body Part Split"),
    CARDIO_STRENGTH("Cardio + Strength")
}

data class DayPlan(
    val dayOfWeek: String,
    val focus: String,
    val duration: String,
    val isRestDay: Boolean = false
)

/**
 * Comparison between two body scans
 */
data class BodyScanComparison(
    val currentScan: BodyScan,
    val previousScan: BodyScan?,
    val daysBetween: Int,
    val scoreChange: Int,
    val bodyFatChange: Float?,
    val improvedZones: List<BodyZone>,
    val needsMoreWorkZones: List<BodyZone>,
    val postureImprovement: Boolean,
    val overallProgress: ProgressAssessment,
    // Detailed improvement analysis
    val improvements: List<ImprovementItem> = emptyList(),
    val declines: List<ImprovementItem> = emptyList(),
    val unchanged: List<ImprovementItem> = emptyList(),
    val summaryMessage: String = "",
    val recommendations: List<String> = emptyList()
)

/**
 * Individual improvement/decline item for comparison
 */
data class ImprovementItem(
    val category: ImprovementCategory,
    val title: String,
    val description: String,
    val previousValue: String?,
    val currentValue: String,
    val changePercent: Float? = null,
    val isPositive: Boolean = true
)

enum class ImprovementCategory(val displayName: String, val icon: String) {
    OVERALL_SCORE("Overall Score", "📊"),
    BODY_FAT("Body Composition", "⚖️"),
    POSTURE("Posture", "🧍"),
    MUSCLE_BALANCE("Muscle Balance", "💪"),
    FOCUS_ZONES("Focus Areas", "🎯"),
    SYMMETRY("Symmetry", "↔️")
}

enum class ProgressAssessment(val displayName: String, val description: String) {
    EXCELLENT("Excellent Progress", "You're making great strides toward your goals!"),
    GOOD("Good Progress", "You're on the right track, keep it up!"),
    STEADY("Steady Progress", "Consistent effort is showing results."),
    SLOW("Slow Progress", "Consider adjusting your routine for better results."),
    PLATEAU("Plateau", "Time to shake things up with new challenges."),
    FIRST_SCAN("First Scan", "Great start! We'll track your progress from here.")
}

/**
 * Body scan with additional display information
 */
data class BodyScanWithDetails(
    val scan: BodyScan,
    val formattedDate: String,
    val goalDisplayName: String,
    val focusZoneCount: Int,
    val hasPostureIssues: Boolean
)

/**
 * Progress summary across all scans
 */
data class BodyProgressSummary(
    val totalScans: Int,
    val firstScanDate: Long?,
    val latestScanDate: Long?,
    val averageScore: Float,
    val bestScore: Int,
    val scoreImprovement: Int, // Latest vs first
    val consistentlyImprovedZones: List<BodyZone>,
    val persistentFocusZones: List<BodyZone>,
    val goalAchievementProgress: Float // 0-100%
)
