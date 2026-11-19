package com.cookbookanything.app.feature.recipelist

import android.content.Context
import androidx.core.content.edit

/** Whether the cook dismissed the getting-started card. */
object GettingStartedPreferences {
    private const val PREFERENCES = "getting_started"
    private const val KEY_HIDDEN = "hidden"

    fun isHidden(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(KEY_HIDDEN, false)

    fun hide(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit { putBoolean(KEY_HIDDEN, true) }
    }
}
