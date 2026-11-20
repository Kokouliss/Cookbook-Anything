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
    /** Free words the cook filters by: "quick", "vegan", "Sunday". */
    val tags: List<String> = emptyList(),
    /** Pinned recipes stay at the top of the list whatever the sort. */
    val pinned: Boolean = false,
    val photos: List<String> = emptyList(),
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<Step> = emptyList(),
    val comments: List<Comment> = emptyList(),
    /** When cooking mode was finished for this recipe, newest last. */
    val cookedAt: List<Long> = emptyList(),
    /** A photo taken after cooking, by the time in [cookedAt] it belongs to. */
    val cookedPhotos: Map<Long, String> = emptyMap(),
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
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
) {
    val totalMinutes: Int get() = (prepMinutes ?: 0) + (cookMinutes ?: 0) + (waitMinutes ?: 0)
}

/** The meal a planned recipe is for; [hour] is the time it is planned at unless the cook picks another. */
enum class MealSlot(val hour: Int) { BREAKFAST(8), LUNCH(13), SNACK(17), DINNER(20) }

data class ScheduledRecipe(
    val id: String,
    val recipeId: String,
    val recipeTitle: String,
    val scheduledAt: Long,
    val slot: MealSlot? = null,
)

/** An ingredient of a recipe that is planned in the calendar. */
data class ShoppingItem(
    val ingredientId: String,
    val text: String,
    val checked: Boolean,
    val recipeId: String,
    val recipeTitle: String,
)
