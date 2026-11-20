package com.cookbookanything.app.data.text

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** How imported recipes are treated; set in Settings. */
@Singleton
class ImportPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("imports", Context.MODE_PRIVATE)
    private val _translate = MutableStateFlow(preferences.getBoolean(KEY_TRANSLATE, false))

    /** Translate pasted, linked and photographed recipes into the phone's language. */
    val translate: StateFlow<Boolean> = _translate

    fun setTranslate(value: Boolean) {
        preferences.edit().putBoolean(KEY_TRANSLATE, value).apply()
        _translate.value = value
    }

    private companion object {
        const val KEY_TRANSLATE = "translate"
    }
}
