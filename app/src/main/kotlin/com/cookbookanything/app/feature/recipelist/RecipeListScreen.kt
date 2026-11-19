@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.recipelist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cookbookanything.app.R
import com.cookbookanything.app.ui.components.AppIcon
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.components.Doodle
import com.cookbookanything.app.ui.components.DoodleImage
import com.cookbookanything.app.ui.components.EmptyState
import com.cookbookanything.app.ui.components.RecipePhoto
import com.cookbookanything.app.ui.components.pressScale
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.sort.RecipeSort

@Composable
fun RecipeListRoute(
    onAddRecipe: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onImportRecipe: (Uri) -> Unit,
    onManageCategories: () -> Unit,
    backupOverdue: Boolean,
    onOpenSettings: () -> Unit,
    viewModel: RecipeListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecipeListScreen(
        uiState = uiState,
        onAddRecipe = onAddRecipe,
        onOpenRecipe = onOpenRecipe,
        onImportRecipe = onImportRecipe,
        onManageCategories = onManageCategories,
        onSelectCategory = viewModel::selectCategory,
        onSearch = viewModel::search,
        onSort = viewModel::setSort,
        onOnlyFavorites = viewModel::setOnlyFavorites,
        backupOverdue = backupOverdue,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun RecipeListScreen(
    uiState: RecipeListUiState,
    onAddRecipe: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onImportRecipe: (Uri) -> Unit,
    onManageCategories: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onSort: (RecipeSort) -> Unit,
    onOnlyFavorites: (Boolean) -> Unit,
    backupOverdue: Boolean,
    onOpenSettings: () -> Unit,
) {
    // Any file type: chat apps deliver recipe files as generic documents.
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportRecipe(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    (uiState as? RecipeListUiState.Success)?.let { SortMenu(current = it.sort, onSort = onSort) }
                    AppIconButton(
                        icon = R.drawable.ic_download,
                        contentDescription = stringResource(R.string.import_recipe),
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                    )
                    AppIconButton(
                        icon = R.drawable.ic_label,
                        contentDescription = stringResource(R.string.categories),
                        onClick = onManageCategories,
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRecipe,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                AppIcon(R.drawable.ic_add, contentDescription = stringResource(R.string.add_recipe))
            }
        },
    ) { padding ->
        when (uiState) {
            RecipeListUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is RecipeListUiState.Success -> RecipeList(
                uiState = uiState,
                contentPadding = padding,
                onOpenRecipe = onOpenRecipe,
                onSelectCategory = onSelectCategory,
                onSearch = onSearch,
                onOnlyFavorites = onOnlyFavorites,
                backupOverdue = backupOverdue,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun SortMenu(current: RecipeSort, onSort: (RecipeSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AppIconButton(R.drawable.ic_sort, stringResource(R.string.sort), onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            RecipeSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(stringResource(sort.label())) },
                    leadingIcon = { RadioButton(selected = sort == current, onClick = null) },
                    onClick = {
                        onSort(sort)
                        open = false
                    },
                )
            }
        }
    }
}

private fun RecipeSort.label(): Int = when (this) {
    RecipeSort.RECENT -> R.string.sort_recent
    RecipeSort.A_TO_Z -> R.string.sort_a_to_z
    RecipeSort.COOKED_RECENTLY -> R.string.sort_cooked_recently
    RecipeSort.NOT_FOR_A_WHILE -> R.string.sort_not_for_a_while
}

@Composable
private fun RecipeList(
    uiState: RecipeListUiState.Success,
    contentPadding: PaddingValues,
    onOpenRecipe: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onOnlyFavorites: (Boolean) -> Unit,
    backupOverdue: Boolean,
    onOpenSettings: () -> Unit,
) {
    // The typed text lives here so the field never lags behind fast typing; the view model only filters.
    var query by rememberSaveable { mutableStateOf("") }
    // After the screen is restored the view model may be new, so it is told the saved text again.
    LaunchedEffect(query) { onSearch(query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        if (backupOverdue && !uiState.cookbookIsEmpty) {
            // Said in words, not only by colour, and gone as soon as a backup folder is set.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.backup_overdue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.backup_overdue_action)) }
            }
        }
        if (!uiState.cookbookIsEmpty) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                trailingIcon = if (query.isNotEmpty()) {
                    { AppIconButton(R.drawable.ic_close, stringResource(R.string.clear_search), onClick = { query = "" }) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            )
        }
        if (!uiState.cookbookIsEmpty) {
            CategoryFilter(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onSelectCategory = onSelectCategory,
                onlyFavorites = uiState.onlyFavorites,
                onOnlyFavorites = onOnlyFavorites,
            )
        }
        when {
            uiState.cookbookIsEmpty -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Doodle.Pot, stringResource(R.string.no_recipes_yet))
            }

            uiState.recipes.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                    when {
                        query.isNotBlank() -> R.string.no_search_results
                        uiState.onlyFavorites -> R.string.no_favorites
                        else -> R.string.no_recipes_in_category
                    },
                ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                val categoryNames = uiState.categories.associate { it.id to it.name }
                // Pictures like a cookbook's index, two across on a phone, more on a tablet.
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    // Bottom space keeps the last card clear of the add button.
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.recipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            categoryName = categoryNames[recipe.categoryId],
                            onClick = { onOpenRecipe(recipe.id) },
                            // A sort or a filter moves cards to their new place instead of redrawing the grid.
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilter(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    onlyFavorites: Boolean,
    onOnlyFavorites: (Boolean) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Favourites narrow whatever category is chosen, so the chip sits apart from the category chips.
        item {
            FilterChip(
                selected = onlyFavorites,
                onClick = { onOnlyFavorites(!onlyFavorites) },
                label = { Text(stringResource(R.string.favorites)) },
                leadingIcon = { AppIcon(R.drawable.ic_favorite, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        item {
            FilterChip(
                selected = selectedCategoryId == null && categories.isNotEmpty(),
                onClick = { onSelectCategory(null) },
                label = { Text(stringResource(R.string.all_recipes)) },
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onSelectCategory(category.id) },
                label = { Text(category.name) },
            )
        }
    }
}

/** One recipe: the photo (or an ink doodle) with the name in the book serif and a line of facts under it. */
@Composable
private fun RecipeCard(recipe: RecipeSummary, categoryName: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(interaction)
            .clip(MaterialTheme.shapes.medium)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick),
    ) {
        val coverPhoto = recipe.coverPhoto
        val photoModifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(MaterialTheme.shapes.medium)
        if (coverPhoto != null) {
            RecipePhoto(name = coverPhoto, modifier = photoModifier)
        } else {
            Box(modifier = photoModifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                DoodleImage(Doodle.Pot)
            }
        }
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val facts = recipeFacts(recipe) ?: categoryName
            if (facts != null) {
                Text(
                    text = facts,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun recipeFacts(recipe: RecipeSummary): String? {
    val resources = LocalContext.current.resources
    val minutes = (recipe.prepMinutes ?: 0) + (recipe.cookMinutes ?: 0) + (recipe.waitMinutes ?: 0)
    return listOfNotNull(
        recipe.rating?.let { "★ $it" },
        minutes.takeIf { it > 0 }?.let { stringResource(R.string.total_minutes, it) },
        recipe.servings?.let { stringResource(R.string.servings_count, it) },
        recipe.timesCooked.takeIf { it > 0 }?.let { resources.getQuantityString(R.plurals.cooked_times, it, it) },
    ).joinToString(" · ").ifEmpty { null }
}
