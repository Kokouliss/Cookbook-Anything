package com.cookbookanything.app.feature.recipedetail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.app.data.share.PdfLabels
import com.cookbookanything.app.data.share.RecipeSharer
import com.cookbookanything.app.reminders.CookingTimer
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
import kotlinx.coroutines.flow.MutableStateFlow
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

/** A running cooking-mode timer: for which step, and when it rings. */
data class StepTimer(val stepId: String, val endsAt: Long)

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
    private val cookingTimer: CookingTimer,
) : ViewModel() {

    private val _timer = MutableStateFlow<StepTimer?>(null)
    val timer: StateFlow<StepTimer?> = _timer

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

    /** Finishing cooking mode adds a line to the recipe's cooked log. */
    fun markCooked() {
        viewModelScope.launch { recipeRepository.markCooked(recipeId) }
    }

    /** One timer at a time: starting another replaces it. It rings through a notification, so leaving the screen is fine. */
    fun startTimer(stepId: String, minutes: Int) {
        val endsAt = System.currentTimeMillis() + minutes * 60_000L
        val title = (uiState.value as? RecipeDetailUiState.Success)?.recipe?.title.orEmpty()
        cookingTimer.start(recipeId, title, endsAt)
        _timer.value = StepTimer(stepId, endsAt)
    }

    fun stopTimer() {
        cookingTimer.cancel()
        _timer.value = null
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
