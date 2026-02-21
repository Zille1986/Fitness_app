package com.runtracker.shared.util

import java.util.Calendar

object TimeUtils {
    const val ONE_SECOND_MS = 1000L
    const val ONE_MINUTE_MS = 60 * ONE_SECOND_MS
    const val ONE_HOUR_MS = 60 * ONE_MINUTE_MS
    const val ONE_DAY_MS = 24 * ONE_HOUR_MS
    const val ONE_WEEK_MS = 7 * ONE_DAY_MS

    /**
     * Returns the timestamp for the start of the day (midnight) for the given timestamp.
     */
    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Returns the timestamp for the start of the week (Monday at midnight, ISO-8601) for the given timestamp.
     */
    fun getStartOfWeek(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        // First, set time to start of day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Calculate days since Monday (Monday = 2 in Calendar)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY

        // Go back to Monday of this week
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)

        return calendar.timeInMillis
    }

    /**
     * Calculates the week number (1-based) from a start date to a current date.
     * Week 1 includes the start date, week 2 starts 7 days later, etc.
     */
    fun calculateWeekNumber(startDate: Long, currentDate: Long): Int {
        val daysDifference = (currentDate - startDate) / ONE_DAY_MS
        return (daysDifference / 7).toInt() + 1
    }
}
