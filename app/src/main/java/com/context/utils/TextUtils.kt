package com.context.utils

import java.util.Locale

object TextUtils {
    /**
     * Converts a string to Title Case (e.g., "google bill" -> "Google Bill")
     */
    fun toTitleCase(input: String): String {
        if (input.isBlank()) return input
        
        return input.lowercase(Locale.getDefault())
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
    }
}

/**
 * Extension function for easier usage: "netflix".toTitleCase()
 */
fun String.toTitleCase(): String = TextUtils.toTitleCase(this)
