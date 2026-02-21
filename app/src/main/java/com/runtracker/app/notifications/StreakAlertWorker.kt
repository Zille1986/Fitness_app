package com.runtracker.app.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.runtracker.shared.data.repository.GymRepository
import com.runtracker.shared.data.repository.RunRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class StreakAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val runRepository: RunRepository,
    private val gymRepository: GymRepository,
    private val notificationManager: AppNotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Calculate current streak
        val runs = runRepository.getAllRunsOnce()
        val gymWorkouts = gymRepository.getAllWorkoutsOnce()
        
        val workoutDates = mutableSetOf<String>()
        runs.forEach { run ->
            val cal = Calendar.getInstance().apply { timeInMillis = run.startTime }
            workoutDates.add("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}")
        }
        gymWorkouts.forEach { workout ->
            val cal = Calendar.getInstance().apply { timeInMillis = workout.startTime }
            workoutDates.add("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}")
        }
        
        val today = Calendar.getInstance()
        val todayKey = "${today.get(Calendar.YEAR)}-${today.get(Calendar.DAY_OF_YEAR)}"
        
        // Check if already worked out today
        if (workoutDates.contains(todayKey)) {
            return Result.success()
        }
        
        // Calculate streak (excluding today)
        var currentStreak = 0
        val checkDate = Calendar.getInstance()
        for (i in 1..365) {
            checkDate.timeInMillis = today.timeInMillis
            checkDate.add(Calendar.DAY_OF_YEAR, -i)
            val dateKey = "${checkDate.get(Calendar.YEAR)}-${checkDate.get(Calendar.DAY_OF_YEAR)}"
            if (workoutDates.contains(dateKey)) {
                currentStreak++
            } else {
                break
            }
        }
        
        // Only alert if there's a streak to protect
        if (currentStreak >= 2) {
            notificationManager.showStreakAtRiskAlert(currentStreak)
        }
        
        return Result.success()
    }

    companion object {
        const val STREAK_ALERT_TAG = "streak_alert"
        
        fun scheduleEveningAlert(context: Context) {
            val workManager = WorkManager.getInstance(context)
            
            workManager.cancelAllWorkByTag(STREAK_ALERT_TAG)
            
            // Schedule for 7 PM every day
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 19)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            val initialDelay = target.timeInMillis - now.timeInMillis
            
            val alertRequest = PeriodicWorkRequestBuilder<StreakAlertWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(STREAK_ALERT_TAG)
                .build()
            
            workManager.enqueueUniquePeriodicWork(
                STREAK_ALERT_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                alertRequest
            )
        }
        
        fun cancelStreakAlerts(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(STREAK_ALERT_TAG)
        }
    }
}
