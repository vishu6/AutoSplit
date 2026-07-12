package com.context.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.context.ui.theme.ElectricBlue
import com.context.utils.TimeRange
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ModernSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search transactions..."
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = { Text(text = placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricBlue) },
        trailingIcon = {
            IconButton(onClick = onClearSearch) {
                Icon(Icons.Default.Close, contentDescription = "Close Search")
            }
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        maxLines = 1,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricBlue,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun ModernTimeRangeFilter(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    calendar: Calendar,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // RESPONSIVE FILTER ROW: Uses weights to ensure 5 items always fit
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeRange.entries.forEach { range ->
                val isSelected = range == selectedRange
                val label = range.name.lowercase().replaceFirstChar { it.uppercase() }
                
                Box(
                    modifier = Modifier
                        .weight(1f) // Ensures 20% width per item
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ElectricBlue.copy(alpha = 0.1f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) ElectricBlue else Color.LightGray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onRangeSelected(range) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        fontSize = 12.sp, // Slightly smaller to guarantee fit on mid-range devices
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (selectedRange != TimeRange.ALL) {
            val dayFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            val weekFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

            val navigatorLabel = when (selectedRange) {
                TimeRange.TODAY -> dayFormat.format(calendar.time)
                TimeRange.WEEK -> {
                    val weekStart = calendar.clone() as Calendar
                    weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
                    val weekEnd = weekStart.clone() as Calendar
                    weekEnd.add(Calendar.DAY_OF_WEEK, 6)
                    "${weekFormat.format(weekStart.time)} - ${weekFormat.format(weekEnd.time)}"
                }
                TimeRange.MONTH -> monthYearFormat.format(calendar.time)
                TimeRange.YEAR -> yearFormat.format(calendar.time)
                TimeRange.ALL -> ""
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Previous",
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = navigatorLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Next",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
