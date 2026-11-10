package com.cookbookanything.app.data.database

import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.Comment
import com.cookbookanything.core.domain.model.Ingredient
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.model.ScheduledRecipe
import com.cookbookanything.core.domain.model.Step

fun CategoryEntity.toModel() = Category(id = id, name = name)

fun RecipeSummaryRow.toModel() =
    RecipeSummary(id = id, title = title, categoryId = categoryId, coverPhoto = coverPhoto)

fun ScheduledRecipeRow.toModel() =
    ScheduledRecipe(id = id, recipeId = recipeId, recipeTitle = recipeTitle, scheduledAt = scheduledAt)

fun RecipeWithDetails.toModel(): Recipe {
    val sortedPhotos = photos.sortedBy { it.position }
    return Recipe(
        id = recipe.id,
        title = recipe.title,
        categoryId = recipe.categoryId,
        photos = sortedPhotos.filter { it.stepId == null }.map { it.fileName },
        ingredients = ingredients.sortedBy { it.position }
            .map { Ingredient(id = it.id, text = it.text, checked = it.checked) },
        steps = steps.sortedBy { it.position }.map { step ->
            Step(
                id = step.id,
                text = step.text,
                done = step.done,
                photos = sortedPhotos.filter { it.stepId == step.id }.map { it.fileName },
            )
        },
        comments = comments.sortedBy { it.createdAt }
            .map { Comment(id = it.id, text = it.text, createdAt = it.createdAt) },
        createdAt = recipe.createdAt,
        updatedAt = recipe.updatedAt,
    )
}

fun Recipe.toEntity() = RecipeEntity(
    id = id,
    title = title,
    categoryId = categoryId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Recipe.toIngredientEntities() = ingredients.mapIndexed { index, ingredient ->
    IngredientEntity(
        id = ingredient.id,
        recipeId = id,
        text = ingredient.text,
        checked = ingredient.checked,
        position = index,
    )
}

fun Recipe.toStepEntities() = steps.mapIndexed { index, step ->
    StepEntity(id = step.id, recipeId = id, text = step.text, done = step.done, position = index)
}

fun Recipe.toPhotoEntities(): List<PhotoEntity> =
    photos.mapIndexed { index, name ->
        PhotoEntity(fileName = name, recipeId = id, stepId = null, position = index)
    } + steps.flatMap { step ->
        step.photos.mapIndexed { index, name ->
            PhotoEntity(fileName = name, recipeId = id, stepId = step.id, position = index)
        }
    }
