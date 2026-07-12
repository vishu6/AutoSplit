package com.context.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.context.app.MainActivity
import com.context.app.R
import com.context.data.ExpenseDatabase
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class BudgetPace {
    ON_TRACK,
    WATCH_OUT,
    OVERSPENDING
}

object BudgetUtils {
    private const val PREF_NAME = "budget_prefs"
    private const val KEY_MONTHLY_BUDGET = "monthly_budget"
    private const val KEY_CATEGORY_LIMITS = "category_limits"
    private const val KEY_BUDGET_NOTIFICATION_SHOWN = "budget_notification_shown_"
    private const val KEY_CAT_NOTIFICATION_SHOWN = "cat_notification_shown_"
    private const val CHANNEL_ID = "budget_alerts"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setMonthlyBudget(context: Context, amount: Double) {
        getPrefs(context).edit { putFloat(KEY_MONTHLY_BUDGET, amount.toFloat()) }
    }

    fun getMonthlyBudget(context: Context): Double {
        return getPrefs(context).getFloat(KEY_MONTHLY_BUDGET, 0f).toDouble()
    }

    fun isBudgetSet(context: Context): Boolean {
        return getMonthlyBudget(context) > 0
    }

    fun setCategoryLimits(context: Context, limits: Map<String, Double>) {
        val json = Gson().toJson(limits)
        getPrefs(context).edit { putString(KEY_CATEGORY_LIMITS, json) }
    }

    fun getCategoryLimits(context: Context): Map<String, Double> {
        val json = getPrefs(context).getString(KEY_CATEGORY_LIMITS, null) ?: return emptyMap()
        return try {
            val map = mutableMapOf<String, Double>()
            val jsonObject = Gson().fromJson(json, JsonObject::class.java)
            jsonObject.entrySet().forEach { entry ->
                map[entry.key] = entry.value.asDouble
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Phase 2: Smart Budget Setup Algorithm
     * Analyzes last 3 months, adds 10% buffer, rounds to nearest hundred.
     */
    suspend fun suggestBudgetLimits(context: Context): Map<String, Double> = withContext(Dispatchers.IO) {
        val db = ExpenseDatabase.getDatabase(context)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -90)
        val ninetyDaysAgo = calendar.timeInMillis
        
        val expenses = db.expenseDao().getExpensesSince(ninetyDaysAgo)
        if (expenses.isEmpty()) return@withContext emptyMap()

        expenses.groupBy { it.category }
            .mapValues { (_, catExpenses) ->
                val monthlyAverage = catExpenses.sumOf { it.amount } / 3.0
                val withBuffer = monthlyAverage * 1.1
                // Round up to nearest 100
                (Math.ceil(withBuffer / 100.0) * 100.0)
            }
    }

    /**
     * Calculates the projected date when the budget will be exhausted based on current pace.
     */
    fun getProjectedExhaustionDate(totalBudget: Double, spentSoFar: Double): Date? {
        if (spentSoFar <= 0 || totalBudget <= 0) return null
        
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        
        val dailyRate = spentSoFar / dayOfMonth
        val remainingBudget = totalBudget - spentSoFar
        
        if (remainingBudget <= 0) return calendar.time // Already exhausted
        
        val daysRemaining = (remainingBudget / dailyRate).toLong()
        calendar.add(Calendar.DAY_OF_YEAR, daysRemaining.toInt())
        
        return calendar.time
    }

    /**
     * Determines the emotional status of the budget based on the projected exhaustion.
     */
    fun getBudgetPace(totalBudget: Double, spentSoFar: Double): BudgetPace {
        if (totalBudget <= 0) return BudgetPace.ON_TRACK
        if (spentSoFar >= totalBudget) return BudgetPace.OVERSPENDING
        
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val dailyRate = spentSoFar / dayOfMonth
        val projectedTotal = dailyRate * daysInMonth
        
        return when {
            projectedTotal <= totalBudget -> BudgetPace.ON_TRACK
            // If projected to exceed in the last 7 days of the month
            (projectedTotal > totalBudget) && (totalBudget / dailyRate) > (daysInMonth - 7) -> BudgetPace.WATCH_OUT
            else -> BudgetPace.OVERSPENDING
        }
    }

    suspend fun getCategoryTotalsForPeriod(context: Context, isCurrentMonth: Boolean): Map<String, Double> = withContext(Dispatchers.IO) {
        val db = ExpenseDatabase.getDatabase(context)
        val cal = Calendar.getInstance()
        
        if (!isCurrentMonth) {
            cal.add(Calendar.MONTH, -1)
        }
        
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis
        
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis
        
        val map = mutableMapOf<String, Double>()
        CategoryUtils.categories.forEach { category ->
            val total = db.expenseDao().getCategoryTotalForPeriod(category, start, end) ?: 0.0
            map[category] = total
        }
        map
    }

    fun checkAndNotifyBudget(context: Context) {
        val budget = getMonthlyBudget(context)
        val categoryLimits = getCategoryLimits(context)
        if (budget <= 0 && categoryLimits.isEmpty()) return

        val monthYear = SimpleDateFormat("MM_yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

        CoroutineScope(Dispatchers.IO).launch {
            val db = ExpenseDatabase.getDatabase(context)
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startTime = calendar.timeInMillis
            
            val expenses = db.expenseDao().getExpensesSince(startTime)
            val totalSpent = expenses.sumOf { it.amount }
            
            // 1. Total Budget Check
            if (budget > 0 && totalSpent >= budget * 0.8 && !getPrefs(context).getBoolean(KEY_BUDGET_NOTIFICATION_SHOWN + monthYear, false)) {
                sendBudgetNotification(context, "Budget Alert", "You've used ${(totalSpent/budget*100).toInt()}% of your monthly budget.", 999)
                getPrefs(context).edit { putBoolean(KEY_BUDGET_NOTIFICATION_SHOWN + monthYear, true) }
            }

            // 2. Category Specific Checks
            categoryLimits.forEach { (category, limit) ->
                if (limit > 0) {
                    val catSpent = expenses.filter { it.category == category }.sumOf { it.amount }
                    val key = KEY_CAT_NOTIFICATION_SHOWN + category + "_" + monthYear
                    
                    if (catSpent >= limit && !getPrefs(context).getBoolean(key, false)) {
                        sendBudgetNotification(context, "$category Limit Hit", "You've exhausted your ₹${limit.toInt()} limit for $category.", category.hashCode())
                        getPrefs(context).edit { putBoolean(key, true) }
                    } else if (catSpent >= limit * 0.8 && !getPrefs(context).getBoolean(key + "_80", false)) {
                        sendBudgetNotification(context, "$category Alert", "You're at 80% of your $category budget.", category.hashCode() + 1)
                        getPrefs(context).edit { putBoolean(key + "_80", true) }
                    }
                }
            }
        }
    }

    private fun sendBudgetNotification(context: Context, title: String, message: String, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Budget Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Add redirection to MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // Fixed: Now redirects to Cleave
            .build()

        notificationManager.notify(id, notification)
    }

    fun getLastMonthCategoryTotals(context: Context): Map<String, Double> {
        return emptyMap() // Deprecated by getCategoryTotalsForPeriod
    }
}
