package com.cookbookanything.app.data.repository

import com.cookbookanything.app.data.database.RecipeDao
import com.cookbookanything.app.data.database.ScheduleDao
import com.cookbookanything.app.data.database.ScheduleEntity
import com.cookbookanything.app.data.database.ScheduledRecipeRow
import com.cookbookanything.app.data.database.ShoppingItemRow
import com.cookbookanything.app.data.database.toModel
import com.cookbookanything.app.reminders.ReminderScheduler
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
) : ScheduleRepository {

    override fun observeSchedule(): Flow<List<ScheduledRecipe>> =
        scheduleDao.observeSchedule().map { rows -> rows.map(ScheduledRecipeRow::toModel) }

    override fun observeShoppingList(from: Long): Flow<List<ShoppingItem>> =
        scheduleDao.observeShoppingList(from).map { rows -> rows.map(ShoppingItemRow::toModel) }

    override suspend fun schedule(recipeId: String, scheduledAt: Long) {
        val title = recipeDao.getRecipe(recipeId)?.recipe?.title ?: return
        val id = UUID.randomUUID().toString()
        scheduleDao.insert(ScheduleEntity(id = id, recipeId = recipeId, scheduledAt = scheduledAt))
        reminders.schedule(scheduleId = id, recipeId = recipeId, recipeTitle = title, at = scheduledAt)
    }

    override suspend fun remove(scheduleId: String) {
        reminders.cancel(scheduleId)
        scheduleDao.delete(scheduleId)
    }
}
