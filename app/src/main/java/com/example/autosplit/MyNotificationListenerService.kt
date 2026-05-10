package com.example.autosplit

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import com.context.utils.CategoryEngine
import com.example.autosplit.ExpenseParser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        
        val parsedExpense = ExpenseParser.parse(text) // Assuming ExpenseParser returns a custom object

        if (parsedExpense != null) {
            val db = ExpenseDatabase.getDatabase(applicationContext)
            val expenseDao = db.expenseDao()

            GlobalScope.launch {
                // 3 Minutes = 3 * 60 * 1000
                val threeMinutesAgo = System.currentTimeMillis() - (180000)

                // Check for duplicate: Same Amount + Within last 3 mins
                val duplicateCount = expenseDao.checkDuplicate(parsedExpense.amount, threeMinutesAgo)

                if (duplicateCount > 0) {
                    // STOP! It's a duplicate. Ignore it.
                    return@launch
                } else {
                    // PROCEED. It's new.
                    val category = CategoryEngine.predictCategory(parsedExpense.merchant)
                    val newExpense = Expense(
                        merchant = parsedExpense.merchant,
                        amount = parsedExpense.amount,
                        timestamp = System.currentTimeMillis(),
                        category = category.label,
                        isAuto = true
                    )
                    expenseDao.insert(newExpense)
                }
            }
        }
    }
}