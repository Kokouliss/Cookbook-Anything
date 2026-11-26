package com.cookbookanything.app.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.cookbookanything.app.R
import com.cookbookanything.app.ui.theme.RecipeNameFont
import com.cookbookanything.core.domain.design.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The bowl of Panos's cookbook icon with the six AnyKind pieces heaped over it (his six-app kit of
 * 25 November 2026), drawn on a square of 100 units: the bowl stays still, the pieces move. Each
 * piece's place is a [Pose]; screens animate the poses and the scene only redraws.
 */
class Pose(val x: Float, val y: Float, val turn: Float, val scale: Float = 1f, val alpha: Float = 1f, val behind: Boolean = false)

/** Where the pieces end: heaped in the air over the bowl, each where the icon has it, the bottom row first. */
val restingPoses = listOf(
    Pose(27f, 42f, 0f), // circle
    Pose(50f, 43f, 0f), // pill
    Pose(73f, 42f, 0f), // plus
    Pose(38.5f, 25.5f, 15f), // square
    Pose(61.5f, 25.5f, -15f), // triangle
    Pose(50f, 9f, 0f), // diamond
)

/** Slows into place and drifts a touch past it before coming back, like something floating, with no hard stop. */
private val Settle = CubicBezierEasing(0.34f, 1.28f, 0.64f, 1f)

/** Where the ingredients are while they circle the plate, at [phase] radians round. */
fun circlingPoses(phase: Float): List<Pose> = List(PIECES) { i ->
    val angle = phase + i * 2f * PI.toFloat() / PIECES
    val depth = sin(angle)
    // A flat ellipse round the plate: nearer ingredients are lower, bigger and drawn over the plate.
    Pose(
        x = 50f + 44f * cos(angle),
        y = RIM + 12f * depth,
        turn = restingPoses[i].turn + angle * 57.3f / 3f,
        scale = 0.85f + 0.15f * depth,
        behind = depth < 0f,
    )
}

/** An ingredient drifting down from [height] units above its place in the air, [t] from 0 to 1 (linear time). */
fun fallingPose(i: Int, t: Float, height: Float = FALL_HEIGHT): Pose {
    val rest = restingPoses[i]
    val left = 1f - Settle.transform(t)
    return Pose(rest.x, rest.y - height * left, rest.turn - FALL_TURN * left, alpha = (t * 3f).coerceAtMost(1f))
}

/** An ingredient gliding from [from] to its place in the air, [t] from 0 to 1 (linear time). */
fun settlingPose(i: Int, from: Pose, t: Float): Pose {
    val rest = restingPoses[i]
    val done = Settle.transform(t)
    // The short way round, however many turns the ingredient made while circling.
    val turn = ((rest.turn - from.turn) % 360f + 540f) % 360f - 180f
    return Pose(
        x = from.x + (rest.x - from.x) * done,
        y = from.y + (rest.y - from.y) * done,
        turn = from.turn + turn * done,
        scale = from.scale + (1f - from.scale) * done,
        behind = from.behind && t < 0.25f,
    )
}

private val PIECES = restingPoses.size
private const val RIM = 57f
private const val FALL_HEIGHT = 60f
private const val FALL_TURN = 40f

/** The plate and the ingredients at [poses], read while drawing so a moving pose never recomposes. */
@Composable
fun PlateScene(poses: () -> List<Pose>, modifier: Modifier = Modifier) {
    val plate = MaterialTheme.colorScheme.onBackground
    val colors = remember { Palette.PIECES.map { Color(it) } }
    val outlines = remember { pieceOutlines() }
    // Decoration: the words next to it say what is happening.
    Canvas(modifier.clearAndSetSemantics {}) {
        val unit = size.minDimension / 100f
        val current = poses()
        current.forEachIndexed { i, pose -> if (pose.behind) drawPiece(i, pose, colors[i], unit, outlines) }
        // The bowl of the icon: a half disc under a flat rim at 57, 68 units across.
        drawArc(plate, 0f, 180f, useCenter = true, topLeft = Offset(16f * unit, (RIM - 34f) * unit), size = Size(68f * unit, 68f * unit))
        current.forEachIndexed { i, pose -> if (!pose.behind) drawPiece(i, pose, colors[i], unit, outlines) }
    }
}

