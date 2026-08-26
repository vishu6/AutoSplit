package com.context.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.context.app.R
import com.context.data.ExpenseDatabase
import com.context.utils.BudgetUtils
import com.context.utils.OnboardingUtils
import com.context.utils.PermissionUtils
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale

class WeeklySummaryWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!PermissionUtils.isWeeklySummaryEnabled(applicationContext)) {
            return Result.success()
        }

        val db = ExpenseDatabase.getDatabase(applicationContext)
        val dao = db.expenseDao()

        val now = Calendar.getInstance()
        val sevenDaysAgo = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
        val fourteenDaysAgo = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -14) }.timeInMillis

        val thisWeekExpenses = dao.getExpensesSince(sevenDaysAgo)
        val lastWeekExpenses = dao.getExpensesSince(fourteenDaysAgo).filter { it.timestamp < sevenDaysAgo }

        val rawName = OnboardingUtils.getUserName(applicationContext).trim()
        val firstName = rawName.split(" ").firstOrNull()?.let { ", $it" } ?: ""
        
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            currency = Currency.getInstance("INR")
        }

        if (thisWeekExpenses.isEmpty()) {
            showNotification(
                title = "Weekly Digest",
                message = "Your week in money 📊\n\nNothing spent this week 🌟\nPerfect week for your wallet!"
            )
            return Result.success()
        }

        val thisWeekTotal = thisWeekExpenses.sumOf { it.amount }
        val lastWeekTotal = lastWeekExpenses.sumOf { it.amount }

        val isBudgetSet = BudgetUtils.isBudgetSet(applicationContext)
        val budget = BudgetUtils.getMonthlyBudget(applicationContext)
        
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
        val monthTotal = dao.getExpensesSince(monthStart).sumOf { it.amount }
        val budgetRemaining = budget - monthTotal

        val headline = when {
            lastWeekExpenses.isNotEmpty() && thisWeekTotal < lastWeekTotal -> "Great week$firstName! 💪"
            isBudgetSet && monthTotal <= budget -> "Keeping tabs$firstName 👀"
            isBudgetSet && monthTotal > budget -> "Heads up$firstName ⚠️"
            else -> "On point this week$firstName 🎯"
        }

        val content = StringBuilder("Your week in money 📊\n\n")
        
        content.append("Spent this week: ${currencyFormatter.format(thisWeekTotal)}")
        
        if (lastWeekExpenses.isNotEmpty()) {
            val diff = thisWeekTotal - lastWeekTotal
            val percent = if (lastWeekTotal > 0) (Math.abs(diff) / lastWeekTotal * 100).toInt() else 0
            val direction = if (diff <= 0) "↓" else "↑"
            content.append("\nvs last week: ${currencyFormatter.format(lastWeekTotal)} ($direction$percent%)")
        } else {
            content.append("\nFirst week tracked! 🎉")
        }

        content.append("\n\n")

        val topCategoryEntry = thisWeekExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
            .maxByOrNull { it.value }
        
        if (topCategoryEntry != null) {
            content.append("Top category: ${topCategoryEntry.key} ${currencyFormatter.format(topCategoryEntry.value)}\n")
        }

        val biggestSpend = thisWeekExpenses.maxByOrNull { it.amount }
        if (biggestSpend != null) {
            content.append("Biggest single spend: ${biggestSpend.merchant} ${currencyFormatter.format(biggestSpend.amount)}\n")
        }

        content.append("\n")

        if (isBudgetSet) {
            val monthName = Calendar.getInstance().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            val statusEmoji = if (monthTotal <= budget) "🟢" else "🔴"
            content.append("You're on track for $monthName $statusEmoji\n")
            content.append("Budget remaining: ${currencyFormatter.format(Math.max(0.0, budgetRemaining))}")
        } else {
            content.append("Set a budget in Settings\nto track your monthly pace →")
        }

        showNotification(
            title = headline,
            message = content.toString()
        )

        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "weekly_summary_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weekly Summaries",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW, "cleave://transactions?range=week".toUri()).apply {
            setPackage(applicationContext.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification_summary)
            .setContentTitle(title)
            .setContentText("Your week in money 📊")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        notificationManager.notify(99, notification)
    }
}
