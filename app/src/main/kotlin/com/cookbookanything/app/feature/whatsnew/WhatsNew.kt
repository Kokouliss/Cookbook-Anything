package com.cookbookanything.app.feature.whatsnew

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.cookbookanything.app.BuildConfig
import com.cookbookanything.app.R

/**
 * Remembers which version the user last saw, so the "What's new" list shows once after an update.
 * A fresh install only records the version: there is nothing new to a first-time user.
 */
object WhatsNew {
    private const val PREFERENCES = "whats_new"
    private const val KEY_SEEN = "seenVersionCode"

    fun isUnseen(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val seen = preferences.getInt(KEY_SEEN, 0)
        if (seen == 0) {
            markSeen(context)
            return false
        }
        return seen < BuildConfig.VERSION_CODE
    }

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit { putInt(KEY_SEEN, BuildConfig.VERSION_CODE) }
    }
}

/** The changes of this release, one line each, from the string array that each release updates. */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whats_new_title, BuildConfig.VERSION_NAME)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                stringArrayResource(R.array.whats_new_items).forEach { Text("•  $it", modifier = Modifier.padding(vertical = 2.dp)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
