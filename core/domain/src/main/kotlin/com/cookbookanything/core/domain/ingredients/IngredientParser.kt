package com.cookbookanything.core.domain.ingredients

import com.cookbookanything.core.domain.model.Step
import com.cookbookanything.core.domain.scaling.AMOUNT
import com.cookbookanything.core.domain.scaling.parseAmount
import com.cookbookanything.core.domain.search.searchKey

enum class UnitKind { WEIGHT, VOLUME, COUNT }

/**
 * A unit an ingredient line can carry. [factor] is grams for weights and millilitres for volumes,
 * so amounts can be added up and converted; counts (cloves, cans) have none. [metric] units are
 * left alone when a recipe is shown in metric; spoons count as metric because every kitchen has them.
 */
enum class MeasureUnit(val kind: UnitKind, val factor: Double, val metric: Boolean, val en: String, val el: String) {
    GRAM(UnitKind.WEIGHT, 1.0, true, "g", "γρ."),
    KILOGRAM(UnitKind.WEIGHT, 1000.0, true, "kg", "κιλό"),
    OUNCE(UnitKind.WEIGHT, 28.35, false, "oz", "oz"),
    POUND(UnitKind.WEIGHT, 453.6, false, "lb", "lb"),
    MILLILITRE(UnitKind.VOLUME, 1.0, true, "ml", "ml"),
    LITRE(UnitKind.VOLUME, 1000.0, true, "l", "λίτρο"),
    TEASPOON(UnitKind.VOLUME, 5.0, true, "tsp", "κ.γ."),
    TABLESPOON(UnitKind.VOLUME, 15.0, true, "tbsp", "κ.σ."),
    CUP(UnitKind.VOLUME, 240.0, false, "cup", "φλ."),
    FLUID_OUNCE(UnitKind.VOLUME, 29.57, false, "fl oz", "fl oz"),
    PINT(UnitKind.VOLUME, 473.0, false, "pint", "pint"),
    QUART(UnitKind.VOLUME, 946.0, false, "quart", "quart"),
    PINCH(UnitKind.COUNT, 0.0, true, "pinch", "πρέζα"),
    CLOVE(UnitKind.COUNT, 0.0, true, "clove", "σκελίδα"),
    CAN(UnitKind.COUNT, 0.0, true, "can", "κονσέρβα"),
    SLICE(UnitKind.COUNT, 0.0, true, "slice", "φέτα"),
    BUNCH(UnitKind.COUNT, 0.0, true, "bunch", "ματσάκι"),
    PIECE(UnitKind.COUNT, 0.0, true, "piece", "τεμ."),
    PACK(UnitKind.COUNT, 0.0, true, "pack", "πακέτο"),
    SPRIG(UnitKind.COUNT, 0.0, true, "sprig", "κλωνάρι"),
    LEAF(UnitKind.COUNT, 0.0, true, "leaf", "φύλλο"),
}

/**
 * An ingredient line taken apart: "1½ cups all-purpose flour, sifted" is amount 1.5, unit CUP,
 * product "all-purpose flour", note "sifted". What was not written is null; [product] is the whole
 * line when nothing else was found.
 */
data class ParsedIngredient(
    val amount: Double?,
    /** The far end of a range ("2-3 cloves"), else null. */
    val amountEnd: Double?,
    val unit: MeasureUnit?,
    /** The unit as it was written ("γρ.", "cups"), to show it back the same way. */
    val unitText: String?,
    val product: String,
    val note: String?,
    /** Where [product] starts in the line: everything before it is the measure. */
    val measureEnd: Int,
) {
    /** The product without accents, capitals, plurals and filler words, to tell the same ingredient in two recipes. */
    val productKey: String get() = productKey(product)
}

private val leadingMeasure = Regex("""^\s*($AMOUNT)(?:\s*(?:[-–]|to|έως|εως|ή)\s*($AMOUNT))?\s*""")
private val parenthetical = Regex("""\s*\([^)]*\)""")
private val whitespace = Regex("""\s+""")
private val notLetter = Regex("""[^\p{L}]+""")
private const val UNIT_WORDS = 4

