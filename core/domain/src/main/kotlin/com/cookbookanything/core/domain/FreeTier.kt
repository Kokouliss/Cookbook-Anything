package com.cookbookanything.core.domain

/** The free version holds [RECIPE_LIMIT] recipes at a time; the lifetime purchase removes the cap. */
object FreeTier {
    const val RECIPE_LIMIT = 5

    fun canAddRecipe(recipeCount: Int, unlocked: Boolean): Boolean =
        unlocked || recipeCount < RECIPE_LIMIT
}
