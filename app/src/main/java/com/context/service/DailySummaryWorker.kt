package com.context.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.context.app.MainActivity
import com.context.app.R
import com.context.data.ExpenseDatabase
import com.context.utils.BudgetPace
import com.context.utils.BudgetUtils
import com.context.utils.ExpenseCategory
import java.text.NumberFormat
import java.util.*

class DailySummaryWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val db = ExpenseDatabase.getDatabase(applicationContext)
        val dao = db.expenseDao()

        // 1. Calculate Today's Total and Top Category
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val dayStart = calendar.timeInMillis
        
        val todayExpenses = dao.getExpensesSince(dayStart)
        val todayTotal = todayExpenses.sumOf { it.amount }

        val topCategory = todayExpenses
            .filter { it.category != "Settlement" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .maxByOrNull { it.value }?.key

        // 2. Calculate Monthly Total
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = calendar.timeInMillis
        val monthExpenses = dao.getExpensesSince(monthStart)
        val monthTotal = monthExpenses.sumOf { it.amount }

        // 3. Get Budget Context
        val budget = BudgetUtils.getMonthlyBudget(applicationContext)
        val pace = BudgetUtils.getBudgetPace(budget, monthTotal)

        val iconRes = getIconForCategory(topCategory)
        showPremiumNotification(todayTotal, monthTotal, budget, pace, iconRes)

        return Result.success()
    }

    private fun getIconForCategory(category: String?): Int {
        return when (category) {
            ExpenseCategory.FOOD.label -> R.drawable.ic_noti_food
            ExpenseCategory.TRANSPORT.label -> R.drawable.ic_noti_transport
            ExpenseCategory.SHOPPING.label -> R.drawable.ic_noti_shopping
            ExpenseCategory.BILLS.label, ExpenseCategory.GROCERY.label -> R.drawable.ic_noti_bills
            else -> R.drawable.ic_notification_summary // Default chart icon
        }
    }

    private fun showPremiumNotification(today: Double, month: Double, budget: Double, pace: BudgetPace, iconRes: Int) {
        val channelId = "daily_summary_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                applicationContext.getString(R.string.notification_daily_summary_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Summarizes your spending at the end of the day"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val todayStr = currencyFormatter.format(today)
        val monthStr = currencyFormatter.format(month)
        
        // Use RemoteViews for custom layout
        val packageName = applicationContext.packageName
        val remoteViews = RemoteViews(packageName, R.layout.notification_daily_summary)
        
        remoteViews.setImageViewResource(R.id.notification_illustration, iconRes)
        remoteViews.setTextViewText(R.id.text_today_amount, todayStr)
        remoteViews.setTextViewText(R.id.text_month_summary, applicationContext.getString(R.string.notification_month_summary_format, monthStr))
        
        // Match Cleave's theme colors dynamically
        if (pace == BudgetPace.OVERSPENDING) {
            remoteViews.setTextViewText(R.id.text_today_label, applicationContext.getString(R.string.notification_overspending))
            remoteViews.setTextColor(R.id.text_today_label, applicationContext.getColor(R.color.fintech_red))
        } else {
            remoteViews.setTextViewText(R.id.text_today_label, applicationContext.getString(R.string.notification_today_label))
            remoteViews.setTextColor(R.id.text_today_label, applicationContext.getColor(R.color.notification_text_secondary))
        }

        // Content Intent (Review)
        val reviewIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val reviewPendingIntent = PendingIntent.getActivity(
            applicationContext, 1, reviewIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Intent (Later)
        val dismissIntent = Intent(applicationContext, NotificationDismissReceiver::class.java).apply {
            putExtra("notification_id", NOTIFICATION_ID)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            applicationContext, 2, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Actions
        val reviewAction = NotificationCompat.Action.Builder(
            0, applicationContext.getString(R.string.notification_action_review), reviewPendingIntent
        ).build()
        
        val laterAction = NotificationCompat.Action.Builder(
            0, applicationContext.getString(R.string.notification_action_later), dismissPendingIntent
        ).build()

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(reviewPendingIntent)
            .addAction(reviewAction)
            .addAction(laterAction)
            .setColor(applicationContext.getColor(R.color.fintech_blue))
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
