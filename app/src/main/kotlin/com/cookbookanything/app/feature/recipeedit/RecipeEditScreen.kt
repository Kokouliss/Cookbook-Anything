@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.recipeedit

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cookbookanything.app.R
import com.cookbookanything.app.feature.recipedetail.shareFile
import com.cookbookanything.app.ui.components.AddPhotoButton
import com.cookbookanything.app.ui.components.AppIcon
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.components.ConfirmDialog
import com.cookbookanything.app.ui.components.PhotoPicker
import com.cookbookanything.app.ui.components.RecipePhoto
import com.cookbookanything.app.ui.components.TextInputDialog
import com.cookbookanything.app.ui.components.rememberPhotoPicker
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.Ingredient
import com.cookbookanything.core.domain.model.Step
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun RecipeEditRoute(
    onClose: () -> Unit,
    onSaved: (String) -> Unit,
    onOpenRecipe: (String) -> Unit,
    viewModel: RecipeEditViewModel,
) {
    LaunchedEffect(viewModel) {
        viewModel.saved.collect { recipeId -> onSaved(recipeId) }
    }
    RecipeEditScreen(
        uiState = viewModel.uiState,
        onClose = onClose,
        onOpenRecipe = onOpenRecipe,
        onSave = viewModel::save,
        onTitleChange = viewModel::setTitle,
        onCategoryChange = viewModel::setCategory,
        onCreateCategory = viewModel::createCategory,
        onServingsChange = viewModel::setServings,
        onPrepMinutesChange = viewModel::setPrepMinutes,
        onCookMinutesChange = viewModel::setCookMinutes,
        onWaitMinutesChange = viewModel::setWaitMinutes,
        onDescriptionChange = viewModel::setDescription,
        onNotesChange = viewModel::setNotes,
        onTagsChange = viewModel::setTags,
        onIngredientChange = viewModel::setIngredientText,
        onIngredientOptionalChange = viewModel::setIngredientOptional,
        onRemoveIngredient = viewModel::removeIngredient,
        onRestoreIngredient = viewModel::restoreIngredient,
        onMoveIngredient = viewModel::moveIngredient,
        onStepChange = viewModel::setStepText,
        onRemoveStep = viewModel::removeStep,
        onRestoreStep = viewModel::restoreStep,
        onMoveStep = viewModel::moveStep,
        onPaste = viewModel::paste,
        linkDetails = viewModel::linkDetails,
        onReadPhotos = if (viewModel.canReadPhotos) viewModel::readPhotos else null,
        readResults = viewModel.readResultFlow,
        onAddPhotos = viewModel::addPhotos,
        onRemovePhoto = viewModel::removePhoto,
        newCameraTarget = viewModel::newCameraTarget,
    )
}

