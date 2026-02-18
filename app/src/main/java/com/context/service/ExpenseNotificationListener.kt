package com.context.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import com.context.utils.CategoryEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "ExpenseListener"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

        val allowedApps = listOf(
            "com.phonepe.app",                       // PhonePe
            "com.google.android.apps.nbu.paisa.user",// GPay
            "net.one97.paytm",                       // Paytm
            "com.google.android.apps.messaging",     // Google Messages
            "com.samsung.android.messaging",     // Samsung Messages
            "com.android.mms",                       // Xiaomi/OnePlus Messages
            "com.truecaller"                         // Truecaller
        )

        if (packageName in allowedApps) {
            Log.d(TAG, "Notification from allowed app ($packageName): $title - $text")
            parseTransactionMessage(text, title)
        }
    }

    private fun parseTransactionMessage(message: String, title: String) {
        val cleanMsg = message.lowercase().replace(",", "")

        if (cleanMsg.contains("otp") || cleanMsg.contains("login") || cleanMsg.contains("credited")) {
            Log.d(TAG, "Ignoring OTP, login, or credit notification.")
            return
        }

        val debitPattern = Regex("(?i)(rs\\.?|inr)\\s*(\\d+(\\.\\d{1,2})?)")
        val isExpense = cleanMsg.contains("debited") ||
                        cleanMsg.contains("spent") ||
                        cleanMsg.contains("paid") ||
                        cleanMsg.contains("sent")

        if (isExpense) {
            val match = debitPattern.find(cleanMsg)
            if (match != null) {
                val amountString = match.groupValues[2]
                val amount = amountString.toDoubleOrNull() ?: 0.0
                val merchant = extractMerchantName(message, title)

                Log.i(TAG, "✅ Parsed Expense: Amount='$amount', Merchant='$merchant'")
                saveExpense(merchant, amount)
            } else {
                Log.w(TAG, "Expense keyword found, but couldn\'t parse amount.")
            }
        } else {
            Log.d(TAG, "No expense-related keywords found.")
        }
    }

    private fun extractMerchantName(message: String, title: String): String {
        val pattern = Regex("(?i)(to|at)\\s+([a-zA-Z0-9 ]+)")
        val match = pattern.find("$title $message")

        return if (match != null) {
            match.groupValues[2].take(25).trim()
        } else {
            // If no merchant found with "to/at", use the notification title as a fallback
            if (title.isNotEmpty() && !title.contains("OTP", ignoreCase = true)) title else "Unknown Expense"
        }
    }

    private fun saveExpense(merchant: String, amount: Double) {
        serviceScope.launch {
            val predictedCategory = CategoryEngine.predictCategory(merchant)
            val expense = Expense(
                merchant = merchant,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                category = predictedCategory.label,
                groupId = null,
                isAuto = true
            )

            try {
                val db = ExpenseDatabase.getDatabase(applicationContext)
                db.expenseDao().insert(expense)
                Log.i(TAG, "✅✅✅ Expense successfully saved to database: $expense")
                
                val intent = Intent("com.context.app.NEW_EXPENSE")
                sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving expense to database", e)
            }
        }
    }
}
