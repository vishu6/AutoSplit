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
        val lowerMsg = message.lowercase()

        // 🚨 BLOCKLIST 1: Marketing/Spam
        val spamKeywords = listOf("recharge now", "click link", "register now", "subscribe", "win", "lottery", "discount", "http")
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

        // ✅ REQUIREMENT: Must contain an Expense "Action"
        val hasExpenseVerb = listOf("debited", "spent", "paid", "sent", "withdrawal", "purchase").any { lowerMsg.contains(it) }
        val hasTxnWithAmount = (lowerMsg.contains("txn") || lowerMsg.contains("transaction")) && 
                               (lowerMsg.contains("₹") || lowerMsg.contains("inr") || lowerMsg.contains("rs."))
        
        return hasExpenseVerb || hasTxnWithAmount
    }

    private fun findAmount(text: String): Double? {
        // Step 1: Strip commas globally first for 100% accurate greedy matching
        val cleaned = text.replace(",", "")
        val lowerCleaned = cleaned.lowercase()

        // Step 2: Priority Pass — Specifically look for DEBIT context if both keywords exist
        // Added 'dr' and 'cr' support to catch Axis/Bank abbreviations
        val hasDebit = lowerCleaned.contains("debited") || lowerCleaned.contains(" dr") || lowerCleaned.contains(" dr.")
        val hasCredit = lowerCleaned.contains("credited") || lowerCleaned.contains(" cr") || lowerCleaned.contains(" cr.")
        
        if (hasDebit && hasCredit) {
            val debitPattern = Regex("(?i)(?:inr|rs\\.?|₹)\\s*(?:dr\\.?)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:debited|dr)")
            debitPattern.find(cleaned)?.let {
                val amount = it.groupValues[1].toDoubleOrNull()
                if (amount != null && isValidAmount(amount, it.groupValues[1])) return amount
            }
        }

        // Step 3: Priority-ordered patterns (Specific to General)
        val patterns = listOf(
            // 1a: Explicit Debit Marker (Highest Priority)
            Regex("(?i)(?:inr|rs\\.?|₹)\\s*(?:dr\\.?)\\s*(\\d+(?:\\.\\d{1,2})?)"),
            
            // 1b: Neutral/Amount Markers (amt, total, or just currency)
            Regex("(?i)(?:inr|rs\\.?|₹)\\s*(?:amt\\.?|total)?\\s*(\\d+(?:\\.\\d{1,2})?)"),
            
            // 1c: Reverse format (62734.04 INR)
            Regex("(?i)(\\d+(?:\\.\\d{1,2})?)\\s*(?:inr|rs\\.?|₹)"),
            
            // 1d: Action-based (debited 1483.00)
            Regex("(?i)(?:debited|spent|paid|sent)\\s*(?:inr|rs\\.?|₹)?\\s*(\\d+(?:\\.\\d{1,2})?)"),
            
            // 1e: Generic label (amount of 1483)
            Regex("(?i)amount\\s*(?:of)?\\s*(?:inr|rs\\.?|₹)?\\s*(\\d+(?:\\.\\d{1,2})?)")
        )

        for (pattern in patterns) {
            val match = pattern.find(cleaned)
            val amountStr = match?.groupValues?.get(1) ?: continue
            val amount = amountStr.toDoubleOrNull() ?: continue

            // Step 4: Sanity check to avoid years, phone numbers, etc.
            if (isValidAmount(amount, amountStr)) {
                Log.d(TAG, "✅ Amount parsed: $amount from pattern: ${pattern.pattern}")
                return amount
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
