package com.cookbookanything.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.cookbookanything.app.MainActivity
import com.cookbookanything.app.R
import com.cookbookanything.app.data.database.ScheduledRecipeRow
import com.cookbookanything.app.di.WorkerEntryPoint
import com.cookbookanything.core.domain.design.Palette
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton

/** A recipe card on the home screen: the next planned recipe, or an invitation to plan one. */
class TonightWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val next = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
            .scheduleDao().nextScheduled(System.currentTimeMillis())
        provideContent { Content(context, next) }
    }

    @Composable
    private fun Content(context: Context, next: ScheduledRecipeRow?) {
        val card = ColorProvider(Color(Palette.Light.CARD), Color(Palette.Dark.CARD))
        val ink = ColorProvider(Color(Palette.Light.INK), Color(Palette.Dark.INK))
        val muted = ColorProvider(Color(Palette.Light.MUTED), Color(Palette.Dark.MUTED))
        val red = ColorProvider(Color(Palette.Light.RED), Color(Palette.Dark.RED))
        val open = Intent(context, MainActivity::class.java).apply {
            if (next != null) putExtra(MainActivity.EXTRA_RECIPE_ID, next.recipeId) else putExtra(MainActivity.EXTRA_OPEN_CALENDAR, true)
        }
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(card)
                .cornerRadius(14.dp)
                .clickable(actionStartActivity(open)),
        ) {
            // The red rule along the top of an index card.
            Box(modifier = GlanceModifier.fillMaxWidth().height(3.dp).background(red)) {}
            Column(modifier = GlanceModifier.padding(12.dp)) {
                Text(
                    text = if (next == null) context.getString(R.string.widget_nothing) else whenLabel(context, next.scheduledAt),
                    style = TextStyle(color = muted, fontSize = 13.sp),
                )
                Text(
                    text = next?.recipeTitle ?: context.getString(R.string.widget_plan),
                    style = TextStyle(color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                    maxLines = 2,
                )
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            runCatching { TonightWidget().updateAll(context) }
        }
    }
}

/** "Today, 19:00", "Tomorrow, 12:30" or "Saturday, 13:00": shared by the widget and the home strip. */
internal fun whenLabel(context: Context, at: Long): String {
    val zone = ZoneId.systemDefault()
    val locale = context.resources.configuration.locales[0]
    val dateTime = Instant.ofEpochMilli(at).atZone(zone)
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(dateTime)
    val today = LocalDate.now(zone)
    return when (dateTime.toLocalDate()) {
        today -> context.getString(R.string.widget_today, time)
        today.plusDays(1) -> context.getString(R.string.widget_tomorrow, time)
        else -> DateTimeFormatter.ofPattern("EEEE", locale).format(dateTime).replaceFirstChar { it.titlecase(locale) } + ", " + time
    }
}

class TonightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TonightWidget()
}

/** Lets the repositories refresh the widget when the calendar changes. */
@Singleton
class TonightWidgetUpdater @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun refresh() = TonightWidget.refresh(context)
}
