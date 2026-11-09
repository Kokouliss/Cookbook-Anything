package com.cookbookanything.app.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    recipeRepository: RecipeRepository,
) : ViewModel() {

    val uiState: StateFlow<CalendarUiState> =
        combine(scheduleRepository.observeSchedule(), recipeRepository.observeSummaries()) { schedule, recipes ->
            CalendarUiState(schedule = schedule, recipes = recipes.sortedBy { it.title.lowercase() })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun schedule(recipeId: String, at: Long) {
        viewModelScope.launch { scheduleRepository.schedule(recipeId, at) }
    }

    fun remove(scheduleId: String) {
        viewModelScope.launch { scheduleRepository.remove(scheduleId) }
    }
}
