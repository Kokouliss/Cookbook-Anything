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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.cookbookanything.app.R
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

@Composable
fun RecipeEditRoute(
    onClose: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: RecipeEditViewModel,
) {
    LaunchedEffect(viewModel) {
        viewModel.saved.collect { recipeId -> onSaved(recipeId) }
    }
    RecipeEditScreen(
        uiState = viewModel.uiState,
        onClose = onClose,
        onSave = viewModel::save,
        onTitleChange = viewModel::setTitle,
        onCategoryChange = viewModel::setCategory,
        onCreateCategory = viewModel::createCategory,
        onIngredientChange = viewModel::setIngredientText,
        onRemoveIngredient = viewModel::removeIngredient,
        onStepChange = viewModel::setStepText,
        onRemoveStep = viewModel::removeStep,
        onAddPhotos = viewModel::addPhotos,
        onRemovePhoto = viewModel::removePhoto,
        newCameraTarget = viewModel::newCameraTarget,
    )
}

@Composable
fun RecipeEditScreen(
    uiState: RecipeEditUiState,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onCreateCategory: (String) -> Unit,
    onIngredientChange: (String, String) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onStepChange: (String, String) -> Unit,
    onRemoveStep: (String) -> Unit,
    onAddPhotos: (String?, List<Uri>) -> Unit,
    onRemovePhoto: (String?, String) -> Unit,
    newCameraTarget: () -> Uri,
) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
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
                    TextButton(
                        onClick = onSave,
                        enabled = !uiState.loading,
                        // The default primary text would vanish on the brand-coloured bar.
                        colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
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

            CategoryPicker(
                categories = uiState.categories,
                selectedCategoryId = uiState.categoryId,
                onCategoryChange = onCategoryChange,
                onCreateCategory = onCreateCategory,
            )

            PhotoStrip(
                photos = uiState.photos,
                onRemove = { onRemovePhoto(null, it) },
                picker = photoPicker,
                onBeforePick = { photoTargetStepId = null },
            )

            SectionTitle(stringResource(R.string.ingredients))
            uiState.ingredients.forEach { ingredient ->
                // Keyed by id so focus stays with the right field when a row above is removed.
                key(ingredient.id) {
                    RemovableField(
                        value = ingredient.text,
                        onValueChange = { onIngredientChange(ingredient.id, it) },
                        placeholder = stringResource(R.string.ingredient_hint),
                        singleLine = true,
                        // The last row is the always-empty spare one.
                        onRemove = { onRemoveIngredient(ingredient.id) }
                            .takeIf { ingredient !== uiState.ingredients.last() },
                    )
                }
            }

            SectionTitle(stringResource(R.string.steps))
            uiState.steps.forEachIndexed { index, step ->
                key(step.id) {
                    RemovableField(
                        value = step.text,
                        onValueChange = { onStepChange(step.id, it) },
                        placeholder = stringResource(R.string.step_hint, index + 1),
                        singleLine = false,
                        onRemove = { onRemoveStep(step.id) }.takeIf { step !== uiState.steps.last() },
                        trailing = {
                            AddPhotoButton(
                                picker = photoPicker,
                                onBeforePick = { photoTargetStepId = step.id },
                                iconOnly = true,
                            )
                        },
                    )
                    if (step.photos.isNotEmpty()) {
                        PhotoStrip(photos = step.photos, onRemove = { onRemovePhoto(step.id, it) })
                    }
                }
            }
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
private fun RemovableField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    onRemove: (() -> Unit)?,
    trailing: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
