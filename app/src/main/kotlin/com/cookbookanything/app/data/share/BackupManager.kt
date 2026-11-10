package com.cookbookanything.app.data.share

import android.content.Context
import android.net.Uri
import com.cookbookanything.app.data.database.CategoryDao
import com.cookbookanything.app.data.database.CategoryEntity
import com.cookbookanything.app.data.database.RecipeDao
import com.cookbookanything.app.data.database.ScheduleDao
import com.cookbookanything.app.data.database.ScheduleEntity
import com.cookbookanything.app.data.database.toCommentEntities
import com.cookbookanything.app.data.database.toEntity
import com.cookbookanything.app.data.database.toIngredientEntities
import com.cookbookanything.app.data.database.toModel
import com.cookbookanything.app.data.database.toPhotoEntities
import com.cookbookanything.app.data.database.toStepEntities
import com.cookbookanything.app.data.photos.PhotoStore
import com.cookbookanything.app.di.IoDispatcher
import com.cookbookanything.app.reminders.ReminderScheduler
import com.cookbookanything.core.domain.FreeTier
import com.cookbookanything.core.domain.backup.Backup
import com.cookbookanything.core.domain.backup.BackupCodec
import com.cookbookanything.core.domain.backup.BackupSchedule
import com.cookbookanything.core.domain.backup.prepareForRestore
import com.cookbookanything.core.domain.model.Recipe
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

sealed interface RestoreResult {
    /** [added] recipes were missing on this phone and are back; it is 0 when nothing was missing. */
    data class Restored(val added: Int) : RestoreResult
    data object NotABackup : RestoreResult
    data object LimitReached : RestoreResult
}

/**
 * Saves the whole cookbook into one file (a zip holding backup.json plus the photos) and brings it
 * back. Restoring only adds what is missing: recipes already on the phone are never overwritten.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recipeDao: RecipeDao,
    private val categoryDao: CategoryDao,
    private val scheduleDao: ScheduleDao,
    private val photoStore: PhotoStore,
    private val reminders: ReminderScheduler,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    /** Writes the backup into [target], a document the user picked. Returns false when that failed. */
    suspend fun export(target: Uri): Boolean = withContext(io) {
        runCatching {
            val recipes = recipeDao.getAllRecipes().map { it.toModel() }
            val backup = Backup(
                categories = categoryDao.getAll().map { it.toModel() },
                recipes = recipes,
                schedules = scheduleDao.getAll().map { BackupSchedule(it.id, it.recipeId, it.scheduledAt) },
            )
            val output = checkNotNull(context.contentResolver.openOutputStream(target, "wt"))
            ZipOutputStream(output.buffered()).use { zip ->
                // The index goes first, so a restore can tell a backup from any other zip at once.
                zip.putNextEntry(ZipEntry(BACKUP_ENTRY))
                zip.write(BackupCodec.encode(backup).toByteArray())
                zip.closeEntry()
                recipes.flatMap(Recipe::allPhotos).distinct().forEach { name ->
                    val photo = photoStore.file(name)
                    if (photo.exists()) {
                        zip.putNextEntry(ZipEntry(RecipeSharer.PHOTO_PREFIX + name))
                        photo.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }.isSuccess
    }

    /**
     * Adds the recipes of the backup in [uri] that are not on this phone. The file comes from
     * outside the app, so sizes are capped and entry names are never used as paths.
     */
    suspend fun restore(uri: Uri, unlocked: Boolean): RestoreResult = withContext(io) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip -> restore(zip, unlocked) }
            }
        }.getOrNull() ?: RestoreResult.NotABackup
    }

    private suspend fun restore(zip: ZipInputStream, unlocked: Boolean): RestoreResult {
        if (zip.nextEntry?.name != BACKUP_ENTRY) return RestoreResult.NotABackup
        val backup = BackupCodec.decode(zip.readText(MAX_JSON_BYTES)) ?: return RestoreResult.NotABackup

        val existingIds = recipeDao.allRecipeIds().toSet()
        val missing = backup.copy(recipes = backup.recipes.filter { it.id !in existingIds })
        if (missing.recipes.isEmpty()) return RestoreResult.Restored(added = 0)
        if (!unlocked && existingIds.size + missing.recipes.size > FreeTier.RECIPE_LIMIT) {
            return RestoreResult.LimitReached
        }

        val wantedPhotos = missing.recipes.flatMap(Recipe::allPhotos).toSet()
        val photoNames = mutableMapOf<String, String>()
        var entries = 0
        while (entries++ < MAX_ENTRIES) {
            val entry = zip.nextEntry ?: break
            val name = entry.name.removePrefix(RecipeSharer.PHOTO_PREFIX)
            if (entry.name.startsWith(RecipeSharer.PHOTO_PREFIX) && name in wantedPhotos) {
                photoStore.importStream(zip, RecipeSharer.MAX_PHOTO_BYTES)?.let { photoNames[name] = it }
            }
        }

        val ready = missing.prepareForRestore(
            photoNames = photoNames,
            knownCategoryIds = categoryDao.getAll().map { it.id }.toSet(),
        )
        categoryDao.insertMissing(ready.categories.map { CategoryEntity(id = it.id, name = it.name) })
        ready.recipes.forEach { recipe ->
            recipeDao.restoreRecipe(
                recipe = recipe.toEntity(),
                ingredients = recipe.toIngredientEntities(),
                steps = recipe.toStepEntities(),
                photos = recipe.toPhotoEntities(),
                comments = recipe.toCommentEntities(),
            )
        }
        val titles = ready.recipes.associate { it.id to it.title }
        ready.schedules.forEach { schedule ->
            val inserted = scheduleDao.insertMissing(
                ScheduleEntity(id = schedule.id, recipeId = schedule.recipeId, scheduledAt = schedule.scheduledAt),
            )
            if (inserted != -1L) {
                reminders.schedule(schedule.id, schedule.recipeId, titles.getValue(schedule.recipeId), schedule.scheduledAt)
            }
        }
        return RestoreResult.Restored(added = ready.recipes.size)
    }

    private companion object {
        const val BACKUP_ENTRY = "backup.json"
        const val MAX_ENTRIES = 20_000
        const val MAX_JSON_BYTES = 20 * 1024 * 1024
    }
}

private fun Recipe.allPhotos(): List<String> = photos + steps.flatMap { it.photos }
