package com.context.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    fun getGreeting(): String {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(calendar: Calendar): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return yesterday.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
               yesterday.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
    }

    fun isThisWeek(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
               today.get(Calendar.WEEK_OF_YEAR) == calendar.get(Calendar.WEEK_OF_YEAR)
    }

    fun isThisMonth(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
               today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
    }

    fun isThisYear(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
    }

    fun getPreciseLabel(range: TimeRange, calendar: Calendar): String {
        return when (range) {
            TimeRange.TODAY -> {
                when {
                    isToday(calendar) -> "Total Spent Today"
                    isYesterday(calendar) -> "Total Spent Yesterday"
                    else -> {
                        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                        "Total Spent on ${sdf.format(calendar.time)}"
                    }
                }
            }
            TimeRange.WEEK -> {
                if (isThisWeek(calendar)) {
                    "Total Spent This Week"
                } else {
                    val start = calendar.clone() as Calendar
                    start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                    val end = start.clone() as Calendar
                    end.add(Calendar.DAY_OF_WEEK, 6)
                    
                    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                    "Total for ${sdf.format(start.time)} - ${sdf.format(end.time)}"
                }
            }
            TimeRange.MONTH -> {
                if (isThisMonth(calendar)) {
                    "Total Spent This Month"
                } else {
                    val sdf = if (isThisYear(calendar)) {
                        SimpleDateFormat("MMMM", Locale.getDefault())
                    } else {
                        SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    }
                    "Total Spent in ${sdf.format(calendar.time)}"
                }
            }
            TimeRange.YEAR -> {
                if (isThisYear(calendar)) {
                    "Total Spent This Year"
                } else {
                    "Total Spent in ${calendar.get(Calendar.YEAR)}"
                }
            }
            TimeRange.ALL -> "Total Spent All Time"
            TimeRange.CUSTOM -> "Total for Period"
        }
    }

    fun getDayOfMonthSuffix(day: Int): String {
        return when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
    }

    /**
     * Returns days left in the month including today.
     */
    fun getDaysRemainingInMonth(): Int {
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        return (daysInMonth - currentDay + 1).coerceAtLeast(1)
    }

    fun getMonthElapsedProgress(): Float {
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH).toFloat()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH).toFloat()
        return (currentDay / daysInMonth).coerceIn(0f, 1f)
    }

    /**
     * Predicts the run-out date based on current spending rate.
     */
    fun getExpectedRunOutDate(spent: Double, limit: Double): String? {
        if (limit <= 0 || spent <= 0 || spent >= limit) return null
        
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Average spend per day since the start of the month
        val dailyBurn = spent / currentDay
        if (dailyBurn <= 0) return null
        
        // Calculate the projected day of the month it will hit zero
        val projectedDayOfMonth = (limit / dailyBurn).toInt()
        
        // Only show if it runs out within this month
        if (projectedDayOfMonth <= daysInMonth && projectedDayOfMonth >= currentDay) {
            val runOutCalendar = Calendar.getInstance()
            runOutCalendar.set(Calendar.DAY_OF_MONTH, projectedDayOfMonth)
            
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            return sdf.format(runOutCalendar.time)
        }
        
        return null
    }
}
