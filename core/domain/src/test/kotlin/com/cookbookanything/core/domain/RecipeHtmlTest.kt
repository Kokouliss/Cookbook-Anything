package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.text.linkIn
import com.cookbookanything.core.domain.text.parseRecipeHtml
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RecipeHtmlTest {
    @Test
    fun `reads the schema org recipe record, inside a graph, with sections and durations`() {
        val html = """
            <html><head>
            <meta property="og:image" content="https://example.com/og.jpg">
            <script type="application/ld+json">
            {"@context":"https://schema.org","@graph":[{"@type":"WebPage","name":"x"},
             {"@type":["Recipe","Thing"],"name":"Σουτζουκάκια &amp; ρύζι","recipeYield":"4 μερίδες",
              "prepTime":"PT20M","totalTime":"PT1H30M",
              "image":{"@type":"ImageObject","url":"https://example.com/dish.jpg"},
              "recipeIngredient":["500 γρ. κιμάς","1 <b>κρεμμύδι</b>"],
              "recipeInstructions":[{"@type":"HowToSection","name":"Κεφτέδες","itemListElement":[{"@type":"HowToStep","text":"1. Ζυμώνουμε τον κιμά."}]},
                                    {"@type":"HowToStep","text":"Ψήνουμε 20 min."}]}]}
            </script></head><body></body></html>
        """.trimIndent()
        val recipe = assertNotNull(parseRecipeHtml(html))
        assertEquals("Σουτζουκάκια & ρύζι", recipe.title)
        assertEquals(4, recipe.servings)
        assertEquals(20, recipe.prepMinutes)
        assertEquals(70, recipe.cookMinutes)
        assertEquals("https://example.com/dish.jpg", recipe.imageUrl)
        assertEquals(listOf("500 γρ. κιμάς", "1 κρεμμύδι"), recipe.ingredients)
        assertEquals(listOf("Ζυμώνουμε τον κιμά.", "Ψήνουμε 20 min."), recipe.steps)
    }

    @Test
    fun `falls back to the caption and thumbnail of an instagram post`() {
        val html = """
            <html><head>
            <meta property="og:title" content="Akis on Instagram: &quot;Κοτόπουλο λεμονάτο&#10;Υλικά&#10;1 κιλό κοτόπουλο&quot;" />
            <meta property="og:description" content="120 likes, 3 comments - akis on 2026-11-01: &quot;Κοτόπουλο λεμονάτο&#10;Υλικά&#10;1 κιλό κοτόπουλο&#10;2 λεμόνια&#10;Εκτέλεση&#10;Ψήνουμε για 40 λεπτά στους 180 βαθμούς.&quot;" />
            <meta property="og:image" content="https://instagram.example/thumb.jpg?a=1&amp;b=2" />
            </head><body>Log in</body></html>
        """.trimIndent()
        val recipe = assertNotNull(parseRecipeHtml(html))
        assertEquals("Κοτόπουλο λεμονάτο", recipe.title)
        assertEquals(listOf("1 κιλό κοτόπουλο", "2 λεμόνια"), recipe.ingredients)
        assertEquals(1, recipe.steps.size)
        assertEquals("https://instagram.example/thumb.jpg?a=1&b=2", recipe.imageUrl)
        assertNull(parseRecipeHtml("<html><head><title>Login</title></head></html>"))
    }

    @Test
    fun `reads microdata boxes with line breaks, section headings and number bubbles`() {
        val html = """
            <html><head><title>Λιγκουίνι Alfredo | Site</title>
            <meta property="og:title" content="Λιγκουίνι Alfredo με βούτυρο">
            <meta property="og:image" content="https://example.com/alfredo.jpg"></head>
            <body itemscope itemtype="https://schema.org/Recipe">
            <meta itemprop="prepTime" content="PT5M"><meta itemprop="cookTime" content="PT15M">
            <meta itemprop="recipeYield" content="2">
            <nav><span itemprop="name">Αρχική</span></nav>
            <div itemprop="recipeIngredient" class="box"><div><h2>Υλικά</h2><div>250 γρ. λιγκουίνι&nbsp;<br>50 γρ. βούτυρο<br>Αλάτι</div></div></div>
            <div itemprop="recipeInstructions"><h2>Εκτέλεση</h2><div><!--[--><div><h3>Βράσιμο</h3>
            <div><div>1</div><div>Βράζουμε τα ζυμαρικά σε αλατισμένο νερό.</div></div>
            <div><div>2</div><div>Σουρώνουμε και κρατάμε λίγο νερό.</div></div></div></div></div>
            <div>Τα σκεύη που θα χρειαστείς</div><div>1 σουρωτήρι</div>
            </body></html>
        """.trimIndent()
        val recipe = assertNotNull(parseRecipeHtml(html))
        assertEquals("Λιγκουίνι Alfredo με βούτυρο", recipe.title)
        assertEquals(2, recipe.servings)
        assertEquals(5, recipe.prepMinutes)
        assertEquals(15, recipe.cookMinutes)
        assertEquals(listOf("250 γρ. λιγκουίνι", "50 γρ. βούτυρο", "Αλάτι"), recipe.ingredients)
        assertEquals(listOf("Βράζουμε τα ζυμαρικά σε αλατισμένο νερό.", "Σουρώνουμε και κρατάμε λίγο νερό."), recipe.steps)
        assertEquals("https://example.com/alfredo.jpg", recipe.imageUrl)
    }

    @Test
    fun `reads the visible text from the ingredients heading when a page has no record`() {
        val html = """
            <html><head><meta property="og:title" content="Φακές σούπα"></head><body>
            <ul><li>Αρχική</li><li>Συνταγές</li><li>Το βιβλίο</li></ul>
            <h1>Φακές σούπα</h1><p>Μια κλασική σούπα για τον χειμώνα, όπως την έφτιαχνε η γιαγιά.</p>
            <h2>Υλικά</h2><ul><li>500 γρ. φακές</li><li>1 κρεμμύδι</li></ul>
            <h2>Εκτέλεση</h2><ol><li>Βράζουμε τις φακές για 10 λεπτά και σουρώνουμε.</li><li>Σοτάρουμε το κρεμμύδι.</li></ol>
            </body></html>
        """.trimIndent()
        val recipe = assertNotNull(parseRecipeHtml(html))
        assertEquals("Φακές σούπα", recipe.title)
        assertEquals(listOf("500 γρ. φακές", "1 κρεμμύδι"), recipe.ingredients)
        assertEquals(listOf("Βράζουμε τις φακές για 10 λεπτά και σουρώνουμε.", "Σοτάρουμε το κρεμμύδι."), recipe.steps)
    }

    @Test
    fun `a shared link is a link, a pasted recipe that mentions one is text`() {
        assertEquals("https://www.instagram.com/reel/abc/", linkIn("Δες αυτό https://www.instagram.com/reel/abc/ "))
        assertNull(linkIn("Κοτόπουλο\n".repeat(20) + "Πηγή: https://example.com/recipe"))
        assertNull(linkIn("Κοτόπουλο λεμονάτο\n1 κιλό κοτόπουλο"))
    }
}
