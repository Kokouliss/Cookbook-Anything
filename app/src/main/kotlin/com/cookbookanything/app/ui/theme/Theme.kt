@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Market-stall palette: tomato red, basil green and aubergine on saffron-tinted cream. Fixed colours
// on purpose, so the app keeps its own look instead of following the wallpaper.
private val LightColors = lightColorScheme(
    primary = Color(0xFFC8321E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD2C2),
    onPrimaryContainer = Color(0xFF3B0900),
    secondary = Color(0xFF2E7D32),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBDE8B5),
    onSecondaryContainer = Color(0xFF05210A),
    tertiary = Color(0xFF7B2D5E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7EA),
    onTertiaryContainer = Color(0xFF36072A),
    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF1E1410),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF1E1410),
    surfaceVariant = Color(0xFFF6E3C4),
    onSurfaceVariant = Color(0xFF4D3F2C),
    outline = Color(0xFF7C6A4F),
    outlineVariant = Color(0xFFDCC9A6),
    surfaceDim = Color(0xFFEBDDC8),
    surfaceBright = Color(0xFFFFFBF5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF4E0),
    surfaceContainer = Color(0xFFFDECCF),
    surfaceContainerHigh = Color(0xFFF9E4C0),
    surfaceContainerHighest = Color(0xFFF5DCB0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A1),
    onPrimary = Color(0xFF5F1407),
    primaryContainer = Color(0xFF8C1D0E),
    onPrimaryContainer = Color(0xFFFFDAD1),
    secondary = Color(0xFF8FD88A),
    onSecondary = Color(0xFF003909),
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFFC5F2BE),
    tertiary = Color(0xFFF5B3D6),
    onTertiary = Color(0xFF4A0E36),
    tertiaryContainer = Color(0xFF632249),
    onTertiaryContainer = Color(0xFFFFD7EA),
    background = Color(0xFF17110B),
    onBackground = Color(0xFFF3E4D0),
    surface = Color(0xFF17110B),
    onSurface = Color(0xFFF3E4D0),
    surfaceVariant = Color(0xFF4D3F2C),
    onSurfaceVariant = Color(0xFFDCC9A6),
    outline = Color(0xFFA4936F),
    outlineVariant = Color(0xFF4D3F2C),
    surfaceDim = Color(0xFF17110B),
    surfaceBright = Color(0xFF40362A),
    surfaceContainerLowest = Color(0xFF110C07),
    surfaceContainerLow = Color(0xFF201810),
    surfaceContainer = Color(0xFF251C13),
    surfaceContainerHigh = Color(0xFF30261B),
    surfaceContainerHighest = Color(0xFF3B3024),
)

@Composable
fun CookbookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

/**
 * Top bars carry the brand red. Both variants are dark with light text, which is why MainActivity
 * asks for light status bar icons in every theme.
 */
@Composable
fun appBarColors(): TopAppBarColors {
    val scheme = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val container = if (dark) scheme.primaryContainer else scheme.primary
    val content = if (dark) scheme.onPrimaryContainer else scheme.onPrimary
    return TopAppBarDefaults.topAppBarColors(
        containerColor = container,
        scrolledContainerColor = container,
        navigationIconContentColor = content,
        titleContentColor = content,
        actionIconContentColor = content,
    )
}
