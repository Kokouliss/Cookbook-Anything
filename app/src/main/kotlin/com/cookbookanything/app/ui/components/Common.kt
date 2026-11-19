package com.cookbookanything.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
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

@Composable
fun AppIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        AppIcon(icon = icon, contentDescription = contentDescription)
    }
}

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
        model = File(photoDirectory(context), name),
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            // Teal for "have it" and "done", so progress reads at a glance.
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.secondary,
                checkmarkColor = MaterialTheme.colorScheme.onSecondary,
            ),
        )
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

private const val STROKE_MILLIS = 350

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

private val RuledLineGap = 32.dp
private const val RULED_LINE_ALPHA = 0.08f

/** Faint ruled lines, like an index card's. Decoration only, well behind whatever is written on top. */
fun Modifier.ruledLines(line: Color): Modifier = drawBehind {
    val gap = RuledLineGap.toPx()
    var y = gap
    while (y < size.height) {
        drawLine(line.copy(alpha = RULED_LINE_ALPHA), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        y += gap
    }
}

/** An index card: card stock with the family red rule across its top. */
@Composable
fun IndexCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    ruled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val body: @Composable ColumnScope.() -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
        Column(modifier = if (ruled) Modifier.ruledLines(MaterialTheme.colorScheme.onSurface) else Modifier, content = content)
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors, content = body)
    } else {
        Card(modifier = modifier, colors = colors, content = body)
    }
}

/** The logo of AnyKind, the collection this app belongs to. Its letters take the colour of the text around it. */
@Composable
fun AnyKindWordmark(modifier: Modifier = Modifier) {
    Box(modifier) {
        Image(painterResource(R.drawable.logo_anykind_mark), contentDescription = "AnyKind")
        Image(
            painter = painterResource(R.drawable.logo_anykind_letters),
            contentDescription = null,
            colorFilter = ColorFilter.tint(LocalContentColor.current),
        )
    }
}
