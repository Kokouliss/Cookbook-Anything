package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.sort.RecipeSort
import com.cookbookanything.core.domain.sort.sortedBy
import kotlin.test.Test
import kotlin.test.assertEquals

class RecipeSortTest {
    private val recipes = listOf(
        RecipeSummary("1", "Σαλάτα", null, null, lastCookedAt = 300),
        RecipeSummary("2", "Άρτος", null, null, lastCookedAt = null),
        RecipeSummary("3", "bread", null, null, lastCookedAt = 100),
    )

    @Test
    fun `a to z ignores accents and capitals`() {
        assertEquals(listOf("3", "2", "1"), recipes.sortedBy(RecipeSort.A_TO_Z).map { it.id })
    }

    @Test
    fun `cooked recently and not for a while are mirror images`() {
        assertEquals(listOf("1", "3", "2"), recipes.sortedBy(RecipeSort.COOKED_RECENTLY).map { it.id })
        assertEquals(listOf("2", "3", "1"), recipes.sortedBy(RecipeSort.NOT_FOR_A_WHILE).map { it.id })
        assertEquals(recipes, recipes.sortedBy(RecipeSort.RECENT))
    }
}
