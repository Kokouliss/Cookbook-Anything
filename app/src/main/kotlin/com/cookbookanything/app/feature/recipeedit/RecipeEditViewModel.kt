package com.cookbookanything.app.feature.recipeedit

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.app.data.photos.PhotoStore
import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.Ingredient
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.Step
import com.cookbookanything.core.domain.repository.CategoryRepository
import com.cookbookanything.core.domain.repository.RecipeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class RecipeEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val title: String = "",
    val titleMissing: Boolean = false,
    val categoryId: String? = null,
    val categories: List<Category> = emptyList(),
    // Kept as typed text; empty means not set.
    val servings: String = "",
    val prepMinutes: String = "",
    val cookMinutes: String = "",
    val photos: List<String> = emptyList(),
    // Both lists always end with one empty row, so there is always a place to type the next item.
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<Step> = emptyList(),
    val dirty: Boolean = false,
)

@HiltViewModel(assistedFactory = RecipeEditViewModel.Factory::class)
class RecipeEditViewModel @AssistedInject constructor(
    @Assisted private val recipeId: String?,
    private val recipeRepository: RecipeRepository,
    private val categoryRepository: CategoryRepository,
    private val photoStore: PhotoStore,
) : ViewModel() {

    // Compose state rather than a StateFlow: text fields need their value updated synchronously,
    // otherwise fast typing can lose characters.
    var uiState by mutableStateOf(RecipeEditUiState(isNew = recipeId == null))
        private set

    private val savedChannel = Channel<String>(Channel.BUFFERED)

    /** Emits the id of the recipe once it has been saved. */
    val saved = savedChannel.receiveAsFlow()

    private var original: Recipe? = null

    init {
        viewModelScope.launch {
            val recipe = recipeId?.let { recipeRepository.getRecipe(it) }
            original = recipe
            uiState = uiState.copy(
                loading = false,
                title = recipe?.title.orEmpty(),
                categoryId = recipe?.categoryId,
                servings = recipe?.servings?.toString().orEmpty(),
                prepMinutes = recipe?.prepMinutes?.toString().orEmpty(),
                cookMinutes = recipe?.cookMinutes?.toString().orEmpty(),
                photos = recipe?.photos.orEmpty(),
                ingredients = recipe?.ingredients.orEmpty().withTrailingBlank(),
                steps = recipe?.steps.orEmpty().withTrailingBlankStep(),
            )
        }
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { uiState = uiState.copy(categories = it) }
        }
    }

    fun setTitle(value: String) = edit { copy(title = value, titleMissing = false) }

    fun setCategory(id: String?) = edit { copy(categoryId = id) }

    fun setServings(value: String) = edit { copy(servings = value.digits(maxLength = 2)) }

    fun setPrepMinutes(value: String) = edit { copy(prepMinutes = value.digits(maxLength = 4)) }

    fun setCookMinutes(value: String) = edit { copy(cookMinutes = value.digits(maxLength = 4)) }

    fun createCategory(name: String) {
        viewModelScope.launch {
            val category = categoryRepository.addCategory(name)
            edit { copy(categoryId = category.id) }
        }
    }

    fun setIngredientText(id: String, text: String) = edit {
        copy(ingredients = ingredients.map { if (it.id == id) it.copy(text = text) else it }.withTrailingBlank())
    }

    fun removeIngredient(id: String) = edit {
        copy(ingredients = ingredients.filterNot { it.id == id }.withTrailingBlank())
    }

    fun setStepText(id: String, text: String) = edit {
        copy(steps = steps.map { if (it.id == id) it.copy(text = text) else it }.withTrailingBlankStep())
    }

    fun removeStep(id: String) = edit {
        copy(steps = steps.filterNot { it.id == id }.withTrailingBlankStep())
    }

    /** Adds picked photos to the step with [stepId], or to the recipe itself when it is null. */
    fun addPhotos(stepId: String?, uris: List<Uri>) {
        viewModelScope.launch {
            val names = uris.mapNotNull { photoStore.import(it) }
            if (names.isEmpty()) return@launch
            edit {
                if (stepId == null) {
                    copy(photos = photos + names)
                } else {
                    copy(
                        steps = steps.map { if (it.id == stepId) it.copy(photos = it.photos + names) else it }
                            .withTrailingBlankStep(),
                    )
                }
            }
        }
    }

    fun removePhoto(stepId: String?, name: String) = edit {
        if (stepId == null) {
            copy(photos = photos - name)
        } else {
            copy(steps = steps.map { if (it.id == stepId) it.copy(photos = it.photos - name) else it })
        }
    }

    fun newCameraTarget(): Uri = photoStore.newCameraTarget()

    fun save() {
        val state = uiState
        if (state.loading) return
        if (state.title.isBlank()) {
            uiState = state.copy(titleMissing = true)
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val recipe = Recipe(
                id = recipeId ?: UUID.randomUUID().toString(),
                title = state.title.trim(),
                categoryId = state.categoryId,
                servings = state.servings.positiveIntOrNull(),
                prepMinutes = state.prepMinutes.positiveIntOrNull(),
                cookMinutes = state.cookMinutes.positiveIntOrNull(),
                photos = state.photos,
                ingredients = state.ingredients.filter { it.text.isNotBlank() }.map { it.copy(text = it.text.trim()) },
                steps = state.steps.filterNot { it.isBlank() }.map { it.copy(text = it.text.trim()) },
                createdAt = original?.createdAt ?: now,
                updatedAt = now,
            )
            recipeRepository.saveRecipe(recipe)
            savedChannel.send(recipe.id)
        }
    }

    private fun edit(change: RecipeEditUiState.() -> RecipeEditUiState) {
        uiState = uiState.change().copy(dirty = true)
    }

    @AssistedFactory
    interface Factory {
        fun create(recipeId: String?): RecipeEditViewModel
    }
}

private fun Step.isBlank() = text.isBlank() && photos.isEmpty()

private fun List<Ingredient>.withTrailingBlank(): List<Ingredient> =
    if (lastOrNull()?.text?.isBlank() == true) this else this + Ingredient(id = newId(), text = "")

private fun List<Step>.withTrailingBlankStep(): List<Step> =
    if (lastOrNull()?.isBlank() == true) this else this + Step(id = newId(), text = "")

private fun newId() = UUID.randomUUID().toString()

private fun String.digits(maxLength: Int) = filter(Char::isDigit).take(maxLength)

private fun String.positiveIntOrNull() = toIntOrNull()?.takeIf { it > 0 }
