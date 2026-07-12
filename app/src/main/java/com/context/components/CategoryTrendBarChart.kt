package com.context.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.context.ui.theme.ElectricBlue

@Composable
fun CategoryTrendBarChart(
    trendData: List<Pair<String, Double>>,
    categoryColor: Color = ElectricBlue,
    modifier: Modifier = Modifier
) {
    if (trendData.isEmpty()) return

    val maxAmount = trendData.maxOf { it.second }.coerceAtLeast(1.0)

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            trendData.forEachIndexed { index, (month, amount) ->
                val isLastMonth = index == trendData.size - 1
                
                val barHeightWeight = (amount / maxAmount).toFloat().coerceIn(0.015f, 1f)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(if (isLastMonth) 1.3f else 1f)
                        .fillMaxHeight()
                ) {
                    // Numeric labels above every bar
                    if (amount > 0) {
                        Text(
                            text = if (amount >= 1000) "₹${(amount / 1000).toInt()}k" else "₹${amount.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = if (isLastMonth) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isLastMonth) categoryColor else Color.Gray
                        )
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    var animationTriggered by remember { mutableStateOf(false) }
                    val animatedWeight by animateFloatAsState(
                        targetValue = if (animationTriggered) barHeightWeight else 0f,
                        animationSpec = tween(durationMillis = 800),
                        label = "BarHeight"
                    )
                    
                    LaunchedEffect(Unit) { animationTriggered = true }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight(animatedWeight * 0.75f) // Reduced to 0.75f to ensure labels fit
                            .width(if (isLastMonth) 32.dp else 24.dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (isLastMonth) categoryColor 
                                else categoryColor.copy(alpha = 0.25f)
                            )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = month,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = if (isLastMonth) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isLastMonth) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
