package com.context.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.context.utils.isNotificationPermissionGranted

@Composable
fun WelcomeScreen(
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // AUTO-DETECT: If user comes back from Settings with permission, go to Home
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isNotificationPermissionGranted(context)) {
                    onNavigateToHome()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp), // Increased padding for a cleaner look
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. THE HERO IMAGE (Updated)
        Icon(
            imageVector = Icons.Default.AutoGraph, 
            contentDescription = "Auto Tracking Illustration",
            modifier = Modifier
                .size(120.dp) // Make it HUGE
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(30.dp) // Squircle shape
                )
                .padding(24.dp), // Padding inside the shape
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 2. Title (More prominent)
        Text(
            text = "Automate Your Expenses",
            style = MaterialTheme.typography.headlineLarge, // Bigger font
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Explanation (Cleaner typography)
        Text(
            text = "Stop manually entering every transaction. Split Mate securely uses notifications from apps like GPay & PhonePe to track spending for you.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // 4. Privacy Reassurance (Highlighted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Security, 
                contentDescription = "Security",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Data stays offline. No personal messages read.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }


        Spacer(modifier = Modifier.weight(1f)) // Push buttons to bottom

        // 5. Enable Button (Modern Style)
        FilledTonalButton( // Tonal buttons look modern
            onClick = { 
                try {
                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    context.startActivity(intent)
                }
             },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Enable Auto-Tracking", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Skip Button
        TextButton(onClick = onNavigateToHome) {
            Text("Skip for now (Manual Mode)")
        }
        
        Spacer(modifier = Modifier.height(16.dp)) // Bottom padding
    }
}