package com.context.app

import android.app.Application
import androidx.work.*
import com.context.service.DailySummaryWorker
import com.context.service.WeeklySummaryWorker
import com.context.sync.RecurringDetectionWorker
import com.context.utils.ReviewManager
import dagger.hilt.android.HiltAndroidApp
import java.util.*
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class ContextApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ReviewManager.init(this)
        scheduleWeeklySummary()
        scheduleDailySummary()
        scheduleRecurringDetection()
    }

    private fun scheduleDailySummary() {
        val workManager = WorkManager.getInstance(this)

        // Calculate delay until 10 PM today
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val currentDate = Calendar.getInstance()

        // If it's already past 10 PM, schedule for tomorrow
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    // Removed setRequiresBatteryNotLow to ensure timely delivery 
                    // similar to other fintech apps like Axio.
                    .build()
            )
            .build()

        // Use KEEP policy so opening the app doesn't reset the 24h timer daily
        workManager.enqueueUniquePeriodicWork(
            "daily_spend_summary",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    private fun scheduleWeeklySummary() {
        val workManager = WorkManager.getInstance(this)

        // Calculate delay until next Sunday at 9 AM
        val dueDate = Calendar.getInstance()
        val currentDate = Calendar.getInstance()

        dueDate.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        dueDate.set(Calendar.HOUR_OF_DAY, 9)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)

        // If today is Sunday and it's already past 9 AM, schedule for next Sunday
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_YEAR, 7)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val weeklyWorkRequest = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        // Use UPDATE policy to ensure any logic changes are applied to existing schedules
        workManager.enqueueUniquePeriodicWork(
            "weekly_spend_summary",
            ExistingPeriodicWorkPolicy.UPDATE,
            weeklyWorkRequest
        )
    }

    private fun scheduleRecurringDetection() {
        val workManager = WorkManager.getInstance(this)

        // Run detection every night at 2 AM
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val currentDate = Calendar.getInstance()

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val recurringWorkRequest = PeriodicWorkRequestBuilder<RecurringDetectionWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresDeviceIdle(true)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "recurring_detection",
            ExistingPeriodicWorkPolicy.KEEP,
            recurringWorkRequest
        )
    }
}
