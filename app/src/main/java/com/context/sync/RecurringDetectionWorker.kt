package com.context.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.context.data.ExpenseDatabase
import com.context.utils.NotificationUtils
import com.context.utils.RecurringExpenseDetector
import kotlinx.coroutines.flow.first
import java.util.Calendar

class RecurringDetectionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = ExpenseDatabase.getDatabase(applicationContext)
        val expenseDao = database.expenseDao()
        val recurringDao = database.recurringExpenseDao()

        try {
            // 1. RUN DETECTION LOGIC
            val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)
            val expenses = expenseDao.getExpensesSince(sixMonthsAgo)

            if (expenses.isNotEmpty()) {
                val detectedPatterns = RecurringExpenseDetector.detect(expenses)
                val existingRecurring = recurringDao.getAllRecurringExpensesSync()

                for (pattern in detectedPatterns) {
                    val existing = existingRecurring.find { 
                        RecurringExpenseDetector.isSameMerchant(it.merchant, pattern.merchant) 
                    }

                    if (existing != null) {
                        if (!existing.isSuppressed) {
                            if (existing.isAutoDetected) {
                                recurringDao.update(existing.copy(
                                    averageAmount = (existing.averageAmount + pattern.averageAmount) / 2,
                                    lastPaidDate = pattern.lastPaidDate,
                                    nextExpectedDate = pattern.nextExpectedDate,
                                    confidenceScore = pattern.confidenceScore
                                ))
                            } else {
                                recurringDao.update(existing.copy(
                                    lastPaidDate = pattern.lastPaidDate,
                                    nextExpectedDate = pattern.nextExpectedDate
                                ))
                            }
                        }
                    } else {
                        recurringDao.insert(pattern)
                    }
                }
            }

            // 2. CHECK FOR UPCOMING BILLS (NOTIFICATION TRIGGER)
            val now = System.currentTimeMillis()
            val tomorrow = now + (24 * 60 * 60 * 1000)
            
            val activeRecurring = recurringDao.getAllRecurringExpensesSync().filter { it.isActive && !it.isSuppressed }
            
            for (recurring in activeRecurring) {
                // If bill is due in the next 24 hours AND we haven't notified today
                val isDueTomorrow = recurring.nextExpectedDate in now..tomorrow
                
                // We only notify if it hasn't been paid this month yet
                // (Using a simple check: if nextExpectedDate is still in the future, it's not paid yet)
                if (isDueTomorrow) {
                    NotificationUtils.showBillReminder(
                        context = applicationContext,
                        merchant = recurring.merchant,
                        amount = recurring.averageAmount,
                        id = recurring.id
                    )
                }
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
