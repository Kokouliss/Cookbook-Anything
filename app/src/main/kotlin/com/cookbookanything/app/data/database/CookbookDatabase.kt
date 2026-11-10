package com.cookbookanything.app.data.database

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
    version = 1,
)
abstract class CookbookDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun categoryDao(): CategoryDao
    abstract fun scheduleDao(): ScheduleDao
}
