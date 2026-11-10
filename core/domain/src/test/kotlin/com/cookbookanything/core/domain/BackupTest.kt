package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.backup.Backup
import com.cookbookanything.core.domain.backup.BackupCodec
import com.cookbookanything.core.domain.backup.BackupSchedule
import com.cookbookanything.core.domain.backup.prepareForRestore
import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.Comment
import com.cookbookanything.core.domain.model.Ingredient
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.Step
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackupTest {
    private val backup = Backup(
        categories = listOf(Category("c1", "Γλυκά")),
        recipes = listOf(
            Recipe(
                id = "r1",
                title = "Cake",
                categoryId = "c1",
                servings = 8,
                prepMinutes = 20,
                cookMinutes = 45,
                photos = listOf("a.jpg", "lost.jpg"),
                ingredients = listOf(Ingredient("i1", "2 eggs", checked = true)),
                steps = listOf(Step("s1", "Mix", done = true, photos = listOf("b.jpg"))),
                comments = listOf(Comment("m1", "Nice", createdAt = 5)),
                createdAt = 1,
                updatedAt = 2,
            ),
            Recipe(id = "r2", title = "Soup", categoryId = "gone"),
        ),
        schedules = listOf(BackupSchedule("s1", "r1", 10), BackupSchedule("s2", "missing", 11)),
    )

    @Test
    fun `survives a round trip`() {
        assertEquals(backup, BackupCodec.decode(BackupCodec.encode(backup)))
    }

    @Test
    fun `rejects other files and newer formats`() {
        assertNull(BackupCodec.decode("not json"))
        assertNull(BackupCodec.decode("""{"formatVersion": 99}"""))
    }

    @Test
    fun `restore renames photos and drops what cannot be kept`() {
        val ready = backup.prepareForRestore(
            photoNames = mapOf("a.jpg" to "new-a.jpg", "b.jpg" to "new-b.jpg"),
            knownCategoryIds = emptySet(),
        )
        assertEquals(listOf("new-a.jpg"), ready.recipes[0].photos)
        assertEquals(listOf("new-b.jpg"), ready.recipes[0].steps[0].photos)
        assertEquals("c1", ready.recipes[0].categoryId)
        assertNull(ready.recipes[1].categoryId)
        assertEquals(listOf("s1"), ready.schedules.map { it.id })
    }
}
