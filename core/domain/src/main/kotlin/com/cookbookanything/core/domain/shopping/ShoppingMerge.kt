package com.cookbookanything.core.domain.shopping

import com.cookbookanything.core.domain.model.ShoppingItem
import com.cookbookanything.core.domain.search.searchKey

/** One line of the shopping list after the same ingredient from several recipes is folded together. */
data class MergedShoppingItem(
    /** The ingredient's name as it was first written. */
    val name: String,
    /** "200 g", "1 cup": one per recipe that gave an amount, in list order. */
    val amounts: List<String>,
    val ingredientIds: List<String>,
    val recipeTitles: List<String>,
    /** Ticked only when every recipe's copy is ticked. */
    val checked: Boolean,
) {
    val label: String get() = if (amounts.isEmpty()) name else amounts.joinToString(" + ") + " " + name
}

private const val AMOUNT = """\d+\s*[½¼¾⅓⅔⅛]|[½¼¾⅓⅔⅛]|\d+\s+\d+/\d+|\d+/\d+|\d+(?:[.,]\d+)?"""
private val leading = Regex("""^\s*($AMOUNT)(\s*[-–]\s*(?:$AMOUNT))?\s*""")
private val units = setOf(
    "g", "gr", "gram", "grams", "kg", "ml", "l", "dl", "cl", "cup", "cups", "tbsp", "tsp", "oz", "lb",
    "pcs", "pc", "piece", "pieces", "pinch", "can", "cans", "clove", "cloves", "slice", "slices", "bunch",
    "γρ", "γραμμαρια", "κιλο", "κιλα", "λιτρο", "λιτρα", "φλ", "φλιτζανι", "φλιτζανια", "κουταλια",
    "κουταλακι", "κουταλακια", "κουταλιες", "πρεζα", "τεμ", "τεμαχια", "τεμαχιο", "σκελιδα", "σκελιδες",
    "φετα", "φετες", "ματσακι", "κονσερβα", "κουτι", "φακελακι", "κσ", "κγ",
).map { it.searchKey() }.toSet()

/**
 * Folds lines that name the same ingredient: "200 g flour" from one recipe and "500 g flour" from
 * another become one line "200 g + 500 g flour", ticked when both are. Names are compared without
 * accents or capitals, so "Αλεύρι" and "αλευρι" meet. Order follows the first appearance.
 */
// shortcut: singular and plural ("egg", "eggs") stay apart, add a stemmer if that annoys
fun List<ShoppingItem>.merged(): List<MergedShoppingItem> =
    groupBy { it.nameKey() }.values.map { group ->
        MergedShoppingItem(
            name = group.first().split().second,
            amounts = group.mapNotNull { it.split().first },
            ingredientIds = group.map { it.ingredientId },
            recipeTitles = group.map { it.recipeTitle }.distinct(),
            checked = group.all { it.checked },
        )
    }

/** Lines to send to another app: every item still to buy, one per line. */
fun List<MergedShoppingItem>.toBuyText(): String =
    filterNot { it.checked }.joinToString("\n") { it.label }

private fun ShoppingItem.nameKey(): String = split().second.searchKey()

/** The leading amount with its unit ("200 g", or null when the line has no amount) and the rest. */
private fun ShoppingItem.split(): Pair<String?, String> {
    val match = leading.find(text) ?: return null to text.trim()
    val rest = text.substring(match.range.last + 1).trim()
    val firstWord = rest.substringBefore(' ')
    val unitKey = firstWord.trimEnd('.', ',').replace(".", "").searchKey()
    val hasUnit = rest.contains(' ') && (unitKey in units || (firstWord.endsWith(".") && firstWord.length <= 5))
    val amount = match.value.trim() + if (hasUnit) " $firstWord" else ""
    val name = if (hasUnit) rest.substringAfter(' ').trim() else rest
    return if (name.isEmpty()) null to text.trim() else amount to name
}
