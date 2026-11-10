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
