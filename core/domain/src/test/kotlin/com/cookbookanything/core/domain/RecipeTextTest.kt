package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.text.parseRecipeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeTextTest {
    @Test
    fun `headers decide the lists and bullets are dropped`() {
        val parsed = parseRecipeText(
            """
            Pancakes
            Serves 4

            Ingredients:
            - 200 g flour
            • 2 eggs
            300 ml milk

            Method
            1. Mix the flour and the eggs.
            2) Add the milk and whisk until smooth.
            Step 3: Fry in a hot pan for 2 minutes a side.
            """.trimIndent(),
        )
        assertEquals("Pancakes", parsed.title)
        assertEquals(4, parsed.servings)
        assertEquals(listOf("200 g flour", "2 eggs", "300 ml milk"), parsed.ingredients)
        assertEquals(
            listOf(
                "Mix the flour and the eggs.",
                "Add the milk and whisk until smooth.",
                "Fry in a hot pan for 2 minutes a side.",
            ),
            parsed.steps,
        )
    }

    @Test
    fun `greek headers and servings`() {
        val parsed = parseRecipeText(
            """
            Φακές
            Μερίδες: 6
            Υλικά
            500 γρ. φακές
            1 κρεμμύδι
            Εκτέλεση
            Βράζουμε τις φακές για 10 λεπτά και στραγγίζουμε.
            Προσθέτουμε το κρεμμύδι.
            """.trimIndent(),
        )
        assertEquals("Φακές", parsed.title)
        assertEquals(6, parsed.servings)
        assertEquals(listOf("500 γρ. φακές", "1 κρεμμύδι"), parsed.ingredients)
        assertEquals(2, parsed.steps.size)
    }

    @Test
    fun `without headers amounts are ingredients and sentences are steps`() {
        val parsed = parseRecipeText(
            """
            2 cups rice
            ½ tsp salt
            Rinse the rice, then simmer it covered for 15 minutes and let it rest.
            Serve warm.
            """.trimIndent(),
        )
        assertNull(parsed.title)
        assertEquals(listOf("2 cups rice", "½ tsp salt"), parsed.ingredients)
        assertEquals(listOf("Rinse the rice, then simmer it covered for 15 minutes and let it rest.", "Serve warm."), parsed.steps)
    }

    @Test
    fun `short lines under an ingredient are ingredients, hashtag lines are dropped`() {
        // The shape of an Instagram caption: two intro sentences, the list, the method, hashtags.
        val parsed = parseRecipeText(
            """
            Στείλε αυτή τη συνταγή στο άτομο που θέλεις να στη φτιάξει ❤️
            Φτιάξε στο σπίτι το απόλυτο Steak Sandwich με ζουμερό μοσχαρίσιο steak και κρεμώδη Peppercorn Sauce!
            1 μπαγκέτα
            150 γρ. κρέμα γάλακτος
            πατάτες κομμένες σε λεπτές φέτες
            αλάτι
            Τηγανίζουμε τις πατάτες μέχρι να είναι έτοιμες.
            Serve warm
            #steak #sandwich
            """.trimIndent(),
        )
        assertEquals(listOf("1 μπαγκέτα", "150 γρ. κρέμα γάλακτος", "πατάτες κομμένες σε λεπτές φέτες", "αλάτι"), parsed.ingredients)
        // The two intro sentences stay as steps, to be removed by hand.
        assertEquals(4, parsed.steps.size)
        assertEquals("Serve warm", parsed.steps.last())
    }

    @Test
    fun `blank text is empty`() {
        assertTrue(parseRecipeText("  \n\n ").isEmpty)
    }
}
