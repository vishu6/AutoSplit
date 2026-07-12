package com.context.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider

/**
 * Custom Theme for Cleave Widgets to match the main app's branding.
 */
object CleaveWidgetTheme {
    
    // Brand Colors (Matching Color.kt)
    private val ElectricBlue = Color(0xFF2962FF)
    private val OffWhite = Color(0xFFF5F7FA)
    private val DarkBackground = Color(0xFF121212)
    private val DarkPrimary = Color(0xFF82B1FF)
    private val FintechRed = Color(0xFFE53935)

    val colors = ColorProviders(
        light = androidx.compose.material3.lightColorScheme(
            primary = ElectricBlue,
            onPrimary = Color.White,
            background = OffWhite,
            onBackground = Color(0xFF1A1A1A),
            surface = Color.White,
            onSurface = Color(0xFF1A1A1A),
            secondaryContainer = ElectricBlue.copy(alpha = 0.1f),
            onSecondaryContainer = ElectricBlue,
            error = FintechRed
        ),
        dark = androidx.compose.material3.darkColorScheme(
            primary = DarkPrimary,
            onPrimary = Color.Black,
            background = DarkBackground,
            onBackground = Color(0xFFE1E1E1),
            surface = Color(0xFF1E1E1E),
            onSurface = Color(0xFFE1E1E1),
            secondaryContainer = DarkPrimary.copy(alpha = 0.15f),
            onSecondaryContainer = DarkPrimary,
            error = FintechRed
        )
    )
}
