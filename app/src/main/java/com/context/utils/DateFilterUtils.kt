package com.context.utils

import java.util.Calendar

enum class TimeRange {
    TODAY, WEEK, MONTH, YEAR, ALL, CUSTOM
}

object DateFilterUtils {
    fun getTimeRange(range: TimeRange, calendar: Calendar): Pair<Long, Long> {
        val cal = calendar.clone() as Calendar

        // For start time, reset to the beginning of the period
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val startTime: Long
        val endTime: Long

        when (range) {
            TimeRange.TODAY -> {
                startTime = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                endTime = cal.timeInMillis - 1
            }
            TimeRange.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                startTime = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                endTime = cal.timeInMillis - 1
            }
            TimeRange.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                startTime = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                endTime = cal.timeInMillis - 1
            }
            TimeRange.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                startTime = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                endTime = cal.timeInMillis - 1
            }
            TimeRange.ALL -> {
                startTime = 0L
                endTime = Long.MAX_VALUE
            }
            TimeRange.CUSTOM -> {
                // For custom, the caller is responsible for providing bounds
                // Returning full range as a fallback
                startTime = 0L
                endTime = Long.MAX_VALUE
            }
        }
        return Pair(startTime, endTime)
    }
}
