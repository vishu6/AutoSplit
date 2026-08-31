package com.context.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.context.data.ExpenseDatabase
import com.context.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onBudgetClick: () -> Unit,
    onManageCategoriesClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val currentThemeMode by ThemeUtils.getThemeModeFlow(context).collectAsState(initial = ThemeUtils.THEME_SYSTEM)
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    
    var userName by remember { mutableStateOf(OnboardingUtils.getUserName(context)) }
    var isSecurityEnabled by remember { mutableStateOf(SecurityUtils.isSecurityEnabled(context)) }
    val canUseBiometrics = remember { BiometricUtils.canAuthenticate(context) }

    // Launchers for CSV operations
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                val success = BackupUtils.exportToCsv(context, it)
                snackbarHostState.showSnackbar(if (success) "Backup saved successfully" else "Failed to save backup")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                val count = BackupUtils.importFromCsv(context, it)
                snackbarHostState.showSnackbar(if (count >= 0) "Imported $count transactions" else "Import failed")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsCard(title = "Account") {
                SettingsItem(
                    title = "Profile & Name",
                    subtitle = userName.ifBlank { "Set your name" },
                    icon = Icons.Default.Person,
                    onClick = { 
                        HapticUtils.playTick(context)
                        showNameDialog = true 
                    }
                )
                
                SettingsItem(
                    title = "Budget Planner",
                    subtitle = "Manage category limits",
                    icon = Icons.Default.AccountBalanceWallet,
                    onClick = { 
                        HapticUtils.playTick(context)
                        onBudgetClick() 
                    }
                )

                SettingsItem(
                    title = "Manage Categories",
                    subtitle = "Add or edit custom categories",
                    icon = Icons.Default.Category,
                    onClick = { 
                        HapticUtils.playTick(context)
                        onManageCategoriesClick()
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            SettingsCard(title = "App Settings") {
                Column {
                    SettingsItem(
                        title = "App Theme",
                        subtitle = when(currentThemeMode) {
                            ThemeUtils.THEME_LIGHT -> "Light Mode"
                            ThemeUtils.THEME_DARK -> "Dark Mode"
                            else -> "System Default"
                        },
                        icon = Icons.Default.Palette,
                        onClick = { 
                            HapticUtils.playTick(context)
                            showThemeDialog = true 
                        }
                    )

                    SecurityToggleItem(
                        title = "App Lock",
                        subtitle = if (canUseBiometrics) "Protect data with fingerprint/PIN" else "Biometrics not set up in phone settings",
                        icon = Icons.Default.Lock,
                        isEnabled = isSecurityEnabled,
                        canToggle = canUseBiometrics,
                        onToggle = { enabled ->
                            HapticUtils.playTick(context)
                            isSecurityEnabled = enabled
                            SecurityUtils.setSecurityEnabled(context, enabled)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsCard(title = "Data Management") {
                Column {
                    SettingsItem(
                        title = "Export CSV Backup",
                        subtitle = "Save all data to your device",
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            HapticUtils.playTick(context)
                            val date = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            exportLauncher.launch("Cleave_Backup_$date.csv")
                        }
                    )
                    
                    SettingsItem(
                        title = "Share Backup",
                        subtitle = "Send CSV to other apps",
                        icon = Icons.Default.Share,
                        onClick = {
                            HapticUtils.playTick(context)
                            scope.launch {
                                val csvFile = generateBackupFile(context)
                                if (csvFile != null) {
                                    try {
                                        val uri = FileProvider.getUriForFile(context, "com.context.app.fileprovider", csvFile)
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            type = "text/csv"
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Export Backup"))
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error sharing file")
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("No transactions found to export")
                                }
                            }
                        }
                    )

                    SettingsItem(
                        title = "Import from CSV",
                        subtitle = "Restore transactions from backup",
                        icon = Icons.Default.FileUpload,
                        onClick = {
                            HapticUtils.playTick(context)
                            importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/csv"))
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsCard(title = "Support") {
                Column {
                    SettingsItem(
                        title = "Give Feedback",
                        subtitle = "Help us improve Cleave",
                        icon = Icons.Default.RateReview,
                        onClick = {
                            HapticUtils.playTick(context)
                            showFeedbackSheet = true
                        }
                    )

                    SettingsItem(
                        title = "Report a Bug",
                        subtitle = "support@cleaveapp.in",
                        icon = Icons.Default.BugReport,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@cleaveapp.in"))
                                putExtra(Intent.EXTRA_SUBJECT, "Cleave Bug Report")
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        }
                    )

                    SettingsItem(
                        title = "Privacy Policy",
                        subtitle = "View data handling practices",
                        icon = Icons.Default.Info,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/cleave-privacy-policy/home"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Made with ❤️ in India", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                Text(text = "Version 1.5.6", style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.7f))
            }
        }
    }

    // Name Editing Dialog
    if (showNameDialog) {
        var tempName by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Update Profile Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (tempName.isNotBlank()) {
                        OnboardingUtils.saveUserName(context, tempName.trim())
                        userName = tempName.trim()
                        showNameDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    ThemeOption("System Default", currentThemeMode == ThemeUtils.THEME_SYSTEM) {
                        ThemeUtils.saveThemeMode(context, ThemeUtils.THEME_SYSTEM)
                        showThemeDialog = false
                    }
                    ThemeOption("Light", currentThemeMode == ThemeUtils.THEME_LIGHT) {
                        ThemeUtils.saveThemeMode(context, ThemeUtils.THEME_LIGHT)
                        showThemeDialog = false
                    }
                    ThemeOption("Dark", currentThemeMode == ThemeUtils.THEME_DARK) {
                        ThemeUtils.saveThemeMode(context, ThemeUtils.THEME_DARK)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Close") } }
        )
    }

    if (showFeedbackSheet) {
        FeedbackBottomSheet(
            onDismiss = { showFeedbackSheet = false },
            onSubmitted = {
                showFeedbackSheet = false
                scope.launch { snackbarHostState.showSnackbar("Opening feedback email...") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(onDismiss: () -> Unit, onSubmitted: () -> Unit) {
    val context = LocalContext.current
    var rating by remember { mutableIntStateOf(0) }
    var feedbackType by remember { mutableStateOf("Feedback") }
    var comments by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("How are we doing?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Your feedback helps us build a better Cleave.", color = Color.Gray, textAlign = TextAlign.Center)

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) { index ->
                    val starIndex = index + 1
                    IconButton(onClick = { 
                        HapticUtils.playTick(context)
                        rating = starIndex 
                    }) {
                        Icon(
                            imageVector = if (starIndex <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (starIndex <= rating) Color(0xFFFFB300) else Color(0xFFE0E0E0),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Issue", "Suggestion", "Praise").forEach { type ->
                    FilterChip(
                        selected = feedbackType == type,
                        onClick = { 
                            HapticUtils.playTick(context)
                            feedbackType = type 
                        },
                        label = { Text(type) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = comments,
                onValueChange = { comments = it },
                placeholder = { Text("What's on your mind?") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    HapticUtils.playThud(context)
                    val body = """
                        Rating: $rating/5
                        Type: $feedbackType
                        
                        Comments:
                        $comments
                        
                        --- Technical Details ---
                        App Version: 1.5.6
                        Device: ${Build.MODEL}
                        Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                    """.trimIndent()

                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@cleaveapp.in"))
                        putExtra(Intent.EXTRA_SUBJECT, "Cleave Feedback [$feedbackType]")
                        putExtra(Intent.EXTRA_TEXT, body)
                    }
                    
                    try { context.startActivity(intent) } catch (e: Exception) {}

                    if (rating == 5) {
                        context.findActivity()?.let { activity ->
                            ReviewManager.launchReviewFlow(activity)
                        }
                    }
                    
                    onSubmitted()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = rating > 0
            ) {
                Text("Submit Feedback", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column {
                content()
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
    }
}

@Composable
fun SecurityToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    canToggle: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (canToggle) onToggle(!isEnabled) 
                else Toast.makeText(context, "Please set up a Fingerprint or PIN in your phone settings first.", Toast.LENGTH_LONG).show()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = null, // Logic handled by Row click
            enabled = canToggle
        )
    }
}

@Composable
fun ThemeOption(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

private suspend fun generateBackupFile(context: android.content.Context): File? {
    return withContext(Dispatchers.IO) {
        try {
            val db = ExpenseDatabase.getDatabase(context)
            val expenses = db.expenseDao().getAllExpenses().first()
            
            if (expenses.isEmpty()) return@withContext null

            val exportsDir = File(context.cacheDir, "exports")
            if (!exportsDir.exists()) exportsDir.mkdirs()
            
            val file = File(exportsDir, "Cleave_Backup.csv")
            FileOutputStream(file).use { output ->
                output.write("Merchant,Amount,Category,Date,PaidBy\n".toByteArray())
                expenses.forEach { exp ->
                    val date = DateUtils.formatDate(exp.timestamp)
                    val line = "\"${exp.merchant}\",${exp.amount},\"${exp.category}\",\"$date\",\"${exp.paidBy}\"\n"
                    output.write(line.toByteArray())
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }
}
