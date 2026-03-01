package com.runtracker.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.BuildConfig
import com.runtracker.shared.data.db.ScheduledGymWorkoutDao
import com.runtracker.shared.data.db.TrainingPlanDao
import com.runtracker.shared.data.model.ScheduledGymWorkout
import com.runtracker.shared.data.model.ScheduledWorkout
import com.runtracker.shared.data.model.TrainingPlan
import com.runtracker.shared.data.model.WorkoutType
import com.runtracker.shared.data.repository.BodyAnalysisRepository
import com.runtracker.shared.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import org.json.JSONArray
import org.json.JSONObject
import com.runtracker.shared.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val trainingPlanDao: TrainingPlanDao,
    private val scheduledGymWorkoutDao: ScheduledGymWorkoutDao,
    private val bodyAnalysisRepository: BodyAnalysisRepository,
    private val gymRepository: GymRepository
) : ViewModel() {
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.7f
            maxOutputTokens = 2048
        }
    )

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }
    
    private fun loadData() {
        loadRunningPlan()
        loadScheduledGymWorkouts()
        checkBodyScan()
    }
    
    private fun checkBodyScan() {
        viewModelScope.launch {
            val latestScan = bodyAnalysisRepository.getLatestScan()
            _uiState.update { it.copy(hasBodyScan = latestScan != null) }
        }
    }
    
    private fun loadRunningPlan() {
        viewModelScope.launch {
            trainingPlanDao.getActivePlanFlow().collect { plan ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        runningPlan = plan
                    )
                }
            }
        }
    }
    
    private fun loadScheduledGymWorkouts() {
        viewModelScope.launch {
            val today = getStartOfDay(System.currentTimeMillis())
            scheduledGymWorkoutDao.getUpcomingWorkouts(today).collect { workouts ->
                _uiState.update { 
                    it.copy(scheduledGymWorkouts = workouts)
                }
            }
        }
    }

    fun selectDate(date: Long) {
        _uiState.update { it.copy(selectedDate = getStartOfDay(date)) }
    }

    fun selectMonth(year: Int, month: Int) {
        _uiState.update { 
            it.copy(
                currentYear = year,
                currentMonth = month
            )
        }
    }

    fun previousMonth() {
        val state = _uiState.value
        val calendar = Calendar.getInstance()
        calendar.set(state.currentYear, state.currentMonth, 1)
        calendar.add(Calendar.MONTH, -1)
        _uiState.update {
            it.copy(
                currentYear = calendar.get(Calendar.YEAR),
                currentMonth = calendar.get(Calendar.MONTH)
            )
        }
    }

    fun nextMonth() {
        val state = _uiState.value
        val calendar = Calendar.getInstance()
        calendar.set(state.currentYear, state.currentMonth, 1)
        calendar.add(Calendar.MONTH, 1)
        _uiState.update {
            it.copy(
                currentYear = calendar.get(Calendar.YEAR),
                currentMonth = calendar.get(Calendar.MONTH)
            )
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        return TimeUtils.getStartOfDay(timestamp)
    }

    fun refresh() {
        loadData()
    }
    
    fun analyzeWorkouts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, aiAnalysis = null, suggestions = emptyList()) }
            
            try {
                val state = _uiState.value
                val workouts = getWorkoutsForMonth()
                val bodyScan = bodyAnalysisRepository.getLatestScan()
                val templates = gymRepository.getAllTemplates().first()
                
                val prompt = buildString {
                    appendLine("Analyze this workout schedule and provide feedback with actionable suggestions.")
                    appendLine()
                    
                    if (state.runningPlan != null) {
                        appendLine("RUNNING PLAN: ${state.runningPlan.name}")
                        appendLine("Goal: ${state.runningPlan.goalType}")
                        appendLine("Weekly schedule:")
                        state.runningPlan.weeklySchedule.groupBy { it.weekNumber }.forEach { (week, workoutsInWeek) ->
                            appendLine("  Week $week: ${workoutsInWeek.joinToString { "${getDayName(it.dayOfWeek)}: ${it.workoutType.name}" }}")
                        }
                    }
                    
                    appendLine()
                    appendLine("GYM TEMPLATES AVAILABLE:")
                    templates.forEach { template ->
                        appendLine("- ${template.name} (ID: ${template.id}): ${template.exercises.joinToString { it.exerciseName }}")
                    }
                    
                    appendLine()
                    appendLine("SCHEDULED GYM WORKOUTS:")
                    state.scheduledGymWorkouts.forEach { gym ->
                        val dateFormat = SimpleDateFormat("EEE MMM d", Locale.getDefault())
                        appendLine("- ${dateFormat.format(Date(gym.scheduledDate))}: ${gym.templateName}")
                    }
                    
                    if (bodyScan != null) {
                        appendLine()
                        appendLine("USER BODY SCAN DATA:")
                        appendLine("- Goal: ${bodyScan.userGoal.displayName}")
                        appendLine("- Body Type: ${bodyScan.bodyType.displayName}")
                        appendLine("- Focus Zones: ${bodyScan.focusZones.joinToString { it.displayName }}")
                        if (bodyScan.postureAssessment.issues.isNotEmpty()) {
                            appendLine("- Posture Issues: ${bodyScan.postureAssessment.issues.joinToString { it.type.displayName }}")
                        }
                    }
                    
                    appendLine()
                    appendLine("Respond in this exact JSON format:")
                    appendLine("""
{
  "summary": "Brief 2-3 sentence overall assessment",
  "suggestions": [
    {
      "type": "swap_running_workout",
      "title": "Short title",
      "description": "What to change",
      "impact": "Why this helps",
      "weekNumber": 1,
      "dayOfWeek": 2,
      "currentWorkoutType": "EASY_RUN",
      "suggestedWorkoutType": "RECOVERY_RUN"
    },
    {
      "type": "add_exercise",
      "title": "Short title",
      "description": "What to add",
      "impact": "Why this helps",
      "templateId": 1,
      "templateName": "Push Day",
      "exerciseName": "Face Pulls",
      "muscleGroup": "SHOULDERS"
    },
    {
      "type": "remove_exercise",
      "title": "Short title",
      "description": "What to remove",
      "impact": "Why this helps",
      "templateId": 1,
      "templateName": "Push Day",
      "exerciseName": "Exercise Name"
    },
    {
      "type": "adjust_run_duration",
      "title": "Short title",
      "description": "Duration change",
      "impact": "Why this helps",
      "weekNumber": 1,
      "dayOfWeek": 3,
      "currentDuration": 30,
      "suggestedDuration": 45
    },
    {
      "type": "swap_gym_template",
      "title": "Short title",
      "description": "Swap template for a day",
      "impact": "Why this helps",
      "scheduledWorkoutId": 1,
      "scheduledDate": 1234567890000,
      "currentTemplateId": 1,
      "currentTemplateName": "Push Day",
      "suggestedTemplateId": 2,
      "suggestedTemplateName": "Pull Day"
    }
  ]
}
                    """.trimIndent())
                    appendLine()
                    appendLine("Valid workout types: EASY_RUN, LONG_RUN, TEMPO_RUN, INTERVAL_TRAINING, RECOVERY_RUN, RACE_PACE, HILL_REPEATS, FARTLEK, CROSS_TRAINING, REST")
                    appendLine("Provide 2-4 specific, actionable suggestions. Only suggest changes that make sense based on the data.")
                    appendLine("Use swap_gym_template when you want to change which template is used on a scheduled gym day.")
                }
                
                val response = generativeModel.generateContent(prompt)
                val responseText = response.text ?: ""
                
                // Parse the JSON response
                val (summary, suggestions) = parseAnalysisResponse(responseText)
                
                _uiState.update { 
                    it.copy(
                        isAnalyzing = false, 
                        aiAnalysis = summary,
                        suggestions = suggestions
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isAnalyzing = false, 
                        aiAnalysis = "Analysis failed: ${e.message ?: "Unknown error"}",
                        suggestions = emptyList()
                    ) 
                }
            }
        }
    }
    
    private fun parseAnalysisResponse(responseText: String): Pair<String, List<WorkoutSuggestion>> {
        val suggestions = mutableListOf<WorkoutSuggestion>()
        var summary = "Analysis complete."
        
        try {
            // Extract JSON from response (it might have markdown code blocks)
            val jsonString = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            val json = JSONObject(jsonString)
            summary = json.optString("summary", "Analysis complete.")
            
            val suggestionsArray = json.optJSONArray("suggestions") ?: JSONArray()
            
            for (i in 0 until suggestionsArray.length()) {
                val suggestionJson = suggestionsArray.getJSONObject(i)
                val type = suggestionJson.getString("type")
                val id = UUID.randomUUID().toString()
                val title = suggestionJson.getString("title")
                val description = suggestionJson.getString("description")
                val impact = suggestionJson.getString("impact")
                
                val suggestion = when (type) {
                    "swap_running_workout" -> WorkoutSuggestion.SwapRunningWorkout(
                        id = id,
                        title = title,
                        description = description,
                        impact = impact,
                        weekNumber = suggestionJson.getInt("weekNumber"),
                        dayOfWeek = suggestionJson.getInt("dayOfWeek"),
                        currentWorkoutType = suggestionJson.getString("currentWorkoutType"),
                        suggestedWorkoutType = suggestionJson.getString("suggestedWorkoutType")
                    )
                    "add_exercise" -> WorkoutSuggestion.AddExerciseToTemplate(
                        id = id,
                        title = title,
                        description = description,
                        impact = impact,
                        templateId = suggestionJson.getLong("templateId"),
                        templateName = suggestionJson.getString("templateName"),
                        exerciseName = suggestionJson.getString("exerciseName"),
                        muscleGroup = suggestionJson.optString("muscleGroup", "")
                    )
                    "remove_exercise" -> WorkoutSuggestion.RemoveExerciseFromTemplate(
                        id = id,
                        title = title,
                        description = description,
                        impact = impact,
                        templateId = suggestionJson.getLong("templateId"),
                        templateName = suggestionJson.getString("templateName"),
                        exerciseName = suggestionJson.getString("exerciseName")
                    )
                    "adjust_run_duration" -> WorkoutSuggestion.AdjustRunDuration(
                        id = id,
                        title = title,
                        description = description,
                        impact = impact,
                        weekNumber = suggestionJson.getInt("weekNumber"),
                        dayOfWeek = suggestionJson.getInt("dayOfWeek"),
                        currentDuration = suggestionJson.getInt("currentDuration"),
                        suggestedDuration = suggestionJson.getInt("suggestedDuration")
                    )
                    "swap_gym_template" -> WorkoutSuggestion.SwapGymTemplate(
                        id = id,
                        title = title,
                        description = description,
                        impact = impact,
                        scheduledWorkoutId = suggestionJson.getLong("scheduledWorkoutId"),
                        scheduledDate = suggestionJson.getLong("scheduledDate"),
                        currentTemplateId = suggestionJson.getLong("currentTemplateId"),
                        currentTemplateName = suggestionJson.getString("currentTemplateName"),
                        suggestedTemplateId = suggestionJson.getLong("suggestedTemplateId"),
                        suggestedTemplateName = suggestionJson.getString("suggestedTemplateName")
                    )
                    else -> null
                }
                
                suggestion?.let { suggestions.add(it) }
            }
        } catch (e: Exception) {
            // If JSON parsing fails, just return the raw text as summary
            summary = responseText.take(500)
        }
        
        return Pair(summary, suggestions)
    }
    
    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "?"
        }
    }
    
    fun applySuggestion(suggestion: WorkoutSuggestion) {
        viewModelScope.launch {
            try {
                when (suggestion) {
                    is WorkoutSuggestion.SwapRunningWorkout -> applySwapRunningWorkout(suggestion)
                    is WorkoutSuggestion.AddExerciseToTemplate -> applyAddExercise(suggestion)
                    is WorkoutSuggestion.RemoveExerciseFromTemplate -> applyRemoveExercise(suggestion)
                    is WorkoutSuggestion.AdjustRunDuration -> applyAdjustDuration(suggestion)
                    is WorkoutSuggestion.SwapGymTemplate -> applySwapGymTemplate(suggestion)
                    else -> { /* Other types not yet implemented */ }
                }
                
                // Remove the applied suggestion from the list
                _uiState.update { state ->
                    state.copy(
                        suggestions = state.suggestions.filter { it.id != suggestion.id },
                        appliedSuggestionMessage = "Applied: ${suggestion.title}"
                    )
                }
                
                // Refresh data
                loadData()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(appliedSuggestionMessage = "Failed to apply: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun applySwapRunningWorkout(suggestion: WorkoutSuggestion.SwapRunningWorkout) {
        val plan = _uiState.value.runningPlan ?: return
        
        val newWorkoutType = try {
            WorkoutType.valueOf(suggestion.suggestedWorkoutType)
        } catch (e: Exception) {
            return
        }
        
        val updatedSchedule = plan.weeklySchedule.map { workout ->
            if (workout.weekNumber == suggestion.weekNumber && workout.dayOfWeek == suggestion.dayOfWeek) {
                workout.copy(workoutType = newWorkoutType)
            } else {
                workout
            }
        }
        
        val updatedPlan = plan.copy(weeklySchedule = updatedSchedule)
        trainingPlanDao.updatePlan(updatedPlan)
    }
    
    private suspend fun applyAddExercise(suggestion: WorkoutSuggestion.AddExerciseToTemplate) {
        val template = gymRepository.getTemplateById(suggestion.templateId) ?: return
        
        // Find the exercise in the database
        val exercises = gymRepository.getAllExercises().first()
        val exerciseToAdd = exercises.find { 
            it.name.equals(suggestion.exerciseName, ignoreCase = true) 
        }
        
        if (exerciseToAdd != null) {
            // Parse suggested reps (e.g., "8-12" -> min=8, max=12)
            val repsParts = suggestion.suggestedReps.split("-")
            val minReps = repsParts.getOrNull(0)?.toIntOrNull() ?: 8
            val maxReps = repsParts.getOrNull(1)?.toIntOrNull() ?: minReps
            
            val newTemplateExercise = com.runtracker.shared.data.model.TemplateExercise(
                id = java.util.UUID.randomUUID().toString(),
                exerciseId = exerciseToAdd.id,
                exerciseName = exerciseToAdd.name,
                sets = suggestion.suggestedSets,
                targetRepsMin = minReps,
                targetRepsMax = maxReps,
                notes = "Added by AI suggestion",
                orderIndex = template.exercises.size
            )
            
            val updatedTemplate = template.copy(
                exercises = template.exercises + newTemplateExercise
            )
            gymRepository.updateTemplate(updatedTemplate)
        }
    }
    
    private suspend fun applyRemoveExercise(suggestion: WorkoutSuggestion.RemoveExerciseFromTemplate) {
        val template = gymRepository.getTemplateById(suggestion.templateId) ?: return
        
        val updatedExercises = template.exercises.filter { 
            !it.exerciseName.equals(suggestion.exerciseName, ignoreCase = true)
        }
        
        val updatedTemplate = template.copy(exercises = updatedExercises)
        gymRepository.updateTemplate(updatedTemplate)
    }
    
    private suspend fun applyAdjustDuration(suggestion: WorkoutSuggestion.AdjustRunDuration) {
        val plan = _uiState.value.runningPlan ?: return
        
        val updatedSchedule = plan.weeklySchedule.map { workout ->
            if (workout.weekNumber == suggestion.weekNumber && workout.dayOfWeek == suggestion.dayOfWeek) {
                workout.copy(targetDurationMinutes = suggestion.suggestedDuration)
            } else {
                workout
            }
        }
        
        val updatedPlan = plan.copy(weeklySchedule = updatedSchedule)
        trainingPlanDao.updatePlan(updatedPlan)
    }
    
    private suspend fun applySwapGymTemplate(suggestion: WorkoutSuggestion.SwapGymTemplate) {
        // Get the scheduled workout and update its template
        val scheduledWorkout = scheduledGymWorkoutDao.getById(suggestion.scheduledWorkoutId) ?: return
        
        val updatedWorkout = scheduledWorkout.copy(
            templateId = suggestion.suggestedTemplateId,
            templateName = suggestion.suggestedTemplateName
        )
        
        scheduledGymWorkoutDao.update(updatedWorkout)
    }
    
    fun dismissSuggestion(suggestionId: String) {
        _uiState.update { state ->
            state.copy(suggestions = state.suggestions.filter { it.id != suggestionId })
        }
    }
    
    fun clearAppliedMessage() {
        _uiState.update { it.copy(appliedSuggestionMessage = null) }
    }
    
    fun clearAnalysis() {
        _uiState.update { it.copy(aiAnalysis = null, suggestions = emptyList()) }
    }
    
    fun getRunningWorkoutForDate(date: Long): ScheduledWorkout? {
        val plan = _uiState.value.runningPlan ?: return null
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // Calculate week number since plan start
        val planStartCalendar = Calendar.getInstance().apply { timeInMillis = plan.startDate }
        val weeksBetween = TimeUtils.calculateWeekNumber(plan.startDate, date)
        
        return plan.weeklySchedule.find { 
            it.dayOfWeek == dayOfWeek && it.weekNumber == weeksBetween
        }
    }
    
    fun getGymWorkoutForDate(date: Long): ScheduledGymWorkout? {
        val startOfDay = getStartOfDay(date)
        return _uiState.value.scheduledGymWorkouts.find { it.scheduledDate == startOfDay }
    }
    
    fun getWorkoutsForMonth(): List<CalendarDayWorkout> {
        val state = _uiState.value
        val workouts = mutableListOf<CalendarDayWorkout>()
        
        val calendar = Calendar.getInstance()
        calendar.set(state.currentYear, state.currentMonth, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val date = getStartOfDay(calendar.timeInMillis)
            
            val runWorkout = getRunningWorkoutForDate(date)
            val gymWorkout = getGymWorkoutForDate(date)
            
            if (runWorkout != null || gymWorkout != null) {
                workouts.add(
                    CalendarDayWorkout(
                        date = date,
                        dayOfMonth = day,
                        hasRunning = runWorkout != null,
                        runningDescription = runWorkout?.description,
                        hasGym = gymWorkout != null,
                        gymTemplateName = gymWorkout?.templateName
                    )
                )
            }
        }
        
        return workouts
    }
}

data class CalendarUiState(
    val isLoading: Boolean = true,
    val runningPlan: TrainingPlan? = null,
    val scheduledGymWorkouts: List<ScheduledGymWorkout> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val isAnalyzing: Boolean = false,
    val aiAnalysis: String? = null,
    val hasBodyScan: Boolean = false,
    val suggestions: List<WorkoutSuggestion> = emptyList(),
    val appliedSuggestionMessage: String? = null
)

data class CalendarDayWorkout(
    val date: Long,
    val dayOfMonth: Int,
    val hasRunning: Boolean,
    val runningDescription: String? = null,
    val hasGym: Boolean,
    val gymTemplateName: String? = null
)
