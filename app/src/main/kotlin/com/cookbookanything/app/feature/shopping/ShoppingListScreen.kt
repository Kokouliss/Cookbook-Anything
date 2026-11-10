@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.shopping

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cookbookanything.app.R
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.components.CheckRow
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.model.ShoppingItem

@Composable
fun ShoppingListRoute(onBack: () -> Unit, viewModel: ShoppingListViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    ShoppingListScreen(entries = items, onBack = onBack, onCheckedChange = viewModel::setChecked)
}

@Composable
fun ShoppingListScreen(
    entries: List<ShoppingItem>?,
    onBack: () -> Unit,
    onCheckedChange: (String, Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text(stringResource(R.string.shopping_list)) },
                navigationIcon = {
                    AppIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back), onClick = onBack)
                },
            )
        },
    ) { padding ->
        when {
            entries == null -> Unit

            entries.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.shopping_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.shopping_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                // The query lists a recipe's ingredients together, so grouping keeps calendar order.
                entries.groupBy { it.recipeId }.forEach { (recipeId, ingredients) ->
                    item(key = recipeId) {
                        Text(
                            text = ingredients.first().recipeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(ingredients, key = { it.ingredientId }) { ingredient ->
                        CheckRow(
                            text = ingredient.text,
                            checked = ingredient.checked,
                            onCheckedChange = { onCheckedChange(ingredient.ingredientId, it) },
                        )
                    }
                }
            }
        }
    }
}
