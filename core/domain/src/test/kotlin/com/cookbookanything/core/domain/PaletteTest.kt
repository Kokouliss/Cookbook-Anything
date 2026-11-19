package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.design.Palette
import com.cookbookanything.core.domain.design.Palette.Dark
import com.cookbookanything.core.domain.design.Palette.Light
import kotlin.test.Test
import kotlin.test.assertTrue

/** Every pair the theme draws text with must meet WCAG AA (4.5:1), control edges 3:1. */
class PaletteTest {
    private val textPairs = listOf(
        "light ink on background" to (Light.INK to Light.BACKGROUND),
        "light ink on card" to (Light.INK to Light.CARD),
        "light muted on background" to (Light.MUTED to Light.BACKGROUND),
        "light muted on card" to (Light.MUTED to Light.CARD),
        "light red on background" to (Light.RED to Light.BACKGROUND),
        "light red on card" to (Light.RED to Light.CARD),
        "light on red" to (Light.ON_RED to Light.RED),
        "light on teal" to (Light.ON_TEAL to Light.TEAL),
        "light on ochre" to (Light.ON_OCHRE to Light.OCHRE),
        "light on red container" to (Light.ON_RED_CONTAINER to Light.RED_CONTAINER),
        "light on teal container" to (Light.ON_TEAL_CONTAINER to Light.TEAL_CONTAINER),
        "light on ochre container" to (Light.ON_OCHRE_CONTAINER to Light.OCHRE_CONTAINER),
        "light error on card" to (Light.ERROR to Light.CARD),
        "dark ink on background" to (Dark.INK to Dark.BACKGROUND),
        "dark ink on card" to (Dark.INK to Dark.CARD),
        "dark muted on background" to (Dark.MUTED to Dark.BACKGROUND),
        "dark muted on card" to (Dark.MUTED to Dark.CARD),
        "dark red on background" to (Dark.RED to Dark.BACKGROUND),
        "dark red on card" to (Dark.RED to Dark.CARD),
        "dark on red" to (Dark.ON_RED to Dark.RED),
        "dark on teal" to (Dark.ON_TEAL to Dark.TEAL),
        "dark on ochre" to (Dark.ON_OCHRE to Dark.OCHRE),
        "dark on red container" to (Dark.ON_RED_CONTAINER to Dark.RED_CONTAINER),
        "dark on teal container" to (Dark.ON_TEAL_CONTAINER to Dark.TEAL_CONTAINER),
        "dark on ochre container" to (Dark.ON_OCHRE_CONTAINER to Dark.OCHRE_CONTAINER),
        "dark error on card" to (Dark.ERROR to Dark.CARD),
    )

    private val edgePairs = listOf(
        "light outline on card" to (Light.OUTLINE to Light.CARD),
        "light outline on background" to (Light.OUTLINE to Light.BACKGROUND),
        "light teal on background" to (Light.TEAL to Light.BACKGROUND),
        "light red on background" to (Light.RED to Light.BACKGROUND),
        "dark outline on card" to (Dark.OUTLINE to Dark.CARD),
        "dark outline on background" to (Dark.OUTLINE to Dark.BACKGROUND),
        "dark teal on background" to (Dark.TEAL to Dark.BACKGROUND),
    )

    @Test
    fun `text pairs reach 4_5 to 1`() {
        textPairs.forEach { (name, pair) ->
            val ratio = Palette.contrastRatio(pair.first, pair.second)
            assertTrue(ratio >= 4.5, "$name is ${"%.2f".format(ratio)}:1")
        }
    }

    @Test
    fun `control edges reach 3 to 1`() {
        edgePairs.forEach { (name, pair) ->
            val ratio = Palette.contrastRatio(pair.first, pair.second)
            assertTrue(ratio >= 3.0, "$name is ${"%.2f".format(ratio)}:1")
        }
    }

    @Test
    fun `black on white is 21 to 1`() {
        assertTrue(Palette.contrastRatio(0xFF000000, 0xFFFFFFFF) > 20.9)
    }
}
