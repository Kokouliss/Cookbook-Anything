package com.cookbookanything.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm kitchen palette: terracotta, olive and cream. Fixed colours on purpose, so the app keeps
// its own look instead of following the wallpaper.
private val LightColors = lightColorScheme(
    primary = Color(0xFFB5481F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF6B5E2F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4E3A9),
    onSecondaryContainer = Color(0xFF221B00),
    tertiary = Color(0xFF4E6540),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD0EBBC),
    onTertiaryContainer = Color(0xFF0C2004),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF231A16),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF231A16),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF53433E),
    outline = Color(0xFF85736D),
    outlineVariant = Color(0xFFD8C2BA),
    surfaceDim = Color(0xFFE8D6CF),
    surfaceBright = Color(0xFFFFF8F4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EB),
    surfaceContainer = Color(0xFFFCEAE3),
    surfaceContainerHigh = Color(0xFFF6E4DD),
    surfaceContainerHighest = Color(0xFFF0DFD8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB59B),
    onPrimary = Color(0xFF5E1700),
    primaryContainer = Color(0xFF8F3209),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFD7C78F),
    onSecondary = Color(0xFF3A3005),
    secondaryContainer = Color(0xFF524619),
    onSecondaryContainer = Color(0xFFF4E3A9),
    tertiary = Color(0xFFB4CFA2),
    onTertiary = Color(0xFF213615),
    tertiaryContainer = Color(0xFF374D2A),
    onTertiaryContainer = Color(0xFFD0EBBC),
    background = Color(0xFF1B110D),
    onBackground = Color(0xFFF1DFD8),
    surface = Color(0xFF1B110D),
    onSurface = Color(0xFFF1DFD8),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BA),
    outline = Color(0xFFA08D86),
    outlineVariant = Color(0xFF53433E),
    surfaceDim = Color(0xFF1B110D),
    surfaceBright = Color(0xFF443731),
    surfaceContainerLowest = Color(0xFF150C08),
    surfaceContainerLow = Color(0xFF231A16),
    surfaceContainer = Color(0xFF281D19),
    surfaceContainerHigh = Color(0xFF332823),
    surfaceContainerHighest = Color(0xFF3E322D),
)

@Composable
fun CookbookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
