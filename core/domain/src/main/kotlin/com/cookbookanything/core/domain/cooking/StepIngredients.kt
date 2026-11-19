package com.cookbookanything.core.domain.cooking

import com.cookbookanything.core.domain.search.searchKey

private val word = Regex("""\p{L}{4,}""")
private const val STEM = 5

/**
 * Which ingredients a step talks about: the indices of [ingredients] whose name shares a word with
 * [step]. Words are compared on their first five letters without accents, so "κρεμμύδι" meets
 * "κρεμμύδια" and "Tomatoes" meets "tomato". Amounts and short words do not count.
 */
fun ingredientsForStep(step: String, ingredients: List<String>): List<Int> {
    val stepStems = word.findAll(step.searchKey()).map { it.value.take(STEM) }.toSet()
    if (stepStems.isEmpty()) return emptyList()
    return ingredients.indices.filter { index ->
        word.findAll(ingredients[index].searchKey()).any { it.value.take(STEM) in stepStems }
    }
}
