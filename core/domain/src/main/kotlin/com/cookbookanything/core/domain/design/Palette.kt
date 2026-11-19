package com.cookbookanything.core.domain.design

import kotlin.math.pow

/**
 * The cookbook palette, as ARGB values. The app's theme reads these; they live here so a unit test
 * can check every text pair against WCAG AA without an Android runtime.
 *
 * From Panos's AnyKind graphite kit (app-palette.css): ink and paper carry the app, the neutrals
 * come from the logo ink, and the lead colour of Cookbook Anything is the family red, for the main
 * button, links and selections. Teal and ochre are the other two shape colours of the logos. In the
 * dark theme the red and the other accents are the kit's lighter "text" tones, so they read on ink.
 */
object Palette {
    object Light {
        const val BACKGROUND = 0xFFF7F7FA
        const val CARD = 0xFFFFFFFF
        const val INK = 0xFF171A21
        const val MUTED = 0xFF6B6C73
        const val RED = 0xFFB4472E
        const val ON_RED = 0xFFFFFFFF
        const val RED_CONTAINER = 0xFFF6E9E6
        const val ON_RED_CONTAINER = 0xFF6B2A1A
        const val TEAL = 0xFF2F7F92
        const val ON_TEAL = 0xFFFFFFFF
        const val TEAL_CONTAINER = 0xFFE6F0F2
        const val ON_TEAL_CONTAINER = 0xFF1E4F5C
        const val OCHRE = 0xFFB08128
        const val ON_OCHRE = 0xFF171A21
        const val OCHRE_CONTAINER = 0xFFF6F0E5
        const val ON_OCHRE_CONTAINER = 0xFF5C4418
        const val OUTLINE = 0xFF6B6C73
        const val OUTLINE_FAINT = 0xFFDCDEE5
        const val SURFACE_VARIANT = 0xFFEDEEF2
        const val SURFACE_DIM = 0xFFE4E5EA
        const val SURFACE_LOWEST = 0xFFFFFFFF
        const val SURFACE_CONTAINER = 0xFFF0F1F4
        const val SURFACE_HIGH = 0xFFEDEEF2
        const val SURFACE_HIGHEST = 0xFFE6E7EC
        const val ERROR = 0xFFB4472E
    }

    object Dark {
        const val BACKGROUND = 0xFF171A21
        const val CARD = 0xFF20242D
        const val INK = 0xFFF2F3F6
        const val MUTED = 0xFF95979C
        const val RED = 0xFFCF8979
        const val ON_RED = 0xFF171A21
        const val RED_CONTAINER = 0xFF442C2D
        const val ON_RED_CONTAINER = 0xFFF2D2CA
        const val TEAL = 0xFF72A8B5
        const val ON_TEAL = 0xFF171A21
        const val TEAL_CONTAINER = 0xFF243A45
        const val ON_TEAL_CONTAINER = 0xFFCDE3E8
        const val OCHRE = 0xFFC7A25C
        const val ON_OCHRE = 0xFF171A21
        const val OCHRE_CONTAINER = 0xFF443B2C
        const val ON_OCHRE_CONTAINER = 0xFFF0E2C6
        const val OUTLINE = 0xFF8E93A1
        const val OUTLINE_FAINT = 0xFF3A404C
        const val SURFACE_VARIANT = 0xFF2B303B
        const val SURFACE_DIM = 0xFF111319
        const val SURFACE_LOWEST = 0xFF111319
        const val SURFACE_CONTAINER = 0xFF262A34
        const val SURFACE_HIGH = 0xFF2B303B
        const val SURFACE_HIGHEST = 0xFF353A46
        const val ERROR = 0xFFCE8675
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
