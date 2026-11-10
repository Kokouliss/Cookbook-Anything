package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.scaling.scaleIngredient
import kotlin.test.Test
import kotlin.test.assertEquals

class IngredientScalingTest {
    @Test
    fun `scales whole numbers, decimals and fractions`() {
        assertEquals("4 eggs", scaleIngredient("2 eggs", 2.0))
        assertEquals("375 g flour", scaleIngredient("250 g flour", 1.5))
        assertEquals("3,75 dl γάλα", scaleIngredient("2,5 dl γάλα", 1.5))
        assertEquals("1 cup sugar", scaleIngredient("1/2 cup sugar", 2.0))
        assertEquals("3 cups rice", scaleIngredient("1 1/2 cups rice", 2.0))
        assertEquals("0.33 tsp salt", scaleIngredient("1 tsp salt", 1.0 / 3))
    }

    @Test
    fun `scales both ends of a range`() {
        assertEquals("4-6 cloves", scaleIngredient("2-3 cloves", 2.0))
        assertEquals("1 – 2 tbsp oil", scaleIngredient("2 – 4 tbsp oil", 0.5))
    }

    @Test
    fun `leaves other lines alone`() {
        assertEquals("salt", scaleIngredient("salt", 3.0))
        assertEquals("a pinch of 2 spices", scaleIngredient("a pinch of 2 spices", 3.0))
        assertEquals("1/0 cup", scaleIngredient("1/0 cup", 2.0))
        assertEquals("2 eggs", scaleIngredient("2 eggs", 1.0))
    }
}
