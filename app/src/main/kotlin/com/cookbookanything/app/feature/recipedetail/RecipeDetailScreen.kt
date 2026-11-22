@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.recipedetail

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cookbookanything.app.R
import com.cookbookanything.app.data.share.PdfLabels
import com.cookbookanything.app.data.share.printPdf
import com.cookbookanything.app.ui.components.AddPhotoButton
import com.cookbookanything.app.ui.components.rememberPhotoPicker
import com.cookbookanything.app.ui.components.AppIcon
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.components.CheckRow
import com.cookbookanything.app.ui.components.ConfirmDialog
import com.cookbookanything.app.ui.components.DateTimePickerDialog
import com.cookbookanything.app.ui.components.RecipePhoto
import com.cookbookanything.app.ui.components.currentLocale
import com.cookbookanything.app.ui.components.rememberNotificationPermissionRequest
import com.cookbookanything.app.ui.theme.RecipeNameFont
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.ingredients.isHeading
import com.cookbookanything.core.domain.ingredients.stepNumbers
import com.cookbookanything.core.domain.model.Comment
import com.cookbookanything.core.domain.model.Ingredient
import com.cookbookanything.core.domain.model.MealSlot
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.ScheduledRecipe
import com.cookbookanything.core.domain.model.Step
import com.cookbookanything.core.domain.scaling.ingredientAmount
import com.cookbookanything.core.domain.scaling.scaleIngredient
import com.cookbookanything.core.domain.units.MeasurementSystem
import com.cookbookanything.core.domain.units.convertIngredient
import com.cookbookanything.core.domain.units.convertTemperatures
import com.cookbookanything.core.domain.units.hasConvertibleUnits
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RecipeDetailRoute(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStartCooking: (servings: Int?, measurement: MeasurementSystem) -> Unit,
    onOpenRecipe: (String) -> Unit,
    viewModel: RecipeDetailViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cookPrompt by viewModel.cookPrompt.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val copySuffix = stringResource(R.string.copy_suffix)
    val snackbarHostState = remember { SnackbarHostState() }
    val shareTitle = stringResource(R.string.share)
    val shareFailedMessage = stringResource(R.string.share_failed)
    val pdfLabels = PdfLabels(
        ingredients = stringResource(R.string.ingredients),
        steps = stringResource(R.string.steps),
        summary = (uiState as? RecipeDetailUiState.Success)?.let { recipeFacts(it.recipe, withServings = true) },
        notes = stringResource(R.string.notes),
        optional = stringResource(R.string.optional_mark),
        source = stringResource(R.string.source),
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RecipeDetailEvent.Deleted -> onBack()
                is RecipeDetailEvent.Share -> context.shareFile(event.uri, event.mimeType, shareTitle)
                RecipeDetailEvent.ShareFailed -> snackbarHostState.showSnackbar(shareFailedMessage)
                is RecipeDetailEvent.Print -> context.printPdf(event.uri, (uiState as? RecipeDetailUiState.Success)?.recipe?.title ?: shareTitle)
                is RecipeDetailEvent.Duplicated -> onOpenRecipe(event.recipeId)
            }
        }
    }

    RecipeDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = onEdit,
        onStartCooking = onStartCooking,
        onDelete = viewModel::delete,
        onShareAsPdf = { viewModel.shareAsPdf(pdfLabels) },
        onShareAsRecipeFile = viewModel::shareAsRecipeFile,
        onPrint = { viewModel.print(pdfLabels) },
        onPin = viewModel::setPinned,
        onDuplicate = { viewModel.duplicate(copySuffix) },
        cookPrompt = cookPrompt,
        onCooked = viewModel::markCooked,
        newCameraTarget = viewModel::newCameraTarget,
        onDismissCookPrompt = viewModel::dismissCookPrompt,
        onIngredientChecked = viewModel::setIngredientChecked,
        onStepDone = viewModel::setStepDone,
        onResetProgress = viewModel::resetProgress,
        onFavorite = viewModel::setFavorite,
        onRate = viewModel::rate,
        onAddComment = viewModel::addComment,
        onDeleteComment = viewModel::deleteComment,
        onSchedule = viewModel::schedule,
        onRemoveSchedule = viewModel::removeSchedule,
    )
}

