package com.runtracker.app.ui.screens.calendar

sealed class WorkoutSuggestion {
    abstract val id: String
    abstract val title: String
    abstract val description: String
    abstract val impact: String
    
    data class AddExerciseToTemplate(
        override val id: String,
        override val title: String,
        override val description: String,
        override val impact: String,
        val templateId: Long,
        val templateName: String,
        val exerciseName: String,
        val muscleGroup: String,
        val suggestedSets: Int = 3,
        val suggestedReps: String = "8-12"
    ) : WorkoutSuggestion()
    
    data class RemoveExerciseFromTemplate(
        override val id: String,
        override val title: String,
        override val description: String,
        override val impact: String,
        val templateId: Long,
        val templateName: String,
        val exerciseName: String
    ) : WorkoutSuggestion()
    
    data class SwapExercise(
        override val id: String,
        override val title: String,
        override val description: String,
        override val impact: String,
        val templateId: Long,
        val templateName: String,
        val currentExercise: String,
        val suggestedExercise: String,
        val reason: String
    ) : WorkoutSuggestion()
    
    data class AddRestDay(
        override val id: String,
        override val title: String,
        override val description: String,
        override val impact: String,
        val dayOfWeek: Int,
        val dayName: String
    ) : WorkoutSuggestion()
    
    data class SwapRunningWorkout(
        override val id: String,
        override val title: String,
        override val description: String,
        override val impact: String,
        val weekNumber: Int,
        val dayOfWeek: Int,
        val currentWorkoutType: String,
        val suggestedWorkoutType: String
    ) : WorkoutSuggestion()
    
    data class AdjustRunDuration(
        override val id: String,
        override val title: String,
        override val description: String,
        override val impact: String,
        val weekNumber: Int,
        val dayOfWeek: Int,
        val currentDuration: Int,
        val suggestedDuration: Int
    ) : WorkoutSuggestion()
    
    data class AddGymDay(
        override val id: String,
        override val title: String,
        override val description: String,
        override val impact: String,
        val dayOfWeek: Int,
        val dayName: String,
        val suggestedTemplateId: Long?,
        val suggestedTemplateName: String?
    ) : WorkoutSuggestion()
    
    data class RemoveGymDay(
        override val id: String,
        override val title: String,
        override val description: String,
        override val impact: String,
        val dayOfWeek: Int,
        val dayName: String
    ) : WorkoutSuggestion()
    
    data class SwapGymTemplate(
        override val id: String,
        override val title: String,
        override val description: String,
        override val impact: String,
        val scheduledWorkoutId: Long,
        val scheduledDate: Long,
        val currentTemplateId: Long,
        val currentTemplateName: String,
        val suggestedTemplateId: Long,
        val suggestedTemplateName: String
    ) : WorkoutSuggestion()
}
