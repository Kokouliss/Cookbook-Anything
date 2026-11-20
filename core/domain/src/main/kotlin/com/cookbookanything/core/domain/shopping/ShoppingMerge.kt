package com.cookbookanything.core.domain.shopping

import com.cookbookanything.core.domain.ingredients.MeasureUnit
import com.cookbookanything.core.domain.ingredients.ParsedIngredient
import com.cookbookanything.core.domain.ingredients.UnitKind
import com.cookbookanything.core.domain.ingredients.isHeading
import com.cookbookanything.core.domain.ingredients.parseIngredient
import com.cookbookanything.core.domain.model.ShoppingItem
import com.cookbookanything.core.domain.scaling.formatAmount

/** One line of the shopping list after the same ingredient from several recipes is folded together. */
data class MergedShoppingItem(
    /** The ingredient's name as it was first written. */
    val name: String,
    /** "700 g", "1 cup": amounts in the same unit are added up, the rest listed in order. */
    val amounts: List<String>,
    val ingredientIds: List<String>,
    val recipeTitles: List<String>,
    /** Ticked only when every recipe's copy is ticked. */
    val checked: Boolean,
    /** The name without accents, capitals and plurals: the key to remember a chosen aisle under. */
    val productKey: String,
    val aisle: Aisle,
) {
    val label: String get() = if (amounts.isEmpty()) name else amounts.joinToString(" + ") + " " + name
}

/**
 * Folds lines that name the same ingredient: "200 g flour" from one recipe and "500 g flour" from
 * another become one line "700 g flour", ticked when both are. Names are compared without accents,
 * capitals or plurals, so "Αλεύρι" and "αλευρι" meet, and so do "egg" and "eggs". Headings ("For the
 * sauce:") are left out. Order follows the first appearance; [aisleOf] may override the guessed aisle.
 */
fun List<ShoppingItem>.merged(aisleOf: (productKey: String) -> Aisle? = { null }): List<MergedShoppingItem> =
    filterNot { isHeading(it.text) }
        .map { it to parseIngredient(it.text) }
        .groupBy { (_, parsed) -> parsed.productKey.ifEmpty { parsed.product.lowercase() } }
        .values.map { group ->
            val key = group.first().second.productKey
            MergedShoppingItem(
                name = group.first().second.product,
                amounts = sumAmounts(group.map { it.second }),
                ingredientIds = group.map { it.first.ingredientId },
                recipeTitles = group.map { it.first.recipeTitle }.distinct(),
                checked = group.all { it.first.checked },
                productKey = key,
                aisle = aisleOf(key) ?: aisleFor(group.first().second.product),
            )
        }

/** Lines to send to another app: every item still to buy, one per line. */
fun List<MergedShoppingItem>.toBuyText(): String =
    filterNot { it.checked }.joinToString("\n") { it.label }

// Amounts in the same unit (or both bare) are added; grams and kilograms meet in the larger of the
// two. Different units stay side by side: "2 κ.σ. + ½ φλ.".
private fun sumAmounts(parsed: List<ParsedIngredient>): List<String> {
    val withAmount = parsed.filter { it.amount != null }
    val groups = withAmount.groupBy { it.unit?.let { unit -> if (unit.kind == UnitKind.COUNT) unit else unit.kind } }
    return groups.values.map { group ->
        val first = group.first()
        if (group.size == 1) return@map first.unitText?.let { "${first.formatted()} $it" } ?: first.formatted()
        val unit = first.unit
        if (unit == null) return@map formatAmount(group.sumOf { it.amount!! })
        if (unit.kind == UnitKind.COUNT || group.all { it.unit == unit }) {
            return@map "${formatAmount(group.sumOf { it.amount!! }, glyphs = unit.likesFractions)} ${first.unitText}"
        }
        if (group.any { it.unit!!.metric != unit.metric } || unit !in metricSums) return@map group.joinToString(" + ") { "${it.formatted()} ${it.unitText}" }
        val largest = group.maxBy { it.unit!!.factor }
        "${formatAmount(group.sumOf { it.amount!! * it.unit!!.factor } / largest.unit!!.factor, glyphs = false)} ${largest.unitText}"
    }
}

private val metricSums = setOf(MeasureUnit.GRAM, MeasureUnit.KILOGRAM, MeasureUnit.MILLILITRE, MeasureUnit.LITRE)

// "1½ cups" reads right, "1½ kg" does not.
private val MeasureUnit?.likesFractions: Boolean get() = this == null || kind == UnitKind.COUNT || this in setOf(MeasureUnit.TEASPOON, MeasureUnit.TABLESPOON, MeasureUnit.CUP)

private fun ParsedIngredient.formatted(): String = formatAmount(amount!!, glyphs = unit.likesFractions)
