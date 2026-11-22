package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.text.instagramEmbedUrl
import com.cookbookanything.core.domain.text.linkIn
import com.cookbookanything.core.domain.text.parseRecipeHtml
import com.cookbookanything.core.domain.text.sourceKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonPrimitive

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
        // The HowToSection's name heads its steps.
        assertEquals(listOf("Κεφτέδες:", "Ζυμώνουμε τον κιμά.", "Ψήνουμε 20 min."), recipe.steps)
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
    fun `an instagram post or reel is read from its embed page`() {
        val embed = "https://www.instagram.com/p/Df0GeX5ivov/embed/captioned/"
        assertEquals(embed, instagramEmbedUrl("https://www.instagram.com/reel/Df0GeX5ivov/?igsh=MXphNXE1eDBnamh2"))
        assertEquals(embed, instagramEmbedUrl("https://instagram.com/some.cook/p/Df0GeX5ivov"))
        assertEquals(embed, instagramEmbedUrl("https://www.instagram.com/reels/Df0GeX5ivov/"))
        assertNull(instagramEmbedUrl("https://www.instagram.com/share/reel/BAxyz123/"))
        assertNull(instagramEmbedUrl("https://www.instagram.com/some.cook/"))
        assertNull(instagramEmbedUrl("https://example.com/p/abc/"))
    }

    @Test
    fun `reads the caption box and picture of an instagram embed page`() {
        val html = """
            <html><head><title>Instagram</title></head><body>
            <img class="EmbeddedMediaImage" alt="x" src="https://scontent.example/reel.jpg?a=1&amp;b=2" />
            <div class="Caption"><a class="CaptionUsername" href="https://www.instagram.com/some.cook/" target="_blank">some.cook</a><br/><br/>
            Κοτόπουλο λεμονάτο 🍋<br/>Υλικά:<br/>1 κιλό κοτόπουλο<br/>2 λεμόνια<br/>Εκτέλεση:<br/>Ψήνουμε για 40 λεπτά στους 180 βαθμούς.<br/>
            <a href="https://www.instagram.com/explore/tags/chicken/">#chicken</a>
            <div class="CaptionComments"><a class="CaptionCommentsExpand" href="#">View all 12 comments</a></div></div>
            <div class="Footer">Υλικά<br/>Log in</div></body></html>
        """.trimIndent()
        val recipe = assertNotNull(parseRecipeHtml(html))
        assertEquals("Κοτόπουλο λεμονάτο", recipe.title)
        assertEquals(listOf("1 κιλό κοτόπουλο", "2 λεμόνια"), recipe.ingredients)
        assertEquals(listOf("Ψήνουμε για 40 λεπτά στους 180 βαθμούς."), recipe.steps)
        assertEquals("https://scontent.example/reel.jpg?a=1&b=2", recipe.imageUrl)
        // Another site's "Caption" box is not a recipe.
        assertNull(parseRecipeHtml("""<html><body><div class="Caption">Photo by someone</div></body></html>"""))
    }

    @Test
    fun `reads the caption from an instagram embed page's data, plain or inside a JSON string`() {
        val caption = "Κέικ \"της γιαγιάς\"\nΥλικά\n2 αυγά\n1/2 φλ. ζάχαρη\nΕκτέλεση\nΨήνουμε 45 λεπτά."
        val data = """{"shortcode_media":{"display_url":"https:\/\/scontent.example\/cake.jpg?a=1\u0026b=2",""" +
            """"edge_media_to_caption":{"edges":[{"node":{"text":${JsonPrimitive(caption)}}}]}}}"""
        val nested = """{"contextJSON":${JsonPrimitive(data)}}"""
        for (script in listOf(data, nested)) {
            val recipe = assertNotNull(parseRecipeHtml("<html><head><title>Instagram</title></head><body><script>s.handle($script)</script></body></html>"))
            assertEquals("Κέικ \"της γιαγιάς\"", recipe.title)
            assertEquals(listOf("2 αυγά", "1/2 φλ. ζάχαρη"), recipe.ingredients)
            assertEquals(listOf("Ψήνουμε 45 λεπτά."), recipe.steps)
            assertEquals("https://scontent.example/cake.jpg?a=1&b=2", recipe.imageUrl)
        }
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
        assertEquals(listOf("Βράσιμο:", "Βράζουμε τα ζυμαρικά σε αλατισμένο νερό.", "Σουρώνουμε και κρατάμε λίγο νερό."), recipe.steps)
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
        assertEquals(sourceKey("https://www.instagram.com/reel/ABC123/?igsh=x"), sourceKey("https://instagram.com/p/ABC123"))
        assertEquals(sourceKey("https://www.example.com/cake/?utm=1"), sourceKey("http://example.com/cake"))
    }

    @Test
    fun `finds the post's video so a spoken recipe can be listened to`() {
        val reel = """<html><head><title>Instagram</title><meta property="og:video" content="https://scontent.example/reel.mp4?x=1" />
            <meta property="og:image" content="https://scontent.example/reel.jpg" /></head><body></body></html>"""
        val recipe = assertNotNull(parseRecipeHtml(reel))
        assertEquals("https://scontent.example/reel.mp4?x=1", recipe.videoUrl)
        assertEquals("https://scontent.example/reel.jpg", recipe.imageUrl)
        assertEquals(true, recipe.isEmpty)

        val tiktok = """<html><head><title>Cook on TikTok</title></head><body><script id="__UNIVERSAL_DATA_FOR_REHYDRATION__" type="application/json">
            {"video":{"playAddr":"https:\/\/v16.example\/video.mp4?a=1\u0026b=2","cover":"x"}}</script></body></html>"""
        assertEquals("https://v16.example/video.mp4?a=1&b=2", assertNotNull(parseRecipeHtml(tiktok)).videoUrl)

        assertNull(parseRecipeHtml("<html><head><title>Nothing</title></head><body><p>Hello</p></body></html>")?.videoUrl)
    }

    @Test
    fun `keeps the video a recipe page embeds, and a player page is no file to listen to`() {
        val blog = """<html><head><title>Μουσακάς</title><script type="application/ld+json">
            {"@type":"Recipe","name":"Μουσακάς","recipeIngredient":["3 μελιτζάνες"],"recipeInstructions":["Ψήνουμε."],
             "video":{"@type":"VideoObject","embedUrl":"https://www.youtube.com/embed/dQw4w9WgXcQ"}}</script></head><body></body></html>"""
        assertEquals("https://www.youtube.com/embed/dQw4w9WgXcQ", assertNotNull(parseRecipeHtml(blog)).videoLink)

        val framed = """<html><head><title>Cake</title></head><body><h1>Cake</h1><h2>Ingredients</h2><ul><li>2 eggs</li></ul>
            <iframe width="560" data-lazy-src="//player.vimeo.com/video/76979871?h=1&amp;x=2"></iframe></body></html>"""
        assertEquals("https://player.vimeo.com/video/76979871?h=1&x=2", assertNotNull(parseRecipeHtml(framed)).videoLink)

        val youtube = """<html><head><meta property="og:title" content="Easy moussaka" />
            <meta property="og:description" content="Moussaka in 30 minutes" />
            <meta property="og:url" content="https://www.youtube.com/watch?v=dQw4w9WgXcQ" />
            <meta property="og:video:url" content="https://www.youtube.com/embed/dQw4w9WgXcQ" /></head><body></body></html>"""
        val video = assertNotNull(parseRecipeHtml(youtube))
        assertNull(video.videoUrl)
        assertEquals("https://www.youtube.com/embed/dQw4w9WgXcQ", video.videoLink)
    }
}
