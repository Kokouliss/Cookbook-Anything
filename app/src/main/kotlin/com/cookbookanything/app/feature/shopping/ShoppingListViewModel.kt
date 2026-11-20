package com.cookbookanything.app.feature.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.app.data.shopping.AislePreferences
import com.cookbookanything.core.domain.repository.RecipeRepository
import com.cookbookanything.core.domain.repository.ScheduleRepository
import com.cookbookanything.core.domain.shopping.Aisle
import com.cookbookanything.core.domain.shopping.MergedShoppingItem
import com.cookbookanything.core.domain.shopping.merged
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    scheduleRepository: ScheduleRepository,
    private val recipeRepository: RecipeRepository,
    private val aislePreferences: AislePreferences,
) : ViewModel() {

    /** Null while loading. Counted from the start of today, so tonight's dinner stays on the list. */
    val items: StateFlow<List<MergedShoppingItem>?> = combine(
        scheduleRepository.observeShoppingList(from = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()),
        aislePreferences.overrides,
    ) { items, overrides -> items.merged { overrides[it] } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Remembered for the product, so it lands in that aisle on every later list. */
    fun moveToAisle(productKey: String, aisle: Aisle) = aislePreferences.set(productKey, aisle)

    // The same tick as on the recipe page: "I have this ingredient", for every recipe that needs it.
    fun setChecked(ingredientIds: List<String>, checked: Boolean) {
        viewModelScope.launch { ingredientIds.forEach { recipeRepository.setIngredientChecked(it, checked) } }
    }
}
