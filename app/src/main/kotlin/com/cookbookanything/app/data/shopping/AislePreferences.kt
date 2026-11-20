package com.cookbookanything.app.data.shopping

import android.content.Context
import androidx.core.content.edit
import com.cookbookanything.core.domain.shopping.Aisle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The aisles the user moved products to, by product key; the dictionary's guess applies to the rest. */
@Singleton
class AislePreferences @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("aisles", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())
    val overrides: StateFlow<Map<String, Aisle>> = state.asStateFlow()

    fun set(productKey: String, aisle: Aisle) {
        preferences.edit { putString(productKey, aisle.name) }
        state.value = load()
    }

    private fun load(): Map<String, Aisle> = preferences.all.mapNotNull { (key, value) ->
        Aisle.entries.firstOrNull { it.name == value }?.let { key to it }
    }.toMap()
}
