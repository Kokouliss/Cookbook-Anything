@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.recipedetail

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cookbookanything.app.R
import com.cookbookanything.app.data.share.PdfLabels
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.components.ConfirmDialog
import com.cookbookanything.app.ui.components.DateTimePickerDialog
import com.cookbookanything.app.ui.components.RecipePhoto
import com.cookbookanything.app.ui.components.currentLocale
import com.cookbookanything.app.ui.components.rememberNotificationPermissionRequest
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.model.Comment
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.ScheduledRecipe
import com.cookbookanything.core.domain.model.Step
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun RecipeDetailRoute(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: RecipeDetailViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val shareTitle = stringResource(R.string.share)
    val shareFailedMessage = stringResource(R.string.share_failed)
    val pdfLabels = PdfLabels(
        ingredients = stringResource(R.string.ingredients),
        steps = stringResource(R.string.steps),
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RecipeDetailEvent.Deleted -> onBack()
                is RecipeDetailEvent.Share -> context.shareFile(event.uri, event.mimeType, shareTitle)
                RecipeDetailEvent.ShareFailed -> snackbarHostState.showSnackbar(shareFailedMessage)
            }
        }
    }

    RecipeDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = onEdit,
        onDelete = viewModel::delete,
        onShareAsPdf = { viewModel.shareAsPdf(pdfLabels) },
        onShareAsRecipeFile = viewModel::shareAsRecipeFile,
        onIngredientChecked = viewModel::setIngredientChecked,
        onStepDone = viewModel::setStepDone,
        onResetProgress = viewModel::resetProgress,
        onAddComment = viewModel::addComment,
        onDeleteComment = viewModel::deleteComment,
        onSchedule = viewModel::schedule,
        onRemoveSchedule = viewModel::removeSchedule,
    )
}

private fun Context.shareFile(uri: Uri, mimeType: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType(mimeType)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // The clip data is what carries the read permission over to the chooser and the receiving app.
    send.clipData = ClipData.newRawUri(null, uri)
    startActivity(Intent.createChooser(send, chooserTitle))
}

