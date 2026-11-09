package com.cookbookanything.app.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String) {
        viewModelScope.launch { categoryRepository.addCategory(name) }
    }

    fun rename(id: String, name: String) {
        viewModelScope.launch { categoryRepository.renameCategory(id, name) }
    }

    fun delete(id: String) {
        viewModelScope.launch { categoryRepository.deleteCategory(id) }
    }
}
