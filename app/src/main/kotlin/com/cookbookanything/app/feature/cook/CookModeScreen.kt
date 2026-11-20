@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.cook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cookbookanything.app.R
import com.cookbookanything.app.feature.recipedetail.RecipeDetailUiState
import com.cookbookanything.app.feature.recipedetail.RecipeDetailViewModel
import com.cookbookanything.app.feature.recipedetail.StepTimer
import com.cookbookanything.app.ui.components.AppIcon
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.components.PageCard
import com.cookbookanything.app.ui.components.RecipePhoto
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.cooking.ingredientsForStep
import com.cookbookanything.core.domain.cooking.timerMinutes
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.ingredients.isHeading
import com.cookbookanything.core.domain.scaling.scaleIngredient
import com.cookbookanything.core.domain.units.MeasurementSystem
import com.cookbookanything.core.domain.units.convertIngredient
import com.cookbookanything.core.domain.units.convertTemperatures
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One step per card in large text, for a phone propped up next to the stove. Swipe between cards. */
@Composable
fun CookModeRoute(servings: Int?, measurement: MeasurementSystem, onClose: () -> Unit, viewModel: RecipeDetailViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timer by viewModel.timer.collectAsStateWithLifecycle()

    // Hands are busy while cooking, so the screen must not switch itself off.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val recipe = (uiState as? RecipeDetailUiState.Success)?.recipe
    if (recipe != null && recipe.steps.any { !isHeading(it.text) }) {
        CookModeScreen(
            recipe = recipe,
            servings = servings,
            measurement = measurement,
            timer = timer,
            onClose = onClose,
            onStepDone = viewModel::setStepDone,
            onFinished = viewModel::markCooked,
            onStartTimer = viewModel::startTimer,
            onStopTimer = viewModel::stopTimer,
        )
    }
}

@Composable
fun CookModeScreen(
    recipe: Recipe,
    servings: Int?,
    measurement: MeasurementSystem,
    timer: StepTimer?,
    onClose: () -> Unit,
    onStepDone: (String, Boolean) -> Unit,
    onFinished: () -> Unit,
    onStartTimer: (stepId: String, minutes: Int) -> Unit,
    onStopTimer: () -> Unit,
) {
    // Headings are shown on the recipe page only; at the stove each card is a thing to do.
    val steps = recipe.steps.filterNot { isHeading(it.text) }
    // Starts where the cook left off: the first step that is not ticked yet.
    val pager = rememberPagerState(initialPage = steps.indexOfFirst { !it.done }.coerceAtLeast(0)) { steps.size }
    val scope = rememberCoroutineScope()
    var showIngredients by rememberSaveable { mutableStateOf(false) }
    val position = pager.currentPage
    val isLast = position == steps.lastIndex
    val baseServings = recipe.servings
    val scale = if (servings != null && baseServings != null) servings.toDouble() / baseServings else 1.0
    val ingredientTexts = remember(recipe, scale, measurement) {
        recipe.ingredients.filterNot { isHeading(it.text) }.map { convertIngredient(scaleIngredient(it.text, scale), measurement) }
    }

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
                        TextButton(onClick = { showIngredients = true }) { Text(stringResource(R.string.ingredients)) }
                    }
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                StepDots(count = steps.size, current = position)
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { scope.launch { pager.animateScrollToPage(position - 1) } },
                        enabled = position > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.previous))
                    }
                    Button(
                        onClick = {
                            // Moving on ticks the step, so the recipe page shows the same progress.
                            onStepDone(steps[position].id, true)
                            if (isLast) {
                                onFinished()
                                onClose()
                            } else {
                                scope.launch { pager.animateScrollToPage(position + 1) }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(if (isLast) R.string.finish else R.string.next))
                    }
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
        ) { page ->
            val step = steps[page]
            val stepIngredients = remember(step.text, ingredientTexts) { ingredientsForStep(step.text, ingredientTexts) }
            val minutes = remember(step.text) { timerMinutes(step.text) }
            PageCard(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.step_of, page + 1, steps.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(convertTemperatures(step.text, measurement), style = MaterialTheme.typography.headlineMedium)
                    if (minutes != null) {
                        TimerChip(
                            minutes = minutes,
                            timer = timer?.takeIf { it.stepId == step.id },
                            onStart = { onStartTimer(step.id, minutes) },
                            onStop = onStopTimer,
                        )
                    }
                    step.photos.forEach { photo ->
                        RecipePhoto(
                            name = photo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(MaterialTheme.shapes.medium),
                        )
                    }
                    if (stepIngredients.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.for_this_step),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            stepIngredients.forEach { index ->
                                Text("•  " + ingredientTexts[index], style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showIngredients) {
        AlertDialog(
            onDismissRequest = { showIngredients = false },
            title = { Text(stringResource(R.string.ingredients)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ingredientTexts.forEach { ingredient ->
                        Text(
                            text = "•  $ingredient",
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

/** "Timer 20 min" before it runs, the time left while it runs, "Done" when it has rung. */
@Composable
private fun TimerChip(minutes: Int, timer: StepTimer?, onStart: () -> Unit, onStop: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer) {
        while (timer != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val remaining = timer?.let { ((it.endsAt - now) / 1_000).coerceAtLeast(0) }
    val label = when {
        remaining == null -> stringResource(R.string.timer_start, minutes)
        remaining == 0L -> stringResource(R.string.timer_finished_short)
        else -> "%d:%02d".format(remaining / 60, remaining % 60)
    }
    AssistChip(
        onClick = if (timer == null) onStart else onStop,
        label = { Text(label) },
        leadingIcon = {
            AppIcon(
                icon = if (timer == null) R.drawable.ic_timer else R.drawable.ic_stop,
                contentDescription = if (timer == null) null else stringResource(R.string.timer_stop),
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

/** One dot per step, the current one in the family red; read out as "Step 2 of 5". */
@Composable
private fun StepDots(count: Int, current: Int) {
    val description = stringResource(R.string.step_of, current + 1, count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}
