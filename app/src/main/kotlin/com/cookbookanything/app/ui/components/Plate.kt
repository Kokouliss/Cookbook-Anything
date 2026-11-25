package com.cookbookanything.app.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The plate of Panos's cookbook icon with its three ingredients (the AnyKind circle, square and
 * triangle), drawn on a square of 100 units: the plate stays still, the ingredients move. Each
 * ingredient's place is a [Pose]; screens animate the poses and the scene only redraws.
 */
class Pose(val x: Float, val y: Float, val turn: Float, val scale: Float = 1f, val alpha: Float = 1f, val behind: Boolean = false)

/** Where the ingredients sit once they are on the plate: side by side on its rim, tilted as in the logo. */
val restingPoses = listOf(Pose(28.5f, 44.5f, 0f), Pose(49.5f, 43.6f, 15f), Pose(70.5f, 44.2f, -15f))

/** Where the ingredients are while they circle the plate, at [phase] radians round. */
fun circlingPoses(phase: Float): List<Pose> = List(INGREDIENTS) { i ->
    val angle = phase + i * 2f * PI.toFloat() / INGREDIENTS
    val depth = sin(angle)
    // A flat ellipse round the plate: nearer ingredients are lower, bigger and drawn over the plate.
    Pose(
        x = 50f + 44f * cos(angle),
        y = 54f + 12f * depth,
        turn = restingPoses[i].turn + angle * 57.3f / 3f,
        scale = 0.85f + 0.15f * depth,
        behind = depth < 0f,
    )
}

/** An ingredient dropped from [height] units above its resting place, [t] from 0 to 1 of the fall. */
fun fallingPose(i: Int, t: Float, height: Float = 130f): Pose {
    val rest = restingPoses[i]
    val y = EaseOutBounce.transform(t)
    return Pose(rest.x, rest.y - height * (1f - y), rest.turn - 90f * (1f - FastOutSlowInEasing.transform(t)), alpha = (t * 4f).coerceAtMost(1f))
}

/** An ingredient tossed from [from] up over its resting place and dropped onto the plate, [t] from 0 to 1. */
fun tossedPose(i: Int, from: Pose, t: Float): Pose {
    val rest = restingPoses[i]
    val up = FastOutSlowInEasing.transform((t / TOSS_RISE).coerceAtMost(1f))
    val down = EaseOutBounce.transform(((t - TOSS_RISE) / (1f - TOSS_RISE)).coerceIn(0f, 1f))
    val top = rest.y - TOSS_HEIGHT
    return Pose(
        x = from.x + (rest.x - from.x) * up,
        y = if (t < TOSS_RISE) from.y + (top - from.y) * up else top + (rest.y - top) * down,
        turn = from.turn + (rest.turn - from.turn) * t,
        scale = from.scale + (1f - from.scale) * up,
    )
}

private const val INGREDIENTS = 3
private const val TOSS_RISE = 0.35f
private const val TOSS_HEIGHT = 22f

/** The plate and the ingredients at [poses], read while drawing so a moving pose never recomposes. */
@Composable
fun PlateScene(poses: () -> List<Pose>, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val plate = MaterialTheme.colorScheme.onBackground
    val colors = if (dark) {
        listOf(Color(Palette.Dark.KIND_CIRCLE), Color(Palette.Dark.KIND_SQUARE), Color(Palette.Dark.KIND_TRIANGLE))
    } else {
        listOf(Color(Palette.Light.KIND_CIRCLE), Color(Palette.Light.KIND_SQUARE), Color(Palette.Light.KIND_TRIANGLE))
    }
    val triangle = remember { equilateral() }
    // Decoration: the words next to it say what is happening.
    Canvas(modifier.clearAndSetSemantics {}) {
        val unit = size.minDimension / 100f
        val current = poses()
        current.forEachIndexed { i, pose -> if (pose.behind) drawIngredient(i, pose, colors[i], unit, triangle) }
        // The plate of the logo: a half disc under a flat rim at 54, 68 units across.
        drawArc(plate, 0f, 180f, useCenter = true, topLeft = Offset(16f * unit, 20f * unit), size = Size(68f * unit, 68f * unit))
        current.forEachIndexed { i, pose -> if (!pose.behind) drawIngredient(i, pose, colors[i], unit, triangle) }
    }
}

private fun DrawScope.drawIngredient(i: Int, pose: Pose, color: Color, unit: Float, triangle: Path) {
    withTransform({
        translate(pose.x * unit, pose.y * unit)
        rotate(pose.turn, pivot = Offset.Zero)
        scale(pose.scale * unit, pose.scale * unit, pivot = Offset.Zero)
    }) {
        when (i) {
            0 -> drawCircle(color, radius = 9.5f, center = Offset.Zero, alpha = pose.alpha)
            1 -> drawRoundRect(color, Offset(-8.5f, -8.5f), Size(17f, 17f), CornerRadius(2f), alpha = pose.alpha)
            else -> drawPath(triangle, color, alpha = pose.alpha)
        }
    }
}

/** The logo's triangle, 24 units a side, around its centre. */
private fun equilateral() = Path().apply {
    moveTo(0f, -13.87f)
    lineTo(12f, 6.93f)
    lineTo(-12f, 6.93f)
    close()
}

/** The phone's animation speed, 0 when the owner switched animations off in the accessibility settings. */
fun animationScale(context: Context): Float =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

private const val FALL_MILLIS = 650
private const val FALL_STAGGER_MILLIS = 130L
private const val FALL_START_MILLIS = 120L
private const val SETTLE_MILLIS = 150L
private const val FADE_MILLIS = 250

/**
 * The opening screen: the plate on the page and the three ingredients falling onto it one after
 * another, then the app shows through. About a second; a tap skips it.
 */
@Composable
fun Preloader(onFinished: () -> Unit) {
    val falls = remember { List(INGREDIENTS) { Animatable(0f) } }
    val appear = remember { Animatable(0f) }
    val leave = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { appear.animateTo(1f, tween(FADE_MILLIS)) }
        falls.forEachIndexed { i, fall ->
            launch {
                delay(FALL_START_MILLIS + i * FALL_STAGGER_MILLIS)
                fall.animateTo(1f, tween(FALL_MILLIS))
            }
        }
        delay(FALL_START_MILLIS + (INGREDIENTS - 1) * FALL_STAGGER_MILLIS + FALL_MILLIS + SETTLE_MILLIS)
        leave.animateTo(1f, tween(FADE_MILLIS))
        onFinished()
    }
    // The page is opaque from the first frame, so the app never shows through before the plate.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - leave.value }
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { awaitPointerEventScope { awaitPointerEvent(); onFinished() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.graphicsLayer { alpha = appear.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlateScene(poses = { List(INGREDIENTS) { fallingPose(it, falls[it].value) } }, modifier = Modifier.size(200.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = RecipeNameFont),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
