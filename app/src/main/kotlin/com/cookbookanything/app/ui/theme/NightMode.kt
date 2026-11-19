package com.cookbookanything.app.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** The light/dark choice in Settings. The default follows the phone. */
object NightMode {
    private const val PREFERENCES = "appearance"
    private const val KEY_MODE = "night_mode"

    val options = listOf(
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES,
    )

    fun current(context: Context): Int =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    /** Called once at start so the first screen already has the chosen look. */
    fun apply(context: Context) = AppCompatDelegate.setDefaultNightMode(current(context))

    fun set(context: Context, mode: Int) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putInt(KEY_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
