package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = StudioHighlight,
    onPrimaryContainer = NeonCyan,
    secondary = NeonAmber,
    onSecondary = Color.Black,
    secondaryContainer = StudioCard,
    onSecondaryContainer = NeonAmber,
    tertiary = NeonPurple,
    background = StudioBackground,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioCard,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioColorScheme,
        typography = Typography,
        content = content
    )
}
