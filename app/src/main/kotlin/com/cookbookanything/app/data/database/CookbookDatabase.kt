package com.cookbookanything.app.data.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [
        CategoryEntity::class,
        RecipeEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        PhotoEntity::class,
        CommentEntity::class,
        ScheduleEntity::class,
        CookedEntity::class,
        DayNoteEntity::class,
    ],
    version = 5,
    // Version 2 added servings and times to recipes. Version 3 added the cooked table.
    // Version 4 added description, notes, source, waiting time, rating and favourite to recipes, and optional to ingredients.
    // Version 5 added tags and pinned to recipes, a photo to cooked, a meal slot to schedules, and the day_notes table.
    autoMigrations = [
        AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5),
    ],
)
abstract class CookbookDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun categoryDao(): CategoryDao
    abstract fun scheduleDao(): ScheduleDao
}
