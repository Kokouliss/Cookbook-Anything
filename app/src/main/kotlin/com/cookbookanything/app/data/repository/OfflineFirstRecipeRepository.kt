package com.cookbookanything.app.data.repository

import com.cookbookanything.app.data.database.CommentEntity
import com.cookbookanything.app.data.database.RecipeDao
import com.cookbookanything.app.data.database.RecipeSummaryRow
import com.cookbookanything.app.data.database.ScheduleDao
import com.cookbookanything.app.data.database.toEntity
import com.cookbookanything.app.data.database.toIngredientEntities
import com.cookbookanything.app.data.database.toModel
import com.cookbookanything.app.data.database.toPhotoEntities
import com.cookbookanything.app.data.database.toStepEntities
import com.cookbookanything.app.reminders.ReminderScheduler
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.repository.RecipeRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The Room database is the source of truth; cloud sync will mirror it once sign-in exists. */
@Singleton
class OfflineFirstRecipeRepository @Inject constructor(
    private val recipeDao: RecipeDao,
    private val scheduleDao: ScheduleDao,
    private val reminders: ReminderScheduler,
) : RecipeRepository {

    override fun observeSummaries(): Flow<List<RecipeSummary>> =
        recipeDao.observeSummaries().map { rows -> rows.map(RecipeSummaryRow::toModel) }

    override fun observeCount(): Flow<Int> = recipeDao.observeCount()

    override fun observeRecipe(id: String): Flow<Recipe?> =
        recipeDao.observeRecipe(id).map { it?.toModel() }

    override suspend fun getRecipe(id: String): Recipe? = recipeDao.getRecipe(id)?.toModel()

    override suspend fun saveRecipe(recipe: Recipe) {
        recipeDao.saveRecipe(
            recipe = recipe.toEntity(),
            ingredients = recipe.toIngredientEntities(),
            steps = recipe.toStepEntities(),
            photos = recipe.toPhotoEntities(),
        )
    }

    override suspend fun deleteRecipe(id: String) {
        scheduleDao.idsForRecipe(id).forEach(reminders::cancel)
        // Photo files are swept on the next app start, see CookbookApplication.
        recipeDao.deleteRecipe(id)
    }

    override suspend fun setIngredientChecked(ingredientId: String, checked: Boolean) =
        recipeDao.setIngredientChecked(ingredientId, checked)

    override suspend fun setStepDone(stepId: String, done: Boolean) =
        recipeDao.setStepDone(stepId, done)

    override suspend fun resetProgress(recipeId: String) = recipeDao.resetProgress(recipeId)

    override suspend fun addComment(recipeId: String, text: String) {
        recipeDao.insertComment(
            CommentEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeId,
                text = text,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteComment(commentId: String) = recipeDao.deleteComment(commentId)
}
