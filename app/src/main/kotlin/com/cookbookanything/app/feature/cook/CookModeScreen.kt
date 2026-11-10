@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.cook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cookbookanything.app.R
import com.cookbookanything.app.feature.recipedetail.RecipeDetailUiState
import com.cookbookanything.app.feature.recipedetail.RecipeDetailViewModel
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.components.RecipePhoto
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.scaling.scaleIngredient

/** One step at a time in large text, for a phone propped up next to the stove. */
@Composable
fun CookModeRoute(servings: Int?, onClose: () -> Unit, viewModel: RecipeDetailViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Hands are busy while cooking, so the screen must not switch itself off.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val recipe = (uiState as? RecipeDetailUiState.Success)?.recipe
    if (recipe != null && recipe.steps.isNotEmpty()) {
        CookModeScreen(recipe = recipe, servings = servings, onClose = onClose, onStepDone = viewModel::setStepDone)
    }
}

@Composable
fun CookModeScreen(
    recipe: Recipe,
    servings: Int?,
    onClose: () -> Unit,
    onStepDone: (String, Boolean) -> Unit,
) {
    val steps = recipe.steps
    // Starts where the cook left off: the first step that is not ticked yet.
    var index by rememberSaveable { mutableIntStateOf(steps.indexOfFirst { !it.done }.coerceAtLeast(0)) }
    var showIngredients by rememberSaveable { mutableStateOf(false) }
    val position = index.coerceIn(steps.indices)
    val step = steps[position]
    val isLast = position == steps.lastIndex

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text(recipe.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    AppIconButton(R.drawable.ic_close, stringResource(R.string.close), onClick = onClose)
                },
                actions = {
                    if (recipe.ingredients.isNotEmpty()) {
                        TextButton(
                            onClick = { showIngredients = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
                        ) {
                            Text(stringResource(R.string.ingredients))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { index = position - 1 },
                    enabled = position > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.previous))
                }
                Button(
                    onClick = {
                        // Moving on ticks the step, so the recipe page shows the same progress.
                        onStepDone(step.id, true)
                        if (isLast) {
                            onClose()
                        } else {
                            index = position + 1
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(if (isLast) R.string.finish else R.string.next))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.step_of, position + 1, steps.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            LinearProgressIndicator(
                progress = { (position + 1f) / steps.size },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(step.text, style = MaterialTheme.typography.headlineMedium)
            step.photos.forEach { photo ->
                RecipePhoto(
                    name = photo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
        }
    }

    if (showIngredients) {
        val baseServings = recipe.servings
        val scale = if (servings != null && baseServings != null) servings.toDouble() / baseServings else 1.0
        AlertDialog(
            onDismissRequest = { showIngredients = false },
            title = { Text(stringResource(R.string.ingredients)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    recipe.ingredients.forEach { ingredient ->
                        Text(
                            text = "•  " + scaleIngredient(ingredient.text, scale),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIngredients = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }
}
