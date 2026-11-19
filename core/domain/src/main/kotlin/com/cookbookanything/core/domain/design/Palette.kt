package com.cookbookanything.core.domain.design

import kotlin.math.pow

/**
 * The recipe-card box palette, as ARGB values. The app's theme reads these; they live here so a unit
 * test can check every text pair against WCAG AA without an Android runtime.
 *
 * Light: index cards (card) in a cream box (background). Dark: the same cards on the ink of the
 * AnyKind logos. The reds, teal and ochre are the family colours, lightened at night for contrast.
 */
object Palette {
    object Light {
        const val BACKGROUND = 0xFFF6EFE3
        const val CARD = 0xFFFFFDF8
        const val INK = 0xFF171A21
        const val MUTED = 0xFF4F5563
        const val RED = 0xFFB4472E
        const val ON_RED = 0xFFFFFFFF
        const val RED_CONTAINER = 0xFFF7D9CF
        const val ON_RED_CONTAINER = 0xFF3B1208
        const val TEAL = 0xFF2F7F92
        const val ON_TEAL = 0xFFFFFFFF
        const val TEAL_CONTAINER = 0xFFCFE6EC
        const val ON_TEAL_CONTAINER = 0xFF0B2F38
        const val OCHRE = 0xFFB58428
        const val ON_OCHRE = 0xFF171A21
        const val OCHRE_CONTAINER = 0xFFF3E2BE
        const val ON_OCHRE_CONTAINER = 0xFF3A2A05
        const val OUTLINE = 0xFF6B7080
        const val OUTLINE_FAINT = 0xFFD9D1C2
        const val SURFACE_VARIANT = 0xFFE9E0CF
        const val SURFACE_DIM = 0xFFE4DBCB
        const val SURFACE_LOWEST = 0xFFFFFFFF
        const val SURFACE_CONTAINER = 0xFFF0E7D8
        const val SURFACE_HIGH = 0xFFEAE0CF
        const val SURFACE_HIGHEST = 0xFFE3D8C5
        const val ERROR = 0xFFBA1A1A
    }

    object Dark {
        const val BACKGROUND = 0xFF171A21
        const val CARD = 0xFF22262F
        const val INK = 0xFFF5F0E4
        const val MUTED = 0xFFB9BCC6
        const val RED = 0xFFE8907A
        const val ON_RED = 0xFF3B1208
        const val RED_CONTAINER = 0xFF7A2C1A
        const val ON_RED_CONTAINER = 0xFFFFDAD1
        const val TEAL = 0xFF7FC3D3
        const val ON_TEAL = 0xFF0B2F38
        const val TEAL_CONTAINER = 0xFF1F5260
        const val ON_TEAL_CONTAINER = 0xFFCFE6EC
        const val OCHRE = 0xFFE0B35A
        const val ON_OCHRE = 0xFF3A2A05
        const val OCHRE_CONTAINER = 0xFF5E4310
        const val ON_OCHRE_CONTAINER = 0xFFF3E2BE
        const val OUTLINE = 0xFF8E93A1
        const val OUTLINE_FAINT = 0xFF3A3F4B
        const val SURFACE_VARIANT = 0xFF2E323C
        const val SURFACE_DIM = 0xFF111319
        const val SURFACE_LOWEST = 0xFF111319
        const val SURFACE_CONTAINER = 0xFF262A34
        const val SURFACE_HIGH = 0xFF2E323C
        const val SURFACE_HIGHEST = 0xFF383D48
        const val ERROR = 0xFFFFB4AB
    }

    /** WCAG 2 contrast ratio between two opaque ARGB colours, from 1.0 to 21.0. */
    fun contrastRatio(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
