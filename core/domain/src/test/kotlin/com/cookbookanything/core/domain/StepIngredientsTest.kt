package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.cooking.ingredientsForStep
import kotlin.test.Test
import kotlin.test.assertEquals

class StepIngredientsTest {
    private val ingredients = listOf("200 g flour", "2 eggs", "3 tomatoes", "1 κρεμμύδι", "αλάτι")

    @Test
    fun `finds the ingredients a step names, in any form`() {
        assertEquals(listOf(0, 1), ingredientsForStep("Mix the flour with the eggs.", ingredients))
        assertEquals(listOf(2), ingredientsForStep("Chop the tomato.", ingredients))
        assertEquals(listOf(3, 4), ingredientsForStep("Σοτάρουμε τα κρεμμύδια και ρίχνουμε αλάτι.", ingredients))
    }

    @Test
    fun `numbers and short words do not match`() {
        assertEquals(emptyList(), ingredientsForStep("Bake for 200 minutes at 2 degrees.", ingredients))
    }
}
