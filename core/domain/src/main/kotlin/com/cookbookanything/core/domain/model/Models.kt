package com.cookbookanything.core.domain.model

import kotlinx.serialization.Serializable

// Serializable because a backup file stores these as they are.

@Serializable
data class Category(
    val id: String,
    val name: String,
)

@Serializable
data class Ingredient(
    val id: String,
    val text: String,
    val checked: Boolean = false,
)

/** [photos] are file names inside the app's photo folder. */
@Serializable
data class Step(
    val id: String,
    val text: String,
    val done: Boolean = false,
    val photos: List<String> = emptyList(),
)

@Serializable
data class Comment(
    val id: String,
    val text: String,
    val createdAt: Long,
)

/** Timestamps are epoch milliseconds. [photos] are file names inside the app's photo folder. */
@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val categoryId: String? = null,
    val servings: Int? = null,
    val prepMinutes: Int? = null,
    val cookMinutes: Int? = null,
    val photos: List<String> = emptyList(),
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<Step> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class RecipeSummary(
    val id: String,
    val title: String,
    val categoryId: String?,
    val coverPhoto: String?,
    /** Every ingredient line of the recipe, for search. */
    val ingredients: String = "",
)

data class ScheduledRecipe(
    val id: String,
    val recipeId: String,
    val recipeTitle: String,
    val scheduledAt: Long,
)

/** An ingredient of a recipe that is planned in the calendar. */
data class ShoppingItem(
    val ingredientId: String,
    val text: String,
    val checked: Boolean,
    val recipeId: String,
    val recipeTitle: String,
)
