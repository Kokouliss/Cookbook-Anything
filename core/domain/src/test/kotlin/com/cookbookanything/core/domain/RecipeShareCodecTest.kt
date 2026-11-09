package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.model.Comment
import com.cookbookanything.core.domain.model.Ingredient
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.Step
import com.cookbookanything.core.domain.share.RecipeShareCodec
import com.cookbookanything.core.domain.share.toRecipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeShareCodecTest {
    private val recipe = Recipe(
        id = "r1",
        title = "Μουσακάς",
        categoryId = "c1",
        photos = listOf("cover.jpg"),
        ingredients = listOf(Ingredient("i1", "2 μελιτζάνες", checked = true)),
        steps = listOf(Step("s1", "Τηγανίζουμε", done = true, photos = listOf("step.jpg", "lost.jpg"))),
        comments = listOf(Comment("m1", "private note", 1L)),
    )

    @Test
    fun `a shared recipe comes back as a fresh recipe without personal data`() {
        val shared = assertNotNull(RecipeShareCodec.decode(RecipeShareCodec.encode(recipe)))
        var next = 0
        val imported = shared.toRecipe(
            newId = { "new${next++}" },
            photoNames = mapOf("cover.jpg" to "a.jpg", "step.jpg" to "b.jpg"),
            now = 42L,
        )

        assertEquals("Μουσακάς", imported.title)
        assertEquals("new0", imported.id)
        assertNull(imported.categoryId)
        assertEquals(listOf("a.jpg"), imported.photos)
        assertEquals(listOf("2 μελιτζάνες"), imported.ingredients.map { it.text })
        assertTrue(imported.ingredients.none { it.checked })
        assertEquals(listOf("b.jpg"), imported.steps.single().photos)
        assertTrue(imported.steps.none { it.done })
        assertTrue(imported.comments.isEmpty())
        assertEquals(42L, imported.createdAt)
    }

    @Test
    fun `the format version is written into the file`() {
        assertTrue("\"formatVersion\":1" in RecipeShareCodec.encode(recipe))
    }

    @Test
    fun `files that are not recipes are rejected`() {
        assertNull(RecipeShareCodec.decode("not json"))
        assertNull(RecipeShareCodec.decode("""{"title":"  "}"""))
        assertNull(RecipeShareCodec.decode("""{"formatVersion":99,"title":"From the future"}"""))
    }

    @Test
    fun `unknown fields from newer minor versions are ignored`() {
        assertNotNull(RecipeShareCodec.decode("""{"title":"Soup","servings":4}"""))
    }
}
