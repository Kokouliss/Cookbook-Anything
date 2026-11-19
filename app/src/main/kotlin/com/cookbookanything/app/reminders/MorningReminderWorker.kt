package com.cookbookanything.app.reminders

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.cookbookanything.app.MainActivity
import com.cookbookanything.app.R
import com.cookbookanything.app.data.database.toModel
import com.cookbookanything.app.di.WorkerEntryPoint
import com.cookbookanything.app.widget.TonightWidget
import com.cookbookanything.core.domain.shopping.merged
import dagger.hilt.android.EntryPointAccessors
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

/**
 * Every morning: if recipes are planned for today and some of their ingredients are not ticked,
 * one notification says how many are missing and opens the shopping list. Also refreshes the widget.
 */
// shortcut: WorkManager drifts a few minutes a day from 09:00, switch to an alarm if the exact hour matters
class MorningReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val context = applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val missing = runBlocking {
            TonightWidget.refresh(context)
            entryPoint.scheduleDao().shoppingItemsBetween(start, end).map { it.toModel() }.merged().filterNot { it.checked }
        }
        if (missing.isEmpty() || !ReminderWorker.canNotify(context)) return Result.success()

        val notification = NotificationCompat.Builder(context, ReminderWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shopping_cart)
            .setContentTitle(context.resources.getQuantityString(R.plurals.missing_today_title, missing.size, missing.size))
            .setContentText(missing.joinToString(", ") { it.name })
            .setStyle(NotificationCompat.BigTextStyle().bigText(missing.joinToString("\n") { it.label }))
            .setContentIntent(
                ReminderWorker.openApp(
                    context,
                    NOTIFICATION_ID,
                    Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_SHOPPING, true),
                ),
            )
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "morning-reminder"
        private const val NOTIFICATION_ID = 9_001
        private const val HOUR = 9

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(LocalTime.of(HOUR, 0))
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<MorningReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
