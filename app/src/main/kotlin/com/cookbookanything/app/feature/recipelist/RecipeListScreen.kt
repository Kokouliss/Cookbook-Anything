@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.recipelist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cookbookanything.app.R
import com.cookbookanything.app.ui.components.AppIcon
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.components.RecipePhoto
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.RecipeSummary

@Composable
fun RecipeListRoute(
    onAddRecipe: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onImportRecipe: (Uri) -> Unit,
    onManageCategories: () -> Unit,
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
            FloatingActionButton(onClick = onAddRecipe, containerColor = MaterialTheme.colorScheme.secondary) {
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
            )
        }
    }
}

@Composable
private fun RecipeList(
    uiState: RecipeListUiState.Success,
    contentPadding: PaddingValues,
    onOpenRecipe: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        if (uiState.categories.isNotEmpty()) {
            CategoryFilter(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onSelectCategory = onSelectCategory,
            )
        }
        if (uiState.recipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (uiState.cookbookIsEmpty) R.string.no_recipes_yet else R.string.no_recipes_in_category,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            val categoryNames = uiState.categories.associate { it.id to it.name }
            LazyColumn(
                // Bottom space keeps the last card clear of the add button.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.recipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        categoryName = categoryNames[recipe.categoryId],
                        onClick = { onOpenRecipe(recipe.id) },
                    )
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
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
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

@Composable
private fun RecipeCard(recipe: RecipeSummary, categoryName: String?, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val photoModifier = Modifier
                .padding(8.dp)
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
            val coverPhoto = recipe.coverPhoto
            if (coverPhoto != null) {
                RecipePhoto(name = coverPhoto, modifier = photoModifier)
            } else {
                Box(
                    modifier = photoModifier.background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(R.drawable.ic_restaurant, contentDescription = null)
                }
            }
            Column(modifier = Modifier.padding(end = 16.dp)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (categoryName != null) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
