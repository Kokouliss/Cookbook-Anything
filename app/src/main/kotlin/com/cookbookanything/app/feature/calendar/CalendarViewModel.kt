package com.cookbookanything.app.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.core.domain.model.MealSlot
import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.model.ScheduledRecipe
import com.cookbookanything.core.domain.repository.RecipeRepository
import com.cookbookanything.core.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarUiState(
    val schedule: List<ScheduledRecipe> = emptyList(),
    val recipes: List<RecipeSummary> = emptyList(),
    /** The cook's note per epoch day. */
    val dayNotes: Map<Long, String> = emptyMap(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    recipeRepository: RecipeRepository,
) : ViewModel() {

    val uiState: StateFlow<CalendarUiState> =
        combine(scheduleRepository.observeSchedule(), recipeRepository.observeSummaries(), scheduleRepository.observeDayNotes()) { schedule, recipes, notes ->
            CalendarUiState(schedule = schedule, recipes = recipes.sortedBy { it.title.lowercase() }, dayNotes = notes)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun schedule(recipeId: String, at: Long, slot: MealSlot?) {
        viewModelScope.launch { scheduleRepository.schedule(recipeId, at, slot) }
    }

    fun setDayNote(epochDay: Long, note: String) {
        viewModelScope.launch { scheduleRepository.setDayNote(epochDay, note) }
    }

    fun remove(scheduleId: String) {
        viewModelScope.launch { scheduleRepository.remove(scheduleId) }
    }
}