internal fun Context.shareFile(uri: Uri, mimeType: String, chooserTitle: String) {
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
    onStartCooking: (servings: Int?, measurement: MeasurementSystem) -> Unit,
    onDelete: () -> Unit,
    onShareAsPdf: () -> Unit,
    onShareAsRecipeFile: () -> Unit,
    onPrint: () -> Unit,
    onPin: (Boolean) -> Unit,
    onDuplicate: () -> Unit,
    cookPrompt: Boolean,
    onCooked: (photo: Uri?) -> Unit,
    newCameraTarget: () -> Uri,
    onDismissCookPrompt: () -> Unit,
    onIngredientChecked: (String, Boolean) -> Unit,
    onStepDone: (String, Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onFavorite: (Boolean) -> Unit,
    onRate: (Int) -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onSchedule: (Long, MealSlot?) -> Unit,
    onRemoveSchedule: (String) -> Unit,
) {
    var shareMenuOpen by remember { mutableStateOf(false) }
    var moreMenuOpen by remember { mutableStateOf(false) }
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
                        val favorite = uiState.recipe.favorite
                        AppIconButton(
                            icon = if (favorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                            contentDescription = stringResource(if (favorite) R.string.unfavorite else R.string.favorite),
                            onClick = { onFavorite(!favorite) },
                            label = stringResource(R.string.label_favorite),
                        )
                        Box {
                            AppIconButton(
                                icon = R.drawable.ic_share,
                                contentDescription = stringResource(R.string.share),
                                onClick = { shareMenuOpen = true },
                                label = stringResource(R.string.label_share),
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
                        AppIconButton(R.drawable.ic_edit, stringResource(R.string.edit), onClick = onEdit, label = stringResource(R.string.label_edit))
                        Box {
                            AppIconButton(R.drawable.ic_more_vert, stringResource(R.string.more), onClick = { moreMenuOpen = true }, label = stringResource(R.string.label_more))
                            DropdownMenu(expanded = moreMenuOpen, onDismissRequest = { moreMenuOpen = false }) {
                                val pinned = uiState.recipe.pinned
                                val actions = listOf<Pair<Int, () -> Unit>>(
                                    (if (pinned) R.string.unpin else R.string.pin) to { onPin(!pinned) },
                                    R.string.duplicate to onDuplicate,
                                    R.string.print to onPrint,
                                    R.string.delete to { confirmDelete = true },
                                )
                                actions.forEach { (label, action) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(label)) },
                                        onClick = {
                                            moreMenuOpen = false
                                            action()
                                        },
                                    )
                                }
                            }
                        }
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
                onStartCooking = onStartCooking,
                onIngredientChecked = onIngredientChecked,
                onStepDone = onStepDone,
                onResetProgress = onResetProgress,
                onRate = onRate,
                onAddComment = onAddComment,
                onDeleteComment = onDeleteComment,
                onSchedule = onSchedule,
                onRemoveSchedule = onRemoveSchedule,
            )
        }
    }

    if (cookPrompt) {
        CookPromptDialog(onCooked = onCooked, newCameraTarget = newCameraTarget, onDismiss = onDismissCookPrompt)
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
    onStartCooking: (servings: Int?, measurement: MeasurementSystem) -> Unit,
    onIngredientChecked: (String, Boolean) -> Unit,
    onStepDone: (String, Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onRate: (Int) -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onSchedule: (Long, MealSlot?) -> Unit,
    onRemoveSchedule: (String) -> Unit,
) {
    val recipe = uiState.recipe
    val locale = currentLocale()
    val context = LocalContext.current
    var pickingDateTime by rememberSaveable { mutableStateOf(false) }
    val requestNotificationPermission = rememberNotificationPermissionRequest()
    // The cook can view the recipe for more or fewer people, or for the amount of one ingredient they
    // have; one factor scales every amount and the stored recipe does not change.
    val baseServings = recipe.servings
    var scale by rememberSaveable(baseServings) { mutableDoubleStateOf(1.0) }
    val shownServings = baseServings?.let { (it * scale).roundToInt().coerceAtLeast(1) }
    var scalingByIngredient by rememberSaveable { mutableStateOf(false) }
    val canScaleByIngredient = recipe.ingredients.any { ingredientAmount(it.text) != null }
    // Cups to millilitres or grams to ounces, on this page and in cooking mode; the stored recipe does not change.
    var measurement by rememberSaveable { mutableStateOf(MeasurementSystem.ORIGINAL) }
    val convertible = remember(recipe) { hasConvertibleUnits(recipe.ingredients.map { it.text }, recipe.steps.map { it.text }) }
    val countableIngredients = recipe.ingredients.filterNot { isHeading(it.text) }
    val countableSteps = recipe.steps.filterNot { isHeading(it.text) }

    // The page of a printed cookbook: the picture, the name in the book serif, the facts between two
    // hairlines, then the sections, each with its own rule.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        if (recipe.photos.isNotEmpty()) {
            item { PhotoRow(photos = recipe.photos, height = 240, modifier = Modifier.padding(top = 4.dp)) }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(recipe.title, style = MaterialTheme.typography.headlineLarge.copy(fontFamily = RecipeNameFont))
                recipe.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                recipeFacts(recipe, withServings = false)?.let { facts ->
                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                    Text(
                        text = facts,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    HorizontalDivider()
                }
                if (recipe.cookedAt.isNotEmpty()) {
                    val times = LocalContext.current.resources.getQuantityString(R.plurals.cooked_times, recipe.cookedAt.size, recipe.cookedAt.size)
                    Text(
                        text = times + ", " + stringResource(R.string.cooked_last, formatDate(recipe.cookedAt.last(), locale)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (recipe.tags.isNotEmpty()) {
                    Text(
                        text = recipe.tags.joinToString("  ") { "#$it" },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Rating(stars = recipe.rating, onRate = onRate)
                recipe.source?.let { source ->
                    SourceRow(source = source, onOpen = {
                        // A bad or unknown link must not crash the page.
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source))) }
                    })
                }
                if (uiState.categoryName != null) {
                    AssistChip(onClick = {}, label = { Text(uiState.categoryName) })
                }
                if (recipe.steps.isNotEmpty()) {
                    Button(
                        // shortcut: cooking mode scales by whole servings, so an ingredient-based factor is rounded to the nearest serving
                        onClick = { onStartCooking(shownServings, measurement) },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.start_cooking))
                    }
                }
            }
        }

        if (recipe.ingredients.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.ingredients),
                    progress = "${countableIngredients.count { it.checked }}/${countableIngredients.size}",
                )
            }
            if (baseServings != null || canScaleByIngredient) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (baseServings != null && shownServings != null) {
                            ServingsStepper(servings = shownServings, onChange = { scale = it.toDouble() / baseServings })
                        }
                        if (canScaleByIngredient) {
                            TextButton(onClick = { scalingByIngredient = true }, modifier = Modifier.padding(start = 8.dp)) {
                                Text(stringResource(R.string.scale_by_ingredient))
                            }
                        }
                    }
                }
            }
            if (convertible) {
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        MeasurementSystem.entries.forEachIndexed { index, system ->
                            SegmentedButton(
                                selected = measurement == system,
                                onClick = { measurement = system },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = MeasurementSystem.entries.size),
                            ) {
                                Text(stringResource(system.label()))
                            }
                        }
                    }
                }
            }
            items(recipe.ingredients, key = { it.id }) { ingredient ->
                if (isHeading(ingredient.text)) {
                    ListHeading(ingredient.text)
                } else {
                    CheckRow(
                        text = convertIngredient(scaleIngredient(ingredient.text, scale), measurement) +
                            if (ingredient.optional) " " + stringResource(R.string.optional_mark) else "",
                        checked = ingredient.checked,
                        onCheckedChange = { onIngredientChecked(ingredient.id, it) },
                    )
                }
            }
        }

        if (recipe.steps.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.steps),
                    progress = "${countableSteps.count { it.done }}/${countableSteps.size}",
                )
            }
            val numbers = recipe.steps.stepNumbers()
            itemsIndexed(recipe.steps, key = { _, step -> step.id }) { index, step ->
                val number = numbers[index]
                if (number == null) {
                    ListHeading(step.text)
                } else {
                    StepRow(number = number, step = step.copy(text = convertTemperatures(step.text, measurement)), onDoneChange = { onStepDone(step.id, it) })
                }
            }
        }

        if (recipe.ingredients.any { it.checked } || recipe.steps.any { it.done }) {
            item {
                TextButton(onClick = onResetProgress, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(stringResource(R.string.reset_checks))
                }
            }
        }

        if (recipe.cookedPhotos.isNotEmpty()) {
            // The cook's own results, newest first: what it really looked like.
            item { SectionHeader(title = stringResource(R.string.cooked_photos)) }
            item { PhotoRow(photos = recipe.cookedPhotos.toSortedMap(reverseOrder()).values.toList(), height = 120) }
        }

        recipe.notes?.let { notes ->
            item { SectionHeader(title = stringResource(R.string.notes)) }
            item { Text(notes, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
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

    if (scalingByIngredient) {
        ScaleByIngredientDialog(
            ingredients = recipe.ingredients.filter { ingredientAmount(it.text) != null },
            onConfirm = { factor ->
                scale = factor
                scalingByIngredient = false
            },
            onDismiss = { scalingByIngredient = false },
        )
    }

    if (pickingDateTime) {
        DateTimePickerDialog(
            onConfirm = { dateTime, slot ->
                pickingDateTime = false
                requestNotificationPermission()
                onSchedule(dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), slot)
            },
            onDismiss = { pickingDateTime = false },
        )
    }

}

/** "Did you cook it?": yes, yes with a photo of the result, or not this time. */
@Composable
private fun CookPromptDialog(onCooked: (Uri?) -> Unit, newCameraTarget: () -> Uri, onDismiss: () -> Unit) {
    val picker = rememberPhotoPicker(newCameraTarget = newCameraTarget, onPicked = { onCooked(it.first()) })
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cook_prompt_title)) },
        text = { Text(stringResource(R.string.cook_prompt_text)) },
        confirmButton = {
            Row {
                TextButton(onClick = { onCooked(null) }) { Text(stringResource(R.string.cooked_yes)) }
                AddPhotoButton(picker = picker, label = stringResource(R.string.cooked_with_photo))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cooked_no)) } },
    )
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
                    .clip(MaterialTheme.shapes.medium),
            )
        }
    }
}

