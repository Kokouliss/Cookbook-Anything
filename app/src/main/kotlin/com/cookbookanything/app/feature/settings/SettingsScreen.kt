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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.cookbookanything.app.BuildConfig
import com.cookbookanything.app.R
import com.cookbookanything.app.data.account.AccountState
import com.cookbookanything.app.data.share.BackupStatus
import com.cookbookanything.app.ui.components.AnyKindWordmark
import com.cookbookanything.app.ui.components.AppIcon
import com.cookbookanything.app.ui.components.currentLocale
import com.cookbookanything.app.ui.theme.NightMode
import com.cookbookanything.app.ui.theme.appBarColors
import com.cookbookanything.core.domain.FreeTier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(
    recipeCount: Int,
    unlocked: Boolean,
    onUnlock: () -> Unit,
    onManageCategories: () -> Unit,
    onOpenImportGuide: () -> Unit,
    onSaveBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    backup: BackupStatus,
    onPickBackupFolder: (Uri) -> Unit,
    onClearBackupFolder: () -> Unit,
    account: AccountState,
    onOpenAccount: () -> Unit,
    onSignOut: () -> Unit,
    onShowWhatsNew: () -> Unit,
) {
    // The user picks where the backup goes (phone storage, Drive, ...), so no storage permission is needed.
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) onSaveBackup(uri)
    }
    val restoreBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onRestoreBackup(uri)
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onPickBackupFolder(uri)
    }
    val context = LocalContext.current
    val locale = currentLocale()

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
            SectionTitle(stringResource(R.string.appearance))
            val currentMode = NightMode.current(context)
            val modeLabels = listOf(R.string.appearance_system, R.string.appearance_light, R.string.appearance_dark)
            NightMode.options.forEachIndexed { index, mode ->
                RadioRow(
                    label = stringResource(modeLabels[index]),
                    selected = currentMode == mode,
                    onSelect = { NightMode.set(context, mode) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.account))
            when (account) {
                AccountState.Unavailable -> Body(stringResource(R.string.account_unavailable))
                AccountState.SignedOut -> {
                    Body(stringResource(R.string.account_hint), muted = true)
                    Button(onClick = onOpenAccount, modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.account_sign_in))
                    }
                }
                is AccountState.SignedIn -> {
                    Body(stringResource(R.string.account_signed_in_as, account.email))
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
                        TextButton(onClick = onOpenAccount, modifier = Modifier.padding(start = 8.dp)) {
                            Text(stringResource(R.string.delete_account))
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.plan))
            Body(
                if (unlocked) {
                    stringResource(R.string.plan_unlocked)
                } else {
                    stringResource(R.string.plan_free, recipeCount.coerceAtMost(FreeTier.RECIPE_LIMIT), FreeTier.RECIPE_LIMIT)
                },
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenImportGuide)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(R.drawable.ic_download, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
                Text(stringResource(R.string.import_guide), style = MaterialTheme.typography.bodyLarge)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.backup_auto))
            Body(stringResource(R.string.backup_auto_hint), muted = true)
            val folder = backup.folder
            if (folder == null) {
                Button(
                    onClick = { pickFolder.launch(null) },
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                ) {
                    Text(stringResource(R.string.backup_choose_folder))
                }
            } else {
                // "primary:Backups" is how Android names a folder; the part after the colon is what the user picked.
                Body(stringResource(R.string.backup_auto_on, folder.lastPathSegment.orEmpty().substringAfter(':')))
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedButton(onClick = { pickFolder.launch(folder) }) { Text(stringResource(R.string.backup_choose_folder)) }
                    TextButton(onClick = onClearBackupFolder, modifier = Modifier.padding(start = 8.dp)) {
                        Text(stringResource(R.string.backup_auto_off))
                    }
                }
            }
            val lastBackup = backup.lastBackupAt
            Text(
                text = if (lastBackup == null) {
                    stringResource(R.string.backup_never)
                } else {
                    stringResource(
                        R.string.backup_last,
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
                            .format(Instant.ofEpochMilli(lastBackup).atZone(ZoneId.systemDefault())),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                // Overdue is said in words and in the error colour, never by colour alone.
                color = if (backup.overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (backup.overdue) Body(stringResource(R.string.backup_overdue), error = true)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.backup))
            Body(stringResource(R.string.backup_hint), muted = true)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowWhatsNew)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(stringResource(R.string.whats_new), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
            // The maker's mark at the foot of the page, as on every AnyKind app.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnyKindWordmark()
                Text(
                    text = stringResource(R.string.anykind_apps),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
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
private fun Body(text: String, muted: Boolean = false, error: Boolean = false) {
    Text(
        text = text,
        style = if (muted) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
        color = when {
            error -> MaterialTheme.colorScheme.error
            muted -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun LanguageOption(label: String, tag: String, currentLanguage: String) {
    RadioRow(
        label = label,
        selected = currentLanguage == tag,
        onSelect = {
            AppCompatDelegate.setApplicationLocales(
                if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
            )
        },
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
