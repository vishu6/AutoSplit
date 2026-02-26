package com.example.autosplit

import android.util.Log

// A simple data class to hold the parsed result.
data class ParsedExpense(
    val amount: Double,
    val merchant: String
)

object ExpenseParser {

    // REGEX: Refined patterns for finding amount and merchant.
    private val amountRegex = Regex("""(?:Rs|INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)|([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:Rs|INR|₹)""")
    private val merchantRegex = Regex("""to\s+([A-Za-z0-9\s.&'-]+)(?:\s+on|\s+with|\s+at|\s*$)""")
    private val merchantRegex2 = Regex("""at\s+([A-Za-z0-9\s.&'-]+)(?:\s+on|\s+with|\s*$)""")

    fun parse(text: String): ParsedExpense? {
        // 1. VALIDATE: Only proceed if it passes the strict check
        if (!isValidTransaction(text)) {
            Log.d("SplitMate", "Ignored Non-Transactional Message: $text")
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
        var merchantMatch = merchantRegex.find(text)
        if (merchantMatch != null) {
            return merchantMatch.groupValues[1].trim().capitalizeWords()
        }

        merchantMatch = merchantRegex2.find(text)
        if (merchantMatch != null) {
            return merchantMatch.groupValues[1].trim().capitalizeWords()
        }

        return null
    }

    // Helper to make merchant names look cleaner, e.g., "dominos pizza" -> "Dominos Pizza"
    private fun String.capitalizeWords(): String = split(' ').joinToString(" ") { it.capitalize() }
}
