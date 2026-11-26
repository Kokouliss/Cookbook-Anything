@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

package com.cookbookanything.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cookbookanything.app.R
import com.cookbookanything.core.domain.design.Palette.Dark
import com.cookbookanything.core.domain.design.Palette.Light

// White and black with red accents: neutral pages and greys, and the family red only where
// Material draws the primary colour (the main button, ticks, switches, focused fields). Containers
// (selected chips, tonal buttons) are grey. Fixed colours on purpose, so the app keeps its own look
// instead of following the wallpaper. The values and their contrast test live in core/domain.
private val LightColors = lightColorScheme(
    primary = Color(Light.RED),
    onPrimary = Color(Light.ON_RED),
    primaryContainer = Color(Light.CONTAINER),
    onPrimaryContainer = Color(Light.ON_CONTAINER),
    secondary = Color(Light.MUTED),
    onSecondary = Color(Light.BACKGROUND),
    secondaryContainer = Color(Light.CONTAINER),
    onSecondaryContainer = Color(Light.ON_CONTAINER),
    tertiary = Color(Light.MUTED),
    onTertiary = Color(Light.BACKGROUND),
    tertiaryContainer = Color(Light.CONTAINER),
    onTertiaryContainer = Color(Light.ON_CONTAINER),
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
    primaryContainer = Color(Dark.CONTAINER),
    onPrimaryContainer = Color(Dark.ON_CONTAINER),
    secondary = Color(Dark.MUTED),
    onSecondary = Color(Dark.BACKGROUND),
    secondaryContainer = Color(Dark.CONTAINER),
    onSecondaryContainer = Color(Dark.ON_CONTAINER),
    tertiary = Color(Dark.MUTED),
    onTertiary = Color(Dark.BACKGROUND),
    tertiaryContainer = Color(Dark.CONTAINER),
    onTertiaryContainer = Color(Dark.ON_CONTAINER),
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

// Literata, the book serif Google made for Play Books (SIL Open Font License, Greek included), cut at
// the optical size of a title. It sets everything from 20 sp up: recipe names, screen titles, headings.
val RecipeNameFont = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(
            resId = R.font.literata,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight), FontVariation.Setting("opsz", 24f)),
        )
    },
)

// Inter (SIL Open Font License, Greek included) for everything smaller. Bundled, so the text looks the
// same on every phone instead of following the maker's system font.
private val TextFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Digits of one width, so a running timer or a changing count does not wobble. */
const val TABULAR_NUMBERS = "tnum"

// One scale for the family: serif from 20 sp up, sans below, line heights that leave room for Greek accents.
private val CookbookTypography = Typography().run {
    fun TextStyle.serif(size: Int, line: Int) =
        copy(fontFamily = RecipeNameFont, fontWeight = FontWeight.Medium, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp)
    fun TextStyle.sans(size: Int, line: Int, weight: FontWeight) =
        copy(fontFamily = TextFont, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp)
    copy(
        displayLarge = displayLarge.copy(fontFamily = RecipeNameFont),
        displayMedium = displayMedium.copy(fontFamily = RecipeNameFont),
        displaySmall = displaySmall.serif(36, 44),
        headlineLarge = headlineLarge.serif(32, 40),
        headlineMedium = headlineMedium.serif(28, 36),
        headlineSmall = headlineSmall.serif(24, 32),
        titleLarge = titleLarge.serif(22, 28),
        titleMedium = titleMedium.sans(17, 24, FontWeight.SemiBold),
        titleSmall = titleSmall.sans(14, 20, FontWeight.SemiBold),
        bodyLarge = bodyLarge.sans(16, 24, FontWeight.Normal),
        bodyMedium = bodyMedium.sans(14, 20, FontWeight.Normal),
        bodySmall = bodySmall.sans(12, 16, FontWeight.Normal),
        labelLarge = labelLarge.sans(14, 20, FontWeight.Medium),
        labelMedium = labelMedium.sans(12, 16, FontWeight.Medium),
        labelSmall = labelSmall.sans(11, 16, FontWeight.Medium),
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
        shapes = CookbookShapes,
        typography = CookbookTypography,
        // Springs with a little life in them for what moves; colour and fades stay without bounce.
        motionScheme = MotionScheme.expressive(),
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