@Composable
fun RecipeEditScreen(
    uiState: RecipeEditUiState,
    onClose: () -> Unit,
    /** Opens a saved recipe instead, when the pasted link was imported before. */
    onOpenRecipe: (String) -> Unit,
    onSave: () -> Unit,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onCreateCategory: (String) -> Unit,
    onServingsChange: (String) -> Unit,
    onPrepMinutesChange: (String) -> Unit,
    onCookMinutesChange: (String) -> Unit,
    onWaitMinutesChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onIngredientChange: (String, String) -> Unit,
    onIngredientOptionalChange: (String, Boolean) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onRestoreIngredient: (Int, Ingredient) -> Unit,
    onMoveIngredient: (Int, Int) -> Unit,
    onStepChange: (String, String) -> Unit,
    onRemoveStep: (String) -> Unit,
    onRestoreStep: (Int, Step) -> Unit,
    onMoveStep: (Int, Int) -> Unit,
    onAddPhotos: (String?, List<Uri>) -> Unit,
    onRemovePhoto: (String?, String) -> Unit,
    newCameraTarget: () -> Uri,
    onPaste: (String) -> Unit,
    linkDetails: () -> Uri?,
    /** Null when sign-in is not set up in this build, which also means no reading from photos. */
    onReadPhotos: ((List<Uri>) -> Unit)?,
    readResults: Flow<ReadResult>,
) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val pasteNothing = stringResource(R.string.paste_nothing)
    val undo = stringResource(R.string.undo)
    val ingredientRemoved = stringResource(R.string.ingredient_removed)
    val stepRemoved = stringResource(R.string.step_removed)
    // Photos of a recipe page are read, not attached; the camera file is swept with the other orphans.
    val readPicker = rememberPhotoPicker(newCameraTarget = newCameraTarget, onPicked = { uris -> onReadPhotos?.invoke(uris) })
    val photoNothing = stringResource(R.string.photo_nothing)
    val photoFailed = stringResource(R.string.photo_failed)
    val photoSavedOffline = stringResource(R.string.photo_saved_offline)
    val linkNothing = stringResource(R.string.link_nothing)
    val linkFailed = stringResource(R.string.link_failed)
    val sendDetails = stringResource(R.string.link_send_details)
    val pastedPartial = stringResource(R.string.pasted_partial)
    val open = stringResource(R.string.open)
    val notTranslated = stringResource(R.string.translate_failed)
    LaunchedEffect(readResults) {
        readResults.collect { result ->
            // A link that was not read offers the page the site answered with, so the reason can be found.
            val unreadLink = result == ReadResult.LinkNothing || result == ReadResult.LinkFailed
            val failedPhoto = result is ReadResult.PhotoFailed
            val duplicate = result as? ReadResult.Duplicate
            val pressed = snackbarHostState.showSnackbar(
                actionLabel = when {
                    unreadLink -> sendDetails
                    duplicate != null -> open
                    else -> null
                },
                duration = if (unreadLink || duplicate != null || failedPhoto) SnackbarDuration.Long else SnackbarDuration.Short,
                message = when (result) {
                    // Only one of the two lists came through: the editor is the place to finish it.
                    is ReadResult.Added ->
                        if ((result.ingredients == 0) != (result.steps == 0)) pastedPartial
                        else context.getString(R.string.pasted_summary, result.ingredients, result.steps)
                    ReadResult.PhotoNothing -> photoNothing
                    is ReadResult.PhotoFailed -> result.reason?.let { "$photoFailed\n$it" } ?: photoFailed
                    ReadResult.PhotoSavedForLater -> photoSavedOffline
                    ReadResult.LinkNothing -> linkNothing
                    ReadResult.LinkFailed -> linkFailed
                    is ReadResult.Duplicate -> context.getString(R.string.link_duplicate, result.title)
                    ReadResult.NotTranslated -> notTranslated
                },
            )
            if (pressed == SnackbarResult.ActionPerformed) {
                if (duplicate != null) onOpenRecipe(duplicate.recipeId) else linkDetails()?.let { context.shareFile(it, "text/plain", sendDetails) }
            }
        }
    }
    /** Removes a row and offers Undo for a moment. */
    fun removeWithUndo(message: String, remove: () -> Unit, restore: () -> Unit) {
        remove()
        scope.launch {
            val result = snackbarHostState.showSnackbar(message, actionLabel = undo, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) restore()
        }
    }
    // Which step the photo picker was opened for; null means the recipe itself.
    var photoTargetStepId by rememberSaveable { mutableStateOf<String?>(null) }
    val photoPicker = rememberPhotoPicker(
        newCameraTarget = newCameraTarget,
        onPicked = { uris -> onAddPhotos(photoTargetStepId, uris) },
    )
    val close: () -> Unit = {
        if (uiState.dirty) {
            confirmDiscard = true
        } else {
            onClose()
        }
    }

    BackHandler(enabled = uiState.dirty) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = {
                    Text(stringResource(if (uiState.isNew) R.string.new_recipe else R.string.edit_recipe))
                },
                navigationIcon = {
                    AppIconButton(R.drawable.ic_close, stringResource(R.string.close), onClick = close)
                },
                actions = {
                    if (onReadPhotos != null) {
                        AddPhotoButton(
                            picker = readPicker,
                            iconOnly = true,
                            icon = R.drawable.ic_document_scanner,
                            label = stringResource(R.string.photo_recipe),
                        )
                    }
                    AppIconButton(
                        icon = R.drawable.ic_content_paste,
                        contentDescription = stringResource(R.string.paste_recipe),
                        enabled = !uiState.loading,
                        onClick = {
                            scope.launch {
                                val clip = clipboard.getClipEntry()?.clipData
                                val text = clip?.let { data ->
                                    (0 until data.itemCount).joinToString("\n") { data.getItemAt(it).coerceToText(context) }
                                }
                                if (text.isNullOrBlank()) snackbarHostState.showSnackbar(pasteNothing) else onPaste(text)
                            }
                        },
                    )
                    TextButton(onClick = onSave, enabled = !uiState.loading) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // A photo or a link takes a few seconds to read; the line says so without blocking typing.
            AnimatedVisibility(visible = uiState.reading) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.recipe_title)) },
                isError = uiState.titleMissing,
                supportingText = if (uiState.titleMissing) {
                    { Text(stringResource(R.string.title_required)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.description)) },
                placeholder = { Text(stringResource(R.string.description_hint)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            CategoryPicker(
                categories = uiState.categories,
                selectedCategoryId = uiState.categoryId,
                onCategoryChange = onCategoryChange,
                onCreateCategory = onCreateCategory,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = uiState.prepMinutes,
                    onValueChange = onPrepMinutesChange,
                    label = stringResource(R.string.prep_time_label),
                    suffix = stringResource(R.string.minutes_short),
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    value = uiState.cookMinutes,
                    onValueChange = onCookMinutesChange,
                    label = stringResource(R.string.cook_time_label),
                    suffix = stringResource(R.string.minutes_short),
                    modifier = Modifier.weight(1f),
                )
            }
            NumberField(
                value = uiState.waitMinutes,
                onValueChange = onWaitMinutesChange,
                label = stringResource(R.string.wait_time_label),
                suffix = stringResource(R.string.minutes_short),
                modifier = Modifier.fillMaxWidth(0.5f),
            )

            PhotoStrip(
                photos = uiState.photos,
                onRemove = { onRemovePhoto(null, it) },
                picker = photoPicker,
                onBeforePick = { photoTargetStepId = null },
            )

            SectionTitle(stringResource(R.string.ingredients))
            // Next to the amounts it describes: the recipe page scales them from this number.
            NumberField(
                value = uiState.servings,
                onValueChange = onServingsChange,
                label = stringResource(R.string.servings),
                modifier = Modifier.fillMaxWidth(0.5f),
            )
            // Rows are dragged by their handle and swiped left to remove; the last row is the always-empty spare one.
            ReorderableColumn(
                list = uiState.ingredients,
                onSettle = onMoveIngredient,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { index, ingredient, _ ->
                // Keyed by id so focus stays with the right field when a row above is removed.
                key(ingredient.id) {
                    val spare = index == uiState.ingredients.lastIndex
                    ReorderableItem {
                        SwipeToRemove(
                            enabled = !spare,
                            onRemove = {
                                removeWithUndo(ingredientRemoved, { onRemoveIngredient(ingredient.id) }, { onRestoreIngredient(index, ingredient) })
                            },
                        ) {
                            RemovableField(
                                value = ingredient.text,
                                onValueChange = { onIngredientChange(ingredient.id, it) },
                                placeholder = stringResource(R.string.ingredient_hint),
                                singleLine = true,
                                onRemove = {
                                    removeWithUndo(ingredientRemoved, { onRemoveIngredient(ingredient.id) }, { onRestoreIngredient(index, ingredient) })
                                }.takeIf { !spare },
                                handle = { DragHandle(modifier = Modifier.draggableHandle(enabled = !spare), visible = !spare) },
                                trailing = {
                                    if (!spare) {
                                        OptionalToggle(
                                            optional = ingredient.optional,
                                            onChange = { onIngredientOptionalChange(ingredient.id, it) },
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.steps))
            ReorderableColumn(
                list = uiState.steps,
                onSettle = onMoveStep,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { index, step, _ ->
                key(step.id) {
                    val spare = index == uiState.steps.lastIndex
                    ReorderableItem {
                        Column {
                            SwipeToRemove(
                                enabled = !spare,
                                onRemove = { removeWithUndo(stepRemoved, { onRemoveStep(step.id) }, { onRestoreStep(index, step) }) },
                            ) {
                                RemovableField(
                                    value = step.text,
                                    onValueChange = { onStepChange(step.id, it) },
                                    placeholder = stringResource(R.string.step_hint, index + 1),
                                    singleLine = false,
                                    onRemove = {
                                        removeWithUndo(stepRemoved, { onRemoveStep(step.id) }, { onRestoreStep(index, step) })
                                    }.takeIf { !spare },
                                    handle = { DragHandle(modifier = Modifier.draggableHandle(enabled = !spare), visible = !spare) },
                                    trailing = {
                                        AddPhotoButton(
                                            picker = photoPicker,
                                            onBeforePick = { photoTargetStepId = step.id },
                                            iconOnly = true,
                                        )
                                    },
                                )
                            }
                            if (step.photos.isNotEmpty()) {
                                PhotoStrip(photos = step.photos, onRemove = { onRemovePhoto(step.id, it) })
                            }
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.notes))
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                placeholder = { Text(stringResource(R.string.notes_hint)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(stringResource(R.string.tags))
            OutlinedTextField(
                value = uiState.tags,
                onValueChange = onTagsChange,
                placeholder = { Text(stringResource(R.string.tags_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.discard_title),
            text = stringResource(R.string.discard_text),
            confirmLabel = stringResource(R.string.discard),
            onConfirm = {
                confirmDiscard = false
                onClose()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CategoryPicker(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategoryChange: (String?) -> Unit,
    onCreateCategory: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.name

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            AppIcon(R.drawable.ic_label, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(selectedName ?: stringResource(R.string.no_category))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.no_category)) },
                onClick = {
                    expanded = false
                    onCategoryChange(null)
                },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        expanded = false
                        onCategoryChange(category.id)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_category)) },
                onClick = {
                    expanded = false
                    creating = true
                },
            )
        }
    }

    if (creating) {
        TextInputDialog(
            title = stringResource(R.string.new_category),
            label = stringResource(R.string.category_name),
            initialValue = "",
            onConfirm = { name ->
                creating = false
                onCreateCategory(name)
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = if (suffix != null) {
            { Text(suffix) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

/** Marks an ingredient as optional: it is shown as such and left off the shopping list. */
@Composable
private fun OptionalToggle(optional: Boolean, onChange: (Boolean) -> Unit) {
    IconToggleButton(checked = optional, onCheckedChange = onChange) {
        AppIcon(
            icon = R.drawable.ic_remove_shopping_cart,
            contentDescription = stringResource(R.string.mark_optional),
            modifier = Modifier.alpha(if (optional) 1f else 0.4f),
        )
    }
}

/** The grip at the start of a row. Pressing it starts the drag at once, so the text field keeps its own long press. */
@Composable
private fun DragHandle(modifier: Modifier, visible: Boolean) {
    if (visible) {
        AppIcon(
            icon = R.drawable.ic_drag_handle,
            contentDescription = stringResource(R.string.drag_to_reorder),
            modifier = modifier
                .padding(end = 4.dp)
                .size(32.dp)
                .padding(4.dp),
        )
    } else {
        Box(modifier = Modifier.width(36.dp))
    }
}

/** Swiping the row to the start removes it; the row itself stays put, because it leaves the list. */
@Composable
private fun SwipeToRemove(enabled: Boolean, onRemove: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onRemove()
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        gesturesEnabled = enabled,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                AppIcon(R.drawable.ic_delete, contentDescription = null)
            }
        },
    ) {
        content()
    }
}

@Composable
private fun RemovableField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    onRemove: (() -> Unit)?,
    handle: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        handle()
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
            ),
            modifier = Modifier.weight(1f),
        )
        trailing()
        if (onRemove != null) {
            AppIconButton(R.drawable.ic_close, stringResource(R.string.remove), onClick = onRemove)
        } else {
            // Keeps every field the same width whether or not it has a remove button.
            Box(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun PhotoStrip(
    photos: List<String>,
    onRemove: (String) -> Unit,
    // Steps add photos with the camera button next to their field, so they pass no picker here.
    picker: PhotoPicker? = null,
    onBeforePick: () -> Unit = {},
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(photos, key = { it }) { photo ->
            Box {
                RecipePhoto(
                    name = photo,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .clickable { onRemove(photo) },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        icon = R.drawable.ic_close,
                        contentDescription = stringResource(R.string.remove_photo),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (picker != null) {
            item {
                AddPhotoButton(picker = picker, onBeforePick = onBeforePick)
            }
        }
    }
}
