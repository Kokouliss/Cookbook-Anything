package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.search.matches
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
}
