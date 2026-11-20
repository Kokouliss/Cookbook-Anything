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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
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

    /** Only the next seven days, or everything planned. */
    val thisWeek = MutableStateFlow(false)

    /** Null while loading. Counted from the start of today, so tonight's dinner stays on the list. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<MergedShoppingItem>?> = combine(
        thisWeek.flatMapLatest { week ->
            val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
            scheduleRepository.observeShoppingList(
                from = today.toInstant().toEpochMilli(),
                to = if (week) today.plusDays(7).toInstant().toEpochMilli() else Long.MAX_VALUE,
            )
        },
        aislePreferences.overrides,
    ) { items, overrides -> items.merged { overrides[it] } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setThisWeek(value: Boolean) {
        thisWeek.value = value
    }

    /** Remembered for the product, so it lands in that aisle on every later list. */
    fun moveToAisle(productKey: String, aisle: Aisle) = aislePreferences.set(productKey, aisle)

    // The same tick as on the recipe page: "I have this ingredient", for every recipe that needs it.
    fun setChecked(ingredientIds: List<String>, checked: Boolean) {
        viewModelScope.launch { ingredientIds.forEach { recipeRepository.setIngredientChecked(it, checked) } }
    }
}
