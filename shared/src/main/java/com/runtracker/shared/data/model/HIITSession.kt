package com.runtracker.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.runtracker.shared.data.db.Converters

@Entity(tableName = "hiit_sessions")
@TypeConverters(Converters::class)
data class HIITSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val templateId: String,
    val templateName: String,
    val totalDurationMs: Long = 0,
    val exerciseCount: Int = 0,
    val roundsCompleted: Int = 0,
    val totalRounds: Int = 0,
    val caloriesEstimate: Int = 0,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val exerciseLog: String = "[]", // JSON array of exercise results
    val isCompleted: Boolean = false,
    val source: String = "phone" // "phone" or "watch"
) {
    val durationFormatted: String
        get() {
            val totalSeconds = totalDurationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }
}
