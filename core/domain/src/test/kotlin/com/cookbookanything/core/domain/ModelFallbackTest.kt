package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.text.firstAvailableModel
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModelFallbackTest {
    private val models = listOf("new", "latest")
    private val unavailable = { e: Exception -> e.message == "404" }

    @Test
    fun `a retired model hands over to the next one, which is remembered by its place`() {
        val asked = mutableListOf<String>()
        val answer = firstAvailableModel(models, 0, unavailable) { asked += it; if (it == "new") throw IOException("404") else "recipe" }
        assertEquals(1 to "recipe", answer)
        assertEquals(models, asked)
        assertEquals(1 to "recipe", firstAvailableModel(models, 1, unavailable) { check(it == "latest"); "recipe" })
    }

    @Test
    fun `other refusals and the last model's are thrown`() {
        var calls = 0
        assertFailsWith<IOException> { firstAvailableModel(models, 0, unavailable) { calls++; throw IOException("403") } }
        assertEquals(1, calls)
        assertFailsWith<IOException> { firstAvailableModel(models, 0, unavailable) { throw IOException("404") } }
    }
}
