package com.context.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.context.data.Expense
import com.context.ui.theme.CategoryStyling

@Composable
fun DonutChart(
    expenses: List<Expense>,
    modifier: Modifier = Modifier,
    chartSize: Dp = 180.dp,
    strokeWidth: Dp = 24.dp
) {
    // 1. Group data by Category and sum amounts
    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }
    
    val totalAmount = categoryTotals.values.sum()
    
    // Animation State
    var animationPlayed by remember { mutableStateOf(false) }
    val animateRotation by animateFloatAsState(
        targetValue = if (animationPlayed) 360f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 100, easing = LinearOutSlowInEasing),
        label = "Donut Chart Animation"
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Box(modifier = modifier.size(chartSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(chartSize)) {
            val strokeWidthPx = strokeWidth.toPx()
            val radius = size.width / 2
            
            var startAngle = -90f // Start from top

            // 2. Draw each slice
            categoryTotals.forEach { (category, amount) ->
                val sweepAngle = (amount.toFloat() / totalAmount.toFloat()) * 360f
                val color = CategoryStyling.getStyle(category).color
                
                // Draw Arc
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * (animateRotation / 360f), // Animate sweep
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt),
                    size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
                    topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
                )
                
                startAngle += sweepAngle
            }
        }
    }
}