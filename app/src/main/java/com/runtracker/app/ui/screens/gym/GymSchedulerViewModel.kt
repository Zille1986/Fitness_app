package com.runtracker.app.ui.screens.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.db.ScheduledGymWorkoutDao
import com.runtracker.shared.data.model.ScheduledGymWorkout
import com.runtracker.shared.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GymSchedulerViewModel @Inject constructor(
    private val scheduledGymWorkoutDao: ScheduledGymWorkoutDao,
    private val gymRepository: GymRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GymSchedulerUiState())
    val uiState: StateFlow<GymSchedulerUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        loadTemplates()
        loadScheduledWorkouts()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            try {
                val templates = gymRepository.getAllTemplates().first()
                _uiState.update {
                    it.copy(
                        templates = templates.map { t ->
                            TemplateInfo(
                                id = t.id,
                                name = t.name,
                                exerciseCount = t.exercises.size
                            )
                        },
                        isLoadingTemplates = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingTemplates = false) }
            }
        }
    }

    private fun loadScheduledWorkouts() {
        viewModelScope.launch {
            val today = getStartOfDay(System.currentTimeMillis())
            scheduledGymWorkoutDao.getUpcomingWorkouts(today).collect { workouts ->
                val displayWorkouts = workouts.map { workout ->
                    val calendar = Calendar.getInstance().apply { timeInMillis = workout.scheduledDate }
                    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                    
                    ScheduledWorkoutDisplay(
                        id = workout.id,
                        templateName = workout.templateName,
                        scheduledDate = workout.scheduledDate,
                        dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).toString(),
                        monthAbbrev = monthFormat.format(calendar.time),
                        dayName = dayFormat.format(calendar.time),
                        isCompleted = workout.isCompleted
                    )
                }
                _uiState.update { it.copy(scheduledWorkouts = displayWorkouts) }
            }
        }
    }

    fun toggleDay(dayOfWeek: Int) {
        _uiState.update { state ->
            val currentAssignments = state.dayAssignments.toMutableMap()
            if (currentAssignments.containsKey(dayOfWeek)) {
                currentAssignments.remove(dayOfWeek)
            } else {
                currentAssignments[dayOfWeek] = null // Selected but no template yet
            }
            state.copy(dayAssignments = currentAssignments)
        }
    }

    fun assignTemplateToDay(dayOfWeek: Int, templateId: Long, templateName: String) {
        _uiState.update { state ->
            val currentAssignments = state.dayAssignments.toMutableMap()
            currentAssignments[dayOfWeek] = DayTemplateAssignment(templateId, templateName)
            state.copy(dayAssignments = currentAssignments)
        }
    }

    fun setDurationWeeks(weeks: Int) {
        _uiState.update { it.copy(durationWeeks = weeks) }
    }

    fun generateSchedule() {
        viewModelScope.launch {
            val state = _uiState.value
            val assignments = state.dayAssignments.filter { it.value != null }
            
            if (assignments.isEmpty()) return@launch
            
            _uiState.update { it.copy(isGenerating = true) }
            
            val workoutsToCreate = mutableListOf<ScheduledGymWorkout>()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = getStartOfDay(System.currentTimeMillis())
            
            // Find the next occurrence of each selected day
            for (week in 0 until state.durationWeeks) {
                for ((dayOfWeek, assignment) in assignments) {
                    if (assignment == null) continue
                    
                    val targetCalendar = Calendar.getInstance()
                    targetCalendar.timeInMillis = calendar.timeInMillis
                    targetCalendar.add(Calendar.WEEK_OF_YEAR, week)
                    
                    // Set to the correct day of week
                    val currentDayOfWeek = targetCalendar.get(Calendar.DAY_OF_WEEK)
                    val daysToAdd = (dayOfWeek - currentDayOfWeek + 7) % 7
                    targetCalendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
                    
                    // Skip if this date is in the past
                    if (targetCalendar.timeInMillis < System.currentTimeMillis()) {
                        targetCalendar.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                    
                    workoutsToCreate.add(
                        ScheduledGymWorkout(
                            templateId = assignment.templateId,
                            templateName = assignment.templateName,
                            scheduledDate = getStartOfDay(targetCalendar.timeInMillis)
                        )
                    )
                }
            }
            
            // Insert all workouts
            scheduledGymWorkoutDao.insertAll(workoutsToCreate)
            
            // Reset the form
            _uiState.update { 
                it.copy(
                    isGenerating = false,
                    dayAssignments = emptyMap(),
                    durationWeeks = 4,
                    scheduleGenerated = true
                ) 
            }
        }
    }

    fun clearScheduleGenerated() {
        _uiState.update { it.copy(scheduleGenerated = false) }
    }

    fun deleteScheduledWorkout(id: Long) {
        viewModelScope.launch {
            scheduledGymWorkoutDao.getById(id)?.let { workout ->
                scheduledGymWorkoutDao.delete(workout)
            }
        }
    }

    fun clearAllScheduledWorkouts() {
        viewModelScope.launch {
            val workouts = _uiState.value.scheduledWorkouts
            workouts.forEach { workout ->
                scheduledGymWorkoutDao.getById(workout.id)?.let {
                    scheduledGymWorkoutDao.delete(it)
                }
            }
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        return com.runtracker.shared.util.TimeUtils.getStartOfDay(timestamp)
    }
}

data class DayTemplateAssignment(
    val templateId: Long,
    val templateName: String
)

data class GymSchedulerUiState(
    val templates: List<TemplateInfo> = emptyList(),
    val scheduledWorkouts: List<ScheduledWorkoutDisplay> = emptyList(),
    val dayAssignments: Map<Int, DayTemplateAssignment?> = emptyMap(), // dayOfWeek -> template
    val durationWeeks: Int = 4,
    val isGenerating: Boolean = false,
    val scheduleGenerated: Boolean = false,
    val isLoadingTemplates: Boolean = true
)
