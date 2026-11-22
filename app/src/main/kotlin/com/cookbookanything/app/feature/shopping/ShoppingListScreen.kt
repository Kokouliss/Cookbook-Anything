@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.shopping

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.cookbookanything.core.domain.shopping.Aisle
import com.cookbookanything.core.domain.shopping.MergedShoppingItem
import com.cookbookanything.core.domain.shopping.toBuyText

private const val LIST_ANYTHING_PACKAGE = "com.kokouliss.listanything"

@Composable
fun ShoppingListRoute(onBack: () -> Unit, viewModel: ShoppingListViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val thisWeek by viewModel.thisWeek.collectAsStateWithLifecycle()
    ShoppingListScreen(
        entries = items,
        thisWeek = thisWeek,
        onThisWeek = viewModel::setThisWeek,
        onBack = onBack,
        onCheckedChange = viewModel::setChecked,
        onMoveToAisle = viewModel::moveToAisle,
    )
}

@Composable
fun ShoppingListScreen(
    entries: List<MergedShoppingItem>?,
    thisWeek: Boolean,
    onThisWeek: (Boolean) -> Unit,
    onBack: () -> Unit,
    onCheckedChange: (List<String>, Boolean) -> Unit,
    onMoveToAisle: (productKey: String, Aisle) -> Unit,
) {
    val context = LocalContext.current
    var moving by remember { mutableStateOf<MergedShoppingItem?>(null) }
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
                            label = stringResource(R.string.label_send),
                            onClick = {
                                // List Anything takes each line as an item, so it gets the flat list; anyone else gets the aisles.
                                val text = if (listAnythingInstalled) entries.toBuyText() else entries.byAisle().joinToString("\n\n") { (aisle, items) ->
                                    context.getString(aisle.label()) + "\n" + items.filterNot { it.checked }.joinToString("\n") { it.label }
                                }
                                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                                if (listAnythingInstalled) send.setPackage(LIST_ANYTHING_PACKAGE)
                                context.startActivity(if (listAnythingInstalled) send else Intent.createChooser(send, shareTitle))
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                listOf(true to R.string.shopping_this_week, false to R.string.shopping_all_planned).forEachIndexed { index, (week, label) ->
                    SegmentedButton(
                        selected = thisWeek == week,
                        onClick = { onThisWeek(week) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            when {
                entries == null -> Unit

                entries.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(Doodle.Basket, stringResource(R.string.shopping_empty))
                }

                else -> ShoppingItems(entries, onCheckedChange, onMoveToAisle = { moving = it })
            }
        }
    }

    moving?.let { item ->
        MoveToAisleDialog(
            item = item,
            onMove = { aisle ->
                onMoveToAisle(item.productKey, aisle)
                moving = null
            },
            onDismiss = { moving = null },
        )
    }
}

@Composable
private fun ShoppingItems(
    entries: List<MergedShoppingItem>,
    onCheckedChange: (List<String>, Boolean) -> Unit,
    onMoveToAisle: (MergedShoppingItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
        entries.byAisle().forEach { (aisle, aisleItems) ->
            item(key = "aisle-${aisle.name}") {
                Text(
                    text = stringResource(aisle.label()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            items(aisleItems, key = { it.ingredientIds.first() }) { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CheckRow(
                        text = item.label,
                        checked = item.checked,
                        onCheckedChange = { onCheckedChange(item.ingredientIds, it) },
                        modifier = Modifier.weight(1f),
                        // Which recipes want it, only when more than one does.
                        detail = item.recipeTitles.takeIf { it.size > 1 }?.let { stringResource(R.string.shopping_from, it.joinToString(", ")) },
                    )
                    AppIconButton(R.drawable.ic_more_vert, stringResource(R.string.move_to_aisle), onClick = { onMoveToAisle(item) }, label = stringResource(R.string.label_move))
                }
            }
        }
    }
}

@Composable
private fun MoveToAisleDialog(item: MergedShoppingItem, onMove: (Aisle) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Aisle.entries.forEach { aisle ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMove(aisle) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = aisle == item.aisle, onClick = null)
                        Text(stringResource(aisle.label()), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** The list in shop order: the aisles as they come walking in, each with its items in list order. */
private fun List<MergedShoppingItem>.byAisle(): List<Pair<Aisle, List<MergedShoppingItem>>> =
    groupBy { it.aisle }.toSortedMap().map { (aisle, items) -> aisle to items }

private fun Aisle.label(): Int = when (this) {
    Aisle.PRODUCE -> R.string.aisle_produce
    Aisle.DAIRY_EGGS -> R.string.aisle_dairy_eggs
    Aisle.MEAT_FISH -> R.string.aisle_meat_fish
    Aisle.BAKERY -> R.string.aisle_bakery
    Aisle.PASTA_GRAINS -> R.string.aisle_pasta_grains
    Aisle.FLOUR_SUGAR -> R.string.aisle_flour_sugar
    Aisle.OILS_VINEGARS -> R.string.aisle_oils_vinegars
    Aisle.SAUCES -> R.string.aisle_sauces
    Aisle.HERBS_SPICES -> R.string.aisle_herbs_spices
    Aisle.PANTRY -> R.string.aisle_pantry
    Aisle.FREEZER -> R.string.aisle_freezer
    Aisle.OTHER -> R.string.aisle_other
}
