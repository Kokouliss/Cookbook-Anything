package com.cookbookanything.core.domain.text

import com.cookbookanything.core.domain.ingredients.isHeading
import com.cookbookanything.core.domain.search.searchKey
import java.util.Locale

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
    /** The sentences that came before the recipe, as a caption opens: kept as the description. */
    val description: String? = null,
    /** A web page the text points to for the full recipe, when it names one that is not a social network. */
    val link: String? = null,
    /** The post's video file, to listen to when the caption holds no recipe. */
    val videoUrl: String? = null,
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
private val onlyHashtags = Regex("""^(?:[#@]\S+\s*)+$""")
private val url = Regex("""https?://\S+""")
private val socialHost = Regex("""^https?://(?:www\.)?(?:[a-z0-9-]+\.)?(?:instagram\.com|tiktok\.com|facebook\.com|fb\.com|fb\.watch|youtube\.com|youtu\.be|twitter\.com|x\.com|pinterest\.\w+|linktr\.ee|threads\.net)""", RegexOption.IGNORE_CASE)
private val recipePrefix = Regex("""^(?:recipe|συνταγη)\s*[:\-–]\s*""", RegexOption.IGNORE_CASE)
private val greekLetters = Regex("""[α-ωΑ-Ωά-ώΆ-Ώ]""")
private val latinLetters = Regex("""[a-zA-Z]""")
private const val SHORT_LINE = 40
private const val TITLE_LENGTH = 60
private const val TITLE_WORDS = 8
private const val POINTER_LINE = 60
private val servingsLine = Regex(
    """(?:^|\b)(?:(\d{1,2})\s*(?:servings?|portions?|people|μεριδεσ|ατομα)|(?:serves|servings?|yield|μεριδεσ|για)\D{0,3}(\d{1,2}))(?:\b|$)""",
)

/**
 * Splits pasted or shared text into a recipe. "Ingredients"/"Υλικά" and "Steps"/"Εκτέλεση" lines
 * switch what the following lines are; without such headers, lines that start with an amount or are
 * short become ingredients and the rest become steps. A short line right after an ingredient
 * ("salt" under "150 g cream") is an ingredient too. Bullets, step numbers and hashtag lines are dropped.
 *
 * Captions from social networks get extra care: the first short line is the title, the sentences
 * before the ingredients are the description, a link to a blog is kept as [ParsedRecipe.link], and
 * when the same list appears in Greek and in English only the one in [language] is kept.
 */
