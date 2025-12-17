package com.context.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    background = OffWhite,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    secondaryContainer = ElectricBlue.copy(alpha = 0.1f) // For button backgrounds
)

@Composable
fun ContextTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        // Assuming a default Typography for now. We can create Typography.kt if needed.
        content = content
    )
}
