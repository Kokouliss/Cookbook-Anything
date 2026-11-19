package com.cookbookanything.app.feature.recipelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.repository.CategoryRepository
import com.cookbookanything.core.domain.repository.RecipeRepository
import com.cookbookanything.core.domain.search.matches
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
    ) : RecipeListUiState
}

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    recipeRepository: RecipeRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")
    // shortcut: the sort is forgotten when the app restarts, store it in preferences if that annoys
    private val sort = MutableStateFlow(RecipeSort.RECENT)

    val uiState: StateFlow<RecipeListUiState> = combine(
        recipeRepository.observeSummaries(),
        categoryRepository.observeCategories(),
        selectedCategoryId,
        query,
        sort,
    ) { recipes, categories, selected, searchText, order ->
        // A deleted category must not keep filtering the list.
        val active = selected?.takeIf { id -> categories.any { it.id == id } }
        RecipeListUiState.Success(
            recipes = recipes.filter { (active == null || it.categoryId == active) && it.matches(searchText) }.sortedBy(order),
            categories = categories,
            selectedCategoryId = active,
            cookbookIsEmpty = recipes.isEmpty(),
            sort = order,
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
}
