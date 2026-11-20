package com.cookbookanything.app.data.repository

import com.cookbookanything.app.data.database.DayNoteEntity
import com.cookbookanything.app.data.database.RecipeDao
import com.cookbookanything.app.data.database.ScheduleDao
import com.cookbookanything.app.data.database.ScheduleEntity
import com.cookbookanything.app.data.database.ScheduledRecipeRow
import com.cookbookanything.app.data.database.ShoppingItemRow
import com.cookbookanything.app.data.database.toModel
import com.cookbookanything.app.reminders.ReminderScheduler
import com.cookbookanything.app.widget.TonightWidgetUpdater
import com.cookbookanything.core.domain.model.MealSlot
import com.cookbookanything.core.domain.model.ScheduledRecipe
import com.cookbookanything.core.domain.model.ShoppingItem
import com.cookbookanything.core.domain.repository.ScheduleRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineFirstScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao,
    private val recipeDao: RecipeDao,
    private val reminders: ReminderScheduler,
    private val widget: TonightWidgetUpdater,
) : ScheduleRepository {

    override fun observeSchedule(): Flow<List<ScheduledRecipe>> =
        scheduleDao.observeSchedule().map { rows -> rows.map(ScheduledRecipeRow::toModel) }

    override fun observeShoppingList(from: Long, to: Long): Flow<List<ShoppingItem>> =
        scheduleDao.observeShoppingList(from, to).map { rows -> rows.map(ShoppingItemRow::toModel) }

    override suspend fun schedule(recipeId: String, scheduledAt: Long, slot: MealSlot?) {
        val recipe = recipeDao.getRecipe(recipeId) ?: return
        val id = UUID.randomUUID().toString()
        scheduleDao.insert(ScheduleEntity(id = id, recipeId = recipeId, scheduledAt = scheduledAt, slot = slot?.name))
        reminders.schedule(
            scheduleId = id,
            recipeId = recipeId,
            recipeTitle = recipe.recipe.title,
            coverPhoto = recipe.coverPhoto,
            at = scheduledAt,
        )
        widget.refresh()
    }

    override suspend fun remove(scheduleId: String) {
        reminders.cancel(scheduleId)
        scheduleDao.delete(scheduleId)
        widget.refresh()
    }

    override fun observeDayNotes(): Flow<Map<Long, String>> =
        scheduleDao.observeDayNotes().map { notes -> notes.associate { it.day to it.note } }

    override suspend fun setDayNote(epochDay: Long, note: String) {
        if (note.isBlank()) scheduleDao.deleteDayNote(epochDay) else scheduleDao.upsertDayNote(DayNoteEntity(epochDay, note.trim()))
    }
}
