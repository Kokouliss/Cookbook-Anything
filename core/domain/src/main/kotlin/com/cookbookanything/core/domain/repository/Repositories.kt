package com.cookbookanything.core.domain.repository

import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.MealSlot
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.model.ScheduledRecipe
import com.cookbookanything.core.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeSummaries(): Flow<List<RecipeSummary>>
    fun observeCount(): Flow<Int>
    fun observeRecipe(id: String): Flow<Recipe?>
    suspend fun getRecipe(id: String): Recipe?

    /** Creates or updates the recipe with its photos, ingredients and steps. Comments are left untouched. */
    suspend fun saveRecipe(recipe: Recipe)
    suspend fun deleteRecipe(id: String)

    suspend fun setIngredientChecked(ingredientId: String, checked: Boolean)
    suspend fun setStepDone(stepId: String, done: Boolean)
    suspend fun resetProgress(recipeId: String)
    suspend fun setFavorite(recipeId: String, favorite: Boolean)
    suspend fun setPinned(recipeId: String, pinned: Boolean)

    /** 1 to 5, or null to clear the rating. */
    suspend fun setRating(recipeId: String, rating: Int?)

    /** Records that the recipe was cooked at [at], with a photo of the result when there is one; the recipe page and the sorts read it. */
    suspend fun markCooked(recipeId: String, at: Long = System.currentTimeMillis(), photo: String? = null)

    /** A new recipe with the same content and copies of the photos, named [title]. Returns its id, or null when the original is gone. */
    suspend fun duplicate(recipeId: String, title: String): String?

    suspend fun addComment(recipeId: String, text: String)
    suspend fun deleteComment(commentId: String)
}

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun addCategory(name: String): Category
    suspend fun renameCategory(id: String, name: String)
    suspend fun deleteCategory(id: String)
}

interface ScheduleRepository {
    fun observeSchedule(): Flow<List<ScheduledRecipe>>

    /** Ingredients of every recipe planned at or after [from] and before [to], in calendar order. */
    fun observeShoppingList(from: Long, to: Long = Long.MAX_VALUE): Flow<List<ShoppingItem>>
    suspend fun schedule(recipeId: String, scheduledAt: Long, slot: MealSlot? = null)
    suspend fun remove(scheduleId: String)

    /** The cook's note for each day that has one, by epoch day. */
    fun observeDayNotes(): Flow<Map<Long, String>>

    /** A blank note removes the day's note. */
    suspend fun setDayNote(epochDay: Long, note: String)
}

/** Whether the lifetime purchase is active. */
interface EntitlementRepository {
    val isUnlocked: Flow<Boolean>
    suspend fun setUnlocked(unlocked: Boolean)
}
