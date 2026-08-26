package com.context.utils

import com.context.data.Expense
import com.context.data.RecurringExpense
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.min

object RecurringExpenseDetector {

    fun detect(expenses: List<Expense>): List<RecurringExpense> {
        val groupsByMerchant = expenses.groupBy { normalizeMerchant(it.merchant) }
        val recurringExpenses = mutableListOf<RecurringExpense>()

        for ((normalizedMerchant, merchantExpenses) in groupsByMerchant) {
            if (merchantExpenses.size < 2) continue

            val sortedExpenses = merchantExpenses.sortedByDescending { it.timestamp }
            
            // Check for monthly recurrence (28-31 days)
            val monthlyPattern = findRecurrencePattern(sortedExpenses, 28..31)
            if (monthlyPattern != null) {
                recurringExpenses.add(monthlyPattern)
                continue
            }

            // Check for weekly recurrence (6-8 days)
            val weeklyPattern = findRecurrencePattern(sortedExpenses, 6..8)
            if (weeklyPattern != null) {
                recurringExpenses.add(weeklyPattern)
            }
        }

        return recurringExpenses
    }

    private fun findRecurrencePattern(expenses: List<Expense>, intervalRange: IntRange): RecurringExpense? {
        if (expenses.size < 2) return null

        val intervals = mutableListOf<Long>()
        for (i in 0 until expenses.size - 1) {
            val diffMillis = expenses[i].timestamp - expenses[i + 1].timestamp
            val diffDays = diffMillis / (1000 * 60 * 60 * 24)
            intervals.add(diffDays)
        }

        // Check if at least 2 consecutive intervals fall within the range
        // For auto-confirm we need 3 occurrences (2 intervals)
        var matches = 0
        val matchingExpenses = mutableListOf<Expense>()
        
        for (i in intervals.indices) {
            if (intervals[i].toInt() in intervalRange) {
                matches++
                matchingExpenses.add(expenses[i])
                matchingExpenses.add(expenses[i+1])
            }
        }

        if (matches >= 1) { // 2 occurrences = 1 interval match (for suggestion)
            val latestExpense = expenses.first()
            val avgAmount = matchingExpenses.map { it.amount }.average()
            val amountVariance = matchingExpenses.map { abs(it.amount - avgAmount) / avgAmount }.average()
            
            val confidence = when {
                matches >= 2 && amountVariance < 0.05 -> 1.0f // 3 occurrences, low variance
                matches >= 2 -> 0.8f // 3 occurrences, higher variance
                amountVariance < 0.05 -> 0.6f // 2 occurrences, low variance
                else -> 0.4f // 2 occurrences, higher variance
            }

            if (confidence < 0.4f) return null

            val frequencyDays = if (intervalRange.start == 28) 30 else 7
            val nextDate = calculateNextDate(latestExpense.timestamp, frequencyDays)

            return RecurringExpense(
                merchant = latestExpense.merchant,
                averageAmount = avgAmount,
                frequencyDays = frequencyDays,
                lastPaidDate = latestExpense.timestamp,
                nextExpectedDate = nextDate,
                isAutoDetected = true,
                confidenceScore = confidence,
                isActive = confidence > 0.7f, // Auto-confirm if high confidence
                category = latestExpense.category
            )
        }

        return null
    }

    private fun calculateNextDate(lastDate: Long, days: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastDate
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.timeInMillis
    }

    fun normalizeMerchant(merchant: String): String {
        return merchant.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    fun isSameMerchant(a: String, b: String): Boolean {
        val normA = normalizeMerchant(a)
        val normB = normalizeMerchant(b)
        
        if (normA == normB) return true
        if (normA.contains(normB) || normB.contains(normA)) return true
        
        return levenshteinDistance(normA, normB) < 3
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }
}