private val unitKeys: Map<String, MeasureUnit> = buildMap {
    fun put(unit: MeasureUnit, vararg spellings: String) = spellings.forEach { put(unitKey(it), unit) }
    put(MeasureUnit.GRAM, "g", "gr", "grs", "gram", "grams", "gramm", "γρ", "γραμ", "γραμμαριο", "γραμμαρια", "γραμμ")
    put(MeasureUnit.KILOGRAM, "kg", "kgs", "kilo", "kilos", "kilogram", "kilograms", "κιλο", "κιλα", "κιλου")
    put(MeasureUnit.OUNCE, "oz", "ounce", "ounces")
    put(MeasureUnit.POUND, "lb", "lbs", "pound", "pounds")
    put(MeasureUnit.MILLILITRE, "ml", "mls", "millilitre", "millilitres", "milliliter", "milliliters")
    put(MeasureUnit.LITRE, "l", "lt", "ltr", "litre", "litres", "liter", "liters", "λιτρο", "λιτρα", "λτ")
    put(
        MeasureUnit.TEASPOON, "tsp", "tsps", "teaspoon", "teaspoons", "κγ", "κ γ", "κουταλακι", "κουταλακια", "κουταλακι γλυκου",
        "κουταλακια γλυκου", "κουταλακι του γλυκου", "κουταλακια του γλυκου", "κουτ γλυκου", "κουταλακι τσαγιου",
    )
    put(
        MeasureUnit.TABLESPOON, "tbsp", "tbsps", "tbs", "tbl", "tablespoon", "tablespoons", "κσ", "κ σ", "κουταλια", "κουταλιες",
        "κουταλι", "κουταλια σουπας", "κουταλιες σουπας", "κουταλι σουπας", "κουταλια της σουπας", "κουταλιες της σουπας", "κουτ σουπας",
    )
    put(
        MeasureUnit.CUP, "cup", "cups", "φλ", "φλιτζανι", "φλιτζανια", "φλ τσαγιου", "φλιτζανι τσαγιου", "φλιτζανια τσαγιου",
        "φλιτζανι του τσαγιου", "φλιτζανια του τσαγιου", "ποτηρι", "ποτηρια", "κουπα", "κουπες",
    )
    put(MeasureUnit.FLUID_OUNCE, "fl oz", "floz", "fluid ounce", "fluid ounces")
    put(MeasureUnit.PINT, "pint", "pints", "pt")
    put(MeasureUnit.QUART, "quart", "quarts", "qt")
    put(MeasureUnit.PINCH, "pinch", "pinches", "πρεζα", "πρεζες")
    put(MeasureUnit.CLOVE, "clove", "cloves", "σκελιδα", "σκελιδες")
    put(MeasureUnit.CAN, "can", "cans", "tin", "tins", "κονσερβα", "κονσερβες", "κουτι", "κουτια")
    put(MeasureUnit.SLICE, "slice", "slices", "φετα", "φετες")
    put(MeasureUnit.BUNCH, "bunch", "bunches", "ματσακι", "ματσακια", "ματσο", "δεματι")
    put(MeasureUnit.PIECE, "piece", "pieces", "pc", "pcs", "τεμ", "τεμαχιο", "τεμαχια", "κομματι", "κομματια")
    put(MeasureUnit.PACK, "pack", "packs", "packet", "packets", "package", "bag", "bags", "πακετο", "πακετα", "φακελακι", "φακελακια", "σακουλακι", "σακουλακια")
    put(MeasureUnit.SPRIG, "sprig", "sprigs", "κλωναρι", "κλωναρια", "κλαδακι", "κλαδακια")
    put(MeasureUnit.LEAF, "leaf", "leaves", "φυλλο", "φυλλα")
}

// "of" and "από" between the measure and the product carry nothing.
private val fillers = setOf("of", "απο")

// Words that say how much or what shape, not what to buy.
private val stopStems = setOf(
    "fresh", "large", "small", "medium", "big", "ripe", "of", "the", "a", "an", "some", "chopped", "sliced", "diced", "minced",
    "φρεσκ", "μεγαλ", "μικρ", "μεσαι", "ωριμ", "λιγ", "ψιλοκομμεν", "κομμεν", "τριμμεν",
)