private fun DrawScope.drawPiece(i: Int, pose: Pose, color: Color, unit: Float, outlines: Map<Int, Path>) {
    withTransform({
        translate(pose.x * unit, pose.y * unit)
        rotate(pose.turn, pivot = Offset.Zero)
        scale(pose.scale * unit, pose.scale * unit, pivot = Offset.Zero)
    }) {
        // The pieces' sizes as in cookbook-icon.svg.
        when (i) {
            0 -> drawCircle(color, radius = 7.56f, center = Offset.Zero, alpha = pose.alpha)
            1 -> drawRoundRect(color, Offset(-8.68f, -3.78f), Size(17.36f, 7.56f), CornerRadius(3.78f), alpha = pose.alpha)
            3 -> drawRoundRect(color, Offset(-6.3f, -6.3f), Size(12.6f, 12.6f), CornerRadius(1.4f), alpha = pose.alpha)
            else -> drawPath(outlines.getValue(i), color, alpha = pose.alpha)
        }
    }
}

/** The pieces drawn as outlines, around their centres: the plus, the triangle and the diamond. */
private fun pieceOutlines() = mapOf(
    2 to polygon(-2.52f, -7.84f, 2.52f, -7.84f, 2.52f, -2.52f, 7.84f, -2.52f, 7.84f, 2.52f, 2.52f, 2.52f, 2.52f, 7.84f, -2.52f, 7.84f, -2.52f, 2.52f, -7.84f, 2.52f, -7.84f, -2.52f, -2.52f, -2.52f),
    4 to polygon(0f, -8.68f, 8.4f, 5.88f, -8.4f, 5.88f),
    5 to polygon(0f, -8.96f, 7.28f, 0f, 0f, 8.96f, -7.28f, 0f),
)

private fun polygon(vararg xy: Float) = Path().apply {
    moveTo(xy[0], xy[1])
    for (k in 2 until xy.size step 2) lineTo(xy[k], xy[k + 1])
    close()
}

/** The phone's animation speed, 0 when the owner switched animations off in the accessibility settings. */
fun animationScale(context: Context): Float =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

private const val FALL_MILLIS = 850
private const val FALL_STAGGER_MILLIS = 90L
private const val FALL_START_MILLIS = 200L
private const val HOLD_MILLIS = 350L
private const val FADE_MILLIS = 250

/**
 * The opening screen: the bowl on the page and the six pieces drifting down one after another into
 * the icon's heap above it, bottom row first, then the app shows through. About 2.1 s, always to the
 * end: the clock starts at the first frame on screen and waits for every piece to land, however
 * long the app took to start. [onSettled] comes when the pieces hang still, the moment to build the
 * app underneath.
 */
@Composable
fun Preloader(onSettled: () -> Unit, onFinished: () -> Unit) {
    val falls = remember { List(PIECES) { Animatable(0f) } }
    val appear = remember { Animatable(0f) }
    val leave = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        launch { appear.animateTo(1f, tween(FADE_MILLIS)) }
        coroutineScope {
            falls.forEachIndexed { i, fall ->
                // Linear here: fallingPose gives the motion its easing. The wait is part of the animation, so it runs on frames too.
                val wait = (FALL_START_MILLIS + i * FALL_STAGGER_MILLIS).toInt()
                launch { fall.animateTo(1f, tween(FALL_MILLIS, delayMillis = wait, easing = LinearEasing)) }
            }
        }
        onSettled()
        delay(HOLD_MILLIS)
        leave.animateTo(1f, tween(FADE_MILLIS))
        onFinished()
    }
    // The page is opaque from the first frame, so the app never shows through before the plate.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - leave.value }
            .background(MaterialTheme.colorScheme.background)
            // Touches stop here: the opening always plays to the end.
            .pointerInput(Unit) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.graphicsLayer { alpha = appear.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlateScene(poses = { List(PIECES) { fallingPose(it, falls[it].value) } }, modifier = Modifier.size(200.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = RecipeNameFont),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
