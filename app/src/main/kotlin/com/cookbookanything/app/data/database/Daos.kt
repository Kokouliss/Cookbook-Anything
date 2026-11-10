package com.cookbookanything.app.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecipeDao {
    @Query(
        """
        SELECT r.id, r.title, r.categoryId,
            (SELECT p.fileName FROM photos p
             WHERE p.recipeId = r.id AND p.stepId IS NULL
             ORDER BY p.position LIMIT 1) AS coverPhoto
        FROM recipes r
        ORDER BY r.updatedAt DESC
        """,
    )
    abstract fun observeSummaries(): Flow<List<RecipeSummaryRow>>

    @Query("SELECT COUNT(*) FROM recipes")
    abstract fun observeCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract fun observeRecipe(id: String): Flow<RecipeWithDetails?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract suspend fun getRecipe(id: String): RecipeWithDetails?

    @Query("SELECT fileName FROM photos")
    abstract suspend fun allPhotoNames(): List<String>

    // Upsert, not REPLACE: replacing the row would cascade-delete its comments and calendar entries.
    @Upsert
    abstract suspend fun upsertRecipe(recipe: RecipeEntity)

    @Insert
    abstract suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Insert
    abstract suspend fun insertSteps(steps: List<StepEntity>)

    @Insert
    abstract suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    abstract suspend fun deleteIngredients(recipeId: String)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    abstract suspend fun deleteSteps(recipeId: String)

    @Query("DELETE FROM photos WHERE recipeId = :recipeId")
    abstract suspend fun deletePhotos(recipeId: String)

    @Transaction
    open suspend fun saveRecipe(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        steps: List<StepEntity>,
        photos: List<PhotoEntity>,
    ) {
        upsertRecipe(recipe)
        deleteIngredients(recipe.id)
        deleteSteps(recipe.id)
        deletePhotos(recipe.id)
        insertIngredients(ingredients)
        insertSteps(steps)
        insertPhotos(photos)
    }

    @Query("DELETE FROM recipes WHERE id = :id")
    abstract suspend fun deleteRecipe(id: String)

    @Query("UPDATE ingredients SET checked = :checked WHERE id = :id")
    abstract suspend fun setIngredientChecked(id: String, checked: Boolean)

    @Query("UPDATE steps SET done = :done WHERE id = :id")
    abstract suspend fun setStepDone(id: String, done: Boolean)

    @Query("UPDATE ingredients SET checked = 0 WHERE recipeId = :recipeId")
    abstract suspend fun uncheckIngredients(recipeId: String)

    @Query("UPDATE steps SET done = 0 WHERE recipeId = :recipeId")
    abstract suspend fun uncheckSteps(recipeId: String)

    @Transaction
    open suspend fun resetProgress(recipeId: String) {
        uncheckIngredients(recipeId)
        uncheckSteps(recipeId)
    }

    @Insert
    abstract suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :id")
    abstract suspend fun deleteComment(id: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ScheduleDao {
    @Query(
        """
        SELECT s.id, s.recipeId, r.title AS recipeTitle, s.scheduledAt
        FROM schedules s JOIN recipes r ON r.id = s.recipeId
        ORDER BY s.scheduledAt
        """,
    )
    fun observeSchedule(): Flow<List<ScheduledRecipeRow>>

    @Query("SELECT id FROM schedules WHERE recipeId = :recipeId")
    suspend fun idsForRecipe(recipeId: String): List<String>

    @Insert
    suspend fun insert(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun delete(id: String)
}
