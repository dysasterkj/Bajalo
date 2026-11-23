package com.kjota.bajalo.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/** El diseño "aurora" solo se ve bien en oscuro, así que la app siempre usa este tema. */
private val AuroraColors = darkColorScheme(
    primary = Color(0xFF7C6CFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF4333C2),
    onPrimaryContainer = Color(0xFFE6E2FF),
    secondary = Color(0xFFC8C4DD),
    onSecondary = Color(0xFF302E42),
    secondaryContainer = Color(0x33FFFFFF),
    onSecondaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF0B0B14),
    onBackground = Color(0xFFF2F0FA),
    surface = Color(0xFF121220),
    onSurface = Color(0xFFF2F0FA),
    surfaceVariant = Color(0xFF2A2A3A),
    onSurfaceVariant = Color(0xFFC9C5D0),
    outline = Color(0x66FFFFFF),
    outlineVariant = Color(0x33FFFFFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0D0D18),
    surfaceContainerLow = Color(0xFF16162A),
    surfaceContainer = Color(0xFF1A1A2E),
    surfaceContainerHigh = Color(0xFF242438),
    surfaceContainerHighest = Color(0xFF2E2E44),
)

@Composable
fun BajaloTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AuroraColors) {
        CompositionLocalProvider(
            LocalContentColor provides AuroraColors.onBackground,
            content = content,
        )
    }
}
