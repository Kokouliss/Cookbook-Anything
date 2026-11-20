package com.cookbookanything.core.domain.search

import com.cookbookanything.core.domain.model.RecipeSummary
import java.text.Normalizer

private val accents = Regex("\\p{M}+")
private val whitespace = Regex("\\s+")

/** Lowercased and without accents, so "ντοματα" finds "Ντομάτα" and "creme" finds "Crème". */
fun String.searchKey(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(accents, "").replace('ς', 'σ')

/** True when every word of [query] appears in the recipe's title or ingredients. */
fun RecipeSummary.matches(query: String): Boolean {
    val text = "$title\n$ingredients".searchKey()
    return query.searchKey().split(whitespace).all { it in text }
}

/** What the list is narrowed to besides the search text and the category. Every set field must hold. */
data class RecipeFilter(
    /** The recipe must carry every one of these. */
    val tags: Set<String> = emptySet(),
    /** Prep, cooking and waiting time together at most this. */
    val maxMinutes: Int? = null,
    /** True for recipes cooked at least once, false for the ones never cooked. */
    val cooked: Boolean? = null,
    val minRating: Int? = null,
    /** "What's in my pantry": ingredients the cook has, separated by commas or lines; any one of them in the recipe counts. */
    val pantry: String = "",
) {
    val isEmpty: Boolean get() = this == RecipeFilter()
}

private val pantrySeparators = Regex("""[,\n;]+""")

fun RecipeSummary.matches(filter: RecipeFilter): Boolean {
    val recipeTags = tags.map { it.searchKey() }
    if (filter.tags.any { it.searchKey() !in recipeTags }) return false
    if (filter.maxMinutes != null && (totalMinutes == 0 || totalMinutes > filter.maxMinutes)) return false
    if (filter.cooked != null && (timesCooked > 0) != filter.cooked) return false
    if (filter.minRating != null && (rating ?: 0) < filter.minRating) return false
    val pantry = filter.pantry.split(pantrySeparators).map { it.trim().searchKey() }.filter { it.isNotEmpty() }
    if (pantry.isNotEmpty()) {
        val lines = ingredients.searchKey()
        if (pantry.none { it in lines }) return false
    }
    return true
}
