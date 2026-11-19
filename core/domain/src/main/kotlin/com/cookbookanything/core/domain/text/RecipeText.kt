package com.cookbookanything.core.domain.text

import com.cookbookanything.core.domain.search.searchKey

/** A recipe read out of plain text. Anything the text did not make clear is empty or null. */
data class ParsedRecipe(
    val title: String? = null,
    val servings: Int? = null,
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val prepMinutes: Int? = null,
    val cookMinutes: Int? = null,
    /** The picture of the dish on a web page, to download as the recipe photo. */
    val imageUrl: String? = null,
) {
    val isEmpty: Boolean get() = title == null && ingredients.isEmpty() && steps.isEmpty()
}

// Compared through searchKey(), which also turns a final ς into σ.
internal val INGREDIENT_HEADERS = setOf("ingredients", "ingredient", "υλικά", "συστατικά", "τα υλικά").map { it.searchKey() }
internal val STEP_HEADERS = setOf(
    "steps", "step", "method", "directions", "instructions", "preparation", "procedure",
    "εκτέλεση", "διαδικασία", "οδηγίες", "βήματα", "παρασκευή", "μέθοδος", "προετοιμασία",
).map { it.searchKey() }
private val bullet = Regex("""^\s*(?:[-–—•·*✓✔►▪]|\d{1,2}\s*[.)]|(?:step|βημα)\s*\d{1,2}\s*[.:)]?)\s*""", RegexOption.IGNORE_CASE)
private val stepNumber = Regex("""^\s*(?:\d{1,2}\s*[.)]|(?:step|βημα)\s*\d{1,2})""", RegexOption.IGNORE_CASE)
private val amountLed = Regex("""^\s*(?:\d|[½¼¾⅓⅔⅛])""")
private val onlyHashtags = Regex("""^(?:#\S+\s*)+$""")
private const val SHORT_LINE = 40
private val servingsLine = Regex(
    """(?:^|\b)(?:(\d{1,2})\s*(?:servings?|portions?|people|μεριδεσ|ατομα)|(?:serves|servings?|yield|μεριδεσ|για)\D{0,3}(\d{1,2}))(?:\b|$)""",
)

/**
 * Splits pasted or shared text into a recipe. "Ingredients"/"Υλικά" and "Steps"/"Εκτέλεση" lines
 * switch what the following lines are; without such headers, lines that start with an amount or are
 * short become ingredients and the rest become steps. A short line right after an ingredient
 * ("salt" under "150 g cream") is an ingredient too. Bullets, step numbers and hashtag lines are dropped.
 */
fun parseRecipeText(text: String): ParsedRecipe {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.isEmpty()) return ParsedRecipe()

    var title: String? = null
    var servings: Int? = null
    val ingredients = mutableListOf<String>()
    val steps = mutableListOf<String>()
    // null until a header says which list we are in
    var mode: Mode? = null
    var lastWasIngredient = false

    lines.forEachIndexed { index, raw ->
        val key = raw.removeSuffix(":").trim().searchKey()
        when {
            key in INGREDIENT_HEADERS -> mode = Mode.INGREDIENTS
            key in STEP_HEADERS -> mode = Mode.STEPS
            else -> {
                val found = servingsLine.find(key)
                if (found != null && servings == null && raw.length <= 40) {
                    servings = (found.groupValues[1].ifEmpty { found.groupValues[2] }).toIntOrNull()
                    return@forEachIndexed
                }
                val line = raw.replace(bullet, "").trim()
                if (line.isEmpty() || onlyHashtags.matches(line)) return@forEachIndexed
                val looksLikeStep = stepNumber.containsMatchIn(raw) || line.length > 60 || line.count { it == '.' } > 1
                val continuesIngredients = lastWasIngredient && line.length <= SHORT_LINE && !line.endsWith(".") && !stepNumber.containsMatchIn(raw)
                lastWasIngredient = false
                when {
                    index == 0 && title == null && mode == null && !amountLed.containsMatchIn(raw) &&
                        !looksLikeStep && !raw.endsWith(".") -> title = line
                    mode == Mode.INGREDIENTS -> ingredients += line
                    mode == Mode.STEPS -> steps += line
                    amountLed.containsMatchIn(raw) && !stepNumber.containsMatchIn(raw) -> { ingredients += line; lastWasIngredient = true }
                    continuesIngredients -> { ingredients += line; lastWasIngredient = true }
                    looksLikeStep || steps.isNotEmpty() -> steps += line
                    else -> { ingredients += line; lastWasIngredient = true }
                }
            }
        }
    }
    return ParsedRecipe(title = title, servings = servings, ingredients = ingredients, steps = steps)
}

private enum class Mode { INGREDIENTS, STEPS }
