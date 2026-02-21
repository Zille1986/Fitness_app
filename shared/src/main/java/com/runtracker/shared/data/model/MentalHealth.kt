package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mood entry for tracking emotional state before/after workouts
 */
@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mood: MoodLevel,
    val energy: EnergyLevel,
    val stress: StressLevel,
    val notes: String = "",
    val relatedRunId: Long? = null,
    val isPreWorkout: Boolean = true,
    val tags: List<String> = emptyList()
)

enum class MoodLevel(val value: Int, val emoji: String, val label: String) {
    VERY_LOW(1, "😢", "Very Low"),
    LOW(2, "😔", "Low"),
    NEUTRAL(3, "😐", "Neutral"),
    GOOD(4, "🙂", "Good"),
    GREAT(5, "😄", "Great")
}

enum class EnergyLevel(val value: Int, val emoji: String, val label: String) {
    EXHAUSTED(1, "😴", "Exhausted"),
    TIRED(2, "🥱", "Tired"),
    MODERATE(3, "😊", "Moderate"),
    ENERGIZED(4, "💪", "Energized"),
    PUMPED(5, "🔥", "Pumped")
}

enum class StressLevel(val value: Int, val emoji: String, val label: String) {
    VERY_HIGH(5, "😰", "Very High"),
    HIGH(4, "😟", "High"),
    MODERATE(3, "😐", "Moderate"),
    LOW(2, "😌", "Low"),
    VERY_LOW(1, "😊", "Very Low")
}

/**
 * Mindfulness/meditation session record
 */
@Entity(tableName = "mindfulness_sessions")
data class MindfulnessSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MindfulnessType,
    val durationSeconds: Int,
    val completed: Boolean = true,
    val relatedRunId: Long? = null,
    val rating: Int? = null // 1-5 stars
)

enum class MindfulnessType(val title: String, val description: String, val defaultDuration: Int) {
    PRE_RUN_FOCUS("Pre-Run Focus", "Clear your mind and set intentions for your run", 180),
    POST_RUN_GRATITUDE("Post-Run Gratitude", "Reflect on your accomplishment and express gratitude", 180),
    BREATHING_EXERCISE("Breathing Exercise", "Calm your nervous system with guided breathing", 120),
    BODY_SCAN("Body Scan", "Check in with your body from head to toe", 300),
    VISUALIZATION("Visualization", "Visualize your perfect run or race", 240),
    STRESS_RELIEF("Stress Relief", "Release tension and find calm", 300),
    SLEEP_PREPARATION("Sleep Preparation", "Wind down for restful recovery sleep", 600),
    MORNING_ENERGIZER("Morning Energizer", "Start your day with positive energy", 180)
}

/**
 * Breathing exercise pattern
 */
data class BreathingPattern(
    val name: String,
    val description: String,
    val inhaleSeconds: Int,
    val holdAfterInhale: Int,
    val exhaleSeconds: Int,
    val holdAfterExhale: Int,
    val cycles: Int,
    val benefits: List<String>
) {
    val cycleDuration: Int
        get() = inhaleSeconds + holdAfterInhale + exhaleSeconds + holdAfterExhale
    
    val totalDuration: Int
        get() = cycleDuration * cycles
}

/**
 * Pre-defined breathing patterns
 */
object BreathingPatterns {
    val boxBreathing = BreathingPattern(
        name = "Box Breathing",
        description = "Equal parts inhale, hold, exhale, hold. Used by Navy SEALs for stress relief.",
        inhaleSeconds = 4,
        holdAfterInhale = 4,
        exhaleSeconds = 4,
        holdAfterExhale = 4,
        cycles = 6,
        benefits = listOf("Reduces stress", "Improves focus", "Calms nervous system")
    )
    
    val relaxingBreath = BreathingPattern(
        name = "4-7-8 Relaxing Breath",
        description = "Dr. Andrew Weil's technique for relaxation and sleep.",
        inhaleSeconds = 4,
        holdAfterInhale = 7,
        exhaleSeconds = 8,
        holdAfterExhale = 0,
        cycles = 4,
        benefits = listOf("Promotes sleep", "Reduces anxiety", "Lowers heart rate")
    )
    
    val energizingBreath = BreathingPattern(
        name = "Energizing Breath",
        description = "Quick, powerful breaths to increase alertness before a run.",
        inhaleSeconds = 2,
        holdAfterInhale = 0,
        exhaleSeconds = 2,
        holdAfterExhale = 0,
        cycles = 15,
        benefits = listOf("Increases energy", "Improves alertness", "Warms up lungs")
    )
    
    val calmingBreath = BreathingPattern(
        name = "Extended Exhale",
        description = "Longer exhale activates parasympathetic nervous system.",
        inhaleSeconds = 4,
        holdAfterInhale = 0,
        exhaleSeconds = 8,
        holdAfterExhale = 2,
        cycles = 6,
        benefits = listOf("Activates rest response", "Slows heart rate", "Reduces cortisol")
    )
    