@Composable
fun RecipeDetailScreen(
    uiState: RecipeDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShareAsPdf: () -> Unit,
    onShareAsRecipeFile: () -> Unit,
    onIngredientChecked: (String, Boolean) -> Unit,
    onStepDone: (String, Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onSchedule: (Long) -> Unit,
    onRemoveSchedule: (String) -> Unit,
) {
    var shareMenuOpen by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val loaded = uiState is RecipeDetailUiState.Success

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = {},
                navigationIcon = {
                    AppIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back), onClick = onBack)
                },
                actions = {
                    if (loaded) {
                        Box {
                            AppIconButton(
                                icon = R.drawable.ic_share,
                                contentDescription = stringResource(R.string.share),
                                onClick = { shareMenuOpen = true },
                            )
                            DropdownMenu(expanded = shareMenuOpen, onDismissRequest = { shareMenuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.share_as_pdf)) },
                                    onClick = {
                                        shareMenuOpen = false
                                        onShareAsPdf()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.share_as_recipe_file)) },
                                    onClick = {
                                        shareMenuOpen = false
                                        onShareAsRecipeFile()
                                    },
                                )
                            }
                        }
                        AppIconButton(R.drawable.ic_edit, stringResource(R.string.edit), onClick = onEdit)
                        AppIconButton(
                            icon = R.drawable.ic_delete,
                            contentDescription = stringResource(R.string.delete),
                            onClick = { confirmDelete = true },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (uiState) {
            RecipeDetailUiState.Loading -> CenteredBox(padding) { CircularProgressIndicator() }

            RecipeDetailUiState.NotFound -> CenteredBox(padding) {
                Text(stringResource(R.string.recipe_not_found), textAlign = TextAlign.Center)
            }

            is RecipeDetailUiState.Success -> RecipeContent(
                uiState = uiState,
                contentPadding = padding,
                onIngredientChecked = onIngredientChecked,
                onStepDone = onStepDone,
                onResetProgress = onResetProgress,
                onAddComment = onAddComment,
                onDeleteComment = onDeleteComment,
                onSchedule = onSchedule,
                onRemoveSchedule = onRemoveSchedule,
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_recipe_title),
            text = stringResource(R.string.delete_recipe_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun CenteredBox(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun RecipeContent(
    uiState: RecipeDetailUiState.Success,
    contentPadding: PaddingValues,
    onIngredientChecked: (String, Boolean) -> Unit,
    onStepDone: (String, Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onSchedule: (Long) -> Unit,
    onRemoveSchedule: (String) -> Unit,
) {
    val recipe = uiState.recipe
    val locale = currentLocale()
    var pickingDateTime by rememberSaveable { mutableStateOf(false) }
    val requestNotificationPermission = rememberNotificationPermissionRequest()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = contentPadding,
    ) {
        if (recipe.photos.isNotEmpty()) {
            item { PhotoRow(photos = recipe.photos, height = 220) }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(recipe.title, style = MaterialTheme.typography.headlineMedium)
                if (uiState.categoryName != null) {
                    AssistChip(onClick = {}, label = { Text(uiState.categoryName) })
                }
            }
        }

        if (recipe.ingredients.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.ingredients),
                    progress = "${recipe.ingredients.count { it.checked }}/${recipe.ingredients.size}",
                )
            }
            items(recipe.ingredients, key = { it.id }) { ingredient ->
                CheckRow(
                    text = ingredient.text,
                    checked = ingredient.checked,
                    onCheckedChange = { onIngredientChecked(ingredient.id, it) },
                )
            }
        }

        if (recipe.steps.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.steps),
                    progress = "${recipe.steps.count { it.done }}/${recipe.steps.size}",
                )
            }
            itemsIndexed(recipe.steps, key = { _, step -> step.id }) { index, step ->
                StepRow(number = index + 1, step = step, onDoneChange = { onStepDone(step.id, it) })
            }
        }

        if (recipe.ingredients.any { it.checked } || recipe.steps.any { it.done }) {
            item {
                TextButton(onClick = onResetProgress, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(stringResource(R.string.reset_checks))
                }
            }
        }

        item { SectionHeader(title = stringResource(R.string.tab_calendar)) }
        items(uiState.upcoming, key = { it.id }) { entry ->
            ScheduleRow(entry = entry, locale = locale, onRemove = { onRemoveSchedule(entry.id) })
        }
        item {
            OutlinedButton(
                onClick = { pickingDateTime = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.add_to_calendar))
            }
        }

        item { SectionHeader(title = stringResource(R.string.comments)) }
        items(recipe.comments, key = { it.id }) { comment ->
            CommentRow(comment = comment, locale = locale, onDelete = { onDeleteComment(comment.id) })
        }
        item { CommentInput(onAddComment = onAddComment) }
    }

    if (pickingDateTime) {
        DateTimePickerDialog(
            onConfirm = { dateTime ->
                pickingDateTime = false
                requestNotificationPermission()
                onSchedule(dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            },
            onDismiss = { pickingDateTime = false },
        )
    }
}

@Composable
private fun PhotoRow(photos: List<String>, height: Int, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(photos, key = { it }) { photo ->
            RecipePhoto(
                name = photo,
                modifier = Modifier
                    .height(height.dp)
                    .width((height * 4 / 3).dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, progress: String? = null) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            if (progress != null) {
                Text(
                    text = progress,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CheckRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            // Green for "have it" and "done", so progress reads at a glance.
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.secondary,
                checkmarkColor = MaterialTheme.colorScheme.onSecondary,
            ),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 16.dp),
        )
    }
}

@Composable
private fun StepRow(number: Int, step: Step, onDoneChange: (Boolean) -> Unit) {
    Column {
        CheckRow(text = "$number. ${step.text}", checked = step.done, onCheckedChange = onDoneChange)
        if (step.photos.isNotEmpty()) {
            PhotoRow(photos = step.photos, height = 120, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

@Composable
private fun ScheduleRow(entry: ScheduledRecipe, locale: Locale, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatDateTime(entry.scheduledAt, locale),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        AppIconButton(R.drawable.ic_close, stringResource(R.string.remove), onClick = onRemove)
    }
}

@Composable
private fun CommentRow(comment: Comment, locale: Locale, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(comment.text, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = formatDateTime(comment.createdAt, locale),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppIconButton(R.drawable.ic_close, stringResource(R.string.delete), onClick = onDelete)
    }
}

@Composable
private fun CommentInput(onAddComment: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.add_comment)) },
            modifier = Modifier.weight(1f),
        )
        AppIconButton(
            icon = R.drawable.ic_send,
            contentDescription = stringResource(R.string.add_comment),
            onClick = {
                onAddComment(text)
                text = ""
            },
            enabled = text.isNotBlank(),
            modifier = Modifier.size(48.dp),
        )
    }
}

private fun formatDateTime(epochMillis: Long, locale: Locale): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
