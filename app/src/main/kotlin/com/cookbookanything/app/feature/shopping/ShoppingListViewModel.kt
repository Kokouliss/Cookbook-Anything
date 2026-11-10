package com.cookbookanything.app.feature.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.core.domain.model.ShoppingItem
import com.cookbookanything.core.domain.repository.RecipeRepository
import com.cookbookanything.core.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    scheduleRepository: ScheduleRepository,
    private val recipeRepository: RecipeRepository,
) : ViewModel() {

    /** Null while loading. Counted from the start of today, so tonight's dinner stays on the list. */
    val items: StateFlow<List<ShoppingItem>?> = scheduleRepository
        .observeShoppingList(from = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // The same tick as on the recipe page: "I have this ingredient".
    fun setChecked(ingredientId: String, checked: Boolean) {
        viewModelScope.launch { recipeRepository.setIngredientChecked(ingredientId, checked) }
    }
}
