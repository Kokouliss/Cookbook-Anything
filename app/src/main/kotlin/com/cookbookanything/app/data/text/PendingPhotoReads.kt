package com.cookbookanything.app.data.text

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cookbookanything.app.MainActivity
import com.cookbookanything.app.R
import com.cookbookanything.app.data.photos.PhotoStore
import com.cookbookanything.app.di.WorkerEntryPoint
import com.cookbookanything.app.reminders.ReminderWorker
import com.cookbookanything.core.domain.FreeTier
import com.cookbookanything.core.domain.model.Ingredient
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.Step
import com.cookbookanything.core.domain.text.parseRecipeText
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Photos of recipe pages taken while offline. They are copied into the photo store, remembered here,
 * and read by [PendingPhotoWorker] as soon as the phone has internet; each becomes a new recipe with
 * the photo attached, announced by a notification.
 */
@Singleton
class PendingPhotoReads @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoStore: PhotoStore,
) {
    private val preferences = context.getSharedPreferences("pending_photos", Context.MODE_PRIVATE)

    fun isOnline(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun names(): Set<String> = preferences.getStringSet(KEY, emptySet()).orEmpty()

    /** Keeps copies of the photos and schedules the read for when the phone is online. */
    suspend fun add(uris: List<Uri>) {
        val names = uris.mapNotNull { photoStore.import(it) }
        if (names.isEmpty()) return
        preferences.edit { putStringSet(KEY, names() + names) }
        PendingPhotoWorker.schedule(context)
    }

    fun remove(name: String) = preferences.edit { putStringSet(KEY, names() - name) }

    private companion object {
        const val KEY = "names"
    }
}

class PendingPhotoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
        val pending = entryPoint.pendingPhotoReads()
        val photoStore = entryPoint.photoStore()
        val recipes = entryPoint.recipeRepository()
        for (name in pending.names()) {
            val file = photoStore.file(name)
            if (!file.exists()) {
                pending.remove(name)
                continue
            }
            val text = try {
                entryPoint.photoTextReader().read(Uri.fromFile(file))
            } catch (e: IOException) {
                // The connection came and went, or Cloud Vision is not switched on: try again later.
                return Result.retry()
            }
            val parsed = text?.let(::parseRecipeText)
            if (parsed == null || parsed.isEmpty) {
                pending.remove(name)
                notify(context, name.hashCode(), context.getString(R.string.photo_recipe), context.getString(R.string.photo_nothing), null)
                continue
            }
            // The free version's limit holds here too; the photo waits until a slot is free.
            val count = recipes.observeSummaries().first().size
            if (!FreeTier.canAddRecipe(count, entryPoint.entitlementRepository().isUnlocked.first())) {
                notify(context, name.hashCode(), context.getString(R.string.photo_read_title), context.getString(R.string.photo_read_full), null)
                continue
            }
            val now = System.currentTimeMillis()
            val recipe = Recipe(
                id = UUID.randomUUID().toString(),
                title = parsed.title ?: context.getString(R.string.photo_recipe_title),
                servings = parsed.servings,
                photos = listOf(name),
                ingredients = parsed.ingredients.map { Ingredient(id = UUID.randomUUID().toString(), text = it) },
                steps = parsed.steps.map { Step(id = UUID.randomUUID().toString(), text = it) },
                createdAt = now,
                updatedAt = now,
            )
            recipes.saveRecipe(recipe)
            pending.remove(name)
            notify(context, name.hashCode(), context.getString(R.string.photo_read_title), recipe.title, recipe.id)
        }
        return Result.success()
    }

    private fun notify(context: Context, id: Int, title: String, text: String, recipeId: String?) {
        if (!ReminderWorker.canNotify(context)) return
        // shortcut: uses the reminders channel, give photo reads their own channel if people mute one and not the other
        val notification = NotificationCompat.Builder(context, ReminderWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_document_scanner)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(
                ReminderWorker.openApp(
                    context,
                    id,
                    Intent(context, MainActivity::class.java).apply { if (recipeId != null) putExtra(MainActivity.EXTRA_RECIPE_ID, recipeId) },
                ),
            )
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        private const val WORK_NAME = "pending-photo-reads"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<PendingPhotoWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
