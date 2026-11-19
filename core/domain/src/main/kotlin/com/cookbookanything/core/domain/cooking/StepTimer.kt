package com.cookbookanything.core.domain.cooking

private val duration = Regex(
    """(\d+(?:[.,]\d+)?)\s*(?:(?:-|–|to|εως|έως|ως)\s*\d+(?:[.,]\d+)?\s*)?(hours?|hrs?|h\b|ώρες|ώρα|ωρες|ωρα|minutes?|mins?|min\b|λεπτά|λεπτα|λεπ\.|λ\.|['′](?!\w))""",
    RegexOption.IGNORE_CASE,
)
private val joiner = Regex("""^\s*(?:and|και|&|,)?\s*$""", RegexOption.IGNORE_CASE)

/**
 * The first duration a step mentions, in minutes, or null: "bake for 20 min" and "ψήνουμε 20 λεπτά"
 * give 20, "1 hour 15 minutes" gives 75 and a range such as "20-25 minutes" gives its lower end, so
 * the cook checks early and adds time. Seconds are ignored.
 */
fun timerMinutes(stepText: String): Int? {
    val matches = duration.findAll(stepText).toList()
    val first = matches.firstOrNull() ?: return null
    var minutes = first.minutes()
    val second = matches.getOrNull(1)
    if (second != null && first.isHours() && !second.isHours() &&
        joiner.matches(stepText.substring(first.range.last + 1, second.range.first))
    ) {
        minutes += second.minutes()
    }
    return minutes.toInt().takeIf { it > 0 }
}

private fun MatchResult.isHours(): Boolean = groupValues[2].lowercase().let { it.startsWith("h") || it.startsWith("ω") || it.startsWith("ώ") }

private fun MatchResult.minutes(): Double {
    val value = groupValues[1].replace(',', '.').toDouble()
    return if (isHours()) value * 60 else value
}
