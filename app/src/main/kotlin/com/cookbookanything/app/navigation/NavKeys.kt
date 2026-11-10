package com.cookbookanything.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object RecipeListKey : NavKey

@Serializable
data object CalendarKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object CategoriesKey : NavKey

@Serializable
data class RecipeDetailKey(val recipeId: String) : NavKey

/** [recipeId] is null when creating a new recipe. */
@Serializable
data class RecipeEditKey(val recipeId: String? = null) : NavKey
