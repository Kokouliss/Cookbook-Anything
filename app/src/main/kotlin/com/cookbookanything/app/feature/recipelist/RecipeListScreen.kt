@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.recipelist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
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
import com.cookbookanything.app.ui.components.IndexTab
import com.cookbookanything.app.ui.components.PageCard
import com.cookbookanything.app.ui.components.StruckText
import com.cookbookanything.app.ui.components.RecipePhoto
import com.cookbookanything.app.ui.components.pressScale
import com.cookbookanything.app.ui.components.sharedPhoto
import com.cookbookanything.app.ui.theme.RecipeNameFont
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.search.RecipeFilter
import com.cookbookanything.core.domain.sort.RecipeSort

@Composable
fun RecipeListRoute(
    onAddRecipe: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onImportRecipe: (Uri) -> Unit,
    onManageCategories: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenShoppingList: () -> Unit,
    backupOverdue: Boolean,
    onOpenSettings: () -> Unit,
    viewModel: RecipeListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var view by remember { mutableStateOf(RecipeViewPreferences.get(context)) }
    RecipeListScreen(
        uiState = uiState,
        view = view,
        onView = {
            RecipeViewPreferences.set(context, it)
            view = it
        },
        onAddRecipe = onAddRecipe,
        onOpenRecipe = onOpenRecipe,
        onImportRecipe = onImportRecipe,
        onManageCategories = onManageCategories,
        onOpenCalendar = onOpenCalendar,
        onOpenShoppingList = onOpenShoppingList,
        onSelectCategory = viewModel::selectCategory,
        onSearch = viewModel::search,
        onSort = viewModel::setSort,
        onOnlyFavorites = viewModel::setOnlyFavorites,
        onFilter = viewModel::setFilter,
        backupOverdue = backupOverdue,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun RecipeListScreen(
    uiState: RecipeListUiState,
    view: RecipeView,
    onView: (RecipeView) -> Unit,
    onAddRecipe: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onImportRecipe: (Uri) -> Unit,
    onManageCategories: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenShoppingList: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onSort: (RecipeSort) -> Unit,
    onOnlyFavorites: (Boolean) -> Unit,
    onFilter: (RecipeFilter) -> Unit,
    backupOverdue: Boolean,
    onOpenSettings: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val atTop by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 } }
    // Any file type: chat apps deliver recipe files as generic documents.
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportRecipe(uri)
    }
    // The typed text lives here so the field never lags behind fast typing; the view model only filters.
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    // After the screen is restored the view model may be new, so it is told the saved text again.
    LaunchedEffect(query) { onSearch(query) }
    val closeSearch = {
        searching = false
        query = ""
    }
    BackHandler(enabled = searching, onBack = closeSearch)
    val hasRecipes = (uiState as? RecipeListUiState.Success)?.cookbookIsEmpty == false

    Scaffold(
        topBar = {
            if (searching) {
                SearchBar(query = query, onQuery = { query = it }, onClose = closeSearch)
            } else {
                TopAppBar(
                    colors = appBarColors(),
                    // One line always: on a narrow phone the four actions leave the title little room.
                    title = { Text(stringResource(R.string.tab_recipes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    actions = {
                        // The three places a cook goes from here, each with its word. The rest is under "More".
                        if (hasRecipes) {
                            AppIconButton(R.drawable.ic_search, stringResource(R.string.search_hint), onClick = { searching = true }, label = stringResource(R.string.label_search))
                            AppIconButton(R.drawable.ic_calendar_plus, stringResource(R.string.plan_a_meal), onClick = onOpenCalendar, label = stringResource(R.string.label_plan))
                            AppIconButton(R.drawable.ic_shopping_cart, stringResource(R.string.shopping_list), onClick = onOpenShoppingList, label = stringResource(R.string.label_shopping))
                        }
                        MoreMenu(onImport = { importLauncher.launch(arrayOf("*/*")) }, onManageCategories = onManageCategories)
                    },
                )
            }
        },
        floatingActionButton = {
            val pressing = remember { MutableInteractionSource() }
            ExtendedFloatingActionButton(
                onClick = onAddRecipe,
                modifier = Modifier.pressScale(pressing),
                interactionSource = pressing,
                // Folds to the plus while the list is scrolled down, so it covers less; the word comes back at the top.
                expanded = atTop,
                icon = { AppIcon(R.drawable.ic_add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_recipe)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
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
                view = view,
                onView = onView,
                query = query,
                onClearQuery = { query = "" },
                gridState = gridState,
                contentPadding = padding,
                onAddRecipe = onAddRecipe,
                onOpenRecipe = onOpenRecipe,
                onSelectCategory = onSelectCategory,
                onSort = onSort,
                onOnlyFavorites = onOnlyFavorites,
                onFilter = onFilter,
                backupOverdue = backupOverdue,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

/** The top bar while searching: the field in place of the title, and one way back. */
@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TopAppBar(
        colors = appBarColors(),
        navigationIcon = { AppIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back), onClick = onClose) },
        title = {
            TextField(
                value = query,
                onValueChange = onQuery,
                placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
        },
        actions = {
            if (query.isNotEmpty()) {
                AppIconButton(R.drawable.ic_close, stringResource(R.string.clear_search), onClick = { onQuery("") })
            }
        },
    )
}

/** What is needed now and then: a recipe file to bring in, and the categories. */
@Composable
private fun MoreMenu(onImport: () -> Unit, onManageCategories: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AppIconButton(R.drawable.ic_more_vert, stringResource(R.string.more), onClick = { open = true }, label = stringResource(R.string.label_more))
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                Triple(R.string.import_recipe, R.drawable.ic_download, onImport),
                Triple(R.string.categories, R.drawable.ic_label, onManageCategories),
            ).forEach { (label, icon, action) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    leadingIcon = { AppIcon(icon, contentDescription = null) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
        }
    }
}

/** How the recipes are shown: the size of the pictures and the order they come in. */
@Composable
private fun ViewMenu(view: RecipeView, onView: (RecipeView) -> Unit, sort: RecipeSort, onSort: (RecipeSort) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        AppIconButton(R.drawable.ic_view, stringResource(R.string.view_and_sort), onClick = { open = true }, label = stringResource(R.string.label_view))
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuHeading(R.string.view_size)
            RecipeView.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label())) },
                    leadingIcon = { RadioButton(selected = option == view, onClick = null) },
                    onClick = {
                        onView(option)
                        open = false
                    },
                )
            }
            HorizontalDivider()
            MenuHeading(R.string.sort)
            RecipeSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label())) },
                    leadingIcon = { RadioButton(selected = option == sort, onClick = null) },
                    onClick = {
                        onSort(option)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MenuHeading(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

private fun RecipeView.label(): Int = when (this) {
    RecipeView.SMALL -> R.string.view_small
    RecipeView.MEDIUM -> R.string.view_medium
    RecipeView.LARGE -> R.string.view_large
    RecipeView.LIST -> R.string.view_list
}

/** The narrowest a tile may get; the grid fits as many as the screen holds. */
private val RecipeView.cell: Dp
    get() = when (this) {
        RecipeView.SMALL -> 100.dp
        RecipeView.MEDIUM -> 160.dp
        RecipeView.LARGE, RecipeView.LIST -> 320.dp
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
    view: RecipeView,
    onView: (RecipeView) -> Unit,
    query: String,
    onClearQuery: () -> Unit,
    gridState: LazyGridState,
    contentPadding: PaddingValues,
    onAddRecipe: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSort: (RecipeSort) -> Unit,
    onOnlyFavorites: (Boolean) -> Unit,
    onFilter: (RecipeFilter) -> Unit,
    backupOverdue: Boolean,
    onOpenSettings: () -> Unit,
) {
    var editingFilter by rememberSaveable { mutableStateOf(false) }
    // The tiles rise into place one after another the first time the list opens, not on every return.
    var risen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(RISE_ALL_MILLIS)
        risen = true
    }

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
            GettingStartedCard(uiState.gettingStarted)
            CategoryFilter(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onSelectCategory = onSelectCategory,
                onlyFavorites = uiState.onlyFavorites,
                onOnlyFavorites = onOnlyFavorites,
                tags = uiState.allTags,
                filter = uiState.filter,
                onFilter = onFilter,
                onEditFilter = { editingFilter = true },
                view = view,
                onView = onView,
                sort = uiState.sort,
                onSort = onSort,
            )
        }
        when {
            uiState.cookbookIsEmpty -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    doodle = Doodle.Pot,
                    title = stringResource(R.string.empty_cookbook_title),
                    text = stringResource(R.string.no_recipes_yet),
                    action = stringResource(R.string.add_recipe),
                    onAction = onAddRecipe,
                )
            }

            // Nothing found is its own page: what was looked for, and the way back out of it.
            uiState.recipes.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    query.isNotBlank() -> EmptyState(
                        doodle = Doodle.Whisk,
                        text = stringResource(R.string.no_search_results),
                        action = stringResource(R.string.clear_search),
                        onAction = onClearQuery,
                    )

                    !uiState.filter.isEmpty -> EmptyState(
                        doodle = Doodle.Whisk,
                        text = stringResource(R.string.no_filter_results),
                        action = stringResource(R.string.clear_filters),
                        onAction = { onFilter(RecipeFilter()) },
                    )

                    else -> EmptyState(
                        doodle = Doodle.Whisk,
                        text = stringResource(if (uiState.onlyFavorites) R.string.no_favorites else R.string.no_recipes_in_category),
                    )
                }
            }

            else -> {
                val categoryNames = uiState.categories.associate { it.id to it.name }
                // Pictures like a cookbook's index, as many across as the picked size lets the screen hold.
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(view.cell),
                    // Bottom space keeps the last card clear of the add button.
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (view == RecipeView.LIST) 4.dp else 12.dp),
                ) {
                    itemsIndexed(uiState.recipes, key = { _, recipe -> recipe.id }) { index, recipe ->
                        RecipeCard(
                            recipe = recipe,
                            categoryName = categoryNames[recipe.categoryId],
                            view = view,
                            onClick = { onOpenRecipe(recipe.id) },
                            // A sort or a filter moves cards to their new place instead of redrawing the grid.
                            modifier = Modifier
                                .animateItem()
                                .then(if (risen) Modifier else Modifier.riseIn(index)),
                        )
                    }
                }
            }
        }
    }

    if (editingFilter) {
        FilterDialog(filter = uiState.filter, onFilter = onFilter, onDismiss = { editingFilter = false })
    }
}