private fun unitKey(text: String): String = text.searchKey().replace(".", " ").replace(whitespace, " ").trim()

/** Takes an ingredient line apart; see [ParsedIngredient]. */
fun parseIngredient(text: String): ParsedIngredient {
    val measure = leadingMeasure.find(text)
    val amount = measure?.let { parseAmount(it.groupValues[1]) }
    val amountEnd = measure?.groupValues?.get(2)?.ifEmpty { null }?.let(::parseAmount)
    var position = if (amount != null) measure!!.range.last + 1 else 0
    var unit: MeasureUnit? = null
    var unitText: String? = null
    if (amount != null) {
        val rest = text.substring(position)
        val words = rest.trim().split(whitespace)
        for (count in minOf(UNIT_WORDS, words.size) downTo 1) {
            val candidate = words.take(count).joinToString(" ")
            val found = unitKeys[unitKey(candidate)] ?: continue
            // "1 φέτα" is one slice of something unsaid: the word is the product, not a unit.
            if (count == words.size) break
            unit = found
            unitText = candidate
            position += rest.indexOf(candidate) + candidate.length
            break
        }
        val afterUnit = text.substring(position)
        val filler = afterUnit.trim().split(whitespace).firstOrNull()
        if (filler != null && filler.searchKey() in fillers && afterUnit.trim().length > filler.length) {
            position += afterUnit.indexOf(filler) + filler.length
        }
        while (position < text.length && text[position].isWhitespace()) position++
    }
    val rest = text.substring(position).trim()
    val notes = parenthetical.findAll(rest).map { it.value.trim().removeSurrounding("(", ")").trim() }.toMutableList()
    val withoutParentheses = rest.replace(parenthetical, " ").replace(whitespace, " ").trim()
    val product = withoutParentheses.substringBefore(',').trim().trimEnd('.', ';', ':')
    withoutParentheses.substringAfter(',', "").trim().ifEmpty { null }?.let(notes::add)
    return ParsedIngredient(
        amount = amount,
        amountEnd = amountEnd,
        unit = unit,
        unitText = unitText,
        product = product.ifEmpty { rest },
        note = notes.joinToString(", ").ifEmpty { null },
        measureEnd = position,
    )
}

/**
 * "For the sauce:" or "Για τη σάλτσα:": a short line ending in a colon that names a part of the
 * recipe, inside the ingredients or the steps. Shown as a heading, never counted or bought.
 */
fun isHeading(text: String): Boolean {
    val line = text.trim()
    return line.endsWith(":") && line.length <= HEADING_LENGTH && line.split(whitespace).size <= HEADING_WORDS && parseIngredient(line).amount == null
}

private const val HEADING_LENGTH = 50
private const val HEADING_WORDS = 7

/** The number each step is shown with, counting past the headings between them; null for a heading. */
fun List<Step>.stepNumbers(): List<Int?> {
    var number = 0
    return map { if (isHeading(it.text)) null else ++number }
}

/** See [ParsedIngredient.productKey]. */
fun productKey(product: String): String =
    product.searchKey().split(notLetter).map(::stem).filter { it.isNotEmpty() && it !in stopStems }.joinToString(" ")

private val greekEndings = listOf("ους", "ησ", "ασ", "οσ", "εσ", "ια", "ων", "ου", "οι", "α", "η", "ι", "ο").map { it.replace('ς', 'σ') }

/**
 * Cuts the ending that changes with number and case, so "αυγά" and "αυγό" meet at "αυγ" and
 * "tomatoes" and "tomato" at "tomato". Short words are left whole.
 */
fun stem(word: String): String {
    if (word.length < 4) return word
    if (word.any { it in 'α'..'ω' }) {
        val ending = greekEndings.firstOrNull { word.endsWith(it) && word.length - it.length >= 3 } ?: return word
        return word.dropLast(ending.length)
    }
    return when {
        word.endsWith("ies") -> word.dropLast(3) + "y"
        word.endsWith("oes") || word.endsWith("ches") || word.endsWith("shes") || word.endsWith("sses") || word.endsWith("xes") -> word.dropLast(2)
        word.endsWith("s") && !word.endsWith("ss") -> word.dropLast(1)
        else -> word
    }
}
