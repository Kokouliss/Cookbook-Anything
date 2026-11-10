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
    ],
    version = 2,
    // Version 2 added servings and times to recipes.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class CookbookDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun categoryDao(): CategoryDao
    abstract fun scheduleDao(): ScheduleDao
}
