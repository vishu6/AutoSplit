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

        // Calculate delay until next Sunday at 8 PM (for example)
        val dueDate = Calendar.getInstance()
        val currentDate = Calendar.getInstance()

        dueDate.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        dueDate.set(Calendar.HOUR_OF_DAY, 20)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24 * 7)
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

        workManager.enqueueUniquePeriodicWork(
            "weekly_spend_summary",
            ExistingPeriodicWorkPolicy.KEEP,
            weeklyWorkRequest
        )
    }
}
