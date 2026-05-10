package com.context.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import com.context.utils.BudgetUtils
import com.context.utils.CategoryEngine
import com.context.utils.HapticUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

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
            "com.phonepe.app",
            "com.google.android.apps.nbu.paisa.user",
            "net.one97.paytm",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.truecaller"
        )

        if (packageName in allowedApps) {
            parseTransactionMessage(text, title)
        }
    }

    private fun parseTransactionMessage(message: String, title: String) {
        val cleanMsg = message.lowercase().replace(",", "")

        if (cleanMsg.contains("otp") || cleanMsg.contains("login") || cleanMsg.contains("credited")) {
            return
        }

        val debitPattern = Regex("(?i)(rs\\.?|inr|₹)\\s*(\\d+(\\.\\d{1,2})?)")
        val isExpense = cleanMsg.contains("debited") ||
                        cleanMsg.contains("spent") ||
                        cleanMsg.contains("paid") ||
                        cleanMsg.contains("sent") ||
                        cleanMsg.contains("transaction")

        if (isExpense) {
            val match = debitPattern.find(cleanMsg)
            if (match != null) {
                val amountString = match.groupValues[2]
                val amount = amountString.toDoubleOrNull() ?: 0.0
                val merchant = extractMerchantName(message, title)

                Log.i(TAG, "✅ Parsed: Amount='$amount', Merchant='$merchant'")
                saveExpense(merchant, amount)
            }
        }
    }

    private fun extractMerchantName(message: String, title: String): String {
        val normalized = message.replace(Regex("\\s+"), " ").trim()
        val lower = normalized.lowercase()

        // 1. Agressive Footer Stripping
        val footerMarkers = listOf(
            "not you?", "sms blockupi", "cust id", "id to", "sms to", 
            "if not you", "report to", "call 1800", "axis bank", "axisbank"
        )
        
        var body = normalized
        var earliestFooter = body.length
        for (marker in footerMarkers) {
            val idx = lower.indexOf(marker)
            if (idx != -1 && idx < earliestFooter) {
                earliestFooter = idx
            }
        }
        body = normalized.substring(0, earliestFooter).trim()

        // 2. Enhanced UPI Extraction (Splitting path segments)
        if (lower.contains("upi/")) {
            val upiPart = body.substring(lower.indexOf("upi/"))
            val segments = upiPart.split('/')
            // Traverse segments backwards to find the merchant name
            for (i in segments.size - 1 downTo 1) {
                val candidate = segments[i].split(' ', '?', ',', '-').first().trim()
                if (candidate.isNotEmpty() && !isNumericId(candidate) && 
                    !listOf("p2m", "p2p", "upi").contains(candidate.lowercase())) {
                    return cleanMerchantName(candidate)
                }
            }
        }

        // 3. Regex Fallback for "paid to" or "at [Merchant]"
        val merchantPattern = Regex("(?i)(?<!id\\s)(?<!sms\\s)(?<!cust\\s)(to|at)\\s+([a-zA-Z0-9.&'-]+)")
        merchantPattern.find(body)?.let {
            val merchant = it.groupValues[2].trim()
            if (!isNumericId(merchant)) {
                return cleanMerchantName(merchant)
            }
        }

        // 4. Fallback to Title
        val genericPatterns = listOf("axis", "hdfc", "icici", "sbi", "bank", "messages", "alert", "truecaller")
        if (title.isNotEmpty() && !title.contains("OTP", ignoreCase = true) && 
            !genericPatterns.any { title.contains(it, ignoreCase = true) }) {
            return title
        }

        return "Unknown Merchant"
    }

    private fun isNumericId(text: String): Boolean {
        val cleaned = text.filter { it.isDigit() }
        // Reject if it's 8+ digits (Phone/Account/Ref number) or has no letters
        return cleaned.length >= 8 || text.all { !it.isLetter() }
    }

    private fun cleanMerchantName(name: String): String {
        var result = name
        val stopWords = listOf(" on ", " at ", " for ", " with ", " using ", " from ", " ref ", " txn ", " is ")
        for (word in stopWords) {
            val i = result.indexOf(word, ignoreCase = true)
            if (i != -1) result = result.substring(0, i)
        }
        
        return result.take(25).trim().split(' ').filter { it.isNotEmpty() }.joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
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
                
                // Haptic feedback for auto-capture
                HapticUtils.playTick(applicationContext)
                
                // Check budget thresholds
                BudgetUtils.checkAndNotifyBudget(applicationContext)

                val intent = Intent("com.context.app.NEW_EXPENSE")
                sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving expense", e)
            }
        }
    }
}
