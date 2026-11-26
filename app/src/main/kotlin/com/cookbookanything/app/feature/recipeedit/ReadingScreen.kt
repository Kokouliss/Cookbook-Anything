package com.cookbookanything.app.feature.recipeedit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cookbookanything.app.R
import com.cookbookanything.app.ui.components.PlateScene
import com.cookbookanything.app.ui.components.Pose
import com.cookbookanything.app.ui.components.animationScale
import com.cookbookanything.app.ui.components.circlingPoses
import com.cookbookanything.app.ui.components.restingPoses
import com.cookbookanything.app.ui.components.settlingPose
import com.cookbookanything.app.ui.theme.RecipeNameFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FADE_MILLIS = 220
private const val LAND_MILLIS = 900
// shortcut: the stages are paced by the clock, not by the reader's real steps, as Panos allowed; report real steps if a stage ever looks wrong.
private const val STAGE_MILLIS = 1_700L
private const val CIRCLE_SECONDS = 2.6f

/**
 * The page over the editor while a link or a photo is read: the ingredients circle the plate and the
 * stages tick off one by one. When the read ends they drop onto the plate and the page fades into the
 * filled form. Pasted text, read in a moment, keeps the thin progress line instead.
 */
@Composable
fun ReadingScreen(kind: ReadKind?, onCancel: () -> Unit) {
    // Kept after the read ends, so the ingredients can land before the page goes.
    var shown by remember { mutableStateOf<ReadKind?>(null) }
    LaunchedEffect(kind) { if (kind != null && kind != ReadKind.TEXT) shown = kind }
    val showing = shown ?: return
    ReadingPage(kind = showing, finished = kind == null, onCancel = onCancel, onGone = { shown = null })
}

@Composable
private fun ReadingPage(kind: ReadKind, finished: Boolean, onCancel: () -> Unit, onGone: () -> Unit) {
    val context = LocalContext.current
    val speed = remember { animationScale(context) }
    val stages = stringArrayResource(if (kind == ReadKind.PHOTO) R.array.reading_photo_stages else R.array.reading_link_stages)
    val shown = remember { Animatable(0f) }
    val land = remember { Animatable(0f) }
    var phase by remember { mutableFloatStateOf(0f) }
    var landingFrom by remember { mutableStateOf<List<Pose>?>(null) }
    var stage by remember { mutableIntStateOf(0) }

    LaunchedEffect(finished) {
        if (!finished) {
            landingFrom = null
            stage = 0
            launch { shown.animateTo(1f, tween(FADE_MILLIS)) }
            launch {
                while (stage < stages.lastIndex) {
                    delay(STAGE_MILLIS)
                    stage++
                }
            }
            // The ingredients circle the plate at the phone's animation speed, and stay put when animations are off.
            if (speed > 0f) {
                var last = withFrameNanos { it }
                while (true) {
                    withFrameNanos { now ->
                        phase += (now - last) / 1e9f * (2f * Math.PI.toFloat() / CIRCLE_SECONDS) / speed
                        last = now
                    }
                }
            }
        } else {
            stage = stages.size
            landingFrom = circlingPoses(phase)
            land.snapTo(0f)
            land.animateTo(1f, tween(LAND_MILLIS, easing = LinearEasing))
            shown.animateTo(0f, tween(FADE_MILLIS))
            onGone()
        }
    }

    BackHandler(enabled = !finished, onBack = onCancel)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = shown.value }
            .background(MaterialTheme.colorScheme.background)
            // The form underneath is being filled in, so it takes no touches meanwhile.
            .pointerInput(Unit) {}
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlateScene(
                poses = {
                    // Each piece leaves the circle a moment after the one before it; the last one lands as the glide ends.
                    landingFrom?.let { from -> from.indices.map { i -> settlingPose(i, from[i], (land.value * 1.3f - i * 0.3f / from.lastIndex).coerceIn(0f, 1f)) } }
                        ?: if (speed > 0f) circlingPoses(phase) else restingPoses
                },
                modifier = Modifier.size(180.dp),
            )
            val title = stringResource(R.string.reading_title)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = RecipeNameFont),
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 16.dp)
                    // Read out again each time a stage begins.
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = listOfNotNull(title, stages.getOrNull(stage)).joinToString(". ")
                    },
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                stages.forEachIndexed { i, text ->
                    StageRow(text = text, status = (i - stage).coerceIn(-1, 1))
                }
            }
        }
        if (!finished) {
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.BottomCenter)) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

/** One stage: ticked when [status] is below 0, under way at 0, still to come above 0. */
@Composable
private fun StageRow(text: String, status: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = status,
                transitionSpec = { (fadeIn(tween(FADE_MILLIS)) + scaleIn(tween(FADE_MILLIS), initialScale = 0.6f)) togetherWith fadeOut(tween(FADE_MILLIS)) },
                label = "stage",
            ) { now ->
                when {
                    now < 0 -> Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    now == 0 -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else -> Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outline, CircleShape))
                }
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (status == 0) FontWeight.Medium else FontWeight.Normal,
            color = if (status == 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
