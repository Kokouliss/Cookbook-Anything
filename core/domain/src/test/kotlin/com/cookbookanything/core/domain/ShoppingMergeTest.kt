package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.model.ShoppingItem
import com.cookbookanything.core.domain.shopping.merged
import com.cookbookanything.core.domain.shopping.toBuyText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShoppingMergeTest {
    private fun item(id: String, text: String, recipe: String, checked: Boolean = false) =
        ShoppingItem(ingredientId = id, text = text, checked = checked, recipeId = recipe, recipeTitle = recipe)

    @Test
    fun `same ingredient from two recipes becomes one line`() {
        val merged = listOf(
            item("a", "200 g flour", "Pancakes"),
            item("b", "2 eggs", "Pancakes", checked = true),
            item("c", "500 g Flour", "Bread"),
            item("d", "Salt", "Bread"),
            item("e", "salt", "Pancakes"),
        ).merged()
        assertEquals(listOf("200 g + 500 g flour", "2 eggs", "Salt"), merged.map { it.label })
        assertEquals(listOf("a", "c"), merged[0].ingredientIds)
        assertEquals(listOf("Pancakes", "Bread"), merged[0].recipeTitles)
        assertTrue(merged[1].checked)
        assertFalse(merged[2].checked)
    }

    @Test
    fun `greek units and fractions`() {
        val merged = listOf(
            item("a", "2 κ.σ. ελαιόλαδο", "A"),
            item("b", "½ φλ. Ελαιολαδο", "B"),
            item("c", "1 κρεμμύδι", "A"),
        ).merged()
        assertEquals(listOf("2 κ.σ. + ½ φλ. ελαιόλαδο", "1 κρεμμύδι"), merged.map { it.label })
    }

    @Test
    fun `text to send skips what is ticked`() {
        val text = listOf(
            item("a", "200 g flour", "A", checked = true),
            item("b", "3 lemons", "A"),
        ).merged().toBuyText()
        assertEquals("3 lemons", text)
    }
}
