package com.cookbookanything.app.data.database

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Relation

// Ids are UUID strings so rows can be synced to the user's account later without renumbering.

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
)

@Entity(
    tableName = "recipes",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId")],
)
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val categoryId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class IngredientEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val text: String,
    val checked: Boolean,
    val position: Int,
)

@Entity(
    tableName = "steps",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class StepEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val text: String,
    val done: Boolean,
    val position: Int,
)

/** A photo of the recipe itself ([stepId] is null) or of one of its steps. */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class PhotoEntity(
    @PrimaryKey val fileName: String,
    val recipeId: String,
    val stepId: String?,
    val position: Int,
)

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val text: String,
    val createdAt: Long,
)

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val scheduledAt: Long,
)

data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["recipeId"]) val ingredients: List<IngredientEntity>,
    @Relation(parentColumns = ["id"], entityColumns = ["recipeId"]) val steps: List<StepEntity>,
    @Relation(parentColumns = ["id"], entityColumns = ["recipeId"]) val photos: List<PhotoEntity>,
    @Relation(parentColumns = ["id"], entityColumns = ["recipeId"]) val comments: List<CommentEntity>,
)

data class RecipeSummaryRow(
    val id: String,
    val title: String,
    val categoryId: String?,
    val coverPhoto: String?,
)

data class ScheduledRecipeRow(
    val id: String,
    val recipeId: String,
    val recipeTitle: String,
    val scheduledAt: Long,
)
