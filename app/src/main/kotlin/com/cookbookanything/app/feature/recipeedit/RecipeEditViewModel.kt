package com.cookbookanything.app.feature.recipeedit

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.app.data.photos.PhotoStore
import com.cookbookanything.app.data.text.LinkRecipeReader
import com.cookbookanything.app.data.text.PendingPhotoReads
import com.cookbookanything.app.data.text.PhotoTextReader
import com.cookbookanything.app.data.text.RecipeTranslator
import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.Ingredient
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.SOURCE_PHOTO
import com.cookbookanything.core.domain.model.Step
import com.cookbookanything.core.domain.repository.CategoryRepository
import com.cookbookanything.core.domain.repository.RecipeRepository
import com.cookbookanything.core.domain.text.ParsedRecipe
import com.cookbookanything.core.domain.text.linkIn
import com.cookbookanything.core.domain.text.parseRecipeText
import com.cookbookanything.core.domain.text.sourceKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** What came of reading a photo or a link; the screen tells the user in one line. */
sealed interface ReadResult {
    data class Added(val ingredients: Int, val steps: Int) : ReadResult
    data object PhotoNothing : ReadResult
    /** [reason] is what Google answered, shown so a missing setting can be found. */
    data class PhotoFailed(val reason: String? = null) : ReadResult
    data object PhotoSavedForLater : ReadResult
    data object LinkNothing : ReadResult
    data object LinkFailed : ReadResult
    /** The link was saved before, as the recipe named here. */
    data class Duplicate(val recipeId: String, val title: String) : ReadResult
    /** Translation was switched on but did not work; the recipe is kept in its own language. */
    data object NotTranslated : ReadResult
}

data class RecipeEditUiState(
    val loading: Boolean = true,
    /** A photo or a link is being read; takes a few seconds. */
    val reading: Boolean = false,
    val isNew: Boolean = true,
    val title: String = "",
    val titleMissing: Boolean = false,
    val categoryId: String? = null,
    val categories: List<Category> = emptyList(),
    // Kept as typed text; empty means not set.
    val servings: String = "",
    val prepMinutes: String = "",
    val cookMinutes: String = "",
    val waitMinutes: String = "",
    val description: String = "",
    val notes: String = "",
    /** Comma-separated, as typed. */
    val tags: String = "",
    /** Set when the recipe was read from a link or a photo; not typed. */
    val source: String? = null,
    val photos: List<String> = emptyList(),
    // Both lists always end with one empty row, so there is always a place to type the next item.
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<Step> = emptyList(),
    val dirty: Boolean = false,
)

