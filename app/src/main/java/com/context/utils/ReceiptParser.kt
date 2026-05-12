package com.context.utils

import com.google.mlkit.vision.text.Text
import kotlin.math.abs

data class ReceiptItem(
    val name: String,
    val price: Double,
    var selectedBy: MutableList<String> = mutableListOf()
)

object ReceiptParser {
    fun parseReceiptText(visionText: Text): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()
        
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return emptyList()

        // 1. Group lines into rows with a larger tolerance (80% of line height)
        val rows = mutableListOf<MutableList<Text.Line>>()
        val sortedLines = allLines.sortedBy { it.boundingBox?.top ?: 0 }

        sortedLines.forEach { line ->
            val top = line.boundingBox?.top ?: 0
            val matchingRow = rows.find { row ->
                val rowLine = row.firstOrNull()
                val rowTop = rowLine?.boundingBox?.top ?: 0
                val rowHeight = rowLine?.boundingBox?.let { it.bottom - it.top } ?: 25
                abs(rowTop - top) < rowHeight * 0.8 
            }

            if (matchingRow != null) {
                matchingRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        // 2. Define strict summary patterns to ignore
        val summaryPrefixes = listOf(
            "TOTAL", "NET", "PAID", "PAYABLE", "SUBTOTAL", "GRAND", 
            "AMOUNT", "CASH", "CHANGE", "WALLET", "BALANCE", "RECEIVED", "SUM OF"
        )
        
        val summaryKeywords = listOf(
            "TOTAL AMOUNT", "NET AMOUNT", "PAID AMOUNT", "TOTAL :", "SUB TOTAL",
            "IN WORDS", "NINE HUNDRED", "ROUND OFF", "GST", "TAX"
        )

        val priceRegex = Regex("""(\d{1,3}(?:[.,\s]\d{3})*[.,]\d{2})(?!\d)""")

        rows.forEach { row ->
            val sortedRowLines = row.sortedBy { it.boundingBox?.left ?: 0 }
            val rowText = sortedRowLines.joinToString(" ") { it.text }.trim()
            val upperRowText = rowText.uppercase()
            
            val matches = priceRegex.findAll(rowText).toList()
            
            if (matches.isNotEmpty()) {
                val mainMatch = matches.last()
                val priceString = mainMatch.value.replace(Regex("""[,\s]"""), "")
                val price = priceString.toDoubleOrNull() ?: 0.0
                
                var nameCandidate = rowText
                matches.forEach { nameCandidate = nameCandidate.replace(it.value, "") }
                
                nameCandidate = nameCandidate
                    .replace(Regex("""[|:₹Rs\.-]"""), " ")
                    .replace(Regex("""\s+"""), " ")
                    .trim()

                val upperName = nameCandidate.uppercase()

                // HEURISTICS TO DISCARD SUMMARY ROWS
                // 1. If the row starts with a summary word (e.g. "Total Amount 900")
                val startsWithSummary = summaryPrefixes.any { upperRowText.startsWith(it) }
                
                // 2. If the name candidate is just a summary keyword
                val isJustSummaryLabel = summaryKeywords.any { upperRowText.contains(it) }
                
                // 3. If the line contains "Received" or "Sum of" (Footer text)
                val isFooterText = upperRowText.contains("RECEIVED") || upperRowText.contains("SUM OF")

                // 4. If the name is empty or mostly symbols/junk
                val isMostlyJunk = nameCandidate.isEmpty() || nameCandidate.all { !it.isLetter() }

                // Only add if it's NOT a summary/footer and has a valid name/price
                if (!startsWithSummary && !isJustSummaryLabel && !isFooterText && !isMostlyJunk && nameCandidate.length > 2 && price > 0.5) {
                    items.add(ReceiptItem(nameCandidate, price))
                }
            }
        }
        
        return items.distinctBy { "${it.name}-${it.price}" }
    }
}
