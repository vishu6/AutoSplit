package com.context.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.context.app.R
import com.context.data.ExpenseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { ExpenseDatabase.getDatabase(context) }
    
    // Load expenses for backup
    val allExpenses by db.expenseDao().getAllExpenses().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Info") },
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
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. APP LOGO & VERSION HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Correctly build the icon from its parts to avoid build errors and crashes
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "App Logo",
                            modifier = Modifier.fillMaxSize(0.7f) // Scale foreground within the background
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Split Mate",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0.0 (Alpha)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            // 2. TOOLS SECTION
            SettingsItem(
                title = "Backup Data (Export CSV)",
                subtitle = "Share your expense history via WhatsApp",
                icon = Icons.Default.Share,
                onClick = {
                    val csvHeader = "Date,Merchant,Amount,Category,Group\n"
                    val csvBody = allExpenses.joinToString("\n") { exp ->
                        val date = java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(exp.timestamp))
                        "$date,${exp.merchant},${exp.amount},${exp.category},${exp.groupId ?: "Personal"}"
                    }
                    
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, csvHeader + csvBody)
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TITLE, "SplitMate_Backup.csv")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Export Backup"))
                }
            )

            SettingsItem(
                title = "Report a Bug",
                subtitle = "Found an issue? Let us know.",
                icon = Icons.Default.BugReport,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("your-email@example.com")) // Put your email here
                        putExtra(Intent.EXTRA_SUBJECT, "Split Mate Bug Report")
                    }
                    try { context.startActivity(intent) } catch (e: Exception) {}
                }
            )

            Spacer(modifier = Modifier.weight(1f)) // Push footer to bottom

            // 3. THE "MADE IN INDIA" FOOTER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Made with ❤️ in India",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🇮🇳 Atmanirbhar Bharat Initiative",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { 
            Icon(
                imageVector = icon, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            ) 
        },
        modifier = Modifier.clickable { onClick() }
    )
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}