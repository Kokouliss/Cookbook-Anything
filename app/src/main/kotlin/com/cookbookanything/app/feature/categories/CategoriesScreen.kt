@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.categories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cookbookanything.app.R
import com.cookbookanything.app.ui.components.AppIcon
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.components.ConfirmDialog
import com.cookbookanything.app.ui.components.TextInputDialog
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.model.Category

@Composable
fun CategoriesRoute(onBack: () -> Unit, viewModel: CategoriesViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    CategoriesScreen(
        categories = categories,
        onBack = onBack,
        onAdd = viewModel::add,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
    )
}

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    // Dialogs remember the category by id so they survive rotation.
    var renamingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text(stringResource(R.string.categories)) },
                navigationIcon = {
                    AppIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back), onClick = onBack)
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }, containerColor = MaterialTheme.colorScheme.secondary) {
                AppIcon(R.drawable.ic_add, contentDescription = stringResource(R.string.new_category))
            }
        },
    ) { padding ->
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_categories_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        AppIconButton(
                            icon = R.drawable.ic_edit,
                            contentDescription = stringResource(R.string.rename),
                            onClick = { renamingId = category.id },
                        )
                        AppIconButton(
                            icon = R.drawable.ic_delete,
                            contentDescription = stringResource(R.string.delete),
                            onClick = { deletingId = category.id },
                        )
                    }
                }
            }
        }
    }

    if (adding) {
        TextInputDialog(
            title = stringResource(R.string.new_category),
            label = stringResource(R.string.category_name),
            initialValue = "",
            onConfirm = { name ->
                adding = false
                onAdd(name)
            },
            onDismiss = { adding = false },
        )
    }

    categories.firstOrNull { it.id == renamingId }?.let { category ->
        TextInputDialog(
            title = stringResource(R.string.rename),
            label = stringResource(R.string.category_name),
            initialValue = category.name,
            onConfirm = { name ->
                renamingId = null
                onRename(category.id, name)
            },
            onDismiss = { renamingId = null },
        )
    }

    categories.firstOrNull { it.id == deletingId }?.let { category ->
        ConfirmDialog(
            title = stringResource(R.string.delete_category_title, category.name),
            text = stringResource(R.string.delete_category_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                deletingId = null
                onDelete(category.id)
            },
            onDismiss = { deletingId = null },
        )
    }
}
