package com.cookbookanything.app.data.cook

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recipes the cook probably made without saying so: the page stayed open for a while. The next time
 * the recipe opens, it asks "Did you cook it?".
 */
@Singleton
class CookingPrompts @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("cooking_prompts", Context.MODE_PRIVATE)

    fun isPending(recipeId: String): Boolean = prefs.getBoolean(recipeId, false)

    fun set(recipeId: String, pending: Boolean) {
        prefs.edit().apply { if (pending) putBoolean(recipeId, true) else remove(recipeId) }.apply()
    }
}
