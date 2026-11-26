package com.cookbookanything.app.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * Deleted recipes wait here, hidden from the list, while the Undo message is on screen. Nothing is
 * removed from the database until the wait ends, so Undo brings the recipe back untouched, and a
 * recipe whose wait is cut short by the app closing is simply still there.
 */
@Singleton
class RecipeTrash @Inject constructor() {
    data class Binned(val recipeId: String, val title: String)

    private val _waiting = MutableStateFlow<Set<String>>(emptySet())
    val waiting: StateFlow<Set<String>> = _waiting

    private val binnedChannel = Channel<Binned>(Channel.BUFFERED)
    val binned = binnedChannel.receiveAsFlow()

    fun bin(recipeId: String, title: String) {
        _waiting.update { it + recipeId }
        binnedChannel.trySend(Binned(recipeId, title))
    }

    /** Undo, or the delete is done: either way the recipe is no longer waiting. */
    fun release(recipeId: String) = _waiting.update { it - recipeId }
}
