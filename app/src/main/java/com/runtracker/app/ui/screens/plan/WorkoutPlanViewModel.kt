package com.runtracker.app.ui.screens.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.db.WorkoutPlanDao
import com.runtracker.shared.data.model.WeeklyWorkoutDay
import com.runtracker.shared.data.model.WeeklyWorkoutType
import com.runtracker.shared.data.model.WorkoutPlan
import com.runtracker.shared.data.repository.BodyAnalysisRepository
import com.runtracker.shared.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig

@HiltViewModel
class WorkoutPlanViewModel @Inject constructor(
    private val gymRepository: GymRepository,
    private val bodyAnalysisRepository: BodyAnalysisRepository,
    private val workoutPlanDao: WorkoutPlanDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutPlanUiState())
    val uiState: StateFlow<WorkoutPlanUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load gym templates
            gymRepository.getAllTemplates().collect { templates ->
                _uiState.update {
                    it.copy(
                        gymTemplates = templates.map { t -> GymTemplateInfo(t.id, t.name) }
                    )
                }
            }
        }
        
        viewModelScope.launch {
            // Check for body scan
            val latestScan = bodyAnalysisRepository.getLatestScan()
            _uiState.update {
                it.copy(
                    hasBodyScan = latestScan != null,
                    bodyScanData = latestScan?.let { scan ->
                        BodyScanSummary(
                            goal = scan.userGoal.displayName,
                            bodyType = scan.bodyType.displayName,
                            focusZones = scan.focusZones.map { z -> z.displayName },
                            overallScore = scan.overallScore,
                            postureIssues = scan.postureAssessment.issues.map { i -> i.type.displayName }
                        )
                    }
                )
            }
        }
    }

    fun toggleDay(dayIndex: Int) {
        _uiState.update { state ->
            val newDays = if (dayIndex in state.selectedDays) {
                state.selectedDays - dayIndex
            } else {
                state.selectedDays + dayIndex
            }
            
            // Remove assignment if day is deselected
            val newAssignments = if (dayIndex !in newDays) {
                state.dayAssignments - dayIndex
            } else {
                state.dayAssignments
            }
            
            state.copy(
                selectedDays = newDays,
                dayAssignments = newAssignments
            )
        }
    }

    fun setDuration(minutes: Int) {
        _uiState.update { it.copy(durationMinutes = minutes) }
    }

    fun assignWorkout(dayIndex: Int, type: WorkoutType) {
        _uiState.update { state ->
            state.copy(
                dayAssignments = state.dayAssignments + (dayIndex to DayAssignment(workoutType = type))
            )
        }
    }

    fun assignTemplate(dayIndex: Int, templateId: Long, templateName: String) {
        _uiState.update { state ->
            val currentAssignment = state.dayAssignments[dayIndex]
            if (currentAssignment != null) {
                state.copy(
                    dayAssignments = state.dayAssignments + (dayIndex to currentAssignment.copy(
                        templateId = templateId,
                        templateName = templateName
                    ))
                )
            } else {
                state
            }
        }
    }

    fun analyzeWithGemini() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, aiAnalysis = null) }
            
            try {
                val state = _uiState.value
                val prompt = buildAnalysisPrompt(state)
                
                val generativeModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = getGeminiApiKey(),
                    generationConfig = generationConfig {
                        temperature = 0.7f
                        maxOutputTokens = 1024
                    }
                )
                
                val response = generativeModel.generateContent(prompt)
                val analysis = response.text ?: "Unable to generate analysis"
                
                _uiState.update { 
                    it.copy(
                        isAnalyzing = false,
                        aiAnalysis = analysis
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkoutPlanVM", "Gemini analysis failed", e)
                _uiState.update { 
                    it.copy(
                        isAnalyzing = false,
                        aiAnalysis = "Analysis unavailable. Please check your internet connection and try again.\n\nError: ${e.message}"
                    )
                }
            }
        }
    }

    private fun buildAnalysisPrompt(state: WorkoutPlanUiState): String {
        val dayNames = listOf("", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        
        val planDescription = state.selectedDays.sorted().mapNotNull { dayIndex ->
            val assignment = state.dayAssignments[dayIndex]
            if (assignment != null) {
                val workoutDesc = when (assignment.workoutType) {
                    WorkoutType.GYM -> "Gym workout${assignment.templateName?.let { " ($it)" } ?: ""}"
                    WorkoutType.RUNNING -> "Running"
                    WorkoutType.REST -> "Rest day"
                }
                "${dayNames[dayIndex]}: $workoutDesc"
            } else null
        }.joinToString("\n")
        
        val bodyScanInfo = state.bodyScanData?.let { scan ->
            """
            
            Body Scan Data:
            - Fitness Goal: ${scan.goal}
            - Body Type: ${scan.bodyType}
            - Overall Score: ${scan.overallScore}/100
            - Focus Zones: ${scan.focusZones.joinToString(", ")}
            - Posture Issues: ${if (scan.postureIssues.isEmpty()) "None" else scan.postureIssues.joinToString(", ")}
            """.trimIndent()
        } ?: ""
        
        return """
            You are a fitness coach analyzing a workout plan. Provide brief, actionable feedback.
            
            Workout Plan:
            - Duration per session: ${state.durationMinutes} minutes
            - Days per week: ${state.selectedDays.size}
            
            Schedule:
            $planDescription
            $bodyScanInfo
            
            Please analyze this plan and provide:
            1. Overall assessment (1-2 sentences)
            2. Strengths of this plan (2-3 bullet points)
            3. Suggestions for improvement (2-3 bullet points)
            4. Recovery recommendations based on the schedule
            
            Keep the response concise and practical. Use bullet points where appropriate.
        """.trimIndent()
    }

    private fun getGeminiApiKey(): String {
        // In production, this should come from BuildConfig or secure storage
        return "AIzaSyDummyKeyForDevelopment"
    }

    fun createPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            
            val state = _uiState.value
            
            // Convert UI assignments to ScheduledWorkout list
            val scheduledWorkouts = state.dayAssignments.map { (dayIndex, assignment) ->
                WeeklyWorkoutDay(
                    dayOfWeek = dayIndex,
                    workoutType = when (assignment.workoutType) {
                        WorkoutType.GYM -> WeeklyWorkoutType.GYM
                        WorkoutType.RUNNING -> WeeklyWorkoutType.RUNNING
                        WorkoutType.REST -> WeeklyWorkoutType.REST
                    },
                    templateId = assignment.templateId,
                    templateName = assignment.templateName
                )
            }
            
            // Deactivate existing plans
            workoutPlanDao.deactivateAllPlans()
            
            // Create and save new plan
            val plan = WorkoutPlan(
                name = "My Workout Plan",
                durationMinutes = state.durationMinutes,
                scheduledWorkouts = scheduledWorkouts,
                isActive = true
            )
            
            workoutPlanDao.insertPlan(plan)
            
            _uiState.update { 
                it.copy(
                    isCreating = false,
                    planCreated = true
                )
            }
        }
    }
}

data class WorkoutPlanUiState(
    val selectedDays: Set<Int> = emptySet(),
    val durationMinutes: Int = 45,
    val dayAssignments: Map<Int, DayAssignment> = emptyMap(),
    val gymTemplates: List<GymTemplateInfo> = emptyList(),
    val hasBodyScan: Boolean = false,
    val bodyScanData: BodyScanSummary? = null,
    val isAnalyzing: Boolean = false,
    val aiAnalysis: String? = null,
    val isCreating: Boolean = false,
    val planCreated: Boolean = false
)

data class BodyScanSummary(
    val goal: String,
    val bodyType: String,
    val focusZones: List<String>,
    val overallScore: Int,
    val postureIssues: List<String>
)
