package com.runtracker.app.assistant

import androidx.compose.runtime.Stable

/**
 * Represents the different moods/animations the assistant can display
 */
enum class AssistantMood {
    IDLE,           // Default floating animation
    WAVING,         // Greeting the user
    CELEBRATING,    // PB or badge achieved
    THINKING,       // Processing a question
    TALKING,        // Delivering a message
    ENCOURAGING,    // Motivating the user
    SLEEPING        // Inactive/minimized state
}

/**
 * Types of events that trigger the assistant to appear
 */
sealed class AssistantTrigger {
    data class NewPB(val exerciseName: String, val weight: Double, val reps: Int) : AssistantTrigger()
    data class BadgeUnlocked(val badgeName: String, val badgeDescription: String) : AssistantTrigger()
    data class MilestoneReached(val milestone: String, val value: String) : AssistantTrigger()
    data class WorkoutComplete(val duration: String, val volume: Double) : AssistantTrigger()
    data class StreakAchieved(val days: Int) : AssistantTrigger()
    object WeeklyGoalMet : AssistantTrigger()
    object FirstWorkoutOfDay : AssistantTrigger()
    data class UserQuestion(val question: String) : AssistantTrigger()
}

/**
 * A message from the assistant
 */
@Stable
data class AssistantMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val mood: AssistantMood = AssistantMood.TALKING,
    val timestamp: Long = System.currentTimeMillis(),
    val isUserMessage: Boolean = false,
    val quickReplies: List<String> = emptyList()
)

/**
 * The overall state of the assistant
 */
@Stable
data class AssistantUiState(
    val isVisible: Boolean = false,
    val isMinimized: Boolean = true,
    val isExpanded: Boolean = false,  // Full chat mode
    val currentMood: AssistantMood = AssistantMood.IDLE,
    val currentMessage: AssistantMessage? = null,
    val chatHistory: List<AssistantMessage> = emptyList(),
    val isThinking: Boolean = false,
    val pendingTrigger: AssistantTrigger? = null
)

/**
 * Quick reply suggestions for common questions
 */
object AssistantQuickReplies {
    val greetings = listOf(
        "What should I do today?",
        "How am I doing this week?",
        "Show me my progress"
    )
    
    val afterWorkout = listOf(
        "How did I do?",
        "What's next?",
        "Show my stats"
    )
    
    val motivation = listOf(
        "Motivate me!",
        "What's my streak?",
        "Any tips?"
    )
}
