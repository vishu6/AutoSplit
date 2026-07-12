package com.context.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.context.utils.HapticUtils
import com.context.utils.ReviewManager
import com.context.utils.findActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpScreen(
    onBack: () -> Unit,
    onSettled: () -> Unit,
    viewModel: SettleUpViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val suggestions by viewModel.settlementSuggestions.collectAsState()
    val group by viewModel.group.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settle Up") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Header Info
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Smart Settlement Suggestions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "We've optimized these transactions to clear all debts in the fewest steps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (suggestions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Check, 
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Everyone is settled up!", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(suggestions) { suggestion ->
                        SettlementCard(
                            suggestion = suggestion,
                            onSettle = {
                                viewModel.recordSettlement(suggestion.from, suggestion.to, suggestion.amount)
                                HapticUtils.playThud(context)
                                
                                // Review logic wrapped in coroutine scope
                                scope.launch {
                                    if (ReviewManager.shouldShowReview(context)) {
                                        context.findActivity()?.let { ReviewManager.launchReviewFlow(it) }
                                    }
                                    onSettled()
                                }
                            },
                            onPayNow = {
                                // Launch UPI intent
                                val upiUri = Uri.parse("upi://pay?pa=recipient@upi&pn=${suggestion.to}&am=${suggestion.amount}&cu=INR")
                                val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                context.startActivity(Intent.createChooser(intent, "Pay with..."))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettlementCard(
    suggestion: SettlementSuggestion,
    onSettle: () -> Unit,
    onPayNow: () -> Unit
) {
    val isUserPaying = suggestion.from == "You"
    val isUserReceiving = suggestion.to == "You"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUserPaying) Color(0xFFFFF1F0) else if (isUserReceiving) Color(0xFFF6FFED) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isUserPaying) Color(0xFFFFCCC7) else if (isUserReceiving) Color(0xFFB7EB8F) else Color.LightGray,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isUserPaying) Icons.Default.Payment else Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isUserPaying) Color(0xFFCF1322) else if (isUserReceiving) Color(0xFF389E0D) else Color.DarkGray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${suggestion.from} → ${suggestion.to}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "₹${String.format("%.2f", suggestion.amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (isUserPaying) Color(0xFFCF1322) else if (isUserReceiving) Color(0xFF389E0D) else Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isUserPaying) {
                    Button(
                        onClick = onPayNow,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF1322))
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pay Now")
                    }
                }
                
                OutlinedButton(
                    onClick = onSettle,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                ) {
                    Text("Record as Settled")
                }
            }
        }
    }
}
