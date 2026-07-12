package com.context.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.context.data.Category
import com.context.data.Expense
import com.context.ui.theme.CategoryStyling
import kotlin.math.atan2

@Composable
fun DonutChart(
    expenses: List<Expense>,
    modifier: Modifier = Modifier,
    selectedCategory: String? = null,
    onCategoryClick: (String?) -> Unit = {},
    categoryMap: Map<String, Category> = emptyMap(),
    chartSize: Dp = 180.dp,
    strokeWidth: Dp = 28.dp // Slightly thicker for better color visibility
) {
    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }
    
    val totalAmount = categoryTotals.values.sum()
    
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
        Canvas(
            modifier = Modifier
                .size(chartSize)
                .pointerInput(expenses) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val angle = (Math.toDegrees(atan2(offset.y - center.y, offset.x - center.x).toDouble()) + 90 + 360) % 360
                        
                        var currentAngle = 0f
                        var found = false
                        categoryTotals.forEach { (category, amount) ->
                            val sweepAngle = (amount.toFloat() / totalAmount.toFloat()) * 360f
                            if (angle >= currentAngle && angle <= currentAngle + sweepAngle) {
                                onCategoryClick(if (selectedCategory == category) null else category)
                                found = true
                            }
                            currentAngle += sweepAngle
                        }
                        if (!found) onCategoryClick(null)
                    }
                }
        ) {
            val strokeWidthPx = strokeWidth.toPx()
            val innerStrokeWidthPx = strokeWidthPx * 1.1f
            
            var startAngle = -90f

            categoryTotals.forEach { (category, amount) ->
                val sweepAngle = (amount.toFloat() / totalAmount.toFloat()) * 360f
                val style = CategoryStyling.getStyle(category, customCategories = categoryMap)
                val isSelected = selectedCategory == category
                
                drawArc(
                    // FIXED: Using boldColor for segments to ensure vibrant colors (Green/Pink) are visible
                    color = if (selectedCategory == null || isSelected) style.boldColor else style.boldColor.copy(alpha = 0.25f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * (animateRotation / 360f),
                    useCenter = false,
                    style = Stroke(
                        width = if (isSelected) innerStrokeWidthPx else strokeWidthPx, 
                        cap = StrokeCap.Round
                    ),
                    size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
                    topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
                )
                
                startAngle += sweepAngle
            }
        }

        Surface(
            onClick = { onCategoryClick(null) },
            modifier = Modifier.size(chartSize / 2),
            color = Color.Transparent,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (selectedCategory != null) {
                    val style = CategoryStyling.getStyle(selectedCategory, customCategories = categoryMap)
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = style.boldColor
                    )
                }
            }
        }
    }
}
