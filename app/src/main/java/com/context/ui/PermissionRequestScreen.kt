package com.context.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.context.ui.theme.ContextTheme
import com.context.ui.theme.ElectricBlue

@Composable
fun PermissionRequestScreen() {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface, // Use pure white for this screen
        bottomBar = {
            Button(
                onClick = { 
                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Enable Auto-Tracking", fontWeight = FontWeight.Bold)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Illustration Placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(ElectricBlue.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications Icon",
                    tint = ElectricBlue,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Headline
            Text(
                text = "Magical Tracking",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Body Text
            Text(
                text = "Context needs permission to read payment notifications from GPay/PhonePe. We strictly process this locally. Your financial data never leaves your phone.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Visual Cue
            VisualBenefitCue()

            Spacer(modifier = Modifier.height(60.dp)) // Spacer to push content above button
        }
    }
}

@Composable
private fun VisualBenefitCue() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Mock Notification
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text("GPay: Paid ₹500 to Starbucks", style = MaterialTheme.typography.bodySmall)
            }
        }

        Icon(
            imageVector = Icons.Default.ArrowDownward,
            contentDescription = "Transforms into",
            tint = ElectricBlue,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Mock Expense Entry
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
             modifier = Modifier.fillMaxWidth()
        ) {
             Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFF0E5)) {
                    Icon(Icons.Default.Fastfood, contentDescription = null, modifier = Modifier.padding(8.dp), tint = ElectricBlue)
                }
                Spacer(modifier = Modifier.padding(start = 12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Starbucks", fontWeight = FontWeight.Bold)
                    Text("Just now", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                Text("-₹500", fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
            }
        }
    }
}

fun isNotificationPermissionGranted(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    val packageName = context.packageName
    return enabledListeners != null && enabledListeners.contains(packageName)
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F7FA)
@Composable
fun PermissionRequestScreenPreview() {
    ContextTheme {
        PermissionRequestScreen()
    }
}
