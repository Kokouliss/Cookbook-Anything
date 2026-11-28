package com.cookbookanything.app.feature.recipelist

import android.content.Context
import androidx.core.content.edit

/** How the recipes are laid out on the home page: three sizes of picture, or a plain list. */
enum class RecipeView { SMALL, MEDIUM, LARGE, LIST }

/** The layout the cook picked, kept between visits. */
object RecipeViewPreferences {
    private const val PREFERENCES = "recipe_view"
    private const val KEY_VIEW = "view"

    fun get(context: Context): RecipeView {
        val saved = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY_VIEW, null)
        return RecipeView.entries.firstOrNull { it.name == saved } ?: RecipeView.MEDIUM
    }

    fun set(context: Context, view: RecipeView) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit { putString(KEY_VIEW, view.name) }
    }
}
