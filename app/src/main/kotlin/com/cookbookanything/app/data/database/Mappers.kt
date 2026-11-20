package com.cookbookanything.app.data.database

import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.Comment
import com.cookbookanything.core.domain.model.Ingredient
import com.cookbookanything.core.domain.model.MealSlot
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.model.ScheduledRecipe
import com.cookbookanything.core.domain.model.ShoppingItem
import com.cookbookanything.core.domain.model.Step
import java.util.UUID

fun CategoryEntity.toModel() = Category(id = id, name = name)

fun RecipeSummaryRow.toModel() =
    RecipeSummary(
        id = id,
        title = title,
        categoryId = categoryId,
        coverPhoto = coverPhoto,
        ingredients = ingredientLines.orEmpty(),
        servings = servings,
        prepMinutes = prepMinutes,
        cookMinutes = cookMinutes,
        waitMinutes = waitMinutes,
        rating = rating,
        favorite = favorite,
        source = source,
        timesCooked = timesCooked,
        lastCookedAt = lastCookedAt,
        tags = tags.toTags(),
        pinned = pinned,
    )

private fun String?.toTags(): List<String> = orEmpty().lines().filter { it.isNotBlank() }

private fun List<String>.toTagsColumn(): String? = joinToString("\n").ifEmpty { null }

/** A slot name from the database; an unknown one (from a newer version) is dropped. */
fun String?.toSlot(): MealSlot? = this?.let { name -> MealSlot.entries.firstOrNull { it.name == name } }

fun ShoppingItemRow.toModel() = ShoppingItem(
    ingredientId = ingredientId,
    text = text,
    checked = checked,
    recipeId = recipeId,
    recipeTitle = recipeTitle,
)

fun ScheduledRecipeRow.toModel() =
    ScheduledRecipe(id = id, recipeId = recipeId, recipeTitle = recipeTitle, scheduledAt = scheduledAt, slot = slot.toSlot())

fun RecipeWithDetails.toModel(): Recipe {
    val sortedPhotos = photos.sortedBy { it.position }
    return Recipe(
        id = recipe.id,
        title = recipe.title,
        categoryId = recipe.categoryId,
        servings = recipe.servings,
        prepMinutes = recipe.prepMinutes,
        cookMinutes = recipe.cookMinutes,
        waitMinutes = recipe.waitMinutes,
        description = recipe.description,
        notes = recipe.notes,
        source = recipe.source,
        rating = recipe.rating,
        favorite = recipe.favorite,
        tags = recipe.tags.toTags(),
        pinned = recipe.pinned,
        photos = sortedPhotos.filter { it.stepId == null }.map { it.fileName },
        ingredients = ingredients.sortedBy { it.position }
            .map { Ingredient(id = it.id, text = it.text, checked = it.checked, optional = it.optional) },
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
        cookedAt = cooked.map { it.cookedAt }.sorted(),
        cookedPhotos = cooked.mapNotNull { entry -> entry.photo?.let { entry.cookedAt to it } }.toMap(),
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
    servings = servings,
    prepMinutes = prepMinutes,
    cookMinutes = cookMinutes,
    waitMinutes = waitMinutes,
    description = description,
    notes = notes,
    source = source,
    rating = rating,
    favorite = favorite,
    tags = tags.toTagsColumn(),
    pinned = pinned,
)

fun Recipe.toIngredientEntities() = ingredients.mapIndexed { index, ingredient ->
    IngredientEntity(
        id = ingredient.id,
        recipeId = id,
        text = ingredient.text,
        checked = ingredient.checked,
        position = index,
        optional = ingredient.optional,
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

fun Recipe.toCommentEntities() = comments.map {
    CommentEntity(id = it.id, recipeId = id, text = it.text, createdAt = it.createdAt)
}

fun Recipe.toCookedEntities() = cookedAt.map {
    CookedEntity(id = UUID.randomUUID().toString(), recipeId = id, cookedAt = it, photo = cookedPhotos[it])
}
