
package com.example.autosplit

// A simple data class to hold the parsed result.
data class ParsedExpense(
    val amount: Double,
    val merchant: String
)

object ExpenseParser {

    // Regex to find amounts like Rs. 1,234.56, Rs 500, 10.00 INR, etc.
    private val amountRegex = Regex("""(?:Rs|INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)|([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:Rs|INR|₹)""")

    // Regex to find merchants, often after "to" or "at"
    private val merchantRegex = Regex("""to\s+([A-Za-z0-9\s.&'-]+)(?:\s+on|\s+with|\s+at|\s*$)""")
    private val merchantRegex2 = Regex("""at\s+([A-Za-z0-9\s.&'-]+)(?:\s+on|\s+with|\s*$)""")


    fun parse(text: String): ParsedExpense? {
        val amount = findAmount(text) ?: return null
        val merchant = findMerchant(text) ?: "Unknown Merchant"

        return ParsedExpense(amount = amount, merchant = merchant)
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
            return merchantMatch.groupValues[1].trim()
        }

        merchantMatch = merchantRegex2.find(text)
        if (merchantMatch != null) {
            return merchantMatch.groupValues[1].trim()
        }

        return null
    }
}