    val recoveryBreath = BreathingPattern(
        name = "Post-Run Recovery",
        description = "Gentle breathing to aid recovery after intense exercise.",
        inhaleSeconds = 5,
        holdAfterInhale = 2,
        exhaleSeconds = 5,
        holdAfterExhale = 2,
        cycles = 8,
        benefits = listOf("Speeds recovery", "Reduces lactic acid", "Restores calm")
    )
    
    fun getAll(): List<BreathingPattern> = listOf(
        boxBreathing, relaxingBreath, energizingBreath, calmingBreath, recoveryBreath
    )
    
    fun getPreRunPatterns(): List<BreathingPattern> = listOf(energizingBreath, boxBreathing)
    fun getPostRunPatterns(): List<BreathingPattern> = listOf(recoveryBreath, calmingBreath)
    fun getSleepPatterns(): List<BreathingPattern> = listOf(relaxingBreath, calmingBreath)
}

/**
 * Daily wellness check-in
 */
@Entity(tableName = "wellness_checkins")
data class WellnessCheckin(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long, // Start of day timestamp
    val sleepHours: Float? = null,
    val sleepQuality: Int? = null, // 1-5
    val restingHeartRate: Int? = null,
    val hrv: Int? = null, // Heart Rate Variability
    val mood: MoodLevel? = null,
    val energy: EnergyLevel? = null,
    val stress: StressLevel? = null,
    val soreness: Int? = null, // 1-5, muscle soreness
    val hydration: Int? = null, // 1-5
    val notes: String = "",
    val readinessScore: Int? = null // Calculated 0-100
)

/**
 * Guided meditation/mindfulness content
 */
data class MindfulnessContent(
    val id: String,
    val title: String,
    val description: String,
    val type: MindfulnessType,
    val durationSeconds: Int,
    val audioUrl: String? = null,
    val instructions: List<String>,
    val tags: List<String> = emptyList()
)

/**
 * Pre-defined mindfulness sessions
 */
object MindfulnessSessions {
    
    val preRunFocus = MindfulnessContent(
        id = "pre_run_focus",
        title = "Pre-Run Mental Preparation",
        description = "Get mentally ready for your run with focused breathing and intention setting.",
        type = MindfulnessType.PRE_RUN_FOCUS,
        durationSeconds = 180,
        instructions = listOf(
            "Find a comfortable standing or seated position",
            "Close your eyes and take 3 deep breaths",
            "Set your intention for today's run",
            "Visualize yourself running strong and confident",
            "Feel gratitude for your body's ability to move",
            "Open your eyes, ready to run"
        ),
        tags = listOf("pre-run", "focus", "intention")
    )
    
    val postRunGratitude = MindfulnessContent(
        id = "post_run_gratitude",
        title = "Post-Run Reflection",
        description = "Celebrate your accomplishment and aid recovery with mindful reflection.",
        type = MindfulnessType.POST_RUN_GRATITUDE,
        durationSeconds = 180,
        instructions = listOf(
            "Find a comfortable position to cool down",
            "Close your eyes and notice your breath returning to normal",
            "Acknowledge what you just accomplished",
            "Thank your body for carrying you through",
            "Notice how you feel - physically and emotionally",
            "Set an intention for recovery"
        ),
        tags = listOf("post-run", "gratitude", "recovery")
    )
    
    val bodyScan = MindfulnessContent(
        id = "body_scan",
        title = "Runner's Body Scan",
        description = "Check in with your body to identify tension and promote recovery.",
        type = MindfulnessType.BODY_SCAN,
        durationSeconds = 300,
        instructions = listOf(
            "Lie down or sit comfortably",
            "Start at the top of your head, notice any tension",
            "Move attention to your face, jaw, neck",
            "Scan your shoulders, arms, hands",
            "Notice your chest, breathing naturally",
            "Feel your core, lower back",
            "Scan your hips, glutes",
            "Move to your thighs, knees",
            "Notice your calves, ankles, feet",
            "Take a moment to feel your whole body as one"
        ),
        tags = listOf("body-scan", "recovery", "awareness")
    )
    
    val stressRelief = MindfulnessContent(
        id = "stress_relief",
        title = "Quick Stress Relief",
        description = "Release tension and find calm in just 5 minutes.",
        type = MindfulnessType.STRESS_RELIEF,
        durationSeconds = 300,
        instructions = listOf(
            "Sit or stand comfortably",
            "Take 3 deep breaths, exhaling fully",
            "Tense your shoulders up to your ears, hold, release",
            "Clench your fists tight, hold, release",
            "Scrunch your face, hold, release",
            "Feel the wave of relaxation through your body",
            "Continue breathing slowly and deeply",
            "Imagine stress leaving your body with each exhale"
        ),
        tags = listOf("stress", "relaxation", "quick")
    )
    
    fun getAll(): List<MindfulnessContent> = listOf(
        preRunFocus, postRunGratitude, bodyScan, stressRelief
    )
    
    fun getPreRunSessions(): List<MindfulnessContent> = listOf(preRunFocus)
    fun getPostRunSessions(): List<MindfulnessContent> = listOf(postRunGratitude, bodyScan)
}
