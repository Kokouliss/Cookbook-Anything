package com.cookbookanything.core.domain.model

data class Category(
    val id: String,
    val name: String,
)

data class Ingredient(
    val id: String,
    val text: String,
    val checked: Boolean = false,
)

/** [photos] are file names inside the app's photo folder. */
data class Step(
    val id: String,
    val text: String,
    val done: Boolean = false,
    val photos: List<String> = emptyList(),
)

data class Comment(
    val id: String,
    val text: String,
    val createdAt: Long,
)

/** Timestamps are epoch milliseconds. [photos] are file names inside the app's photo folder. */
data class Recipe(
    val id: String,
    val title: String,
    val categoryId: String? = null,
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
)

data class ScheduledRecipe(
    val id: String,
    val recipeId: String,
    val recipeTitle: String,
    val scheduledAt: Long,
)
