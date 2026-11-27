package com.cookbookanything.core.domain.text

/**
 * Calls [call] with [models] from [start] on, and answers with the place of the model that worked
 * and what it said. A model that [gone] says no longer exists hands over to the next one, so a model
 * the provider retires does not stop the app; the last one's refusal, and any other failure, is thrown.
 */
inline fun <T> firstAvailableModel(models: List<String>, start: Int, gone: (Exception) -> Boolean, call: (String) -> T): Pair<Int, T> {
    var index = start.coerceIn(0, models.lastIndex)
    while (true) {
        try {
            return index to call(models[index])
        } catch (e: Exception) {
            if (index == models.lastIndex || !gone(e)) throw e
            index++
        }
    }
}
