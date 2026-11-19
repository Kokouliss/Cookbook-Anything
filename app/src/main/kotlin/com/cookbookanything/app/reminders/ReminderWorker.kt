package com.cookbookanything.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.cookbookanything.app.MainActivity
import com.cookbookanything.app.R
import com.cookbookanything.app.data.photos.photoDirectory
import java.io.File

/** "Time to cook" at the planned time, with buttons that open cooking mode or the shopping list. */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val recipeId = inputData.getString(KEY_RECIPE_ID) ?: return Result.success()
        val title = inputData.getString(KEY_RECIPE_TITLE).orEmpty()
        val context = applicationContext
        if (!canNotify(context)) return Result.success()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_restaurant)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(title)
            .setContentIntent(openApp(context, recipeId.hashCode(), Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_RECIPE_ID, recipeId)))
            .addAction(
                0,
                context.getString(R.string.start_cooking),
                openApp(
                    context,
                    recipeId.hashCode() + 1,
                    Intent(context, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_RECIPE_ID, recipeId)
                        .putExtra(MainActivity.EXTRA_START_COOKING, true),
                ),
            )
            .addAction(
                0,
                context.getString(R.string.shopping_list),
                openApp(context, recipeId.hashCode() + 2, Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_SHOPPING, true)),
            )
            .setAutoCancel(true)
        inputData.getString(KEY_COVER_PHOTO)?.let { name ->
            // Decoded small: the notification only needs a thumbnail.
            val photo = File(photoDirectory(context), name)
            if (photo.exists()) {
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(photo.path, options)?.let(notification::setLargeIcon)
            }
        }
        NotificationManagerCompat.from(context).notify(id.hashCode(), notification.build())
        return Result.success()
    }

    companion object {
        const val KEY_RECIPE_ID = "recipeId"
        const val KEY_RECIPE_TITLE = "recipeTitle"
        const val KEY_COVER_PHOTO = "coverPhoto"
        const val CHANNEL_ID = "reminders"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel),
                NotificationManager.IMPORTANCE_HIGH,
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun canNotify(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        /** A tap that brings the app to the front with [intent]'s extras. */
        fun openApp(context: Context, requestCode: Int, intent: Intent): PendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
