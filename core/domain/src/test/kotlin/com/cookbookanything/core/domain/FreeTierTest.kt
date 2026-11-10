package com.cookbookanything.core.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreeTierTest {
    @Test
    fun `free users can add until they hold five recipes`() {
        assertTrue(FreeTier.canAddRecipe(recipeCount = 4, unlocked = false))
        assertFalse(FreeTier.canAddRecipe(recipeCount = 5, unlocked = false))
    }

    @Test
    fun `unlocked users are never capped`() {
        assertTrue(FreeTier.canAddRecipe(recipeCount = 500, unlocked = true))
    }
}
