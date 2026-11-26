package com.cookbookanything.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val STAMP_RAISED = 1.6f

/**
 * A rubber stamp on the page: capitals in a frame, a little askew, in the family red. [text] comes in
 * capitals from the strings, so Greek is written without accents. A [fresh] stamp is pressed on: it
 * comes down onto the page with a tap the hand can feel, then tells [onPressed] so it is inked once.
 */
@Composable
fun Stamp(text: String, modifier: Modifier = Modifier, tilt: Float = -4f, fresh: Boolean = false, onPressed: () -> Unit = {}) {
    val ink = MaterialTheme.colorScheme.primary
    val haptics = LocalHapticFeedback.current
    val press = remember { Animatable(if (fresh) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!fresh) return@LaunchedEffect
        haptics.performHapticFeedback(Touch.Done)
        press.animateTo(1f, PaperSpring)
        onPressed()
    }
    Text(
        text = text,
        color = ink,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
        maxLines = 1,
        modifier = modifier
            .graphicsLayer {
                val scale = STAMP_RAISED - (STAMP_RAISED - 1f) * press.value
                scaleX = scale
                scaleY = scale
                alpha = press.value.coerceIn(0f, 1f)
                rotationZ = tilt
            }
            .border(1.5.dp, ink, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private val TabShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)

/**
 * A divider tab of a cookbook: round at the top, square where it meets the page. The open section's
 * tab stands taller and is inked, so it is told apart by shape as well as by colour. Set a row of
 * them on a rule in the ink colour.
 */
@Composable
fun IndexTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val height by animateDpAsState(if (selected) 40.dp else 34.dp, spring(dampingRatio = 0.75f, stiffness = 400f), label = "tab")
    // 48 dp to touch, of which the tab itself fills the lower part.
    Box(
        modifier = modifier
            .height(48.dp)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .height(height)
                .clip(TabShape)
                .background(if (selected) scheme.onSurface else scheme.surfaceContainerHighest)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) scheme.surface else scheme.onSurface,
                maxLines = 1,
            )
        }
    }
}
