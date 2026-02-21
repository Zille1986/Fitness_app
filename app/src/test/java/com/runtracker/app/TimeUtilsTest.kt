package com.runtracker.app

import com.runtracker.shared.util.TimeUtils
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class TimeUtilsTest {

    @Test
    fun `getStartOfDay returns midnight`() {
        // Given: A timestamp in the middle of the day
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 15, 14, 30, 45)
            set(Calendar.MILLISECOND, 500)
        }
        val middayTimestamp = calendar.timeInMillis

        // When: We get the start of day
        val startOfDay = TimeUtils.getStartOfDay(middayTimestamp)

        // Then: It should be midnight
        val resultCalendar = Calendar.getInstance().apply { timeInMillis = startOfDay }
        assertEquals(0, resultCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCalendar.get(Calendar.MINUTE))
        assertEquals(0, resultCalendar.get(Calendar.SECOND))
        assertEquals(0, resultCalendar.get(Calendar.MILLISECOND))
        assertEquals(2024, resultCalendar.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, resultCalendar.get(Calendar.MONTH))
        assertEquals(15, resultCalendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `getStartOfWeek returns Monday for any day`() {
        // Given: A Wednesday
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 13, 12, 0, 0) // Wednesday March 13, 2024
            set(Calendar.MILLISECOND, 0)
        }
        val wednesdayTimestamp = calendar.timeInMillis

        // When: We get the start of week
        val startOfWeek = TimeUtils.getStartOfWeek(wednesdayTimestamp)

        // Then: It should be Monday
        val resultCalendar = Calendar.getInstance().apply { timeInMillis = startOfWeek }
        assertEquals(Calendar.MONDAY, resultCalendar.get(Calendar.DAY_OF_WEEK))
        assertEquals(11, resultCalendar.get(Calendar.DAY_OF_MONTH)) // Monday March 11, 2024
        assertEquals(0, resultCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCalendar.get(Calendar.MINUTE))
        assertEquals(0, resultCalendar.get(Calendar.SECOND))
        assertEquals(0, resultCalendar.get(Calendar.MILLISECOND))
    }

    @Test
    fun `getStartOfWeek handles Sunday correctly (ISO-8601)`() {
        // Given: A Sunday
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 17, 15, 30, 0) // Sunday March 17, 2024
            set(Calendar.MILLISECOND, 0)
        }
        val sundayTimestamp = calendar.timeInMillis

        // When: We get the start of week
        val startOfWeek = TimeUtils.getStartOfWeek(sundayTimestamp)

        // Then: It should be the previous Monday (ISO-8601)
        val resultCalendar = Calendar.getInstance().apply { timeInMillis = startOfWeek }
        assertEquals(Calendar.MONDAY, resultCalendar.get(Calendar.DAY_OF_WEEK))
        assertEquals(11, resultCalendar.get(Calendar.DAY_OF_MONTH)) // Monday March 11, 2024
    }

    @Test
    fun `getStartOfWeek returns same Monday when called on Monday`() {
        // Given: A Monday
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 11, 9, 0, 0) // Monday March 11, 2024
            set(Calendar.MILLISECOND, 0)
        }
        val mondayTimestamp = calendar.timeInMillis

        // When: We get the start of week
        val startOfWeek = TimeUtils.getStartOfWeek(mondayTimestamp)

        // Then: It should be the same Monday at midnight
        val resultCalendar = Calendar.getInstance().apply { timeInMillis = startOfWeek }
        assertEquals(Calendar.MONDAY, resultCalendar.get(Calendar.DAY_OF_WEEK))
        assertEquals(11, resultCalendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, resultCalendar.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `calculateWeekNumber returns 1 for same week`() {
        val startDate = System.currentTimeMillis()
        val currentDate = startDate + TimeUtils.ONE_DAY_MS * 3 // 3 days later

        val weekNumber = TimeUtils.calculateWeekNumber(startDate, currentDate)

        assertEquals(1, weekNumber)
    }

    @Test
    fun `calculateWeekNumber returns correct week for 14 days later`() {
        val startDate = System.currentTimeMillis()
        val currentDate = startDate + TimeUtils.ONE_DAY_MS * 14 // 14 days later

        val weekNumber = TimeUtils.calculateWeekNumber(startDate, currentDate)

        assertEquals(3, weekNumber) // Week 1 (days 0-6), Week 2 (days 7-13), Week 3 (days 14-20)
    }

    @Test
    fun `calculateWeekNumber returns 2 for exactly 7 days later`() {
        val startDate = System.currentTimeMillis()
        val currentDate = startDate + TimeUtils.ONE_WEEK_MS

        val weekNumber = TimeUtils.calculateWeekNumber(startDate, currentDate)

        assertEquals(2, weekNumber)
    }

    @Test
    fun `constants have correct values`() {
        assertEquals(1000L, TimeUtils.ONE_SECOND_MS)
        assertEquals(60_000L, TimeUtils.ONE_MINUTE_MS)
        assertEquals(3_600_000L, TimeUtils.ONE_HOUR_MS)
        assertEquals(86_400_000L, TimeUtils.ONE_DAY_MS)
        assertEquals(604_800_000L, TimeUtils.ONE_WEEK_MS)
    }

    @Test
    fun `ONE_WEEK_MS equals 7 days in milliseconds`() {
        assertEquals(7 * 24 * 60 * 60 * 1000L, TimeUtils.ONE_WEEK_MS)
    }
}
