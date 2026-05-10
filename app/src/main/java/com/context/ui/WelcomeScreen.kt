package com.context.ui

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.context.ui.theme.*
import com.context.utils.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun WelcomeScreen(
    onEnableClicked: () -> Unit,
    onSkipClicked: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 5 })
    
    var userName by remember { mutableStateOf("") }
    var monthlyBudget by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (PermissionUtils.isNotificationServiceEnabled(context)) {
                    if (pagerState.currentPage == 3) {
                        scope.launch { pagerState.animateScrollToPage(4) }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = when (pagerState.currentPage) {
            0 -> OnboardingTeal
            1 -> OnboardingCream
            2 -> OnboardingNavy
            else -> OnboardingCream
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    0 -> HookScreen()
                    1 -> SolutionScreen()
                    2 -> PrivacyScreen()
                    3 -> PermissionScreen(onEnableClicked)
                    4 -> PersonalizationScreen(
                        name = userName,
                        onNameChange = { userName = it },
                        budget = monthlyBudget,
                        onBudgetChange = { monthlyBudget = it },
                        onFinish = {
                            val finalName = userName.ifBlank { "User" }
                            val budgetVal = monthlyBudget.toDoubleOrNull() ?: 0.0
                            OnboardingUtils.saveUserName(context, finalName)
                            if (budgetVal > 0) BudgetUtils.setMonthlyBudget(context, budgetVal)
                            OnboardingUtils.setOnboardingCompleted(context)
                            onSkipClicked()
                        }
                    )
                }
            }

            OnboardingBottomNav(
                pagerState = pagerState,
                onNext = {
                    scope.launch { 
                        if (pagerState.currentPage < 4) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun HookScreen() {
    var count by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val target = 23400
        val duration = 2000L
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < duration) {
            val progress = (System.currentTimeMillis() - startTime).toFloat() / duration
            count = (target * progress).toInt()
            kotlinx.coroutines.delay(16)
        }
        count = target
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "₹${String.format(java.util.Locale.getDefault(), "%,d", count)}",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 56.sp
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Gone. Just like that.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "The average Indian spends ₹23,400 a month without knowing where half of it went.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Swipe to see how →",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SolutionScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(ElectricBlue, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("HDFC Bank", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                        Text("₹500 debited via UPI to Swiggy", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.padding(vertical = 12.dp).size(28.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ElectricBlue),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fastfood, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Swiggy", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("₹500", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Every rupee. Captured.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OnboardingTeal,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Cleave reads your bank alerts and logs every payment automatically.\nSwiggy. Blinkit. Cab. Done.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PrivacyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = FintechGreen
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Your money stays\non your phone.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PrivacyPoint(Icons.Default.Storage, "No servers. No cloud.")
            PrivacyPoint(Icons.Default.NoAccounts, "No account required.")
            PrivacyPoint(Icons.Default.VisibilityOff, "We never read OTPs or chats.")
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PrivacyPoint(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PermissionScreen(onEnable: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "One permission.\nThat's all we need.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OnboardingTeal,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BANK ALERT", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("HDFC Bank: ₹500 debited from account XX1234 via UPI", fontSize = 14.sp)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))
                Text("✓ We read the amount & merchant", color = FintechGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("✗ We NEVER read OTPs or private chats", color = FintechRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onEnable,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .scale(scale),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
        ) {
            Text("Enable Auto-Tracking", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PersonalizationScreen(
    name: String,
    onNameChange: (String) -> Unit,
    budget: String,
    onBudgetChange: (String) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Last thing —\nwhat should we call you?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OnboardingTeal,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Your first name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Text(
            text = "Stored only on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Set a monthly budget? (optional)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnboardingTeal
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = budget,
            onValueChange = { if (it.all { c -> c.isDigit() }) onBudgetChange(it) },
            prefix = { Text("₹ ") },
            placeholder = { Text("0") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OnboardingTeal)
        ) {
            Text("Start Tracking →", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingBottomNav(
    pagerState: PagerState,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pagerState.currentPage == 4) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == 0 || pagerState.currentPage == 2) {
                                if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.3f)
                            } else {
                                if (pagerState.currentPage == index) OnboardingTeal else OnboardingTeal.copy(alpha = 0.3f)
                            }
                        )
                )
            }
        }

        if (pagerState.currentPage < 3) {
            TextButton(onClick = onNext) {
                Text(
                    text = "Next →",
                    color = if (pagerState.currentPage == 0 || pagerState.currentPage == 2) Color.White else OnboardingTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else if (pagerState.currentPage == 3) {
            TextButton(onClick = onNext) {
                Text(
                    text = "Skip for now",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}
