package com.cookbookanything.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cookbookanything.app.MainActivity
import com.cookbookanything.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rings when a step's timer ends even if the cook left the screen. The countdown on screen is exact;
 * this alarm is the safety net behind it.
 */
// shortcut: an inexact alarm, which Android may hold back some minutes while the phone dozes; ask
// for the exact-alarm permission if cooks complain
@Singleton
class CookingTimer @Inject constructor(@ApplicationContext private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun start(recipeId: String, recipeTitle: String, endsAt: Long) {
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, pendingIntent(recipeId, recipeTitle))
    }

    fun cancel() {
        alarms.cancel(pendingIntent(recipeId = null, recipeTitle = null))
    }

    // One request code: only one timer rings at a time, and cancel() must match whatever was started.
    private fun pendingIntent(recipeId: String?, recipeTitle: String?): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, TimerReceiver::class.java)
            .putExtra(MainActivity.EXTRA_RECIPE_ID, recipeId)
            .putExtra(ReminderWorker.KEY_RECIPE_TITLE, recipeTitle),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ReminderWorker.canNotify(context)) return
        val recipeId = intent.getStringExtra(MainActivity.EXTRA_RECIPE_ID)
        val notification = NotificationCompat.Builder(context, ReminderWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.timer_done))
            .setContentText(intent.getStringExtra(ReminderWorker.KEY_RECIPE_TITLE))
            .setContentIntent(
                ReminderWorker.openApp(
                    context,
                    NOTIFICATION_ID,
                    Intent(context, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_RECIPE_ID, recipeId)
                        .putExtra(MainActivity.EXTRA_START_COOKING, true),
                ),
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val NOTIFICATION_ID = 9_002
    }
}
