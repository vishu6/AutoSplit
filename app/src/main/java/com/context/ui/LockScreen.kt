package com.context.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.context.utils.BiometricUtils

@Composable
fun LockScreen(
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(Unit) {
        if (activity != null) {
            BiometricUtils.showBiometricPrompt(
                activity = activity,
                onSuccess = onAuthenticated,
                onError = { /* Handle error or show a button to retry */ }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Split Mate is Locked",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Use your fingerprint or PIN to unlock",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = {
                if (activity != null) {
                    BiometricUtils.showBiometricPrompt(
                        activity = activity,
                        onSuccess = onAuthenticated,
                        onError = { }
                    )
                }
            }
        ) {
            Text("Try Again")
        }
    }
}
