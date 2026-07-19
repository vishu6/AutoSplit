package com.context.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyMasker {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    /**
     * Formats an amount. If privacy mode is enabled, it returns a masked string.
     */
    fun formatAmount(amount: Double, isPrivacyMode: Boolean): String {
        return if (isPrivacyMode) {
            "₹••••"
        } else {
            currencyFormatter.format(amount)
        }
    }

    /**
     * Mask for smaller labels or inline text.
     */
    fun formatSmallAmount(amount: Double, isPrivacyMode: Boolean): String {
        return if (isPrivacyMode) {
            "₹••"
        } else {
            "₹${amount.toInt()}"
        }
    }
}
