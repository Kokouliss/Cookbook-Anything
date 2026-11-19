@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cookbookanything.app.R
import com.cookbookanything.core.domain.design.Palette.Dark
import com.cookbookanything.core.domain.design.Palette.Light

// A printed cookbook: paper, ink, and the family red as the one lead colour for buttons, links and
// selections (secondaryContainer carries selected chips and tonal buttons, so it is red too). Fixed
// colours on purpose, so the app keeps its own look instead of following the wallpaper. The values
// and their contrast test live in core/domain.
private val LightColors = lightColorScheme(
    primary = Color(Light.RED),
    onPrimary = Color(Light.ON_RED),
    primaryContainer = Color(Light.RED_CONTAINER),
    onPrimaryContainer = Color(Light.ON_RED_CONTAINER),
    secondary = Color(Light.TEAL),
    onSecondary = Color(Light.ON_TEAL),
    secondaryContainer = Color(Light.RED_CONTAINER),
    onSecondaryContainer = Color(Light.ON_RED_CONTAINER),
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
    secondaryContainer = Color(Dark.RED_CONTAINER),
    onSecondaryContainer = Color(Dark.ON_RED_CONTAINER),
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

// Literata, the book serif Google made for Play Books (SIL Open Font License, Greek included), for
// every heading and title, cut at the optical size of a title. Body text stays in the phone's sans.
private val Literata = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(
            resId = R.font.literata,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight), FontVariation.Setting("opsz", 24f)),
        )
    },
)

private val CookbookTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = Literata),
        displayMedium = base.displayMedium.copy(fontFamily = Literata),
        displaySmall = base.displaySmall.copy(fontFamily = Literata),
        headlineLarge = base.headlineLarge.copy(fontFamily = Literata),
        headlineMedium = base.headlineMedium.copy(fontFamily = Literata),
        headlineSmall = base.headlineSmall.copy(fontFamily = Literata),
        titleLarge = base.titleLarge.copy(fontFamily = Literata),
        titleMedium = base.titleMedium.copy(fontFamily = Literata),
    )
}

// Corners nearer to square than Material's, like a printed page and its photos.
private val CookbookShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
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

/** Top bars are part of the page, not a coloured band: same colour as the background, ink text. */
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
