package com.cookbookanything.core.domain.scaling

import kotlin.math.roundToLong

private const val AMOUNT = """\d+\s*[½¼¾⅓⅔⅛]|[½¼¾⅓⅔⅛]|\d+\s+\d+/\d+|\d+/\d+|\d+(?:[.,]\d+)?"""
private val vulgarFractions = mapOf('½' to 0.5, '¼' to 0.25, '¾' to 0.75, '⅓' to 1.0 / 3, '⅔' to 2.0 / 3, '⅛' to 0.125)
private val leadingAmount = Regex("""^(\s*)($AMOUNT)""")
private val rangeEnd = Regex("""^(\s*[-–]\s*)($AMOUNT)""")

/**
 * Multiplies the amount an ingredient line starts with: "2 eggs" becomes "4 eggs", "1/2 cup" becomes
 * "1 cup", "½ φλ." becomes "1 φλ." and "2-3 cloves" becomes "4-6 cloves". The unit after the amount
 * ("g", "κ.σ.", "φλ.") is left as it is. Lines that do not start with a number are returned unchanged.
 */
// shortcut: amounts later in the line ("1 can (400 g)") are not scaled, parse the whole line if that matters
fun scaleIngredient(text: String, factor: Double): String {
    if (factor == 1.0) return text
    val first = leadingAmount.find(text) ?: return text
    val scaledFirst = scaleAmount(first.groupValues[2], factor) ?: return text
    val rest = text.substring(first.range.last + 1)
    val second = rangeEnd.find(rest)
    val scaledSecond = second?.let { scaleAmount(it.groupValues[2], factor) }
    return buildString {
        append(first.groupValues[1]).append(scaledFirst)
        if (second != null && scaledSecond != null) {
            append(second.groupValues[1]).append(scaledSecond).append(rest.substring(second.range.last + 1))
        } else {
            append(rest)
        }
    }
}

private fun scaleAmount(amount: String, factor: Double): String? {
    val fraction = amount.lastOrNull()?.let(vulgarFractions::get)
    val value = if (fraction != null) {
        amount.dropLast(1).trim().toDoubleOrNull().let { it ?: 0.0 } + fraction
    } else if ('/' in amount) {
        val parts = amount.trim().split(Regex("\\s+"))
        val (numerator, denominator) = parts.last().split('/').map(String::toDouble)
        if (denominator == 0.0) return null
        (if (parts.size == 2) parts[0].toDouble() else 0.0) + numerator / denominator
    } else {
        amount.replace(',', '.').toDouble()
    }
    val rounded = (value * factor * 100).roundToLong() / 100.0
    val whole = rounded.toLong()
    if (fraction != null) {
        // Written with a fraction glyph, so the answer keeps that look when it has a neat fraction.
        val glyph = vulgarFractions.entries.firstOrNull { ((rounded - whole) * 100).roundToLong() == (it.value * 100).roundToLong() }?.key
        if (glyph != null) return (if (whole == 0L) "" else whole.toString()) + glyph
    }
    val text = if (rounded == whole.toDouble()) whole.toString() else rounded.toString()
    return if (',' in amount) text.replace('.', ',') else text
}
