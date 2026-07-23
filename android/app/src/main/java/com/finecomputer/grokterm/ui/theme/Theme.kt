package com.finecomputer.grokterm.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// xAI / Grok inspired dark palette
private val GrokDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),          // cyan accent
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF00363F),
    secondary = Color(0xFFB0BEC5),
    onSecondary = Color(0xFF000000),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E1E1E),
    error = Color(0xFFCF6679)
)

private val GrokLightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    onPrimary = Color.White,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF121212),
    surface = Color.White,
    onSurface = Color(0xFF121212)
)

@Composable
fun GrokTermTheme(
    darkTheme: Boolean = true, // force dark for terminal feel
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GrokDarkColorScheme else GrokLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
