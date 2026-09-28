package com.context.data

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.context.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

sealed class AskCleaveResult {
    object Idle : AskCleaveResult()
    object Loading : AskCleaveResult()
    data class Success(val amount: Double?, val explanation: String) : AskCleaveResult()
    data class Error(val message: String) : AskCleaveResult()
}

@Singleton
class AskCleaveRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {
    private val TAG = "AskCleave"

    /**
     * Entry point for processing user queries.
     * 100% On-Device Rule-Based Parser.
     * Fulfills privacy promise: No data leaves the phone.
     */
    suspend fun processQuery(userQuery: String): AskCleaveResult = withContext(Dispatchers.IO) {
        try {
            val query = userQuery.lowercase().trim()
            Log.d(TAG, "Analyzing local intent for: $query")

            // Identify the pattern and extract entities (Merchant, Category, Timeframe)
            val intent = parseLocalIntent(query)
                ?: return@withContext AskCleaveResult.Error(
                    "I'm not sure how to answer that yet. Try asking 'How much on Swiggy this month?' or 'Biggest expense this year'."
                )

            Log.d(TAG, "Executing deterministic SQL: ${intent.sql}")
            val result = expenseDao.executeRawQueryForDouble(SimpleSQLiteQuery(intent.sql))
            
            AskCleaveResult.Success(result, intent.explanation)
        } catch (e: Exception) {
            Log.e(TAG, "Local query failed", e)
            AskCleaveResult.Error("Something went wrong while analyzing your local records.")
        }
    }

    private fun parseLocalIntent(query: String): ParsedIntent? {
        val now = Calendar.getInstance()
        
        // --- 1. MERCHANT SPECIFIC QUERIES ---

        // "How much on Swiggy this month?"
        Regex("how much (?:did i spend |have i spent )?on (.+?) this month").find(query)?.let {
            val merchant = it.groupValues[1].trim()
            return ParsedIntent(
                sql = "SELECT SUM(amount) FROM expenses WHERE merchant LIKE '%$merchant%' AND timestamp >= ${getStartOfMonth(now)} AND category != 'Settlement'",
                explanation = "Summing your '$merchant' transactions for the current month."
            )
        }

        // "How much on Swiggy last month?"
        Regex("how much (?:did i spend |have i spent )?on (.+?) last month").find(query)?.let {
            val merchant = it.groupValues[1].trim()
            val start = getStartOfLastMonth(now)
            val end = getStartOfMonth(now) - 1
            return ParsedIntent(
                sql = "SELECT SUM(amount) FROM expenses WHERE merchant LIKE '%$merchant%' AND timestamp BETWEEN $start AND $end AND category != 'Settlement'",
                explanation = "Calculated total for '$merchant' during the previous month."
            )
        }

        // "How much on Swiggy in 2026?"
        Regex("how much (?:did i spend |have i spent )?on (.+?) in (\\d{4})").find(query)?.let {
            val merchant = it.groupValues[1].trim()
            val year = it.groupValues[2].toInt()
            val start = getStartOfYear(now, year)
            val end = getEndOfYear(now, year)
            return ParsedIntent(
                sql = "SELECT SUM(amount) FROM expenses WHERE merchant LIKE '%$merchant%' AND timestamp BETWEEN $start AND $end AND category != 'Settlement'",
                explanation = "Every recorded spending on '$merchant' in $year."
            )
        }

        // --- 2. CATEGORY SPECIFIC QUERIES ---

        // "Total spent on Food this month?"
        Regex("total (?:spent )?on (.+?) this month").find(query)?.let {
            val category = it.groupValues[1].trim()
            return ParsedIntent(
                sql = "SELECT SUM(amount) FROM expenses WHERE category LIKE '%$category%' AND timestamp >= ${getStartOfMonth(now)} AND category != 'Settlement'",
                explanation = "Total spent in '$category' category this month."
            )
        }

        // --- 3. AGGREGATE & EXTREME QUERIES ---

        // "Total spent this month?"
        if ((query.contains("total spent") || query.contains("how much spent")) && query.contains("this month")) {
            return ParsedIntent(
                sql = "SELECT SUM(amount) FROM expenses WHERE timestamp >= ${getStartOfMonth(now)} AND category != 'Settlement'",
                explanation = "This is the sum of every expense recorded this month."
            )
        }

        // "Biggest expense this month?" / "Largest expense this year?"
        if (query.contains("biggest") || query.contains("largest") || query.contains("highest")) {
            val start = if (query.contains("year")) getStartOfYear(now) else getStartOfMonth(now)
            val label = if (query.contains("year")) "this year" else "this month"
            return ParsedIntent(
                sql = "SELECT MAX(amount) FROM expenses WHERE timestamp >= $start AND category != 'Settlement'",
                explanation = "Scanning for your single largest transaction $label."
            )
        }

        // --- 4. DAILY QUERIES ---

        // "How much did I spend today?"
        if (query.contains("today")) {
            return ParsedIntent(
                sql = "SELECT SUM(amount) FROM expenses WHERE timestamp >= ${getStartOfDay(now)} AND category != 'Settlement'",
                explanation = "Total expenses logged since midnight."
            )
        }

        // "How much did I spend yesterday?"
        if (query.contains("yesterday")) {
            val start = getStartOfYesterday(now)
            val end = getStartOfDay(now) - 1
            return ParsedIntent(
                sql = "SELECT SUM(amount) FROM expenses WHERE timestamp BETWEEN $start AND $end AND category != 'Settlement'",
                explanation = "Sum of your expenses from yesterday."
            )
        }

        // --- 5. YEARLY TOTALS ---

        // "Total spent in 2024?"
        Regex("total (?:spent )?in (\\d{4})").find(query)?.let {
            val year = it.groupValues[1].toInt()
            val start = getStartOfYear(now, year)
            val end = getEndOfYear(now, year)
            return ParsedIntent(
                sql = "SELECT SUM(amount) FROM expenses WHERE timestamp BETWEEN $start AND $end AND category != 'Settlement'",
                explanation = "Your total spending footprint for the year $year."
            )
        }

        return null
    }

    private data class ParsedIntent(val sql: String, val explanation: String)

    private fun getStartOfDay(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getStartOfYesterday(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, -1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getStartOfMonth(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getStartOfLastMonth(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.add(Calendar.MONTH, -1)
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getStartOfYear(cal: Calendar, year: Int? = null): Long {
        val c = cal.clone() as Calendar
        if (year != null) c.set(Calendar.YEAR, year)
        c.set(Calendar.DAY_OF_YEAR, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getEndOfYear(cal: Calendar, year: Int): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.YEAR, year)
        c.set(Calendar.MONTH, Calendar.DECEMBER)
        c.set(Calendar.DAY_OF_MONTH, 31)
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }
}