fun parseRecipeText(text: String, language: String = Locale.getDefault().language): ParsedRecipe {
    var link: String? = null
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { raw ->
        val found = url.find(raw) ?: return@mapNotNull raw
        if (link == null && !socialHost.containsMatchIn(found.value)) link = found.value.trimEnd('.', ',', ')')
        // "Full recipe: https://..." points elsewhere; a long sentence that happens to hold a link stays.
        val rest = raw.replace(url, "").trim()
        if (rest.length <= POINTER_LINE) null else rest
    }
    if (lines.isEmpty()) return ParsedRecipe(link = link)

    var title: String? = null
    var servings: Int? = null
    val description = mutableListOf<String>()
    val ingredients = mutableListOf<String>()
    val steps = mutableListOf<String>()
    // null until a header says which list we are in
    var mode: Mode? = null
    var lastWasIngredient = false

    lines.forEach { raw ->
        val key = raw.trim { !it.isLetterOrDigit() }.searchKey()
        when {
            key in INGREDIENT_HEADERS -> mode = Mode.INGREDIENTS
            key in STEP_HEADERS -> mode = Mode.STEPS
            else -> {
                val found = servingsLine.find(key)
                if (found != null && servings == null && raw.length <= 40) {
                    servings = (found.groupValues[1].ifEmpty { found.groupValues[2] }).toIntOrNull()
                    return@forEach
                }
                val line = raw.replace(bullet, "").trim()
                if (line.isEmpty() || onlyHashtags.matches(line) || line.none { it.isLetterOrDigit() }) return@forEach
                val looksLikeStep = stepNumber.containsMatchIn(raw) || line.length > 60 || line.count { it == '.' } > 1
                val continuesIngredients = lastWasIngredient && line.length <= SHORT_LINE && !line.endsWith(".") && !stepNumber.containsMatchIn(raw)
                val beforeRecipe = mode == null && ingredients.isEmpty() && steps.isEmpty()
                lastWasIngredient = false
                when {
                    isHeading(line) -> {
                        if (mode == Mode.STEPS || (mode == null && steps.isNotEmpty())) steps += line else { ingredients += line; lastWasIngredient = true }
                    }
                    beforeRecipe && title == null && looksLikeTitle(raw, line) -> title = cleanTitle(line)
                    mode == Mode.INGREDIENTS -> ingredients += line
                    mode == Mode.STEPS -> steps += line
                    amountLed.containsMatchIn(raw) && !stepNumber.containsMatchIn(raw) -> { ingredients += line; lastWasIngredient = true }
                    continuesIngredients -> { ingredients += line; lastWasIngredient = true }
                    beforeRecipe && !stepNumber.containsMatchIn(raw) -> description += line
                    looksLikeStep || steps.isNotEmpty() || line.endsWith(".") -> steps += line
                    else -> { ingredients += line; lastWasIngredient = true }
                }
            }
        }
    }
    // Only prose was pasted: it is the method, not a description of nothing.
    if (ingredients.isEmpty() && steps.isEmpty() && description.isNotEmpty()) {
        steps += description
        description.clear()
    }
    val greek = language == "el"
    return ParsedRecipe(
        title = title,
        servings = servings,
        ingredients = oneLanguage(ingredients, greek),
        steps = oneLanguage(steps, greek),
        description = description.joinToString("\n").ifEmpty { null },
        link = link,
    )
}

private fun looksLikeTitle(raw: String, line: String): Boolean =
    line.length <= TITLE_LENGTH && line.split(Regex("""\s+""")).size <= TITLE_WORDS && !amountLed.containsMatchIn(raw) &&
        !stepNumber.containsMatchIn(raw) && !line.trimEnd('.', '!').contains(". ") && !line.endsWith(".")

/** Drops a "Recipe:" prefix and the emoji or punctuation a caption decorates its title with. */
private fun cleanTitle(line: String): String =
    line.replace(recipePrefix, "").trim { !it.isLetterOrDigit() && it != ')' && it != '"' && it != '»' }.ifEmpty { line }

/**
 * A caption that gives the list twice, once in Greek and once in English, has two runs of lines in
 * different scripts. When both runs are long enough to be a list, keep the one in the reader's language.
 */
internal fun oneLanguage(lines: List<String>, greek: Boolean): List<String> {
    val runs = mutableListOf<Pair<Boolean, MutableList<String>>>()
    for (line in lines) {
        val greekLine = when {
            greekLetters.containsMatchIn(line) && !latinLetters.containsMatchIn(line) -> true
            latinLetters.containsMatchIn(line) && !greekLetters.containsMatchIn(line) -> false
            greekLetters.findAll(line).count() > latinLetters.findAll(line).count() -> true
            latinLetters.containsMatchIn(line) -> false
            else -> runs.lastOrNull()?.first
        }
        if (greekLine == null || runs.lastOrNull()?.first == greekLine) runs.lastOrNull()?.second?.add(line) ?: runs.add((greekLine ?: greek) to mutableListOf(line))
        else runs.add(greekLine to mutableListOf(line))
    }
    val wanted = runs.filter { it.first == greek && it.second.size >= 2 }
    val other = runs.filter { it.first != greek && it.second.size >= 2 }
    if (wanted.isEmpty() || other.isEmpty()) return lines
    return runs.filter { it.first == greek }.flatMap { it.second }
}

private enum class Mode { INGREDIENTS, STEPS }
