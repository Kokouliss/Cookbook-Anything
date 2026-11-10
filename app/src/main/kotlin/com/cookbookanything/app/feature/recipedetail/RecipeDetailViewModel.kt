package com.cookbookanything.app.feature.recipedetail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.app.data.share.PdfLabels
import com.cookbookanything.app.data.share.RecipeSharer
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.ScheduledRecipe
import com.cookbookanything.core.domain.repository.CategoryRepository
import com.cookbookanything.core.domain.repository.RecipeRepository
import com.cookbookanything.core.domain.repository.ScheduleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RecipeDetailUiState {
    data object Loading : RecipeDetailUiState
    data object NotFound : RecipeDetailUiState

    data class Success(
        val recipe: Recipe,
        val categoryName: String?,
        val upcoming: List<ScheduledRecipe>,
    ) : RecipeDetailUiState
}

sealed interface RecipeDetailEvent {
    data object Deleted : RecipeDetailEvent
    data class Share(val uri: Uri, val mimeType: String) : RecipeDetailEvent
    data object ShareFailed : RecipeDetailEvent
}

@HiltViewModel(assistedFactory = RecipeDetailViewModel.Factory::class)
class RecipeDetailViewModel @AssistedInject constructor(
    @Assisted private val recipeId: String,
    private val recipeRepository: RecipeRepository,
    categoryRepository: CategoryRepository,
    private val scheduleRepository: ScheduleRepository,
    private val recipeSharer: RecipeSharer,
) : ViewModel() {

    val uiState: StateFlow<RecipeDetailUiState> = combine(
        recipeRepository.observeRecipe(recipeId),
        categoryRepository.observeCategories(),
        scheduleRepository.observeSchedule(),
    ) { recipe, categories, schedule ->
        if (recipe == null) {
            RecipeDetailUiState.NotFound
        } else {
            val now = System.currentTimeMillis()
            RecipeDetailUiState.Success(
                recipe = recipe,
                categoryName = categories.firstOrNull { it.id == recipe.categoryId }?.name,
                upcoming = schedule.filter { it.recipeId == recipeId && it.scheduledAt >= now },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeDetailUiState.Loading)

    private val eventChannel = Channel<RecipeDetailEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun setIngredientChecked(ingredientId: String, checked: Boolean) {
        viewModelScope.launch { recipeRepository.setIngredientChecked(ingredientId, checked) }
    }

    fun setStepDone(stepId: String, done: Boolean) {
        viewModelScope.launch { recipeRepository.setStepDone(stepId, done) }
    }

    fun resetProgress() {
        viewModelScope.launch { recipeRepository.resetProgress(recipeId) }
    }

    fun addComment(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { recipeRepository.addComment(recipeId, text.trim()) }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch { recipeRepository.deleteComment(commentId) }
    }

    fun schedule(at: Long) {
        viewModelScope.launch { scheduleRepository.schedule(recipeId, at) }
    }

    fun removeSchedule(scheduleId: String) {
        viewModelScope.launch { scheduleRepository.remove(scheduleId) }
    }

    fun delete() {
        viewModelScope.launch {
            recipeRepository.deleteRecipe(recipeId)
            eventChannel.send(RecipeDetailEvent.Deleted)
        }
    }

    fun shareAsPdf(labels: PdfLabels) = share(RecipeSharer.PDF_MIME_TYPE) { recipeSharer.exportPdf(it, labels) }

    fun shareAsRecipeFile() = share(RecipeSharer.RECIPE_FILE_MIME_TYPE) { recipeSharer.exportRecipeFile(it) }

    private fun share(mimeType: String, export: suspend (Recipe) -> Uri) {
        viewModelScope.launch {
            val uri = runCatching { recipeRepository.getRecipe(recipeId)?.let { export(it) } }.getOrNull()
            eventChannel.send(
                if (uri == null) RecipeDetailEvent.ShareFailed else RecipeDetailEvent.Share(uri, mimeType),
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(recipeId: String): RecipeDetailViewModel
    }
}
