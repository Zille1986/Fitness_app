package com.runtracker.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.runtracker.app.MainActivity
import com.runtracker.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_HYDRATION = "hydration"
        const val CHANNEL_ACHIEVEMENTS = "achievements"
        const val CHANNEL_TRACKING = "tracking"
        
        const val NOTIFICATION_WORKOUT_REMINDER = 1001
        const val NOTIFICATION_HYDRATION = 1002
        const val NOTIFICATION_ACHIEVEMENT = 1003
        const val NOTIFICATION_REST_DAY = 1004
        const val NOTIFICATION_STREAK_ALERT = 1005
        const val NOTIFICATION_BADGE_EARNED = 1006
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Workout Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders for scheduled workouts"
                },
                NotificationChannel(
                    CHANNEL_HYDRATION,
                    "Hydration Reminders",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Reminders to drink water"
                },
                NotificationChannel(
                    CHANNEL_ACHIEVEMENTS,
                    "Achievements",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Personal records and achievements"
                },
                NotificationChannel(
                    CHANNEL_TRACKING,
                    "Activity Tracking",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Ongoing activity tracking notifications"
                }
            )
            
            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }

    fun showWorkoutReminder(workoutType: String, scheduledTime: String) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time for your $workoutType!")
            .setContentText("Your scheduled workout is ready. Let's go! 💪")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Start Now",
                pendingIntent
            )
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_WORKOUT_REMINDER, notification)
    }

    fun showHydrationReminder(currentMl: Int, targetMl: Int) {
        if (!hasNotificationPermission()) return
        
        val remaining = targetMl - currentMl
        val percentage = (currentMl * 100) / targetMl

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_HYDRATION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("💧 Stay Hydrated!")
            .setContentText("You've had ${currentMl}ml ($percentage%). ${remaining}ml to go!")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_HYDRATION, notification)
    }

    fun showAchievement(title: String, description: String) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🏆 $title")
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ACHIEVEMENT, notification)
    }

    fun showRestDaySuggestion() {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("😴 Rest Day Suggestion")
            .setContentText("You've been training hard! Consider taking a rest day for recovery.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_REST_DAY, notification)
    }

    fun showStreakAtRiskAlert(currentStreak: Int) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔥 Don't break your $currentStreak day streak!")
            .setContentText("You haven't worked out today. Keep the momentum going!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Start Workout",
                pendingIntent
            )
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_STREAK_ALERT, notification)
    }

    fun showBadgeEarned(badgeName: String, badgeDescription: String) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🏅 New Badge: $badgeName")
            .setContentText(badgeDescription)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_BADGE_EARNED, notification)
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
