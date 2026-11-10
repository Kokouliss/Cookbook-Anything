@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cookbookanything.app.R
import com.cookbookanything.app.ui.components.AppIcon
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.components.DateTimePickerDialog
import com.cookbookanything.app.ui.components.currentLocale
import com.cookbookanything.app.ui.components.rememberNotificationPermissionRequest
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.calendar.monthGrid
import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.model.ScheduledRecipe
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarRoute(onOpenRecipe: (String) -> Unit, viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        uiState = uiState,
        onOpenRecipe = onOpenRecipe,
        onSchedule = viewModel::schedule,
        onRemove = viewModel::remove,
    )
}

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onOpenRecipe: (String) -> Unit,
    onSchedule: (String, Long) -> Unit,
    onRemove: (String) -> Unit,
) {
    val locale = currentLocale()
    val zone = remember { ZoneId.systemDefault() }
    // Saved as plain numbers so the selection survives rotation.
    var monthIndex by rememberSaveable { mutableIntStateOf(YearMonth.now().let { it.year * 12 + it.monthValue - 1 }) }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var pickingRecipe by rememberSaveable { mutableStateOf(false) }
    var pickedRecipeId by rememberSaveable { mutableStateOf<String?>(null) }

    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val byDate = remember(uiState.schedule, zone) {
        uiState.schedule.groupBy { Instant.ofEpochMilli(it.scheduledAt).atZone(zone).toLocalDate() }
    }
    val requestNotificationPermission = rememberNotificationPermissionRequest()

    Scaffold(
        topBar = { TopAppBar(colors = appBarColors(), title = { Text(stringResource(R.string.tab_calendar)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { pickingRecipe = true }, containerColor = MaterialTheme.colorScheme.secondary) {
                AppIcon(R.drawable.ic_add, contentDescription = stringResource(R.string.schedule_recipe))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item {
                MonthHeader(
                    month = month,
                    locale = locale,
                    onPrevious = { monthIndex -= 1 },
                    onNext = { monthIndex += 1 },
                )
            }
            item {
                MonthGrid(
                    month = month,
                    locale = locale,
                    selectedDate = selectedDate,
                    datesWithEntries = byDate.keys,
                    onSelect = { selectedEpochDay = it.toEpochDay() },
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            val entries = byDate[selectedDate].orEmpty()
            if (entries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.nothing_scheduled),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            items(entries, key = { it.id }) { entry ->
                ScheduledRow(
                    entry = entry,
                    zone = zone,
                    locale = locale,
                    onOpen = { onOpenRecipe(entry.recipeId) },
                    onRemove = { onRemove(entry.id) },
                )
            }
        }
    }

    if (pickingRecipe) {
        RecipePickerDialog(
            recipes = uiState.recipes,
            onPick = { recipeId ->
                pickingRecipe = false
                pickedRecipeId = recipeId
            },
            onDismiss = { pickingRecipe = false },
        )
    }

    pickedRecipeId?.let { recipeId ->
        DateTimePickerDialog(
            date = selectedDate,
            onConfirm = { dateTime ->
                pickedRecipeId = null
                requestNotificationPermission()
                onSchedule(recipeId, dateTime.atZone(zone).toInstant().toEpochMilli())
            },
            onDismiss = { pickedRecipeId = null },
        )
    }
}

@Composable
private fun MonthHeader(month: YearMonth, locale: Locale, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(R.drawable.ic_chevron_left, stringResource(R.string.previous_month), onClick = onPrevious)
        Text(
            // "LLLL" is the stand-alone month name, which is the right grammatical form in Greek.
            text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                .replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        AppIconButton(R.drawable.ic_chevron_right, stringResource(R.string.next_month), onClick = onNext)
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    locale: Locale,
    selectedDate: LocalDate,
    datesWithEntries: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
) {
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val today = LocalDate.now()
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row {
            repeat(7) { offset ->
                Text(
                    text = firstDayOfWeek.plus(offset.toLong()).shortName(locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                )
            }
        }
        monthGrid(month, firstDayOfWeek).chunked(7).forEach { week ->
            Row {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                selected = date == selectedDate,
                                today = date == today,
                                hasEntries = date in datesWithEntries,
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DayOfWeek.shortName(locale: Locale): String =
    getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, locale)

@Composable
private fun DayCell(date: LocalDate, selected: Boolean, today: Boolean, hasEntries: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxSize()
            .clip(CircleShape)
            .background(if (selected) colors.primary else colors.surface)
            .then(if (today && !selected) Modifier.border(1.dp, colors.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colors.onPrimary else colors.onSurface,
        )
        // The dot is always laid out so the day number sits at the same height in every cell.
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !hasEntries -> colors.surface.copy(alpha = 0f)
                        selected -> colors.onPrimary
                        else -> colors.primary
                    },
                ),
        )
    }
}

@Composable
private fun ScheduledRow(
    entry: ScheduledRecipe,
    zone: ZoneId,
    locale: Locale,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
                .format(Instant.ofEpochMilli(entry.scheduledAt).atZone(zone)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp),
        )
        Text(
            text = entry.recipeTitle,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        AppIconButton(R.drawable.ic_close, stringResource(R.string.remove), onClick = onRemove)
    }
}

@Composable
private fun RecipePickerDialog(recipes: List<RecipeSummary>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.schedule_recipe)) },
        text = {
            if (recipes.isEmpty()) {
                Text(stringResource(R.string.add_a_recipe_first))
            } else {
                LazyColumn {
                    items(recipes, key = { it.id }) { recipe ->
                        Text(
                            text = recipe.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(recipe.id) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
