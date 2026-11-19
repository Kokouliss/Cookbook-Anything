package com.cookbookanything.app.di

import com.cookbookanything.app.data.database.ScheduleDao
import com.cookbookanything.app.data.photos.PhotoStore
import com.cookbookanything.app.data.share.BackupManager
import com.cookbookanything.app.data.share.BackupPreferences
import com.cookbookanything.app.data.text.PendingPhotoReads
import com.cookbookanything.app.data.text.PhotoTextReader
import com.cookbookanything.core.domain.repository.EntitlementRepository
import com.cookbookanything.core.domain.repository.RecipeRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** How workers and the widget, which Hilt does not build, reach the app's singletons. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun backupManager(): BackupManager
    fun backupPreferences(): BackupPreferences
    fun scheduleDao(): ScheduleDao
    fun photoStore(): PhotoStore
    fun photoTextReader(): PhotoTextReader
    fun pendingPhotoReads(): PendingPhotoReads
    fun recipeRepository(): RecipeRepository
    fun entitlementRepository(): EntitlementRepository
}
