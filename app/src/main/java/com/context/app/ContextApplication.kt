package com.context.app

import android.app.Application
import androidx.work.*
import com.context.service.WeeklySummaryWorker
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
    }

    private fun scheduleWeeklySummary() {
        val workManager = WorkManager.getInstance(this)

        // Calculate delay until next Sunday at 8 PM
        val dueDate = Calendar.getInstance()
        val currentDate = Calendar.getInstance()

        dueDate.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        dueDate.set(Calendar.HOUR_OF_DAY, 20)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)

        // If today is Sunday and it's already past 8 PM, schedule for next Sunday
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
}
