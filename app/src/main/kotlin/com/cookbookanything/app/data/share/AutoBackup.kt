package com.cookbookanything.app.data.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.cookbookanything.app.di.WorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What Settings and the recipe list say about backups. */
data class BackupStatus(
    /** The folder automatic backups go to, or null when they are off. */
    val folder: Uri? = null,
    val lastBackupAt: Long? = null,
    /** True when nothing was backed up for a month (counted from the first start before the first backup). */
    val overdue: Boolean = false,
)

@Singleton
class BackupPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val preferences = context.getSharedPreferences("backup", Context.MODE_PRIVATE)
    private val _status = MutableStateFlow(read())
    val status: StateFlow<BackupStatus> = _status

    init {
        if (!preferences.contains(KEY_FIRST_START)) {
            preferences.edit().putLong(KEY_FIRST_START, System.currentTimeMillis()).apply()
        }
    }

    fun markBackedUp(at: Long) {
        preferences.edit().putLong(KEY_LAST_BACKUP, at).apply()
        _status.value = read()
    }

    /** Remembers the folder, keeps the right to write there and starts the weekly backups. */
    fun setFolder(folder: Uri) {
        context.contentResolver.takePersistableUriPermission(
            folder,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        preferences.edit().putString(KEY_FOLDER, folder.toString()).apply()
        _status.value = read()
        AutoBackupWorker.schedule(context)
        AutoBackupWorker.runNow(context)
    }

    fun clearFolder() {
        read().folder?.let { folder ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    folder,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        preferences.edit().remove(KEY_FOLDER).apply()
        _status.value = read()
        AutoBackupWorker.cancel(context)
    }

    private fun read(): BackupStatus {
        val last = preferences.getLong(KEY_LAST_BACKUP, 0L).takeIf { it > 0 }
        val since = last ?: preferences.getLong(KEY_FIRST_START, System.currentTimeMillis())
        return BackupStatus(
            folder = preferences.getString(KEY_FOLDER, null)?.toUri(),
            lastBackupAt = last,
            overdue = System.currentTimeMillis() - since > OVERDUE_AFTER_MS,
        )
    }

    private companion object {
        const val KEY_FOLDER = "folder"
        const val KEY_LAST_BACKUP = "last_backup_at"
        const val KEY_FIRST_START = "first_start_at"
        const val OVERDUE_AFTER_MS = 30L * 24 * 60 * 60 * 1000
    }
}

/** Writes the weekly backup into the chosen folder. Runs with the app closed, so it gets its parts through Hilt's entry point. */
class AutoBackupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, WorkerEntryPoint::class.java)
        val folder = entryPoint.backupPreferences().status.value.folder ?: return Result.success()
        val ok = kotlinx.coroutines.runBlocking { entryPoint.backupManager().exportToFolder(folder) }
        // The folder may be on a drive that is offline right now; WorkManager tries again later.
        return if (ok) Result.success() else Result.retry()
    }

    companion object {
        private const val WEEKLY = "auto-backup"
        private const val ONCE = "auto-backup-now"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WEEKLY, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun runNow(context: Context) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONCE, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<AutoBackupWorker>().build())
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WEEKLY)
        }
    }
}