@HiltViewModel(assistedFactory = RecipeEditViewModel.Factory::class)
class RecipeEditViewModel @AssistedInject constructor(
    @Assisted("recipeId") private val recipeId: String?,
    @Assisted("pastedText") pastedText: String?,
    @Assisted("photoUris") photoUris: List<String>?,
    private val recipeRepository: RecipeRepository,
    private val categoryRepository: CategoryRepository,
    private val photoStore: PhotoStore,
    private val photoTextReader: PhotoTextReader,
    private val pendingPhotoReads: PendingPhotoReads,
    private val linkRecipeReader: LinkRecipeReader,
    private val translator: RecipeTranslator,
) : ViewModel() {

    // Compose state rather than a StateFlow: text fields need their value updated synchronously,
    // otherwise fast typing can lose characters.
    var uiState by mutableStateOf(RecipeEditUiState(isNew = recipeId == null))
        private set

    private val savedChannel = Channel<String>(Channel.BUFFERED)
    private val readResults = Channel<ReadResult>(Channel.BUFFERED)
    val readResultFlow = readResults.receiveAsFlow()

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
                waitMinutes = recipe?.waitMinutes?.toString().orEmpty(),
                description = recipe?.description.orEmpty(),
                notes = recipe?.notes.orEmpty(),
                tags = recipe?.tags.orEmpty().joinToString(", "),
                source = recipe?.source,
                photos = recipe?.photos.orEmpty(),
                ingredients = recipe?.ingredients.orEmpty().withTrailingBlank(),
                steps = recipe?.steps.orEmpty().withTrailingBlankStep(),
            )
            if (pastedText != null) paste(pastedText)
            if (photoUris != null) {
                val uris = photoUris.map(Uri::parse)
                // Without the Firebase config the shared screenshots are at least kept as the recipe's photos.
                if (canReadPhotos) readPhotos(uris) else addPhotos(null, uris)
            }
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

    fun setWaitMinutes(value: String) = edit { copy(waitMinutes = value.digits(maxLength = 4)) }

    fun setDescription(value: String) = edit { copy(description = value) }

    fun setNotes(value: String) = edit { copy(notes = value) }

    fun setTags(value: String) = edit { copy(tags = value) }

    fun setIngredientOptional(id: String, optional: Boolean) = edit {
        copy(ingredients = ingredients.map { if (it.id == id) it.copy(optional = optional) else it })
    }

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

    /** Puts a removed ingredient back where it was, for Undo. */
    fun restoreIngredient(index: Int, ingredient: Ingredient) = edit {
        copy(ingredients = ingredients.toMutableList().apply { add(index.coerceIn(0, size), ingredient) }.withTrailingBlank())
    }

    fun moveIngredient(from: Int, to: Int) = edit {
        // The spare blank row goes back to the end whatever was dragged past it.
        copy(ingredients = ingredients.toMutableList().apply { add(to, removeAt(from)) }.filterNot { it.text.isBlank() }.withTrailingBlank())
    }

    fun setStepText(id: String, text: String) = edit {
        copy(steps = steps.map { if (it.id == id) it.copy(text = text) else it }.withTrailingBlankStep())
    }

    fun removeStep(id: String) = edit {
        copy(steps = steps.filterNot { it.id == id }.withTrailingBlankStep())
    }

    fun restoreStep(index: Int, step: Step) = edit {
        copy(steps = steps.toMutableList().apply { add(index.coerceIn(0, size), step) }.withTrailingBlankStep())
    }

    fun moveStep(from: Int, to: Int) = edit {
        copy(steps = steps.toMutableList().apply { add(to, removeAt(from)) }.filterNot { it.isBlank() }.withTrailingBlankStep())
    }

    /** Pasted or shared text: a link is fetched and read, anything else is split into the form. */
    fun paste(text: String) {
        val link = linkIn(text)
        // Plain text goes through read() too, so the progress line shows while it is translated.
        if (link != null) readLink(link) else read(ReadResult.Added(0, 0)) { applyParsed(parseRecipeText(text)) }
    }

    /** What the site answered to the link that was just not read, to send on. */
    fun linkDetails(): Uri? = linkRecipeReader.detailsFile()

    private fun readLink(url: String) = read(ReadResult.LinkFailed) {
        val key = sourceKey(url)
        recipeRepository.observeSummaries().first()
            .firstOrNull { it.id != recipeId && it.source?.let(::sourceKey) == key }
            ?.let { return@read ReadResult.Duplicate(it.id, it.title) }
        val (recipe, photo) = linkRecipeReader.read(url) ?: return@read ReadResult.LinkNothing
        edit { copy(photos = if (photo != null) photos + photo else photos, source = source ?: url) }
        applyParsed(recipe)
    }

    /**
     * Fills the form from a parsed recipe. The name, servings and times are only taken when the form
     * has none yet; ingredients and steps are added after the ones already typed.
     */
    private suspend fun applyParsed(read: ParsedRecipe): ReadResult {
        if (read.isEmpty) return ReadResult.Added(0, 0)
        val translated = translator.translate(read)
        if (translated == null) readResults.send(ReadResult.NotTranslated)
        val parsed = translated ?: read
        edit {
            copy(
                title = title.ifBlank { parsed.title ?: title },
                titleMissing = false,
                servings = servings.ifBlank { parsed.servings?.toString() ?: servings },
                prepMinutes = prepMinutes.ifBlank { parsed.prepMinutes?.toString() ?: prepMinutes },
                cookMinutes = cookMinutes.ifBlank { parsed.cookMinutes?.toString() ?: cookMinutes },
                description = description.ifBlank { parsed.description ?: description },
                ingredients = (ingredients.filterNot { it.text.isBlank() } + parsed.ingredients.map { Ingredient(id = newId(), text = it) })
                    .withTrailingBlank(),
                steps = (steps.filterNot { it.isBlank() } + parsed.steps.map { Step(id = newId(), text = it) }).withTrailingBlankStep(),
            )
        }
        return ReadResult.Added(parsed.ingredients.size, parsed.steps.size)
    }

    /** Runs a read with the progress line shown, and reports [failure] when the call throws. */
    private fun read(failure: ReadResult, block: suspend () -> ReadResult) {
        viewModelScope.launch {
            edit { copy(reading = true) }
            val result = try {
                block()
            } catch (e: IOException) {
                if (failure is ReadResult.PhotoFailed) ReadResult.PhotoFailed(e.message) else failure
            }
            edit { copy(reading = false) }
            readResults.send(result)
        }
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

    /** False in a build without the Firebase config, where the "read from a photo" button is hidden. */
    val canReadPhotos: Boolean get() = photoTextReader.available

    /**
     * Reads the recipe off photos of a page (several photos for a long recipe) and fills the form
     * like pasted text. Offline, the photos are kept and read later as a new recipe.
     */
    fun readPhotos(uris: List<Uri>) = read(ReadResult.PhotoFailed()) {
        if (!pendingPhotoReads.isOnline()) {
            pendingPhotoReads.add(uris)
            return@read ReadResult.PhotoSavedForLater
        }
        val text = photoTextReader.read(uris) ?: return@read ReadResult.PhotoNothing
        edit { copy(source = source ?: SOURCE_PHOTO) }
        applyParsed(parseRecipeText(text))
    }

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
                waitMinutes = state.waitMinutes.positiveIntOrNull(),
                description = state.description.trim().ifEmpty { null },
                notes = state.notes.trim().ifEmpty { null },
                tags = state.tags.split(',').map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinct(),
                source = state.source,
                rating = original?.rating,
                favorite = original?.favorite ?: false,
                pinned = original?.pinned ?: false,
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
        fun create(
            @Assisted("recipeId") recipeId: String?,
            @Assisted("pastedText") pastedText: String?,
            @Assisted("photoUris") photoUris: List<String>?,
        ): RecipeEditViewModel
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
