@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.cookbookanything.core.domain.design.Palette.Dark
import com.cookbookanything.core.domain.design.Palette.Light

// A box of index cards: cream box, white cards, the family red for the rule across each card, teal
// for ticks and ochre for the category tabs. Fixed colours on purpose, so the app keeps its own look
// instead of following the wallpaper. The values and their contrast test live in core/domain.
private val LightColors = lightColorScheme(
    primary = Color(Light.RED),
    onPrimary = Color(Light.ON_RED),
    primaryContainer = Color(Light.RED_CONTAINER),
    onPrimaryContainer = Color(Light.ON_RED_CONTAINER),
    secondary = Color(Light.TEAL),
    onSecondary = Color(Light.ON_TEAL),
    secondaryContainer = Color(Light.TEAL_CONTAINER),
    onSecondaryContainer = Color(Light.ON_TEAL_CONTAINER),
    tertiary = Color(Light.OCHRE),
    onTertiary = Color(Light.ON_OCHRE),
    tertiaryContainer = Color(Light.OCHRE_CONTAINER),
    onTertiaryContainer = Color(Light.ON_OCHRE_CONTAINER),
    background = Color(Light.BACKGROUND),
    onBackground = Color(Light.INK),
    surface = Color(Light.BACKGROUND),
    onSurface = Color(Light.INK),
    surfaceVariant = Color(Light.SURFACE_VARIANT),
    onSurfaceVariant = Color(Light.MUTED),
    outline = Color(Light.OUTLINE),
    outlineVariant = Color(Light.OUTLINE_FAINT),
    surfaceDim = Color(Light.SURFACE_DIM),
    surfaceBright = Color(Light.CARD),
    surfaceContainerLowest = Color(Light.SURFACE_LOWEST),
    surfaceContainerLow = Color(Light.CARD),
    surfaceContainer = Color(Light.SURFACE_CONTAINER),
    surfaceContainerHigh = Color(Light.SURFACE_HIGH),
    surfaceContainerHighest = Color(Light.SURFACE_HIGHEST),
    error = Color(Light.ERROR),
)

private val DarkColors = darkColorScheme(
    primary = Color(Dark.RED),
    onPrimary = Color(Dark.ON_RED),
    primaryContainer = Color(Dark.RED_CONTAINER),
    onPrimaryContainer = Color(Dark.ON_RED_CONTAINER),
    secondary = Color(Dark.TEAL),
    onSecondary = Color(Dark.ON_TEAL),
    secondaryContainer = Color(Dark.TEAL_CONTAINER),
    onSecondaryContainer = Color(Dark.ON_TEAL_CONTAINER),
    tertiary = Color(Dark.OCHRE),
    onTertiary = Color(Dark.ON_OCHRE),
    tertiaryContainer = Color(Dark.OCHRE_CONTAINER),
    onTertiaryContainer = Color(Dark.ON_OCHRE_CONTAINER),
    background = Color(Dark.BACKGROUND),
    onBackground = Color(Dark.INK),
    surface = Color(Dark.BACKGROUND),
    onSurface = Color(Dark.INK),
    surfaceVariant = Color(Dark.SURFACE_VARIANT),
    onSurfaceVariant = Color(Dark.MUTED),
    outline = Color(Dark.OUTLINE),
    outlineVariant = Color(Dark.OUTLINE_FAINT),
    surfaceDim = Color(Dark.SURFACE_DIM),
    surfaceBright = Color(Dark.SURFACE_HIGHEST),
    surfaceContainerLowest = Color(Dark.SURFACE_LOWEST),
    surfaceContainerLow = Color(Dark.CARD),
    surfaceContainer = Color(Dark.SURFACE_CONTAINER),
    surfaceContainerHigh = Color(Dark.SURFACE_HIGH),
    surfaceContainerHighest = Color(Dark.SURFACE_HIGHEST),
    error = Color(Dark.ERROR),
)

// Headings in the phone's serif, like the printed head of a recipe card; body text stays sans.
private val CookbookTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = FontFamily.Serif),
        displayMedium = base.displayMedium.copy(fontFamily = FontFamily.Serif),
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif),
        headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Serif),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif),
        titleMedium = base.titleMedium.copy(fontFamily = FontFamily.Serif),
    )
}

// Corners a little tighter than Material's, like cut card stock.
private val CookbookShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CookbookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = CookbookTypography,
        shapes = CookbookShapes,
        content = content,
    )
}

/** Top bars are part of the box, not a coloured band: same colour as the background, ink text. */
@Composable
fun appBarColors(): TopAppBarColors {
    val scheme = MaterialTheme.colorScheme
    return TopAppBarDefaults.topAppBarColors(
        containerColor = scheme.background,
        scrolledContainerColor = scheme.background,
        navigationIconContentColor = scheme.onBackground,
        titleContentColor = scheme.onBackground,
        actionIconContentColor = scheme.onBackground,
    )
}
