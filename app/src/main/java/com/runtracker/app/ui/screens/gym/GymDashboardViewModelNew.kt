package com.runtracker.app.ui.screens.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.db.ScheduledGymWorkoutDao
import com.runtracker.shared.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.runtracker.shared.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GymDashboardViewModelNew @Inject constructor(
    private val gymRepository: GymRepository,
    private val scheduledGymWorkoutDao: ScheduledGymWorkoutDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(GymDashboardUiStateNew())
    val uiState: StateFlow<GymDashboardUiStateNew> = _uiState.asStateFlow()

    init {
        initializeData()
        loadData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            gymRepository.initializeDefaultExercises()
            gymRepository.initializeDefaultTemplates()
        }
    }

    private fun loadData() {
        loadNextWorkout()
        loadRecentWorkouts()
        loadWeeklyStats()
        loadTemplates()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            gymRepository.getAllTemplates().collect { templates ->
                _uiState.update {
                    it.copy(
                        templates = templates.map { t ->
                            TemplateInfo(
                                id = t.id,
                                name = t.name,
                                exerciseCount = t.exercises.size
                            )
                        }
                    )
                }
            }
        }
    }

    private fun loadNextWorkout() {
        viewModelScope.launch {
            // Get start of today
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = calendar.timeInMillis
            
            // Get upcoming scheduled gym workouts from today onwards
            val upcomingWorkouts = scheduledGymWorkoutDao.getUpcomingWorkouts(todayStart).first()
            
            if (upcomingWorkouts.isNotEmpty()) {
                val nextScheduled = upcomingWorkouts.first()
                
                // Get the template details
                val template = gymRepository.getTemplateById(nextScheduled.templateId)
                val exerciseCount = template?.exercises?.size ?: 0
                
                // Calculate days from now
                val scheduledDate = Calendar.getInstance().apply { timeInMillis = nextScheduled.scheduledDate }
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val daysDiff = ((scheduledDate.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                
                val scheduledLabel = when (daysDiff) {
                    0 -> "Today"
                    1 -> "Tomorrow"
                    else -> "In $daysDiff days"
                }
                
                _uiState.update {
                    it.copy(
                        nextWorkout = NextWorkoutInfo(
                            templateId = nextScheduled.templateId,
                            name = nextScheduled.templateName,
                            exerciseCount = exerciseCount,
                            lastPerformed = null,
                            scheduledDate = scheduledLabel
                        )
                    )
                }
            } else {
                // Fallback to first template if no scheduled workouts
                val templates = gymRepository.getAllTemplates().first()
                if (templates.isNotEmpty()) {
                    val template = templates.first()
                    
                    _uiState.update {
                        it.copy(
                            nextWorkout = NextWorkoutInfo(
                                templateId = template.id,
                                name = template.name,
                                exerciseCount = template.exercises.size,
                                lastPerformed = null,
                                scheduledDate = null
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadRecentWorkouts() {
        viewModelScope.launch {
            gymRepository.getAllWorkouts().collect { workouts ->
                val recentWorkouts = workouts
                    .sortedByDescending { it.startTime }
                    .take(5)
                    .map { workout ->
                        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                        val totalSets = workout.exercises.sumOf { it.sets.size }
                        
                        RecentWorkoutInfo(
                            id = workout.id,
                            name = workout.name,
                            date = dateFormat.format(Date(workout.startTime)),
                            exerciseCount = workout.exercises.size,
                            totalSets = totalSets
                        )
                    }
                
                _uiState.update { it.copy(recentWorkouts = recentWorkouts) }
            }
        }
    }

    private fun loadWeeklyStats() {
        viewModelScope.launch {
            val weekStart = TimeUtils.getStartOfWeek()
            val firstDayOfWeek = Calendar.getInstance().firstDayOfWeek

            gymRepository.getAllWorkouts().collect { allWorkouts ->
                val weeklyWorkouts = allWorkouts.filter { it.startTime >= weekStart }

                // Calculate daily volume for the week
                val dailyVolume = MutableList(7) { 0f }
                var totalSets = 0

                weeklyWorkouts.forEach { workout ->
                    val workoutCalendar = Calendar.getInstance().apply { timeInMillis = workout.startTime }
                    val dayOfWeek = (workoutCalendar.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
                    
                    var workoutVolume = 0.0
                    workout.exercises.forEach { exercise ->
                        exercise.sets.forEach { set ->
                            workoutVolume += set.weight * set.reps
                            totalSets++
                        }
                    }
                    dailyVolume[dayOfWeek] += workoutVolume.toFloat()
                }

                val totalVolume = dailyVolume.sum()

                _uiState.update {
                    it.copy(
                        weeklyVolume = dailyVolume,
                        totalWeeklyVolume = totalVolume,
                        totalWeeklySets = totalSets
                    )
                }
            }
        }
    }

    fun refresh() {
        loadData()
    }
}

data class GymDashboardUiStateNew(
    val nextWorkout: NextWorkoutInfo? = null,
    val recentWorkouts: List<RecentWorkoutInfo> = emptyList(),
    val weeklyVolume: List<Float> = List(7) { 0f },
    val totalWeeklyVolume: Float = 0f,
    val totalWeeklySets: Int = 0,
    val templates: List<TemplateInfo> = emptyList()
)

data class TemplateInfo(
    val id: Long,
    val name: String,
    val exerciseCount: Int
)
