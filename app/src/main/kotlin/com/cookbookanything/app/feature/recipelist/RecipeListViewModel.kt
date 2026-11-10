package com.cookbookanything.app.feature.recipelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.repository.CategoryRepository
import com.cookbookanything.core.domain.repository.RecipeRepository
import com.cookbookanything.core.domain.search.matches
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
    ) : RecipeListUiState
}

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    recipeRepository: RecipeRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<RecipeListUiState> = combine(
        recipeRepository.observeSummaries(),
        categoryRepository.observeCategories(),
        selectedCategoryId,
        query,
    ) { recipes, categories, selected, searchText ->
        // A deleted category must not keep filtering the list.
        val active = selected?.takeIf { id -> categories.any { it.id == id } }
        RecipeListUiState.Success(
            recipes = recipes.filter { (active == null || it.categoryId == active) && it.matches(searchText) },
            categories = categories,
            selectedCategoryId = active,
            cookbookIsEmpty = recipes.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeListUiState.Loading)

    fun search(text: String) {
        query.value = text
    }

    fun selectCategory(categoryId: String?) {
        selectedCategoryId.value = categoryId
    }
}
