package com.runtracker.shared.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.runtracker.shared.data.model.*

@Database(
    entities = [
        Run::class, 
        TrainingPlan::class, 
        UserProfile::class,
        Exercise::class,
        GymWorkout::class,
        WorkoutTemplate::class,
        ExerciseHistory::class,
        DailyNutrition::class,
        NutritionGoals::class,
        PersonalBest::class,
        CustomRunWorkout::class,
        CustomTrainingPlan::class,
        UserGamification::class,
        Achievement::class,
        UserAchievement::class,
        DailyRings::class,
        XpTransaction::class,
        MoodEntry::class,
        MindfulnessSession::class,
        WellnessCheckin::class,
        PersonalizedPlan::class,
        BodyScan::class,
        WorkoutPlan::class,
        ScheduledGymWorkout::class,
        SwimmingWorkout::class,
        SwimmingTrainingPlan::class,
        CyclingWorkout::class,
        CyclingTrainingPlan::class,
        SafetySettings::class,
        CheckInSession::class
    ],
    version = 14,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RunTrackerDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao
    abstract fun trainingPlanDao(): TrainingPlanDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun gymWorkoutDao(): GymWorkoutDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun exerciseHistoryDao(): ExerciseHistoryDao
    abstract fun dailyNutritionDao(): DailyNutritionDao
    abstract fun nutritionGoalsDao(): NutritionGoalsDao
    abstract fun personalBestDao(): PersonalBestDao
    abstract fun customRunWorkoutDao(): CustomRunWorkoutDao
    abstract fun customTrainingPlanDao(): CustomTrainingPlanDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun mentalHealthDao(): MentalHealthDao
    abstract fun personalizedPlanDao(): PersonalizedPlanDao
    abstract fun bodyScanDao(): BodyScanDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun scheduledGymWorkoutDao(): ScheduledGymWorkoutDao
    abstract fun swimmingWorkoutDao(): SwimmingWorkoutDao
    abstract fun swimmingTrainingPlanDao(): SwimmingTrainingPlanDao
    abstract fun cyclingWorkoutDao(): CyclingWorkoutDao
    abstract fun cyclingTrainingPlanDao(): CyclingTrainingPlanDao
    abstract fun safetyDao(): SafetyDao

    companion object {
        @Volatile
        private var INSTANCE: RunTrackerDatabase? = null

        fun getDatabase(context: Context): RunTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RunTrackerDatabase::class.java,
                    "runtracker_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
