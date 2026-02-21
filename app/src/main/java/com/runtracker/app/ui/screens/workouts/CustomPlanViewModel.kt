package com.runtracker.app.ui.screens.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.CustomWorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CustomPlanViewModel @Inject constructor(
    private val repository: CustomWorkoutRepository
) : ViewModel() {

    private val _plans = MutableStateFlow<List<CustomTrainingPlan>>(emptyList())
    val plans: StateFlow<List<CustomTrainingPlan>> = _plans.asStateFlow()

    private val _activePlan = MutableStateFlow<CustomTrainingPlan?>(null)
    val activePlan: StateFlow<CustomTrainingPlan?> = _activePlan.asStateFlow()

    private val _customWorkouts = MutableStateFlow<List<CustomRunWorkout>>(emptyList())
    val customWorkouts: StateFlow<List<CustomRunWorkout>> = _customWorkouts.asStateFlow()

    // Plan builder state
    private val _builderState = MutableStateFlow(PlanBuilderState())
    val builderState: StateFlow<PlanBuilderState> = _builderState.asStateFlow()

    init {
        loadPlans()
        loadCustomWorkouts()
    }

    private fun loadPlans() {
        viewModelScope.launch {
            repository.getAllPlansFlow().collect { planList ->
                _plans.value = planList
            }
        }
        viewModelScope.launch {
            repository.getActivePlanFlow().collect { plan ->
                _activePlan.value = plan
            }
        }
    }

    private fun loadCustomWorkouts() {
        viewModelScope.launch {
            repository.getAllWorkoutsFlow().collect { workouts ->
                _customWorkouts.value = workouts
            }
        }
    }

    fun toggleFavorite(planId: Long) {
        viewModelScope.launch {
            val plan = repository.getPlanById(planId)
            plan?.let {
                repository.setPlanFavorite(planId, !it.isFavorite)
            }
        }
    }

    fun deletePlan(planId: Long) {
        viewModelScope.launch {
            repository.deletePlanById(planId)
        }
    }

    fun setActivePlan(planId: Long) {
        viewModelScope.launch {
            repository.setActivePlan(planId)
        }
    }

    // Plan Builder Methods
    fun startNewPlan() {
        _builderState.value = PlanBuilderState()
    }

    fun editPlan(plan: CustomTrainingPlan) {
        _builderState.value = PlanBuilderState(
            id = plan.id,
            name = plan.name,
            description = plan.description,
            goalType = plan.goalType,
            targetRaceDistance = plan.targetRaceDistance,
            targetRaceTime = plan.targetRaceTime,
            durationWeeks = plan.durationWeeks,
            weeks = plan.weeks.toMutableList(),
            isEditing = true
        )
    }

    fun updatePlanName(name: String) {
        _builderState.value = _builderState.value.copy(name = name)
    }

    fun updatePlanDescription(description: String) {
        _builderState.value = _builderState.value.copy(description = description)
    }

    fun updateGoalType(goalType: GoalType) {
        _builderState.value = _builderState.value.copy(goalType = goalType)
    }

    fun updateDurationWeeks(weeks: Int) {
        val currentWeeks = _builderState.value.weeks.toMutableList()
        
        if (weeks > currentWeeks.size) {
            // Add new weeks
            for (i in currentWeeks.size until weeks) {
                currentWeeks.add(PlanWeek(
                    weekNumber = i + 1,
                    name = "Week ${i + 1}",
                    weekType = WeekType.BUILD
                ))
            }
        } else if (weeks < currentWeeks.size) {
            // Remove weeks from end
            while (currentWeeks.size > weeks) {
                currentWeeks.removeAt(currentWeeks.size - 1)
            }
        }
        
        _builderState.value = _builderState.value.copy(
            durationWeeks = weeks,
            weeks = currentWeeks
        )
    }

    fun updateWeek(weekIndex: Int, week: PlanWeek) {
        val currentWeeks = _builderState.value.weeks.toMutableList()
        if (weekIndex in currentWeeks.indices) {
            currentWeeks[weekIndex] = week
            _builderState.value = _builderState.value.copy(weeks = currentWeeks)
        }
    }

    fun updateWeekType(weekIndex: Int, weekType: WeekType) {
        val currentWeeks = _builderState.value.weeks.toMutableList()
        if (weekIndex in currentWeeks.indices) {
            currentWeeks[weekIndex] = currentWeeks[weekIndex].copy(weekType = weekType)
            _builderState.value = _builderState.value.copy(weeks = currentWeeks)
        }
    }

    fun addWorkoutToWeek(weekIndex: Int, workout: PlanWorkout) {
        val currentWeeks = _builderState.value.weeks.toMutableList()
        if (weekIndex in currentWeeks.indices) {
            val week = currentWeeks[weekIndex]
            val updatedWorkouts = week.workouts.toMutableList()
            updatedWorkouts.add(workout)
            currentWeeks[weekIndex] = week.copy(workouts = updatedWorkouts)
            _builderState.value = _builderState.value.copy(weeks = currentWeeks)
        }
    }

    fun updateWorkoutInWeek(weekIndex: Int, workoutIndex: Int, workout: PlanWorkout) {
        val currentWeeks = _builderState.value.weeks.toMutableList()
        if (weekIndex in currentWeeks.indices) {
            val week = currentWeeks[weekIndex]
            val updatedWorkouts = week.workouts.toMutableList()
            if (workoutIndex in updatedWorkouts.indices) {
                updatedWorkouts[workoutIndex] = workout
                currentWeeks[weekIndex] = week.copy(workouts = updatedWorkouts)
                _builderState.value = _builderState.value.copy(weeks = currentWeeks)
            }
        }
    }

    fun removeWorkoutFromWeek(weekIndex: Int, workoutIndex: Int) {
        val currentWeeks = _builderState.value.weeks.toMutableList()
        if (weekIndex in currentWeeks.indices) {
            val week = currentWeeks[weekIndex]
            val updatedWorkouts = week.workouts.toMutableList()
            if (workoutIndex in updatedWorkouts.indices) {
                updatedWorkouts.removeAt(workoutIndex)
                currentWeeks[weekIndex] = week.copy(workouts = updatedWorkouts)
                _builderState.value = _builderState.value.copy(weeks = currentWeeks)
            }
        }
    }

    fun copyWeek(fromWeekIndex: Int, toWeekIndex: Int) {
        val currentWeeks = _builderState.value.weeks.toMutableList()
        if (fromWeekIndex in currentWeeks.indices && toWeekIndex in currentWeeks.indices) {
            val sourceWeek = currentWeeks[fromWeekIndex]
            val copiedWorkouts = sourceWeek.workouts.map { it.copy(id = UUID.randomUUID().toString()) }
            currentWeeks[toWeekIndex] = currentWeeks[toWeekIndex].copy(
                workouts = copiedWorkouts,
                weekType = sourceWeek.weekType
            )
            _builderState.value = _builderState.value.copy(weeks = currentWeeks)
        }
    }

    fun generateBasicPlan() {
        val weeks = _builderState.value.durationWeeks
        val generatedWeeks = mutableListOf<PlanWeek>()
        
        for (i in 0 until weeks) {
            val weekType = when {
                i < weeks * 0.3 -> WeekType.BASE
                i < weeks * 0.7 -> WeekType.BUILD
                i == weeks - 2 -> WeekType.PEAK
                i == weeks - 1 -> WeekType.TAPER
                (i + 1) % 4 == 0 -> WeekType.RECOVERY
                else -> WeekType.BUILD
            }
            
            val workouts = generateWeekWorkouts(weekType, i + 1)
            
            generatedWeeks.add(PlanWeek(
                weekNumber = i + 1,
                name = "Week ${i + 1}",
                weekType = weekType,
                workouts = workouts
            ))
        }
        
        _builderState.value = _builderState.value.copy(weeks = generatedWeeks)
    }

    private fun generateWeekWorkouts(weekType: WeekType, weekNumber: Int): List<PlanWorkout> {
        return when (weekType) {
            WeekType.BASE -> listOf(
                PlanWorkout(dayOfWeek = 2, workoutType = WorkoutType.EASY_RUN, name = "Easy Run", targetDurationMinutes = 30),
                PlanWorkout(dayOfWeek = 4, workoutType = WorkoutType.EASY_RUN, name = "Easy Run", targetDurationMinutes = 35),
                PlanWorkout(dayOfWeek = 6, workoutType = WorkoutType.LONG_RUN, name = "Long Run", targetDurationMinutes = 45)
            )
            WeekType.BUILD -> listOf(
                PlanWorkout(dayOfWeek = 2, workoutType = WorkoutType.EASY_RUN, name = "Easy Run", targetDurationMinutes = 35),
                PlanWorkout(dayOfWeek = 3, workoutType = WorkoutType.INTERVAL_TRAINING, name = "Speed Work"),
                PlanWorkout(dayOfWeek = 5, workoutType = WorkoutType.TEMPO_RUN, name = "Tempo Run", targetDurationMinutes = 40),
                PlanWorkout(dayOfWeek = 7, workoutType = WorkoutType.LONG_RUN, name = "Long Run", targetDurationMinutes = 60 + weekNumber * 5)
            )
            WeekType.PEAK -> listOf(
                PlanWorkout(dayOfWeek = 2, workoutType = WorkoutType.EASY_RUN, name = "Easy Run", targetDurationMinutes = 40),
                PlanWorkout(dayOfWeek = 3, workoutType = WorkoutType.VO2_MAX_INTERVALS, name = "VO2 Max Intervals"),
                PlanWorkout(dayOfWeek = 5, workoutType = WorkoutType.THRESHOLD_RUN, name = "Threshold Run", targetDurationMinutes = 45),
                PlanWorkout(dayOfWeek = 7, workoutType = WorkoutType.LONG_RUN, name = "Peak Long Run", targetDurationMinutes = 90)
            )
            WeekType.TAPER -> listOf(
                PlanWorkout(dayOfWeek = 2, workoutType = WorkoutType.EASY_RUN, name = "Easy Run", targetDurationMinutes = 25),
                PlanWorkout(dayOfWeek = 4, workoutType = WorkoutType.SHAKE_OUT_RUN, name = "Shake Out", targetDurationMinutes = 20),
                PlanWorkout(dayOfWeek = 6, workoutType = WorkoutType.REST_DAY, name = "Rest Day")
            )
            WeekType.RECOVERY -> listOf(
                PlanWorkout(dayOfWeek = 2, workoutType = WorkoutType.RECOVERY_RUN, name = "Recovery Run", targetDurationMinutes = 25),
                PlanWorkout(dayOfWeek = 4, workoutType = WorkoutType.EASY_RUN, name = "Easy Run", targetDurationMinutes = 30),
                PlanWorkout(dayOfWeek = 6, workoutType = WorkoutType.LONG_RUN, name = "Easy Long Run", targetDurationMinutes = 40)
            )
            WeekType.RACE -> listOf(
                PlanWorkout(dayOfWeek = 2, workoutType = WorkoutType.SHAKE_OUT_RUN, name = "Pre-Race Shake Out", targetDurationMinutes = 15),
                PlanWorkout(dayOfWeek = 4, workoutType = WorkoutType.REST_DAY, name = "Rest Day"),
                PlanWorkout(dayOfWeek = 6, workoutType = WorkoutType.RACE_SIMULATION, name = "Race Day!")
            )
        }
    }

    fun savePlan(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _builderState.value
            if (state.name.isBlank()) return@launch

            val plan = CustomTrainingPlan(
                id = if (state.isEditing) state.id else 0,
                name = state.name,
                description = state.description,
                goalType = state.goalType,
                targetRaceDistance = state.targetRaceDistance,
                targetRaceTime = state.targetRaceTime,
                durationWeeks = state.durationWeeks,
                weeks = state.weeks,
                lastModified = System.currentTimeMillis()
            )

            if (state.isEditing) {
                repository.updatePlan(plan)
            } else {
                repository.savePlan(plan)
            }
            
            _builderState.value = PlanBuilderState()
            onSuccess()
        }
    }
}

data class PlanBuilderState(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val goalType: GoalType = GoalType.CUSTOM,
    val targetRaceDistance: Double? = null,
    val targetRaceTime: Long? = null,
    val durationWeeks: Int = 12,
    val weeks: List<PlanWeek> = emptyList(),
    val isEditing: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank() && durationWeeks > 0
    
    val totalWorkouts: Int
        get() = weeks.sumOf { it.workouts.size }
}
