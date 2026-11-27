package com.cookbookanything.app.feature.recipedetail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.app.data.cook.CookingPrompts
import com.cookbookanything.app.data.photos.PhotoStore
import com.cookbookanything.app.data.repository.RecipeTrash
import com.cookbookanything.app.data.share.PdfLabels
import com.cookbookanything.app.data.share.RecipeSharer
import com.cookbookanything.app.data.text.LinkRecipeReader
import com.cookbookanything.app.reminders.CookingTimer
import com.cookbookanything.core.domain.ingredients.isHeading
import com.cookbookanything.core.domain.model.MealSlot
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.ScheduledRecipe
import com.cookbookanything.core.domain.repository.CategoryRepository
import com.cookbookanything.core.domain.repository.RecipeRepository
import com.cookbookanything.core.domain.repository.ScheduleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
    data class Print(val uri: Uri) : RecipeDetailEvent
    data class Duplicated(val recipeId: String) : RecipeDetailEvent
}

private const val VIDEO_FILE_TIMEOUT_MS = 5_000L

@HiltViewModel(assistedFactory = RecipeDetailViewModel.Factory::class)
class RecipeDetailViewModel @AssistedInject constructor(
    @Assisted private val recipeId: String,
    private val recipeRepository: RecipeRepository,
    categoryRepository: CategoryRepository,
    private val scheduleRepository: ScheduleRepository,
    private val recipeSharer: RecipeSharer,
    private val cookingTimer: CookingTimer,
    private val cookingPrompts: CookingPrompts,
    private val photoStore: PhotoStore,
    private val recipeTrash: RecipeTrash,
    private val linkRecipeReader: LinkRecipeReader,
) : ViewModel() {

    /**
     * The video file to play for [link] in place of its site's player, or null to use the player.
     * The player is the fallback, so the lookup gets a few seconds and no more.
     */
    suspend fun videoFile(link: String): String? {
        // Awaited, not called, so the limit holds even while the request itself cannot be interrupted.
        val file = viewModelScope.async { runCatching { linkRecipeReader.videoFile(link) }.getOrNull() }
        return withTimeoutOrNull(VIDEO_FILE_TIMEOUT_MS) { file.await() }
    }

    private val _timer = MutableStateFlow<StepTimer?>(null)
    val timer: StateFlow<StepTimer?> = _timer

    private val openedAt = System.currentTimeMillis()

    /** True while the page should ask "Did you cook it?". */
    val cookPrompt = MutableStateFlow(cookingPrompts.isPending(recipeId))

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
        val steps = (uiState.value as? RecipeDetailUiState.Success)?.recipe?.steps.orEmpty().filterNot { isHeading(it.text) }
        // Ticking the last step on the page is as good as finishing cooking mode, so ask.
        if (done && steps.all { it.id == stepId || it.done }) cookPrompt.value = true
        viewModelScope.launch { recipeRepository.setStepDone(stepId, done) }
    }

    fun newCameraTarget(): Uri = photoStore.newCameraTarget()

    fun dismissCookPrompt() {
        cookPrompt.value = false
        cookingPrompts.set(recipeId, false)
    }

    override fun onCleared() {
        super.onCleared()
        val recipe = (uiState.value as? RecipeDetailUiState.Success)?.recipe ?: return
        val cookedSinceOpened = (recipe.cookedAt.lastOrNull() ?: 0) >= openedAt
        if (System.currentTimeMillis() - openedAt >= LIKELY_COOKED_MS && !cookedSinceOpened) cookingPrompts.set(recipeId, true)
    }

    fun resetProgress() {
        viewModelScope.launch { recipeRepository.resetProgress(recipeId) }
    }

    fun setFavorite(favorite: Boolean) {
        viewModelScope.launch { recipeRepository.setFavorite(recipeId, favorite) }
    }

    fun setPinned(pinned: Boolean) {
        viewModelScope.launch { recipeRepository.setPinned(recipeId, pinned) }
    }

    /** [copySuffix] is the translated " (copy)" appended to the title. */
    fun duplicate(copySuffix: String) {
        viewModelScope.launch {
            val title = (uiState.value as? RecipeDetailUiState.Success)?.recipe?.title.orEmpty() + copySuffix
            recipeRepository.duplicate(recipeId, title)?.let { eventChannel.send(RecipeDetailEvent.Duplicated(it)) }
        }
    }

    /** Tapping the star that is already the rating clears it. */
    fun rate(stars: Int) {
        val current = (uiState.value as? RecipeDetailUiState.Success)?.recipe?.rating
        viewModelScope.launch { recipeRepository.setRating(recipeId, stars.takeIf { it != current }) }
    }

    /** Finishing cooking mode, or answering "Did you cook it?", adds a line to the recipe's cooked log, with a photo of the result when given. */
    fun markCooked(photo: Uri? = null) {
        dismissCookPrompt()
        viewModelScope.launch { recipeRepository.markCooked(recipeId, photo = photo?.let { photoStore.import(it) }) }
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

    fun schedule(at: Long, slot: MealSlot?) {
        viewModelScope.launch { scheduleRepository.schedule(recipeId, at, slot) }
    }

    fun removeSchedule(scheduleId: String) {
        viewModelScope.launch { scheduleRepository.remove(scheduleId) }
    }

    /** No question asked: the page closes and the recipe waits in the trash while Undo is offered. */
    fun delete() {
        recipeTrash.bin(recipeId, (uiState.value as? RecipeDetailUiState.Success)?.recipe?.title.orEmpty())
        eventChannel.trySend(RecipeDetailEvent.Deleted)
    }

    fun shareAsPdf(labels: PdfLabels) = share(RecipeSharer.PDF_MIME_TYPE) { recipeSharer.exportPdf(it, labels) }

    fun shareAsRecipeFile() = share(RecipeSharer.RECIPE_FILE_MIME_TYPE) { recipeSharer.exportRecipeFile(it) }

    fun print(labels: PdfLabels) {
        viewModelScope.launch {
            val uri = runCatching { recipeRepository.getRecipe(recipeId)?.let { recipeSharer.exportPdf(it, labels) } }.getOrNull()
            eventChannel.send(if (uri == null) RecipeDetailEvent.ShareFailed else RecipeDetailEvent.Print(uri))
        }
    }

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

    private companion object {
        /** A recipe page kept open this long was probably cooked from. */
        const val LIKELY_COOKED_MS = 20 * 60_000L
    }
}
