@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.cookbookanything.app.BuildConfig
import com.cookbookanything.app.R
import com.cookbookanything.app.ui.components.AppIcon
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.FreeTier
import java.time.LocalDate

@Composable
fun SettingsScreen(
    recipeCount: Int,
    unlocked: Boolean,
    onUnlock: () -> Unit,
    onManageCategories: () -> Unit,
    onSaveBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
) {
    // The user picks where the backup goes (phone storage, Drive, ...), so no storage permission is needed.
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) onSaveBackup(uri)
    }
    val restoreBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onRestoreBackup(uri)
    }

    Scaffold(
        topBar = { TopAppBar(colors = appBarColors(), title = { Text(stringResource(R.string.tab_settings)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(stringResource(R.string.language))
            // Changing the language recreates the activity, so reading it once per composition is enough.
            val currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
            LanguageOption(stringResource(R.string.language_system), tag = "", currentLanguage)
            // Language names stay in their own language so they can be found from any setting.
            LanguageOption("English", tag = "en", currentLanguage)
            LanguageOption("Ελληνικά", tag = "el", currentLanguage)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.plan))
            Text(
                text = if (unlocked) {
                    stringResource(R.string.plan_unlocked)
                } else {
                    stringResource(R.string.plan_free, recipeCount.coerceAtMost(FreeTier.RECIPE_LIMIT), FreeTier.RECIPE_LIMIT)
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (!unlocked) {
                Button(onClick = onUnlock, modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.paywall_title))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onManageCategories)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(R.drawable.ic_label, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
                Text(stringResource(R.string.categories), style = MaterialTheme.typography.bodyLarge)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.backup))
            Text(
                text = stringResource(R.string.backup_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Button(
                onClick = { saveBackup.launch("cookbook-backup-${LocalDate.now()}.zip") },
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
            ) {
                Text(stringResource(R.string.backup_save))
            }
            OutlinedButton(
                onClick = { restoreBackup.launch(arrayOf("*/*")) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.backup_restore))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun LanguageOption(label: String, tag: String, currentLanguage: String) {
    val select = {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = select)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = currentLanguage == tag, onClick = select)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
