package com.cookbookanything.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A small pen drawing for a card or page with nothing on it yet. Drawn on a square of 96: [ink] is
 * the pen, [accent] the one stroke in the family red.
 */
enum class Doodle(internal val ink: String, internal val accent: String) {
    Pot(
        "M22 40H74V70A8 8 0 0 1 66 78H30A8 8 0 0 1 22 70ZM22 48H12M74 48H84M26 40C26 32 70 32 70 40",
        "M44 30H52M40 24C38 20 42 16 40 12M56 24C54 20 58 16 56 12",
    ),
    Whisk(
        "M48 86V54M48 54C34 54 28 40 30 26C32 12 44 10 48 10C52 10 64 12 66 26C68 40 62 54 48 54" +
            "M48 54C40 54 38 40 40 26C41 16 46 10 48 10M48 54C56 54 58 40 56 26C55 16 50 10 48 10",
        "M44 62H52M44 70H52",
    ),
    Basket(
        "M30 44C30 16 66 16 66 44M16 44H80M22 44L28 78H68L74 44M38 54V68M48 54V68M58 54V68",
        "M52 40C52 30 58 24 68 22C70 32 64 40 56 42",
    ),
    Calendar(
        "M18 26H78V80H18ZM18 40H78M30 18V32M66 18V32",
        "M40 60a8 8 0 1 1 16 0a8 8 0 1 1 -16 0",
    ),
}

private const val DOODLE_SIZE = 96f

/** Decoration: the text next to it says what the page is. */
@Composable
fun DoodleImage(doodle: Doodle, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val (inkPath, accentPath) = remember(doodle) {
        PathParser().parsePathString(doodle.ink).toPath() to PathParser().parsePathString(doodle.accent).toPath()
    }
    Canvas(modifier.size(DOODLE_SIZE.dp)) {
        scale(size.minDimension / DOODLE_SIZE, pivot = Offset.Zero) {
            val pen = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(inkPath, ink, style = pen)
            drawPath(accentPath, accent, style = pen)
        }
    }
}

/** A doodle over a line of muted text, for a screen with nothing to list. */
@Composable
fun EmptyState(doodle: Doodle, text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        DoodleImage(doodle, modifier = Modifier.padding(bottom = 16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
