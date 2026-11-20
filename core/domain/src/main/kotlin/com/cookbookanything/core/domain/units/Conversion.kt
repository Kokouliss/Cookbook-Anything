package com.cookbookanything.core.domain.units

import com.cookbookanything.core.domain.ingredients.MeasureUnit
import com.cookbookanything.core.domain.ingredients.UnitKind
import com.cookbookanything.core.domain.ingredients.parseIngredient
import com.cookbookanything.core.domain.scaling.formatAmount
import kotlin.math.roundToInt

/** How a recipe's amounts are shown: as written, or converted to grams and millilitres, or to cups and ounces. */
enum class MeasurementSystem { ORIGINAL, METRIC, IMPERIAL }

private val greek = Regex("""[α-ωΑ-Ω]""")
private val temperature = Regex("""(\d{2,3})\s*(?:°|º|˚|degrees?\s*|βαθμ(?:ους|ουσ|οι)?\s*)\s*(F|C)(?![\p{L}])""")

// Spoons are the same in both systems, so a recipe measured only in spoons has nothing to convert.
private val spoons = setOf(MeasureUnit.TEASPOON, MeasureUnit.TABLESPOON)

/** True when any line carries a cup, ounce, gram or millilitre amount, or a step names a temperature with its scale. */
fun hasConvertibleUnits(ingredients: List<String>, steps: List<String>): Boolean =
    ingredients.any { parseIngredient(it).unit?.let { unit -> unit.kind != UnitKind.COUNT && unit !in spoons } == true } ||
        steps.any { temperature.containsMatchIn(it) }

/**
 * "1 cup flour" becomes "240 ml flour" in metric; "250 g flour" becomes "8.8 oz flour" in imperial.
 * Spoons stay as they are in both systems, counts ("2 eggs") are never touched, and the unit keeps
 * its language: "500 γρ." becomes "1.1 lb" and "2 cups" becomes "480 ml".
 */
fun convertIngredient(text: String, system: MeasurementSystem): String {
    if (system == MeasurementSystem.ORIGINAL) return text
    val parsed = parseIngredient(text)
    val unit = parsed.unit ?: return text
    val amount = parsed.amount ?: return text
    if (unit.kind == UnitKind.COUNT || unit.metric == (system == MeasurementSystem.METRIC)) return text
    val useGreek = greek.containsMatchIn(parsed.unitText.orEmpty())
    val start = convert(amount * unit.factor, unit.kind, system, useGreek)
    val end = parsed.amountEnd?.let { convert(it * unit.factor, unit.kind, system, useGreek) }
    val measure = if (end != null && end.second == start.second) "${start.first}-${end.first} ${start.second}" else "${start.first} ${start.second}"
    return measure + " " + text.substring(parsed.measureEnd)
}

/** "350°F" becomes "180°C" in metric and "180°C" becomes "350°F" in imperial, wherever a step names one. */
fun convertTemperatures(text: String, system: MeasurementSystem): String {
    if (system == MeasurementSystem.ORIGINAL) return text
    return temperature.replace(text) { match ->
        val degrees = match.groupValues[1].toInt()
        when {
            // Oven dials: 350°F is 180°C, 180°C is 350°F.
            system == MeasurementSystem.METRIC && match.groupValues[2] == "F" -> "${roundTo(((degrees - 32) * 5.0 / 9), 10)}°C"
            system == MeasurementSystem.IMPERIAL && match.groupValues[2] == "C" -> "${roundTo(degrees * 9.0 / 5 + 32, 25)}°F"
            else -> match.value
        }
    }
}

private fun roundTo(value: Double, step: Int): Int = (value / step).roundToInt() * step

/** [base] is grams or millilitres; returns the amount and the unit to print. */
private fun convert(base: Double, kind: UnitKind, system: MeasurementSystem, greek: Boolean): Pair<String, String> {
    fun label(unit: MeasureUnit, amount: Double): String {
        val name = if (greek) unit.el else unit.en
        return if (!greek && amount > 1 && unit in setOf(MeasureUnit.CUP, MeasureUnit.PINT, MeasureUnit.QUART)) name + "s" else name
    }
    return when {
        system == MeasurementSystem.METRIC && kind == UnitKind.WEIGHT ->
            if (base >= 1000) formatAmount(roundTo(base, 50) / 1000.0, glyphs = false) to label(MeasureUnit.KILOGRAM, 0.0)
            else formatAmount(roundTo(base, if (base >= 50) 5 else 1).toDouble(), glyphs = false) to label(MeasureUnit.GRAM, 0.0)
        system == MeasurementSystem.METRIC ->
            if (base >= 1000) formatAmount(roundTo(base, 50) / 1000.0, glyphs = false) to label(MeasureUnit.LITRE, 0.0)
            else formatAmount(roundTo(base, if (base >= 50) 5 else 1).toDouble(), glyphs = false) to label(MeasureUnit.MILLILITRE, 0.0)
        kind == UnitKind.WEIGHT ->
            if (base >= MeasureUnit.POUND.factor) tenths(base / MeasureUnit.POUND.factor) to label(MeasureUnit.POUND, 0.0)
            else tenths(base / MeasureUnit.OUNCE.factor) to label(MeasureUnit.OUNCE, 0.0)
        else -> {
            // Small volumes read better as spoons; the rest as cups to the nearest quarter.
            val cups = (base / MeasureUnit.CUP.factor * 4).roundToInt() / 4.0
            when {
                base < 12 -> formatAmount(base / MeasureUnit.TEASPOON.factor) to label(MeasureUnit.TEASPOON, 0.0)
                base < 50 -> formatAmount(base / MeasureUnit.TABLESPOON.factor) to label(MeasureUnit.TABLESPOON, 0.0)
                else -> formatAmount(maxOf(cups, 0.25)) to label(MeasureUnit.CUP, cups)
            }
        }
    }
}

private fun tenths(value: Double): String = formatAmount((value * 10).roundToInt() / 10.0, glyphs = false)
