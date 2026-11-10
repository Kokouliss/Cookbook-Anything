package com.cookbookanything.app.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// shortcut: WorkManager survives reboots with no extra code but can fire some minutes late while the
// phone dozes, move to exact alarms if reminders must be on the minute
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule(scheduleId: String, recipeId: String, recipeTitle: String, at: Long) {
        val delay = at - System.currentTimeMillis()
        if (delay <= 0) return
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_RECIPE_ID to recipeId,
                    ReminderWorker.KEY_RECIPE_TITLE to recipeTitle,
                ),
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(scheduleId, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(scheduleId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(scheduleId)
    }
}