/** "Getting started, 2 of 5 done": the first things to try, until all are done or the cook hides it. */
@Composable
private fun GettingStartedCard(started: GettingStarted) {
    val context = LocalContext.current
    var hidden by remember { mutableStateOf(GettingStartedPreferences.isHidden(context)) }
    if (hidden || started.allDone) return
    val labels = listOf(R.string.started_add, R.string.started_import, R.string.started_plan, R.string.started_cook, R.string.started_favorite)
    PageCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.getting_started), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.started_progress, started.done, started.steps.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            started.steps.forEachIndexed { index, done ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Text(if (done) "✓" else "○", color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    StruckText(
                        text = stringResource(labels[index]),
                        struck = done,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            TextButton(
                onClick = {
                    GettingStartedPreferences.hide(context)
                    hidden = true
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.hide))
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
    tags: List<String>,
    filter: RecipeFilter,
    onFilter: (RecipeFilter) -> Unit,
    onEditFilter: () -> Unit,
    view: RecipeView,
    onView: (RecipeView) -> Unit,
    sort: RecipeSort,
    onSort: (RecipeSort) -> Unit,
) {
    // What narrows the list scrolls on the left; how it is shown stays put on the right.
    Row(verticalAlignment = Alignment.CenterVertically) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                // Time, cooked, rating and pantry live in a dialog; the chip says how many of them are set.
                val set = listOf(filter.maxMinutes != null, filter.cooked != null, filter.minRating != null, filter.pantry.isNotBlank()).count { it }
                FilterChip(
                    selected = set > 0,
                    onClick = onEditFilter,
                    label = { Text(if (set > 0) stringResource(R.string.filters) + " ($set)" else stringResource(R.string.filters)) },
                )
            }
            item {
                FilterChip(
                    selected = onlyFavorites,
                    onClick = { onOnlyFavorites(!onlyFavorites) },
                    label = { Text(stringResource(R.string.favorites)) },
                    // Filled heart when on, outline when off: told apart by shape, not only by the chip's shade.
                    leadingIcon = {
                        AppIcon(if (onlyFavorites) R.drawable.ic_favorite else R.drawable.ic_favorite_border, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
            items(tags, key = { "tag-$it" }) { tag ->
                val selected = tag in filter.tags
                FilterChip(
                    selected = selected,
                    onClick = { onFilter(filter.copy(tags = if (selected) filter.tags - tag else filter.tags + tag)) },
                    label = { Text("#$tag") },
                )
            }
        }
        ViewMenu(view = view, onView = onView, sort = sort, onSort = onSort, modifier = Modifier.padding(end = 8.dp))
    }
    if (categories.isNotEmpty()) {
        // The categories are the divider tabs of the book, standing on a hairline the recipes slide under.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            item { IndexTab(stringResource(R.string.all_recipes), selected = selectedCategoryId == null, onClick = { onSelectCategory(null) }) }
            items(categories, key = { it.id }) { category ->
                IndexTab(category.name, selected = selectedCategoryId == category.id, onClick = { onSelectCategory(category.id) })
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Total time, cooked or not, rating and "what's in my pantry"; applied as each chip is tapped. */
@Composable
private fun FilterDialog(filter: RecipeFilter, onFilter: (RecipeFilter) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filters)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.filter_time), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60).forEach { minutes ->
                        FilterChip(
                            selected = filter.maxMinutes == minutes,
                            onClick = { onFilter(filter.copy(maxMinutes = minutes.takeIf { it != filter.maxMinutes })) },
                            label = { Text(stringResource(R.string.filter_under_minutes, minutes)) },
                        )
                    }
                }
                Text(stringResource(R.string.filter_cooked), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true to R.string.filter_cooked_yes, false to R.string.filter_cooked_no).forEach { (cooked, label) ->
                        FilterChip(
                            selected = filter.cooked == cooked,
                            onClick = { onFilter(filter.copy(cooked = cooked.takeIf { it != filter.cooked })) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
                Text(stringResource(R.string.filter_rating), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 4, 5).forEach { stars ->
                        FilterChip(
                            selected = filter.minRating == stars,
                            onClick = { onFilter(filter.copy(minRating = stars.takeIf { it != filter.minRating })) },
                            label = { Text("$stars+ ★") },
                        )
                    }
                }
                OutlinedTextField(
                    value = filter.pantry,
                    onValueChange = { onFilter(filter.copy(pantry = it)) },
                    label = { Text(stringResource(R.string.filter_pantry)) },
                    placeholder = { Text(stringResource(R.string.filter_pantry_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
        dismissButton = { TextButton(onClick = { onFilter(RecipeFilter()) }) { Text(stringResource(R.string.clear_filters)) } },
    )
}

/** One recipe: the photo (or an ink doodle) with the name in the book serif and a line of facts, sized by [view]. */
@Composable
private fun RecipeCard(recipe: RecipeSummary, categoryName: String?, view: RecipeView, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val card = modifier
        .pressScale(interaction)
        .clip(MaterialTheme.shapes.medium)
        .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
    val photo: @Composable (Modifier) -> Unit = { shape ->
        val coverPhoto = recipe.coverPhoto
        val photoModifier = shape.clip(if (view == RecipeView.LIST) MaterialTheme.shapes.small else MaterialTheme.shapes.medium)
        if (coverPhoto != null) {
            RecipePhoto(name = coverPhoto, modifier = Modifier.sharedPhoto(coverPhoto).then(photoModifier))
        } else {
            Box(modifier = photoModifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                // The doodle is drawn for a medium tile; smaller pictures get a smaller one.
                DoodleImage(Doodle.Pot, modifier = if (view == RecipeView.SMALL || view == RecipeView.LIST) Modifier.size(40.dp) else Modifier)
            }
        }
    }
    val words: @Composable (TextStyle, Boolean) -> Unit = { nameStyle, withFacts ->
        Text(text = recipe.title, style = nameStyle.copy(fontFamily = RecipeNameFont), maxLines = 2, overflow = TextOverflow.Ellipsis)
        val facts = if (withFacts) recipeFacts(recipe) ?: categoryName else null
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
    if (view == RecipeView.LIST) {
        Row(modifier = card.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            photo(Modifier.size(64.dp))
            Column(modifier = Modifier.padding(start = 12.dp, end = 4.dp)) { words(MaterialTheme.typography.titleMedium, true) }
        }
        return
    }
    Column(modifier = card) {
        photo(
            Modifier
                .fillMaxWidth()
                .aspectRatio(
                    when (view) {
                        RecipeView.SMALL -> 1f
                        RecipeView.LARGE -> 16f / 9f
                        else -> 4f / 3f
                    },
                ),
        )
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
            when (view) {
                // Three across leaves room for the name only.
                RecipeView.SMALL -> words(MaterialTheme.typography.titleSmall, false)
                RecipeView.LARGE -> words(MaterialTheme.typography.titleLarge, true)
                else -> words(MaterialTheme.typography.titleMedium, true)
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

private const val RISE_MILLIS = 320
private const val RISE_STAGGER_MILLIS = 40L
private const val RISE_MAX_STAGGERED = 8
private const val RISE_ALL_MILLIS = RISE_MAX_STAGGERED * RISE_STAGGER_MILLIS + RISE_MILLIS + 60L

/** Fades a tile in while it rises a little, the [index]th a moment after the one before. */
@Composable
private fun Modifier.riseIn(index: Int): Modifier {
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(RISE_MAX_STAGGERED) * RISE_STAGGER_MILLIS)
        rise.animateTo(1f, tween(RISE_MILLIS, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        alpha = rise.value
        translationY = (1f - rise.value) * 24.dp.toPx()
    }
}
