package com.cookbookanything.core.domain.text

/**
 * Calls [call] with [models] from [start] on, and answers with the place of the model that worked
 * and what it said. A model that [unavailable] says cannot answer now (retired, overloaded, out of
 * quota) hands over to the next one, so one model's trouble does not stop the app; the last one's
 * refusal, and any other failure, is thrown.
 */
inline fun <T> firstAvailableModel(models: List<String>, start: Int, unavailable: (Exception) -> Boolean, call: (String) -> T): Pair<Int, T> {
    var index = start.coerceIn(0, models.lastIndex)
    while (true) {
        try {
            return index to call(models[index])
        } catch (e: Exception) {
            if (index == models.lastIndex || !unavailable(e)) throw e
            index++
        }
    }
}
