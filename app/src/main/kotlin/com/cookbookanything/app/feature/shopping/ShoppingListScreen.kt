@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.shopping

import android.content.Intent
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cookbookanything.app.R
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.components.CheckRow
import com.cookbookanything.app.ui.components.Doodle
import com.cookbookanything.app.ui.components.EmptyState
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.shopping.MergedShoppingItem
import com.cookbookanything.core.domain.shopping.toBuyText

private const val LIST_ANYTHING_PACKAGE = "com.kokouliss.listanything"

@Composable
fun ShoppingListRoute(onBack: () -> Unit, viewModel: ShoppingListViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    ShoppingListScreen(entries = items, onBack = onBack, onCheckedChange = viewModel::setChecked)
}

@Composable
fun ShoppingListScreen(
    entries: List<MergedShoppingItem>?,
    onBack: () -> Unit,
    onCheckedChange: (List<String>, Boolean) -> Unit,
) {
    val context = LocalContext.current
    // List Anything, the sister app, takes shared text as list items; any other app gets the chooser.
    val listAnythingInstalled = remember { context.packageManager.getLaunchIntentForPackage(LIST_ANYTHING_PACKAGE) != null }
    val shareTitle = stringResource(R.string.shopping_share)

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text(stringResource(R.string.shopping_list)) },
                navigationIcon = {
                    AppIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back), onClick = onBack)
                },
                actions = {
                    if (!entries.isNullOrEmpty()) {
                        AppIconButton(
                            icon = R.drawable.ic_send,
                            contentDescription = stringResource(if (listAnythingInstalled) R.string.shopping_send else R.string.shopping_share),
                            onClick = {
                                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, entries.toBuyText())
                                if (listAnythingInstalled) send.setPackage(LIST_ANYTHING_PACKAGE)
                                context.startActivity(if (listAnythingInstalled) send else Intent.createChooser(send, shareTitle))
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            entries == null -> Unit

            entries.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(Doodle.Basket, stringResource(R.string.shopping_empty))
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
                items(entries, key = { it.ingredientIds.first() }) { item ->
                    CheckRow(
                        text = item.label,
                        checked = item.checked,
                        onCheckedChange = { onCheckedChange(item.ingredientIds, it) },
                        // Which recipes want it, only when more than one does.
                        detail = item.recipeTitles.takeIf { it.size > 1 }?.let { stringResource(R.string.shopping_from, it.joinToString(", ")) },
                    )
                }
            }
        }
    }
}
