package com.cookbookanything.app.data.repository

import com.cookbookanything.app.data.database.CommentEntity
import com.cookbookanything.app.data.database.CookedEntity
import com.cookbookanything.app.data.database.RecipeDao
import com.cookbookanything.app.data.database.RecipeSummaryRow
import com.cookbookanything.app.data.database.ScheduleDao
import com.cookbookanything.app.data.database.toEntity
import com.cookbookanything.app.data.database.toIngredientEntities
import com.cookbookanything.app.data.database.toModel
import com.cookbookanything.app.data.database.toPhotoEntities
import com.cookbookanything.app.data.database.toStepEntities
import com.cookbookanything.app.data.photos.PhotoStore
import com.cookbookanything.app.reminders.ReminderScheduler
import com.cookbookanything.app.widget.TonightWidgetUpdater
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
    private val widget: TonightWidgetUpdater,
    private val photoStore: PhotoStore,
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
        // Its calendar entries went with it.
        widget.refresh()
    }

    override suspend fun setIngredientChecked(ingredientId: String, checked: Boolean) =
        recipeDao.setIngredientChecked(ingredientId, checked)

    override suspend fun setStepDone(stepId: String, done: Boolean) =
        recipeDao.setStepDone(stepId, done)

    override suspend fun resetProgress(recipeId: String) = recipeDao.resetProgress(recipeId)

    override suspend fun setFavorite(recipeId: String, favorite: Boolean) = recipeDao.setFavorite(recipeId, favorite)

    override suspend fun setPinned(recipeId: String, pinned: Boolean) = recipeDao.setPinned(recipeId, pinned)

    override suspend fun setRating(recipeId: String, rating: Int?) = recipeDao.setRating(recipeId, rating?.coerceIn(1, 5))

    override suspend fun markCooked(recipeId: String, at: Long, photo: String?) {
        recipeDao.insertCooked(
            listOf(CookedEntity(id = UUID.randomUUID().toString(), recipeId = recipeId, cookedAt = at, photo = photo)),
        )
    }

    override suspend fun duplicate(recipeId: String, title: String): String? {
        val original = recipeDao.getRecipe(recipeId)?.toModel() ?: return null
        val now = System.currentTimeMillis()
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            title = title,
            photos = original.photos.mapNotNull { photoStore.copy(it) },
            ingredients = original.ingredients.map { it.copy(id = UUID.randomUUID().toString(), checked = false) },
            steps = original.steps.map { step ->
                step.copy(id = UUID.randomUUID().toString(), done = false, photos = step.photos.mapNotNull { photoStore.copy(it) })
            },
            comments = emptyList(),
            cookedAt = emptyList(),
            cookedPhotos = emptyMap(),
            createdAt = now,
            updatedAt = now,
        )
        saveRecipe(copy)
        return copy.id
    }

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
