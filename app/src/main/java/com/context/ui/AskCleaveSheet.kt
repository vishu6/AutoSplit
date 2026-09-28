package com.context.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.context.data.AskCleaveResult
import com.context.ui.theme.ElectricBlue
import com.context.ui.theme.LightBlue
import com.context.utils.CurrencyMasker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskCleaveSheet(
    homeViewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val result by homeViewModel.askCleaveResult.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val privacyMode by remember { com.context.utils.SecurityUtils.getPrivacyModeFlow(context) }.collectAsState(initial = false)

    val smartStarters = listOf(
        "How much on Swiggy this month?",
        "Am I overspending on Food?",
        "Biggest expense in August?",
        "Total spent on groceries this year"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding())
            .padding(bottom = 24.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ask Cleave",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    color = LightBlue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "100% ON-DEVICE AI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = ElectricBlue
                        )
                    }
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        // --- CONTENT AREA ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 400.dp)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (result) {
                is AskCleaveResult.Idle -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricBlue.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "How can I help with your finances today?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is AskCleaveResult.Loading -> {
                    AIThinkingState()
                }
                is AskCleaveResult.Success -> {
                    val success = result as AskCleaveResult.Success
                    AIResultCard(
                        amount = CurrencyMasker.formatSmallAmount(success.amount ?: 0.0, privacyMode),
                        insight = "Based on your local records.",
                        explanation = success.explanation
                    )
                }
                is AskCleaveResult.Error -> {
                    Text(
                        text = (result as AskCleaveResult.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SMART STARTERS ---
        if (result is AskCleaveResult.Idle) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(smartStarters) { starter ->
                    AssistChip(
                        onClick = {
                            query = starter
                            homeViewModel.askCleave(starter)
                        },
                        label = { Text(starter) },
                        shape = RoundedCornerShape(12.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- INPUT HUB ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(20.dp)
                )
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Ask anything...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                if (query.isEmpty() && result is AskCleaveResult.Idle) {
                    IconButton(onClick = { /* Voice Input */ }) {
                        Icon(Icons.Rounded.Mic, contentDescription = "Voice Input", tint = Color.Gray)
                    }
                } else {
                    val canSend = query.isNotBlank() && result !is AskCleaveResult.Loading
                    IconButton(
                        onClick = {
                            if (result !is AskCleaveResult.Idle) {
                                homeViewModel.resetAskCleave()
                                query = ""
                            } else if (query.isNotBlank()) {
                                homeViewModel.askCleave(query)
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (canSend) ElectricBlue else Color.Gray.copy(alpha = 0.2f))
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (result is AskCleaveResult.Idle) Icons.AutoMirrored.Rounded.Send else Icons.Default.Refresh,
                            contentDescription = "Action",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AIThinkingState() {
    val infiniteTransition = rememberInfiniteTransition(label = "SparklePulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkleAlpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = ElectricBlue.copy(alpha = alpha),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Consulting your local data...",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = ElectricBlue
        )
    }
}

@Composable
fun AIResultCard(
    amount: String,
    insight: String,
    explanation: String
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = ElectricBlue.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "How I found this?",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (expanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
