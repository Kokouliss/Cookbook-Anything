package com.cookbookanything.app.ui.components

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cookbookanything.app.R
import com.cookbookanything.app.data.photos.photoDirectory
import java.io.File
import java.util.Locale

/** The language the screen is shown in, which can differ from the phone's when picked in settings. */
@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun AppIcon(@DrawableRes icon: Int, contentDescription: String?, modifier: Modifier = Modifier) {
    Icon(painter = painterResource(icon), contentDescription = contentDescription, modifier = modifier)
}

/**
 * An icon button. With a [label], the word sits under the icon so the button says what it does, and
 * screen readers read that word; without one only standard icons (back, close, plus) should be used.
 */
@Composable
fun AppIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    if (label == null) {
        IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            AppIcon(icon = icon, contentDescription = contentDescription)
        }
        return
    }
    val color = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = DISABLED_ALPHA)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .widthIn(min = 48.dp)
            .heightIn(min = 48.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private const val DISABLED_ALPHA = 0.38f

/** A stored recipe photo, looked up by file name. */
@Composable
fun RecipePhoto(
    name: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    AsyncImage(
        // A photo fades in once decoded instead of popping into a blank space.
        model = ImageRequest.Builder(context).data(File(photoDirectory(context), name)).crossfade(true).build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }, enabled = value.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** A line that can be ticked off: an ingredient the cook has, or a step that is done. */
@Composable
fun CheckRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            // The whole row is the checkbox: one thing to touch, read out with its state.
            .toggleable(value = checked, role = Role.Checkbox) {
                haptics.toggle(it)
                onCheckedChange(it)
            }
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
        Column(modifier = Modifier.padding(end = 16.dp, top = 8.dp, bottom = 8.dp)) {
            StruckText(
                text = text,
                struck = checked,
                style = MaterialTheme.typography.bodyLarge,
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private const val STROKE_MILLIS = 200

/** Text that a pen strikes through when [struck], from left to right, and clears again when not. */
@Composable
fun StruckText(text: String, struck: Boolean, style: TextStyle, color: Color) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val stroke by animateFloatAsState(if (struck) 1f else 0f, tween(STROKE_MILLIS), label = "strike")
    Text(
        text,
        style = style,
        color = color,
        onTextLayout = { layout = it },
        modifier = Modifier.drawWithContent {
            drawContent()
            val lines = layout ?: return@drawWithContent
            if (stroke == 0f) return@drawWithContent
            // One stroke per line of text, each drawn as far as the pen has got.
            for (line in 0 until lines.lineCount) {
                val y = (lines.getLineTop(line) + lines.getLineBottom(line)) / 2
                val from = lines.getLineLeft(line)
                val to = from + (lines.getLineRight(line) - from) * stroke
                drawLine(color, Offset(from, y), Offset(to, y), 1.5.dp.toPx(), StrokeCap.Round)
            }
        },
    )
}

/** Set around the screens, so a photo on one screen can grow into the same photo on the next. */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** The recipe's cover photo: on the list and on the recipe page it is one photo that moves between them. */
@Composable
fun Modifier.sharedPhoto(name: String): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val screen = LocalNavAnimatedContentScope.current
    return with(shared) { this@sharedPhoto.sharedElement(rememberSharedContentState("photo-$name"), screen) }
}

private const val PRESSED_SCALE = 0.97f

/** The app's one spring: quick, with a small overshoot, like paper put down on a table. */
val PaperSpring = spring<Float>(dampingRatio = 0.75f, stiffness = 400f)

/** Shrinks what is pressed a little while the finger is down, like a key going in, and springs back. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) PRESSED_SCALE else 1f, PaperSpring, label = "press")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** A page of the book: white paper with a hairline edge, no shadow. */
@Composable
fun PageCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val edge = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Card(onClick = onClick, modifier = modifier.pressScale(interaction), colors = colors, border = edge, interactionSource = interaction, content = content)
    } else {
        Card(modifier = modifier, colors = colors, border = edge, content = content)
    }
}

/** The wordmark of AnyKind, the collection this app belongs to, from Panos's logo files. */
@Composable
fun AnyKindWordmark(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.logo_anykind_wordmark), contentDescription = "AnyKind", modifier = modifier.height(32.dp))
}
