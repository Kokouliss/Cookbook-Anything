package com.cookbookanything.core.domain.repository

import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.model.ScheduledRecipe
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
    suspend fun schedule(recipeId: String, scheduledAt: Long)
    suspend fun remove(scheduleId: String)
}

/** Whether the lifetime purchase is active. */
interface EntitlementRepository {
    val isUnlocked: Flow<Boolean>
    suspend fun setUnlocked(unlocked: Boolean)
}
