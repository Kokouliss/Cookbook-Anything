package com.cookbookanything.app

import android.app.Application
import com.cookbookanything.app.data.database.RecipeDao
import com.cookbookanything.app.data.photos.PhotoStore
import com.cookbookanything.app.data.share.AutoBackupWorker
import com.cookbookanything.app.data.text.PendingPhotoReads
import com.cookbookanything.app.data.text.PendingPhotoWorker
import com.cookbookanything.app.data.share.BackupPreferences
import com.cookbookanything.app.reminders.MorningReminderWorker
import com.cookbookanything.app.reminders.ReminderWorker
import com.cookbookanything.app.ui.theme.NightMode
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class CookbookApplication : Application() {
    @Inject lateinit var recipeDao: RecipeDao
    @Inject lateinit var photoStore: PhotoStore
    @Inject lateinit var backupPreferences: BackupPreferences
    @Inject lateinit var pendingPhotoReads: PendingPhotoReads

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NightMode.apply(this)
        ReminderWorker.createChannel(this)
        MorningReminderWorker.schedule(this)
        if (backupPreferences.status.value.folder != null) AutoBackupWorker.schedule(this)
        // Photos taken offline that were not read yet, for instance when the cookbook was full.
        if (pendingPhotoReads.names().isNotEmpty()) PendingPhotoWorker.schedule(this)

        // Photo files of deleted recipes and abandoned edits are cleaned up here, once per start.
        val startedAt = System.currentTimeMillis()
        applicationScope.launch {
            photoStore.sweepOrphans(referenced = recipeDao.allPhotoNames().toSet(), olderThan = startedAt)
        }
    }
}
