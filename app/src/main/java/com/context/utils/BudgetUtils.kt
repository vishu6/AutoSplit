package com.context.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.context.app.R
import com.context.data.ExpenseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object BudgetUtils {
    private const val PREF_NAME = "budget_prefs"
    private const val KEY_MONTHLY_BUDGET = "monthly_budget"
    private const val KEY_CATEGORY_LIMITS = "category_limits"
    private const val KEY_BUDGET_NOTIFICATION_SHOWN = "budget_notification_shown_"
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
        val type = object : TypeToken<Map<String, Double>>() {}.type
        return try {
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun checkAndNotifyBudget(context: Context) {
        val budget = getMonthlyBudget(context)
        if (budget <= 0) return

        val monthYear = SimpleDateFormat("MM_yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        if (getPrefs(context).getBoolean(KEY_BUDGET_NOTIFICATION_SHOWN + monthYear, false)) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = ExpenseDatabase.getDatabase(context)
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            
            val totalSpent = db.expenseDao().getExpensesSince(startTime).sumOf { it.amount }
            
            if (totalSpent >= budget * 0.8) {
                sendBudgetNotification(context, totalSpent, budget, monthYear)
            }
        }
    }

    private fun sendBudgetNotification(context: Context, spent: Double, budget: Double, monthYear: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Budget Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().time)
        val remaining = budget - spent
        val message = String.format(Locale.getDefault(), "You've used %d%% of your %s budget. ₹%.0f remaining.", (spent/budget*100).toInt(), monthName, remaining)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Budget Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(999, notification)
        getPrefs(context).edit { putBoolean(KEY_BUDGET_NOTIFICATION_SHOWN + monthYear, true) }
    }
}
