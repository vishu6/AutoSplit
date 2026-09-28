package com.example.autosplit

import android.util.Log

// A simple data class to hold the parsed result.
data class ParsedExpense(
    val amount: Double,
    val merchant: String
)

object ExpenseParser {

    private const val TAG = "ExpenseParser"

    fun parse(text: String): ParsedExpense? {
        // 1. VALIDATE: Ensure it's a real expense, not a statement or balance check
        if (!isValidTransaction(text)) {
            return null
        }

        // 2. PARSE: Extract amount and merchant
        val amount = findAmount(text) ?: return null
        val merchant = findMerchant(text)

        return ParsedExpense(amount = amount, merchant = merchant)
    }

    private fun isValidTransaction(message: String): Boolean {
        // 🚨 BLOCKLIST 0: Future/Pending Payments & Reminders (Reject scheduled/upcoming/reminders)
        val futurePayment = Regex(
            """\b(will be debited|scheduled|due on|upcoming|reminder|autopay due|mandate|ensure balance)\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(message)

        if (futurePayment) return false

        // 🚨 BLOCKLIST 1: Marketing/Spam
        val spamKeywords = listOf("recharge now", "click link", "register now", "subscribe", "win", "lottery", "discount", "http")
        val lowerMsg = message.lowercase()
        if (spamKeywords.any { lowerMsg.contains(it) }) return false

        // 🚨 BLOCKLIST 2: Credit Card Statements
        val isStatement = lowerMsg.contains("statement") && 
            (lowerMsg.contains("credit card") || lowerMsg.contains("due on") || 
             lowerMsg.contains("min amt due") || lowerMsg.contains("minimum amount due") || 
             lowerMsg.contains("pay by"))
        if (isStatement) return false
        
        // 🚨 BLOCKLIST 3: Balance Enquiry
        val infoKeywords = listOf("available balance", "avl bal", "a/c balance", "account balance is", "your balance")
        if (infoKeywords.any { lowerMsg.contains(it) } && 
            !lowerMsg.contains("debited") && !lowerMsg.contains("spent") && !lowerMsg.contains("paid")) return false

        // 🚨 INCOME / REFUNDS SHIELD: Reject credited, refund, reversed, cashback, received so income/refunds do not become expenses
        val incomeVerbs = listOf("credited", "refund", "reversed", "cashback", "received", "cr")
        if (incomeVerbs.any { verb -> Regex("\\b$verb\\b", RegexOption.IGNORE_CASE).containsMatchIn(message) }) {
            return false
        }

        // ✅ REQUIREMENT 1: Must contain an explicit outgoing/debit action verb or signal with word boundaries
        val debitVerbs = listOf("debited", "spent", "paid", "sent", "withdrawal", "purchase", "dr")
        val hasExpenseVerb = debitVerbs.any { verb -> Regex("\\b$verb\\b", RegexOption.IGNORE_CASE).containsMatchIn(message) }
        if (!hasExpenseVerb) return false

        return true
    }

    private fun findAmount(text: String): Double? {
        // Step 1: Priority-ordered currency-linked patterns
        val patterns = listOf(
            Regex("(?i)(?:₹|inr|rs\\.?)\\s*([0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)"),
            Regex("(?i)([0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)\\s*(?:inr|rs\\.?|₹)")
        )

        for (pattern in patterns) {
            for (match in pattern.findAll(text)) {
                val amountStrWithCommas = match.groupValues[1]
                val amountStr = amountStrWithCommas.replace(",", "")
                val amount = amountStr.toDoubleOrNull() ?: continue

                if (isValidAmount(amount, amountStr)) {
                    Log.d(TAG, "✅ Amount parsed: $amount from pattern: ${pattern.pattern}")
                    return amount
                }
            }
        }

        return null
    }

    private fun isValidAmount(amount: Double, rawString: String): Boolean {
        // Reject amounts under ₹1 or over ₹10 Lakhs
        if (amount < 1.0 || amount > 1000000.0) return false
        
        // Reject Years (2020-2030 range)
        if (amount in 2020.0..2030.0) return false
        
        val digitsOnly = rawString.replace(".", "").trim()
        
        // Reject if 10 digits (Phone number)
        if (digitsOnly.length == 10) return false
        
        // Reject DDMMYYYY dates (8 digits) but ONLY if there's no decimal point
        if (digitsOnly.length == 8 && !rawString.contains(".")) return false

        return true
    }

    private fun findMerchant(text: String): String {
        // UPI pattern: UPI/P2M/Ref/MerchantName
        Regex("(?i)upi/[^/]+/[^/]+/([^/\\s]+)").find(text)?.let {
            val merchant = it.groupValues[1].trim()
            if (merchant.isNotEmpty() && !isNumericId(merchant)) return merchant.capitalizeWords()
        }

        // Standard "to/at [Merchant]"
        val merchantPatterns = listOf(
            Regex("(?i)(?<!id\\s)(?<!sms\\s)to\\s+([A-Za-z0-9\\s.&'-]+)(?:\\s+on|\\s+with|\\s+at|\\s*$)"),
            Regex("(?i)at\\s+([A-Za-z0-9\\s.&'-]+)(?:\\s+on|\\s+with|\\s*$)")
        )

        for (pattern in merchantPatterns) {
            pattern.find(text)?.let {
                val merchant = it.groupValues[1].trim()
                if (!isNumericId(merchant)) return merchant.capitalizeWords()
            }
        }

        return "Unknown Merchant"
    }

    private fun isNumericId(text: String): Boolean {
        val cleaned = text.filter { it.isDigit() }
        return cleaned.length >= 8 || text.all { !it.isLetter() }
    }

    private fun String.capitalizeWords(): String = split(' ').filter { it.isNotEmpty() }.joinToString(" ") { 
        if (it.length > 1) it.lowercase().replaceFirstChar { char -> char.uppercase() } else it.lowercase()
    }
}
