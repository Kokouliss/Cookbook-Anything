package com.cookbookanything.app

import android.app.Application
import com.cookbookanything.app.data.database.RecipeDao
import com.cookbookanything.app.data.photos.PhotoStore
import com.cookbookanything.app.reminders.ReminderWorker
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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ReminderWorker.createChannel(this)

        // Photo files of deleted recipes and abandoned edits are cleaned up here, once per start.
        val startedAt = System.currentTimeMillis()
        applicationScope.launch {
            photoStore.sweepOrphans(referenced = recipeDao.allPhotoNames().toSet(), olderThan = startedAt)
        }
    }
}
