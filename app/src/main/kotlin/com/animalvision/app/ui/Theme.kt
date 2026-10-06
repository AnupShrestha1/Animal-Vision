package com.animalvision.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Dark-first palette: camera apps live in the dark (spec §25). */
val AvInk = Color(0xFF0B0E12)
val AvPanel = Color(0xFF151A21)
val AvPanelElevated = Color(0xFF1E252E)
val AvAccent = Color(0xFF64D2FF)
val AvAccentAlt = Color(0xFFFFD60A)
val AvWarning = Color(0xFFFF9F0A)
val AvDanger = Color(0xFFFF453A)
val AvMuted = Color(0xFF8E99A4)
val AvTextPrimary = Color(0xFFF2F5F7)

private val AvDarkScheme = darkColorScheme(
    primary = AvAccent,
    onPrimary = Color(0xFF00232E),
    secondary = AvAccentAlt,
    onSecondary = Color(0xFF3A2E00),
    background = AvInk,
    onBackground = AvTextPrimary,
    surface = AvPanel,
    onSurface = AvTextPrimary,
    surfaceVariant = AvPanelElevated,
    onSurfaceVariant = AvMuted,
    error = AvDanger,
    onError = Color.White,
)

@Composable
fun AnimalVisionTheme(content: @Composable () -> Unit) {
    // Always dark: viewfinder exposure and contrast readability (spec §25).
    MaterialTheme(colorScheme = AvDarkScheme, content = content)
}
