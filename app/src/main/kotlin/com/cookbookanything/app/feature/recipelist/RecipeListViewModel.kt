package com.cookbookanything.app.feature.recipelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.model.ScheduledRecipe
import com.cookbookanything.core.domain.repository.CategoryRepository
import com.cookbookanything.core.domain.repository.RecipeRepository
import com.cookbookanything.core.domain.repository.ScheduleRepository
import com.cookbookanything.core.domain.search.RecipeFilter
import com.cookbookanything.core.domain.search.matches
import com.cookbookanything.core.domain.search.searchKey
import com.cookbookanything.core.domain.sort.RecipeSort
import com.cookbookanything.core.domain.sort.sortedBy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface RecipeListUiState {
    data object Loading : RecipeListUiState

    data class Success(
        val recipes: List<RecipeSummary>,
        val categories: List<Category>,
        val selectedCategoryId: String?,
        val cookbookIsEmpty: Boolean,
        val sort: RecipeSort,
        val onlyFavorites: Boolean,
        val filter: RecipeFilter,
        /** Every tag in the cookbook, for the chips. */
        val allTags: List<String>,
        /** The next planned recipe, for the strip at the top. */
        val next: ScheduledRecipe?,
        val gettingStarted: GettingStarted,
    ) : RecipeListUiState
}

/** The first things to try, ticked as the cookbook shows they happened. */
data class GettingStarted(val added: Boolean, val imported: Boolean, val planned: Boolean, val cooked: Boolean, val favorited: Boolean) {
    val steps: List<Boolean> get() = listOf(added, imported, planned, cooked, favorited)
    val done: Int get() = steps.count { it }
    val allDone: Boolean get() = done == steps.size
}

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    recipeRepository: RecipeRepository,
    categoryRepository: CategoryRepository,
    scheduleRepository: ScheduleRepository,
) : ViewModel() {
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")
    // shortcut: the sort is forgotten when the app restarts, store it in preferences if that annoys
    private val sort = MutableStateFlow(RecipeSort.RECENT)
    private val onlyFavorites = MutableStateFlow(false)
    private val filter = MutableStateFlow(RecipeFilter())

    private data class Options(val sort: RecipeSort, val favorites: Boolean, val filter: RecipeFilter, val schedule: List<ScheduledRecipe>)

    val uiState: StateFlow<RecipeListUiState> = combine(
        recipeRepository.observeSummaries(),
        categoryRepository.observeCategories(),
        selectedCategoryId,
        query,
        combine(sort, onlyFavorites, filter, scheduleRepository.observeSchedule(), ::Options),
    ) { recipes, categories, selected, searchText, (order, favorites, narrowing, schedule) ->
        // A deleted category must not keep filtering the list.
        val active = selected?.takeIf { id -> categories.any { it.id == id } }
        val now = System.currentTimeMillis()
        RecipeListUiState.Success(
            recipes = recipes.filter {
                (active == null || it.categoryId == active) && (!favorites || it.favorite) && it.matches(searchText) && it.matches(narrowing)
            }.sortedBy(order),
            categories = categories,
            selectedCategoryId = active,
            cookbookIsEmpty = recipes.isEmpty(),
            sort = order,
            onlyFavorites = favorites,
            filter = narrowing,
            allTags = recipes.flatMap { it.tags }.distinctBy { it.searchKey() }.sortedBy { it.searchKey() },
            next = schedule.firstOrNull { it.scheduledAt >= now },
            gettingStarted = GettingStarted(
                added = recipes.isNotEmpty(),
                imported = recipes.any { it.source != null },
                planned = schedule.isNotEmpty(),
                cooked = recipes.any { it.timesCooked > 0 },
                favorited = recipes.any { it.favorite },
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeListUiState.Loading)

    fun search(text: String) {
        query.value = text
    }

    fun selectCategory(categoryId: String?) {
        selectedCategoryId.value = categoryId
    }

    fun setSort(order: RecipeSort) {
        sort.value = order
    }

    fun setOnlyFavorites(value: Boolean) {
        onlyFavorites.value = value
    }

    fun setFilter(value: RecipeFilter) {
        filter.value = value
    }
}
