package com.context.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.context.data.ExpenseDatabase
import com.context.utils.PermissionUtils
import java.util.Calendar
import java.util.Locale

class WeeklySummaryWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Check if user has enabled weekly summaries
        if (!PermissionUtils.isWeeklySummaryEnabled(applicationContext)) {
            return Result.success()
        }

        val db = ExpenseDatabase.getDatabase(applicationContext)
        val dao = db.expenseDao()

        // Calculate time 7 days ago
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis

        val expenses = dao.getExpensesSince(startTime)
        if (expenses.isEmpty()) return Result.success()

        val totalSpent = expenses.sumOf { it.amount }
        
        // Group by category to find the top one
        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        val topCategoryEntry = categoryTotals.maxByOrNull { it.value }
        
        val message = if (topCategoryEntry != null) {
            val percentage = (topCategoryEntry.value / totalSpent * 100).toInt()
            val emoji = getEmojiForCategory(topCategoryEntry.key)
            "You spent ₹${String.format(Locale.getDefault(), "%.0f", totalSpent)} this week. ${topCategoryEntry.key} ate $percentage% of it $emoji"
        } else {
            "You spent ₹${String.format(Locale.getDefault(), "%.0f", totalSpent)} this week. Keep tracking with Cleave!"
        }

        showNotification(message)
        return Result.success()
    }

    private fun showNotification(message: String) {
        val channelId = "weekly_summary_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weekly Summaries",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using a system icon for now
            .setContentTitle("Weekly Spend Summary")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        notificationManager.notify(99, notification)
    }

    private fun getEmojiForCategory(category: String): String {
        return when (category.lowercase()) {
            "food", "dining" -> "🍔"
            "groceries" -> "🛒"
            "transport", "travel" -> "🚗"
            "shopping" -> "🛍️"
            "entertainment" -> "🎬"
            "bills", "utilities" -> "⚡"
            "health" -> "💊"
            else -> "📈"
        }
    }
}
