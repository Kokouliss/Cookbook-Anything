package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.search.RecipeFilter
import com.cookbookanything.core.domain.search.matches
import com.cookbookanything.core.domain.sort.RecipeSort
import com.cookbookanything.core.domain.sort.sortedBy
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecipeSearchTest {
    private val recipe = RecipeSummary(
        id = "1",
        title = "Γεμιστά της γιαγιάς",
        categoryId = null,
        coverPhoto = null,
        ingredients = "6 ντομάτες\n1 κούπα ρύζι\nCrème fraîche",
    )

    @Test
    fun `finds by title or ingredient without accents or case`() {
        assertTrue(recipe.matches("γεμιστα"))
        assertTrue(recipe.matches("ΝΤΟΜΑΤΕΣ"))
        assertTrue(recipe.matches("creme"))
    }

    @Test
    fun `final sigma matches a word that continues`() {
        assertTrue(recipe.matches("γιαγιας"))
        assertTrue(recipe.matches("ντοματες ρυζι"))
    }

    @Test
    fun `every word has to appear`() {
        assertFalse(recipe.matches("ρύζι κιμάς"))
        assertTrue(recipe.matches("  "))
    }

    @Test
    fun `filters by tags, time, cooked, rating and pantry`() {
        val quick = recipe.copy(tags = listOf("Quick", "vegan"), prepMinutes = 10, cookMinutes = 15, timesCooked = 2, rating = 5)
        assertTrue(quick.matches(RecipeFilter(tags = setOf("quick"))))
        assertFalse(recipe.matches(RecipeFilter(tags = setOf("quick"))))
        assertTrue(quick.matches(RecipeFilter(maxMinutes = 30)))
        assertFalse(quick.matches(RecipeFilter(maxMinutes = 15)))
        // No times known: it cannot promise to be quick.
        assertFalse(recipe.matches(RecipeFilter(maxMinutes = 60)))
        assertTrue(quick.matches(RecipeFilter(cooked = true)))
        assertTrue(recipe.matches(RecipeFilter(cooked = false)))
        assertFalse(recipe.matches(RecipeFilter(minRating = 4)))
        assertTrue(recipe.matches(RecipeFilter(pantry = "κιμάς, ρύζι")))
        assertFalse(recipe.matches(RecipeFilter(pantry = "κιμάς")))
        assertTrue(recipe.matches(RecipeFilter()))
        assertTrue(RecipeFilter().isEmpty)
    }

    @Test
    fun `pinned recipes lead every order`() {
        val a = recipe.copy(id = "a", title = "Zucchini")
        val b = recipe.copy(id = "b", title = "Apple", pinned = true)
        val c = recipe.copy(id = "c", title = "Mango")
        assertEquals(listOf("b", "a", "c"), listOf(a, b, c).sortedBy(RecipeSort.RECENT).map { it.id })
        assertEquals(listOf("b", "c", "a"), listOf(a, b, c).sortedBy(RecipeSort.A_TO_Z).map { it.id })
    }
}
