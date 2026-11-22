package com.cookbookanything.core.domain.design

import kotlin.math.pow

/**
 * The app's palette, as ARGB values. The theme reads these; they live here so a unit test can check
 * every text pair against WCAG AA without an Android runtime.
 *
 * White and black with red accents (Panos's pick, 2026-11-22): plain white pages in light mode,
 * near-black in dark mode, neutral greys without a tint, and the family red of his AnyKind kit only
 * for the main button, ticks and switches. Selected chips and tonal buttons are grey, not tinted. In
 * the dark theme the red is a lighter tone with black text on it, so it reads on black.
 */
object Palette {
    object Light {
        const val BACKGROUND = 0xFFFFFFFF
        const val CARD = 0xFFFFFFFF
        const val INK = 0xFF141414
        const val MUTED = 0xFF6B6B6B
        const val RED = 0xFFB4472E
        const val ON_RED = 0xFFFFFFFF
        const val CONTAINER = 0xFFF0F0F0
        const val ON_CONTAINER = 0xFF141414
        const val OUTLINE = 0xFF767676
        const val OUTLINE_FAINT = 0xFFE6E6E6
        const val SURFACE_VARIANT = 0xFFF2F2F2
        const val SURFACE_DIM = 0xFFE3E3E3
        const val SURFACE_LOWEST = 0xFFFFFFFF
        const val SURFACE_CONTAINER = 0xFFF5F5F5
        const val SURFACE_HIGH = 0xFFEFEFEF
        const val SURFACE_HIGHEST = 0xFFE9E9E9
        const val ERROR = 0xFFB4472E
    }

    object Dark {
        const val BACKGROUND = 0xFF121212
        const val CARD = 0xFF1C1C1C
        const val INK = 0xFFF2F2F2
        const val MUTED = 0xFFA3A3A3
        const val RED = 0xFFDE6B52
        const val ON_RED = 0xFF121212
        const val CONTAINER = 0xFF2A2A2A
        const val ON_CONTAINER = 0xFFF2F2F2
        const val OUTLINE = 0xFF8C8C8C
        const val OUTLINE_FAINT = 0xFF333333
        const val SURFACE_VARIANT = 0xFF262626
        const val SURFACE_DIM = 0xFF0E0E0E
        const val SURFACE_LOWEST = 0xFF0E0E0E
        const val SURFACE_CONTAINER = 0xFF202020
        const val SURFACE_HIGH = 0xFF262626
        const val SURFACE_HIGHEST = 0xFF303030
        const val ERROR = 0xFFDE6B52
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
