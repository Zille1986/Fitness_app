package com.runtracker.app.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.runtracker.app.BuildConfig
import com.runtracker.shared.data.repository.GymRepository
import com.runtracker.shared.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val gymRepository: GymRepository,
    private val runRepository: RunRepository,
    private val eventBus: AssistantEventBus
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()
    
    init {
        // Listen for events from other parts of the app
        viewModelScope.launch {
            eventBus.events
                .catch { e -> Log.e("AssistantViewModel", "Error collecting events", e) }
                .collect { trigger ->
                    triggerEvent(trigger)
                }
        }
    }
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    
    private val assistantName = "Buddy"
    
    fun showAssistant() {
        val greeting = getTimeBasedGreeting()
        val message = AssistantMessage(
            text = "$greeting How can I help you today?",
            mood = AssistantMood.WAVING,
            quickReplies = AssistantQuickReplies.greetings
        )
        _uiState.update { 
            it.copy(
                isVisible = true, 
                isMinimized = false,
                currentMood = AssistantMood.WAVING,
                currentMessage = message,
                chatHistory = if (it.chatHistory.isEmpty()) listOf(message) else it.chatHistory
            ) 
        }
    }
    
    fun hideAssistant() {
        _uiState.update { it.copy(isVisible = false) }
    }
    
    fun minimizeAssistant() {
        _uiState.update { it.copy(isMinimized = true, isExpanded = false) }
    }
    
    fun expandChat() {
        _uiState.update { it.copy(isExpanded = true, isMinimized = false) }
    }
    
    fun collapseChat() {
        _uiState.update { it.copy(isExpanded = false) }
    }
    
    fun triggerEvent(trigger: AssistantTrigger) {
        viewModelScope.launch {
            val message = when (trigger) {
                is AssistantTrigger.NewPB -> {
                    AssistantMessage(
                        text = "🎉 NEW PERSONAL BEST! You just hit ${trigger.weight}kg × ${trigger.reps} on ${trigger.exerciseName}! You're getting stronger! 💪",
                        mood = AssistantMood.CELEBRATING,
                        quickReplies = listOf("Thanks!", "What's my next goal?", "Show my progress")
                    )
                }
                is AssistantTrigger.BadgeUnlocked -> {
                    AssistantMessage(
                        text = "🏆 Badge Unlocked: ${trigger.badgeName}! ${trigger.badgeDescription}",
                        mood = AssistantMood.CELEBRATING,
                        quickReplies = listOf("Awesome!", "What badges can I get next?")
                    )
                }
                is AssistantTrigger.MilestoneReached -> {
                    AssistantMessage(
                        text = "🌟 Milestone: ${trigger.milestone} - ${trigger.value}! Keep crushing it!",
                        mood = AssistantMood.CELEBRATING,
                        quickReplies = listOf("Thanks!", "What's next?")
                    )
                }
                is AssistantTrigger.WorkoutComplete -> {
                    AssistantMessage(
                        text = "Great workout! ${trigger.duration} of hard work, ${String.format("%.1f", trigger.volume)}kg total volume. You should be proud! 💪",
                        mood = AssistantMood.ENCOURAGING,
                        quickReplies = AssistantQuickReplies.afterWorkout
                    )
                }
                is AssistantTrigger.StreakAchieved -> {
                    AssistantMessage(
                        text = "🔥 ${trigger.days} day streak! You're on fire! Consistency is the key to success!",
                        mood = AssistantMood.CELEBRATING,
                        quickReplies = listOf("Keep it going!", "What's my record?")
                    )
                }
                AssistantTrigger.WeeklyGoalMet -> {
                    AssistantMessage(
                        text = "🎯 Weekly goal achieved! You've hit your target for this week. Amazing dedication!",
                        mood = AssistantMood.CELEBRATING,
                        quickReplies = listOf("Thanks!", "Set a new goal")
                    )
                }
                AssistantTrigger.FirstWorkoutOfDay -> {
                    val greeting = getTimeBasedGreeting()
                    AssistantMessage(
                        text = "$greeting Ready to crush it today? Let's go! 💪",
                        mood = AssistantMood.WAVING,
                        quickReplies = AssistantQuickReplies.greetings
                    )
                }
                is AssistantTrigger.UserQuestion -> {
                    // This will be handled by askQuestion
                    return@launch
                }
            }
            
            _uiState.update { 
                it.copy(
                    isVisible = true,
                    isMinimized = false,
                    currentMood = message.mood,
                    currentMessage = message,
                    chatHistory = it.chatHistory + message
                )
            }
        }
    }
    
    fun askQuestion(question: String) {
        viewModelScope.launch {
            // Add user message to chat
            val userMessage = AssistantMessage(
                text = question,
                isUserMessage = true
            )
            
            _uiState.update { 
                it.copy(
                    isThinking = true,
                    currentMood = AssistantMood.THINKING,
                    chatHistory = it.chatHistory + userMessage
                )
            }
            
            try {
                val response = generateResponse(question)
                val assistantMessage = AssistantMessage(
                    text = response,
                    mood = AssistantMood.TALKING,
                    quickReplies = getContextualQuickReplies(question)
                )
                
                _uiState.update { 
                    it.copy(
                        isThinking = false,
                        currentMood = AssistantMood.TALKING,
                        currentMessage = assistantMessage,
                        chatHistory = it.chatHistory + assistantMessage
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AssistantVM", "Gemini API error: ${e.message}", e)
                val errorMessage = AssistantMessage(
                    text = "Oops! I had trouble thinking about that. Can you try asking again?",
                    mood = AssistantMood.IDLE
                )
                _uiState.update { 
                    it.copy(
                        isThinking = false,
                        currentMood = AssistantMood.IDLE,
                        currentMessage = errorMessage,
                        chatHistory = it.chatHistory + errorMessage
                    )
                }
            }
        }
    }
    
    private suspend fun generateResponse(question: String): String {
        // Gather context about the user's fitness data
        val context = buildUserContext()
        
        val prompt = """
            You are $assistantName, a friendly and encouraging fitness assistant in a workout tracking app.
            You're like a supportive gym buddy - enthusiastic, knowledgeable, and motivating.
            Keep responses concise (2-3 sentences max) and use occasional emojis.
            
            User's fitness context:
            $context
            
            User's question: $question
            
            Respond helpfully and encouragingly. If they ask about what to do, give specific suggestions based on their data.
        """.trimIndent()
        
        val response = generativeModel.generateContent(prompt)
        return response.text ?: "I'm here to help! What would you like to know?"
    }
    
    private suspend fun buildUserContext(): String {
        return buildString {
            try {
                // Get weekly gym stats
                val gymStats = gymRepository.getWeeklyGymStats()
                appendLine("This week: ${gymStats.workoutCount} gym workouts, ${gymStats.totalVolumeFormatted} total volume")
                
                // Get recent workouts
                val recentWorkouts = gymRepository.getRecentWorkouts(5).first()
                if (recentWorkouts.isNotEmpty()) {
                    val lastWorkout = recentWorkouts.first()
                    val daysSince = (System.currentTimeMillis() - lastWorkout.startTime) / (1000 * 60 * 60 * 24)
                    appendLine("Last gym workout: ${lastWorkout.name} (${daysSince.toInt()} days ago)")
                }
                
                // Get personal records
                val prs = gymRepository.getPersonalRecordsSnapshot()
                if (prs.isNotEmpty()) {
                    appendLine("Recent PRs: ${prs.take(3).joinToString { it.bestWeight.toString() + "kg" }}")
                }
                
                // Get run stats
                val runs = runRepository.getRecentRuns(5).first()
                if (runs.isNotEmpty()) {
                    appendLine("Recent runs: ${runs.size} in the last week")
                }
                
            } catch (e: Exception) {
                appendLine("User is actively tracking workouts")
            }
            
            appendLine("Current date: ${SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())}")
        }
    }
    
    private fun getTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning! ☀️"
            hour < 17 -> "Good afternoon! 👋"
            else -> "Good evening! 🌙"
        }
    }
    
    private fun getContextualQuickReplies(lastQuestion: String): List<String> {
        return when {
            lastQuestion.contains("today", ignoreCase = true) -> 
                listOf("Start workout", "Show my schedule", "I need rest")
            lastQuestion.contains("week", ignoreCase = true) -> 
                listOf("Compare to last week", "Set new goal", "Thanks!")
            lastQuestion.contains("progress", ignoreCase = true) -> 
                listOf("Show charts", "What's my best lift?", "Thanks!")
            else -> AssistantQuickReplies.motivation
        }
    }
    
    fun clearChat() {
        _uiState.update { it.copy(chatHistory = emptyList(), currentMessage = null) }
    }
}
