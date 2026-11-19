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
    /** Nice to have, not needed: the shopping list leaves it out. */
    val optional: Boolean = false,
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
    /** Marinating, resting, rising: time the cook waits. */
    val waitMinutes: Int? = null,
    /** A line under the name: what the dish is. */
    val description: String? = null,
    /** The cook's own notes: "next time less salt". */
    val notes: String? = null,
    /** Where the recipe came from: the link it was read from, or [SOURCE_PHOTO]. */
    val source: String? = null,
    /** 1 to 5 stars, null when not rated. */
    val rating: Int? = null,
    val favorite: Boolean = false,
    val photos: List<String> = emptyList(),
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<Step> = emptyList(),
    val comments: List<Comment> = emptyList(),
    /** When cooking mode was finished for this recipe, newest last. */
    val cookedAt: List<Long> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

/** The [Recipe.source] of a recipe read from a photo; anything else there is a link. */
const val SOURCE_PHOTO = "photo"

data class RecipeSummary(
    val id: String,
    val title: String,
    val categoryId: String?,
    val coverPhoto: String?,
    /** Every ingredient line of the recipe, for search. */
    val ingredients: String = "",
    val servings: Int? = null,
    val prepMinutes: Int? = null,
    val cookMinutes: Int? = null,
    val waitMinutes: Int? = null,
    val rating: Int? = null,
    val favorite: Boolean = false,
    val source: String? = null,
    val timesCooked: Int = 0,
    val lastCookedAt: Long? = null,
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
