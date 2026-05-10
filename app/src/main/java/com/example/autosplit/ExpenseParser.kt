package com.example.autosplit

import android.util.Log

// A simple data class to hold the parsed result.
data class ParsedExpense(
    val amount: Double,
    val merchant: String
)

object ExpenseParser {

    // REGEX: Refined patterns for finding amount and merchant.
    private val amountRegex = Regex("""(?i)(?:Rs|INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)|([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:Rs|INR|₹)""")
    
    // Pattern for UPI merchants often found in Indian bank SMS (e.g., UPI/P2M/Ref/Merchant)
    private val upiMerchantRegex = Regex("(?i)UPI/[^/]+/[^/]+/([^/\\s]+)")
    
    // Refined merchant regex with negative lookbehind to avoid "ID to" or "SMS to"
    // Also added "spent at", "paid to", etc.
    private val merchantRegex = Regex("""(?i)(?<!id\s)(?<!sms\s)to\s+([A-Za-z0-9\s.&'-]+)(?:\s+on|\s+with|\s+at|\s*$)""")
    private val merchantRegex2 = Regex("""(?i)at\s+([A-Za-z0-9\s.&'-]+)(?:\s+on|\s+with|\s*$)""")

    fun parse(text: String): ParsedExpense? {
        // 1. VALIDATE: Only proceed if it passes the strict check
        if (!isValidTransaction(text)) {
            return null
        }

        // 2. PARSE: If validated, extract the details.
        val amount = findAmount(text) ?: return null
        val merchant = findMerchant(text) ?: "Unknown Merchant"

        return ParsedExpense(amount = amount, merchant = merchant)
    }

    private fun isValidTransaction(message: String): Boolean {
        val lowerMsg = message.lowercase()

        // 🚨 1. BLOCKLIST: Immediately reject known spam patterns
        val spamKeywords = listOf(
            "recharge now",
            "click link",
            "click here",
            "register now",
            "subscribe",
            "win",
            "lottery",
            "off",
            "discount",
            "http",
            "https"
        )

        if (spamKeywords.any { lowerMsg.contains(it) }) {
            return false // It's spam/marketing
        }

        // ✅ 2. REQUIREMENT: Must contain a "Transaction Verb"
        val validVerbs = listOf(
            "debited",
            "credited",
            "spent",
            "paid",
            "sent",
            "withdrawal",
            "purchase",
            "txn",
            "transaction"
        )

        return validVerbs.any { lowerMsg.contains(it) }
    }

    private fun findAmount(text: String): Double? {
        return amountRegex.find(text)?.let {
            val value = it.groupValues.drop(1).firstOrNull { g -> g.isNotEmpty() }
            value?.replace(",", "")?.toDoubleOrNull()
        }
    }

    private fun findMerchant(text: String): String? {
        // 1. Check for UPI pattern first as it is more specific (e.g., UPI/P2M/Ref/Blinkit)
        upiMerchantRegex.find(text)?.let {
            val merchant = it.groupValues[1].trim()
            // Ensure it's not just a reference number (usually very long digits)
            if (merchant.isNotEmpty() && !(merchant.length >= 10 && merchant.all { c -> c.isDigit() })) {
                return merchant.capitalizeWords()
            }
        }

        // 2. Standard "to" merchant
        merchantRegex.find(text)?.let {
            val merchant = it.groupValues[1].trim()
            // Basic check to ensure it's not a phone number or support ID
            if (!(merchant.length >= 10 && merchant.all { c -> c.isDigit() })) {
                return merchant.capitalizeWords()
            }
        }

        // 3. Standard "at" merchant
        merchantRegex2.find(text)?.let {
            val merchant = it.groupValues[1].trim()
            if (!(merchant.length >= 10 && merchant.all { c -> c.isDigit() })) {
                return merchant.capitalizeWords()
            }
        }

        return null
    }

    // Helper to make merchant names look cleaner, e.g., "dominos pizza" -> "Dominos Pizza"
    private fun String.capitalizeWords(): String = split(' ').joinToString(" ") { 
        if (it.length > 1) it.lowercase().capitalize() else it.lowercase()
    }
}