/** A section head as a cookbook sets it: the name in red over a hairline, the count at the right. */
@Composable
private fun SectionHeader(title: String, progress: String? = null) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            if (progress != null) {
                // The count rolls up like a counter wheel when a line is ticked.
                AnimatedContent(
                    targetState = progress,
                    transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                    label = "progress",
                ) { count ->
                    Text(count, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider()
    }
}

/**
 * "I have 300 g of flour": pick the ingredient, type the amount at hand, and every amount in the
 * recipe is scaled by the same factor.
 */
@Composable
private fun ScaleByIngredientDialog(ingredients: List<Ingredient>, onConfirm: (Double) -> Unit, onDismiss: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var have by rememberSaveable { mutableStateOf("") }
    val chosen = ingredients.getOrNull(selected)
    val original = chosen?.let { ingredientAmount(it.text) }
    val factor = have.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && original != null && original > 0 }?.let { it / original!! }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scale_by_ingredient)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ingredients.forEachIndexed { index, ingredient ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = index },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = index == selected, onClick = { selected = index })
                        Text(ingredient.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = have,
                    onValueChange = { have = it },
                    label = { Text(stringResource(R.string.scale_have)) },
                    // The unit is whatever the line uses: "300" against "250 g flour" means 300 g.
                    placeholder = { chosen?.let { Text(it.text) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { factor?.let(onConfirm) }, enabled = factor != null) { Text(stringResource(R.string.scale_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ServingsStepper(servings: Int, onChange: (Int) -> Unit) {
    Row(modifier = Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.servings), style = MaterialTheme.typography.bodyLarge)
        AppIconButton(
            icon = R.drawable.ic_remove,
            contentDescription = stringResource(R.string.fewer_servings),
            onClick = { onChange(servings - 1) },
            enabled = servings > 1,
        )
        Text(servings.toString(), style = MaterialTheme.typography.titleMedium)
        AppIconButton(
            icon = R.drawable.ic_add,
            contentDescription = stringResource(R.string.more_servings),
            onClick = { onChange(servings + 1) },
            enabled = servings < 99,
        )
    }
}

/** Servings and times on one line, or null when the recipe has none of them. */
@Composable
private fun recipeFacts(recipe: Recipe, withServings: Boolean): String? = listOfNotNull(
    recipe.servings?.takeIf { withServings }?.let { stringResource(R.string.servings_count, it) },
    recipe.prepMinutes?.let { stringResource(R.string.prep_time, it) },
    recipe.cookMinutes?.let { stringResource(R.string.cook_time, it) },
    recipe.waitMinutes?.let { stringResource(R.string.wait_time, it) },
).joinToString(" · ").ifEmpty { null }

/** Five stars; tapping one rates the recipe, tapping the lit one again clears it. */
@Composable
private fun Rating(stars: Int?, onRate: (Int) -> Unit) {
    Row {
        (1..5).forEach { star ->
            AppIconButton(
                icon = if (star <= (stars ?: 0)) R.drawable.ic_star else R.drawable.ic_star_border,
                contentDescription = stringResource(R.string.rate_stars, star),
                onClick = { onRate(star) },
            )
        }
    }
}

/** Where the recipe came from: the site it was read from (tap opens it) or "From a photo". */
@Composable
private fun SourceRow(source: String, onOpen: () -> Unit) {
    val isLink = source.startsWith("http")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isLink) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLink) AppIcon(R.drawable.ic_link, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(
            text = if (isLink) stringResource(R.string.source) + ": " + (Uri.parse(source).host ?: source) else stringResource(R.string.source_photo),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isLink) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "For the sauce:" between the rows, set small in the book's red. */
@Composable
private fun ListHeading(text: String) {
    Text(
        text = text.trimEnd(':'),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

private fun MeasurementSystem.label(): Int = when (this) {
    MeasurementSystem.ORIGINAL -> R.string.measure_original
    MeasurementSystem.METRIC -> R.string.measure_metric
    MeasurementSystem.IMPERIAL -> R.string.measure_imperial
}

/** A step with its number set large in red in the margin, as in a printed method. */
@Composable
private fun StepRow(number: Int, step: Step, onDoneChange: (Boolean) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = if (step.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .width(36.dp)
                    .padding(top = 10.dp),
            )
            CheckRow(text = step.text, checked = step.done, onCheckedChange = onDoneChange, modifier = Modifier.weight(1f))
        }
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

private fun formatDate(epochMillis: Long, locale: Locale): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
