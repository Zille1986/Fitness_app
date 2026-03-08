package com.runtracker.app.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.runtracker.app.BuildConfig
import com.runtracker.shared.data.repository.BodyAnalysisRepository
import com.runtracker.shared.data.repository.GymRepository
import com.runtracker.shared.data.repository.HIITRepository
import com.runtracker.shared.data.repository.NutritionRepository
import com.runtracker.shared.data.repository.RunRepository
import com.runtracker.shared.data.repository.TrainingPlanRepository
import com.runtracker.shared.data.repository.UserRepository
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
    private val hiitRepository: HIITRepository,
    private val nutritionRepository: NutritionRepository,
    private val userRepository: UserRepository,
    private val bodyAnalysisRepository: BodyAnalysisRepository,
    private val trainingPlanRepository: TrainingPlanRepository,
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
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private var chat = generativeModel.startChat()
    private var chatInitialized = false

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
    
    private suspend fun initializeChatIfNeeded() {
        if (chatInitialized) return
        chatInitialized = true

        val context = buildUserContext()
        val systemPrompt = """
You are $assistantName, an expert fitness coach and assistant built into a workout tracking app. You have deep knowledge of exercise science, nutrition, programming, injury prevention, and sports psychology.

Your personality: Friendly, knowledgeable, and genuinely invested in the user's success. You're like having a personal trainer in your pocket — you give real, actionable advice, not just cheerful platitudes.

Guidelines:
- Give specific, actionable advice based on the user's actual data and goals
- When asked about training, consider their current activity level, recent workouts, and goals
- For nutrition questions, reference their actual intake data when available
- Be honest — if something isn't going well (skipped workouts, poor nutrition), address it constructively
- Adapt response length to the question: quick questions get short answers, complex topics (vacation workout plans, program design, plateaus) get detailed responses
- Use your fitness expertise: periodization, progressive overload, recovery, mobility, etc.
- When the user mentions travel, injuries, equipment limitations, or life changes, proactively suggest adapted routines
- You can reference exercises from the app's library and suggest workouts they can do
- Use emojis sparingly and naturally

Here is the user's current fitness profile and recent activity:
$context
        """.trimIndent()

        // Send the system context as the first message in the chat
        try {
            chat = generativeModel.startChat(
                history = listOf(
                    content(role = "user") { text(systemPrompt) },
                    content(role = "model") { text("Got it! I have your fitness profile and recent activity loaded. I'm ready to help with anything — workouts, nutrition, recovery, travel plans, you name it. What's on your mind?") }
                )
            )
        } catch (e: Exception) {
            Log.e("AssistantVM", "Failed to initialize chat", e)
        }
    }

    private suspend fun generateResponse(question: String): String {
        initializeChatIfNeeded()

        val response = chat.sendMessage(question)
        return response.text ?: "I'm here to help! What would you like to know?"
    }

    private suspend fun buildUserContext(): String {
        return buildString {
            try {
                // User profile
                val profile = userRepository.getProfileOnce()
                if (profile != null) {
                    val profileParts = mutableListOf<String>()
                    profile.name.takeIf { it.isNotBlank() }?.let { profileParts.add("Name: $it") }
                    profile.age?.let { profileParts.add("Age: $it") }
                    profile.weight?.let { profileParts.add("Weight: ${it}kg") }
                    profile.height?.let { profileParts.add("Height: ${it}cm") }
                    profile.gender?.let { profileParts.add("Gender: ${it.name}") }
                    if (profileParts.isNotEmpty()) {
                        appendLine("USER PROFILE: ${profileParts.joinToString(", ")}")
                    }
                }

                // Active training plan
                val activePlan = trainingPlanRepository.getActivePlanOnce()
                if (activePlan != null) {
                    appendLine("ACTIVE TRAINING PLAN: \"${activePlan.name}\" (${activePlan.weeklySchedule.size} scheduled workouts)")
                    val todaysWorkout = trainingPlanRepository.getTodaysWorkout()
                    if (todaysWorkout != null) {
                        appendLine("Today's scheduled workout: ${todaysWorkout.description} (${todaysWorkout.workoutType})")
                    }
                }

                // Gym activity
                val gymStats = gymRepository.getWeeklyGymStats()
                appendLine("GYM THIS WEEK: ${gymStats.workoutCount} workouts, ${gymStats.totalVolumeFormatted} total volume")

                val recentWorkouts = gymRepository.getRecentWorkouts(5).first()
                if (recentWorkouts.isNotEmpty()) {
                    val lastWorkout = recentWorkouts.first()
                    val daysSince = (System.currentTimeMillis() - lastWorkout.startTime) / (1000 * 60 * 60 * 24)
                    appendLine("Last gym workout: ${lastWorkout.name} (${daysSince.toInt()} days ago)")
                    val workoutNames = recentWorkouts.take(5).joinToString(", ") { it.name }
                    appendLine("Recent gym sessions: $workoutNames")
                }

                // Personal records
                val prs = gymRepository.getPersonalRecordsSnapshot()
                if (prs.isNotEmpty()) {
                    val prSummary = prs.take(5).joinToString(", ") { "${it.bestWeight}kg x ${it.bestReps} (1RM: ${String.format("%.0f", it.estimatedOneRepMax)}kg)" }
                    appendLine("RECENT PERSONAL RECORDS: $prSummary")
                }

                // HIIT activity
                val hiitStats = hiitRepository.getWeeklyStats()
                if (hiitStats.sessionCount > 0) {
                    appendLine("HIIT THIS WEEK: ${hiitStats.sessionCount} sessions, ${hiitStats.totalCalories} cal burned")
                }

                // Running
                val runs = runRepository.getRecentRuns(5).first()
                if (runs.isNotEmpty()) {
                    appendLine("RECENT RUNS: ${runs.size} runs")
                    val lastRun = runs.first()
                    val distanceKm = lastRun.distanceMeters / 1000.0
                    appendLine("Last run: ${String.format("%.1f", distanceKm)}km")
                }

                // Nutrition
                val todayNutrition = nutritionRepository.getTodayNutritionOnce()
                if (todayNutrition != null) {
                    appendLine("TODAY'S NUTRITION: ${todayNutrition.consumedCalories}/${todayNutrition.targetCalories} cal, " +
                            "${todayNutrition.consumedProteinGrams}g protein, " +
                            "${todayNutrition.waterMl}ml water")
                    val weeklyAvg = nutritionRepository.getWeeklyAverages()
                    if (weeklyAvg.avgCalories > 0) {
                        appendLine("Weekly avg: ${weeklyAvg.avgCalories} cal/day, ${weeklyAvg.avgProtein}g protein/day")
                    }
                }

                // Body analysis
                val latestScan = bodyAnalysisRepository.getLatestScan()
                if (latestScan != null) {
                    appendLine("LATEST BODY SCAN: Score ${latestScan.overallScore}/100, " +
                            "Goal: ${latestScan.userGoal.displayName}")
                    latestScan.estimatedBodyFatPercentage?.let {
                        appendLine("Estimated body fat: ${String.format("%.1f", it)}%")
                    }
                    if (latestScan.focusZones.isNotEmpty()) {
                        appendLine("Focus zones: ${latestScan.focusZones.joinToString(", ") { it.displayName }}")
                    }
                }

            } catch (e: Exception) {
                Log.e("AssistantVM", "Error building context", e)
                appendLine("User is actively tracking workouts")
            }

            appendLine("Current date: ${SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())}")
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
        val q = lastQuestion.lowercase()
        return when {
            q.contains("vacation") || q.contains("travel") || q.contains("holiday") ->
                listOf("Bodyweight routine?", "How to stay on track?", "Thanks!")
            q.contains("injury") || q.contains("pain") || q.contains("hurt") ->
                listOf("What can I still do?", "Recovery tips", "Should I rest?")
            q.contains("plateau") || q.contains("stuck") || q.contains("not progressing") ->
                listOf("Change my program?", "Deload week?", "Check my nutrition")
            q.contains("nutrition") || q.contains("diet") || q.contains("eat") || q.contains("food") ->
                listOf("Meal ideas", "How much protein?", "Pre-workout meal?")
            q.contains("today") || q.contains("workout") ->
                listOf("What muscles to hit?", "I'm tired today", "Quick 20 min session?")
            q.contains("week") || q.contains("schedule") || q.contains("plan") ->
                listOf("Adjust my split?", "Add more cardio?", "Rest day advice")
            q.contains("progress") || q.contains("improve") || q.contains("stronger") ->
                listOf("Where am I weakest?", "Set a new goal", "Compare to last month")
            else -> listOf("What should I do today?", "Review my week", "Nutrition check")
        }
    }
    
    fun clearChat() {
        chatInitialized = false
        chat = generativeModel.startChat()
        _uiState.update { it.copy(chatHistory = emptyList(), currentMessage = null) }
    }
}
