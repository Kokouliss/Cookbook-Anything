package com.cookbookanything.app.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.cookbookanything.app.BuildConfig
import com.cookbookanything.app.MainEvent
import com.cookbookanything.app.MainViewModel
import com.cookbookanything.app.R
import com.cookbookanything.app.feature.calendar.CalendarRoute
import com.cookbookanything.app.feature.categories.CategoriesRoute
import com.cookbookanything.app.feature.recipedetail.RecipeDetailRoute
import com.cookbookanything.app.feature.recipedetail.RecipeDetailViewModel
import com.cookbookanything.app.feature.recipeedit.RecipeEditRoute
import com.cookbookanything.app.feature.recipeedit.RecipeEditViewModel
import com.cookbookanything.app.feature.recipelist.RecipeListRoute
import com.cookbookanything.app.feature.settings.SettingsScreen
import com.cookbookanything.app.ui.components.AppIcon
import com.cookbookanything.core.domain.FreeTier

private val topLevelKeys: List<NavKey> = listOf(RecipeListKey, CalendarKey, SettingsKey)

@Composable
fun CookbookApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(RecipeListKey)
    val snackbarHostState = remember { SnackbarHostState() }
    var showPaywall by rememberSaveable { mutableStateOf(false) }
    val importFailedMessage = stringResource(R.string.import_failed)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MainEvent.OpenRecipe -> backStack.add(RecipeDetailKey(event.recipeId))
                MainEvent.ImportFailed -> snackbarHostState.showSnackbar(importFailedMessage)
                MainEvent.LimitReached -> showPaywall = true
            }
        }
    }

    val currentKey = backStack.lastOrNull()
    Scaffold(
        bottomBar = {
            if (currentKey in topLevelKeys) {
                CookbookBottomBar(
                    currentKey = currentKey,
                    onSelect = { key ->
                        // The recipe list stays at the bottom of the stack, so Back from another tab returns to it.
                        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        if (key != RecipeListKey) backStack.add(key)
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Each screen has its own Scaffold that handles the system bars.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<RecipeListKey> {
                    RecipeListRoute(
                        onAddRecipe = {
                            if (uiState.canAddRecipe) {
                                backStack.add(RecipeEditKey())
                            } else {
                                showPaywall = true
                            }
                        },
                        onOpenRecipe = { backStack.add(RecipeDetailKey(it)) },
                        onImportRecipe = viewModel::importRecipe,
                        onManageCategories = { backStack.add(CategoriesKey) },
                    )
                }
                entry<RecipeDetailKey> { key ->
                    RecipeDetailRoute(
                        onBack = { backStack.removeLastOrNull() },
                        onEdit = { backStack.add(RecipeEditKey(key.recipeId)) },
                        viewModel = hiltViewModel<RecipeDetailViewModel, RecipeDetailViewModel.Factory> { factory ->
                            factory.create(key.recipeId)
                        },
                    )
                }
                entry<RecipeEditKey> { key ->
                    RecipeEditRoute(
                        onClose = { backStack.removeLastOrNull() },
                        onSaved = { recipeId ->
                            backStack.removeLastOrNull()
                            // A new recipe opens on its page; an edited one is already underneath.
                            if (key.recipeId == null) backStack.add(RecipeDetailKey(recipeId))
                        },
                        viewModel = hiltViewModel<RecipeEditViewModel, RecipeEditViewModel.Factory> { factory ->
                            factory.create(key.recipeId)
                        },
                    )
                }
                entry<CategoriesKey> {
                    CategoriesRoute(onBack = { backStack.removeLastOrNull() })
                }
                entry<CalendarKey> {
                    CalendarRoute(onOpenRecipe = { backStack.add(RecipeDetailKey(it)) })
                }
                entry<SettingsKey> {
                    SettingsScreen(
                        recipeCount = uiState.recipeCount,
                        unlocked = uiState.unlocked,
                        onUnlock = { showPaywall = true },
                        onManageCategories = { backStack.add(CategoriesKey) },
                    )
                }
            },
        )
    }

    if (showPaywall) {
        PaywallDialog(
            onUnlock = {
                viewModel.unlock()
                showPaywall = false
            },
            onDismiss = { showPaywall = false },
        )
    }
}

@Composable
private fun CookbookBottomBar(currentKey: NavKey?, onSelect: (NavKey) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = currentKey == RecipeListKey,
            onClick = { onSelect(RecipeListKey) },
            icon = { AppIcon(R.drawable.ic_restaurant, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_recipes)) },
        )
        NavigationBarItem(
            selected = currentKey == CalendarKey,
            onClick = { onSelect(CalendarKey) },
            icon = { AppIcon(R.drawable.ic_calendar, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_calendar)) },
        )
        NavigationBarItem(
            selected = currentKey == SettingsKey,
            onClick = { onSelect(SettingsKey) },
            icon = { AppIcon(R.drawable.ic_settings, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_settings)) },
        )
    }
}

@Composable
private fun PaywallDialog(onUnlock: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppIcon(R.drawable.ic_lock, contentDescription = null) },
        title = { Text(stringResource(R.string.paywall_title)) },
        text = { Text(stringResource(R.string.paywall_text, FreeTier.RECIPE_LIMIT)) },
        confirmButton = {
            // shortcut: test builds unlock for free until Google Play Billing is wired in
            if (BuildConfig.DEBUG) {
                TextButton(onClick = onUnlock) { Text(stringResource(R.string.paywall_unlock_test)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
