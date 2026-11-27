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
        // The two intro sentences are the description, not steps.
        assertEquals(2, parsed.description?.lines()?.size)
        assertEquals(listOf("Τηγανίζουμε τις πατάτες μέχρι να είναι έτοιμες.", "Serve warm"), parsed.steps)
        assertNull(parsed.title)
    }

    @Test
    fun `caption with a decorated title, a blog link and headings`() {
        val parsed = parseRecipeText(
            """
            🍋 LEMON OLIVE OIL CAKE 🍋
            The easiest cake you will ever bake, and it stays moist for days.
            Full recipe on the blog: https://example.com/lemon-cake
            For the cake:
            200 g flour
            3 eggs
            For the glaze:
            100 g icing sugar
            Whisk the eggs with the sugar until pale, then fold in the flour.
            Bake at 180°C for 40 minutes.
            @somebaker #cake #lemon
            """.trimIndent(),
            language = "en",
        )
        assertEquals("LEMON OLIVE OIL CAKE", parsed.title)
        assertEquals("The easiest cake you will ever bake, and it stays moist for days.", parsed.description)
        assertEquals("https://example.com/lemon-cake", parsed.link)
        assertEquals(listOf("For the cake:", "200 g flour", "3 eggs", "For the glaze:", "100 g icing sugar"), parsed.ingredients)
        assertEquals(2, parsed.steps.size)
    }

    @Test
    fun `bilingual caption keeps the reader's language`() {
        val text = """
            Υλικά:
            200 γρ. αλεύρι
            3 αυγά
            Ingredients:
            200 g flour
            3 eggs
            Εκτέλεση:
            Ανακατεύουμε το αλεύρι με τα αυγά.
            Ψήνουμε για 20 λεπτά.
            Method:
            Mix the flour with the eggs.
            Bake for 20 minutes.
        """.trimIndent()
        val greek = parseRecipeText(text, language = "el")
        assertEquals(listOf("200 γρ. αλεύρι", "3 αυγά"), greek.ingredients)
        assertEquals(listOf("Ανακατεύουμε το αλεύρι με τα αυγά.", "Ψήνουμε για 20 λεπτά."), greek.steps)
        val english = parseRecipeText(text, language = "en")
        assertEquals(listOf("200 g flour", "3 eggs"), english.ingredients)
        assertEquals(listOf("Mix the flour with the eggs.", "Bake for 20 minutes."), english.steps)
        // One language only: nothing is dropped.
        assertEquals(2, parseRecipeText("Υλικά\n200 γρ. αλεύρι\n3 αυγά", language = "en").ingredients.size)
    }

    @Test
    fun `a header with the servings in brackets and dashed amounts, in two languages`() {
        // The shape of the reel caption that came out as the name "6 αυγά" and eleven steps.
        val caption = """
            Αλμυρό κέικ ομελέτας με μπρόκολο, γαλοπούλα, φέτα και γκούντα 🥦

            Μια συνταγή με λίγα υλικά και χωρίς πολλά σκεύη. Χωρίς αλεύρι, και κόβεται σε φέτες! 🧀

            Υλικά (4–6 μερίδες)

            - 300 γρ. μπρόκολο
            - 6 αυγά
            - 120 γρ. στραγγιστό γιαούρτι 2%
            - 1 κ.σ. ελαιόλαδο
            - Λίγο αλάτι και πιπέρι

            Εσύ θα το προτιμούσες για πρωινό ή για βραδινό;

            🇺🇸

            Savory Broccoli, Turkey, Feta & Gouda Egg Loaf 🥦

            A recipe with few ingredients and few dishes. No flour, and it cuts into slices! 🧀

            Ingredients (4–6 servings)

            - 300 g broccoli
            - 6 eggs
            - 120 g strained Greek yogurt (2%)
            - 1 tbsp olive oil
            - A little salt and black pepper

            Would you have this for breakfast or dinner?

            #eggloaf #easyrecipes
        """.trimIndent()
        val greek = parseRecipeText(caption, language = "el")
        assertEquals("Αλμυρό κέικ ομελέτας με μπρόκολο, γαλοπούλα, φέτα και γκούντα", greek.title)
        assertEquals("Μια συνταγή με λίγα υλικά και χωρίς πολλά σκεύη. Χωρίς αλεύρι, και κόβεται σε φέτες! 🧀", greek.description)
        assertEquals(6, greek.servings)
        assertEquals(
            listOf("300 γρ. μπρόκολο", "6 αυγά", "120 γρ. στραγγιστό γιαούρτι 2%", "1 κ.σ. ελαιόλαδο", "Λίγο αλάτι και πιπέρι"),
            greek.ingredients,
        )
        assertTrue(greek.steps.isEmpty())
        // shortcut: an English reader still gets the second title and intro on top of the list, split the caption at the language change if that is reported.
        assertEquals(
            listOf("300 g broccoli", "6 eggs", "120 g strained Greek yogurt (2%)", "1 tbsp olive oil", "A little salt and black pepper"),
            parseRecipeText(caption, language = "en").ingredients.takeLast(5),
        )
    }

    @Test
    fun `blank text is empty`() {
        assertTrue(parseRecipeText("  \n\n ").isEmpty)
    }
}
