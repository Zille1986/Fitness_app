package com.runtracker.app.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class WorkoutReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: AppNotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val workoutType = inputData.getString(KEY_WORKOUT_TYPE) ?: "workout"
        val message = inputData.getString(KEY_MESSAGE) ?: "Time to train!"
        
        notificationManager.showWorkoutReminder(workoutType, message)
        
        return Result.success()
    }

    companion object {
        const val KEY_WORKOUT_TYPE = "workout_type"
        const val KEY_MESSAGE = "message"
        const val DAILY_REMINDER_TAG = "daily_workout_reminder"
        
        fun scheduleDailyReminder(
            context: Context,
            hourOfDay: Int,
            minute: Int,
            workoutType: String = "workout"
        ) {
            val workManager = WorkManager.getInstance(context)
            
            // Cancel any existing daily reminders
            workManager.cancelAllWorkByTag(DAILY_REMINDER_TAG)
            
            // Calculate delay until next reminder time
            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                if (before(now)) {
                    add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
            }
            val initialDelay = target.timeInMillis - now.timeInMillis
            
            val inputData = workDataOf(
                KEY_WORKOUT_TYPE to workoutType,
                KEY_MESSAGE to "Don't break your streak! Time to train."
            )
            
            val reminderRequest = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(DAILY_REMINDER_TAG)
                .build()
            
            workManager.enqueueUniquePeriodicWork(
                DAILY_REMINDER_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderRequest
            )
        }
        
        fun cancelDailyReminder(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(DAILY_REMINDER_TAG)
        }
    }
}
