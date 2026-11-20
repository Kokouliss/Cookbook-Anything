package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.ingredients.MeasureUnit
import com.cookbookanything.core.domain.ingredients.isHeading
import com.cookbookanything.core.domain.ingredients.parseIngredient
import com.cookbookanything.core.domain.ingredients.productKey
import com.cookbookanything.core.domain.shopping.Aisle
import com.cookbookanything.core.domain.shopping.aisleFor
import com.cookbookanything.core.domain.units.MeasurementSystem
import com.cookbookanything.core.domain.units.convertIngredient
import com.cookbookanything.core.domain.units.convertTemperatures
import com.cookbookanything.core.domain.units.hasConvertibleUnits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IngredientParserTest {
    @Test
    fun `amount, unit, product and note`() {
        val parsed = parseIngredient("1½ cups all-purpose flour, sifted (about 180 g)")
        assertEquals(1.5, parsed.amount)
        assertEquals(MeasureUnit.CUP, parsed.unit)
        assertEquals("cups", parsed.unitText)
        assertEquals("all-purpose flour", parsed.product)
        assertEquals("about 180 g, sifted", parsed.note)
    }

    @Test
    fun `greek multi-word units and ranges`() {
        val spoon = parseIngredient("2 κουταλιές σούπας ελαιόλαδο")
        assertEquals(MeasureUnit.TABLESPOON, spoon.unit)
        assertEquals("ελαιόλαδο", spoon.product)
        val tsp = parseIngredient("1 κ.γ. αλάτι")
        assertEquals(MeasureUnit.TEASPOON, tsp.unit)
        assertEquals("αλάτι", tsp.product)
        val range = parseIngredient("2-3 cloves of garlic")
        assertEquals(2.0, range.amount)
        assertEquals(3.0, range.amountEnd)
        assertEquals(MeasureUnit.CLOVE, range.unit)
        assertEquals("garlic", range.product)
    }

    @Test
    fun `lines without a measure keep the whole text as the product`() {
        val salt = parseIngredient("Salt and pepper to taste")
        assertNull(salt.amount)
        assertEquals("Salt and pepper to taste", salt.product)
        val eggs = parseIngredient("3 eggs")
        assertEquals(3.0, eggs.amount)
        assertNull(eggs.unit)
        assertEquals("eggs", eggs.product)
        // A unit word with nothing after it is the product.
        assertEquals("φέτες", parseIngredient("2 φέτες").product)
    }

    @Test
    fun `product keys meet across plurals, accents and fillers`() {
        assertEquals(productKey("eggs"), productKey("Egg"))
        assertEquals(productKey("αυγά"), productKey("αυγό"))
        assertEquals(productKey("fresh tomatoes"), productKey("tomato"))
        assertEquals(productKey("Ντομάτες"), productKey("ντοματα"))
    }

    @Test
    fun `headings end with a colon and carry no amount`() {
        assertTrue(isHeading("For the sauce:"))
        assertTrue(isHeading("Για τη ζύμη:"))
        assertFalse(isHeading("200 g flour:"))
        assertFalse(isHeading("Mix the flour with the eggs and the milk until the batter is smooth and thick:"))
    }

    @Test
    fun `aisles are guessed from the product, longest stem wins`() {
        assertEquals(Aisle.OILS_VINEGARS, aisleFor("extra virgin olive oil"))
        assertEquals(Aisle.PRODUCE, aisleFor("Kalamata olives"))
        assertEquals(Aisle.HERBS_SPICES, aisleFor("black pepper"))
        assertEquals(Aisle.PRODUCE, aisleFor("red bell pepper"))
        assertEquals(Aisle.DAIRY_EGGS, aisleFor("αυγά"))
        assertEquals(Aisle.PANTRY, aisleFor("coconut milk"))
        assertEquals(Aisle.FREEZER, aisleFor("frozen peas"))
        assertEquals(Aisle.PASTA_GRAINS, aisleFor("ρύζι"))
        assertEquals(Aisle.OTHER, aisleFor("something unknown"))
    }

    @Test
    fun `metric and imperial conversion`() {
        assertEquals("240 ml flour", convertIngredient("1 cup flour", MeasurementSystem.METRIC))
        assertEquals("1.1 lb αλεύρι", convertIngredient("500 γρ. αλεύρι", MeasurementSystem.IMPERIAL))
        assertEquals("8.8 oz flour", convertIngredient("250 g flour", MeasurementSystem.IMPERIAL))
        assertEquals("1 cup milk", convertIngredient("240 ml milk", MeasurementSystem.IMPERIAL))
        assertEquals("2 tsp vanilla", convertIngredient("10 ml vanilla", MeasurementSystem.IMPERIAL))
        assertEquals("2 eggs", convertIngredient("2 eggs", MeasurementSystem.IMPERIAL))
        assertEquals("1 tbsp oil", convertIngredient("1 tbsp oil", MeasurementSystem.METRIC))
        assertEquals("225 g butter", convertIngredient("8 oz butter", MeasurementSystem.METRIC))
        assertEquals("1 cup flour", convertIngredient("1 cup flour", MeasurementSystem.ORIGINAL))
        assertEquals("Bake at 180°C for 20 minutes.", convertTemperatures("Bake at 350°F for 20 minutes.", MeasurementSystem.METRIC))
        assertEquals("Bake at 350°F.", convertTemperatures("Bake at 180 degrees C.", MeasurementSystem.IMPERIAL))
        assertTrue(hasConvertibleUnits(listOf("1 cup flour"), emptyList()))
        assertFalse(hasConvertibleUnits(listOf("2 eggs", "1 tbsp oil"), listOf("Bake for 20 minutes.")))
    }
}
