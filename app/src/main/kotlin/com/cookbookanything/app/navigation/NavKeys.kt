package com.cookbookanything.app.navigation

import androidx.navigation3.runtime.NavKey
import com.cookbookanything.core.domain.units.MeasurementSystem
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
data object AccountKey : NavKey

@Serializable
data class RecipeDetailKey(val recipeId: String) : NavKey

/**
 * [recipeId] is null when creating a new recipe; [pastedText] is text shared from another app to
 * start it from, [photoUris] screenshots shared to read it from.
 */
@Serializable
data class RecipeEditKey(val recipeId: String? = null, val pastedText: String? = null, val photoUris: List<String>? = null) : NavKey

/** [servings] and [measurement] are what the cook chose on the recipe page; null servings means the recipe as written. */
@Serializable
data class CookModeKey(val recipeId: String, val servings: Int? = null, val measurement: MeasurementSystem = MeasurementSystem.ORIGINAL) : NavKey

@Serializable
data object ImportGuideKey : NavKey

@Serializable
data object ShoppingListKey : NavKey
