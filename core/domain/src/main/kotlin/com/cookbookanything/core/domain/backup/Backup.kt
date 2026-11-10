package com.cookbookanything.core.domain.backup

import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.model.Recipe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything in the cookbook, as stored in a backup file next to the photo files. */
@Serializable
data class Backup(
    val formatVersion: Int = BackupCodec.FORMAT_VERSION,
    val categories: List<Category> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val schedules: List<BackupSchedule> = emptyList(),
)

@Serializable
data class BackupSchedule(
    val id: String,
    val recipeId: String,
    val scheduledAt: Long,
)

object BackupCodec {
    const val FORMAT_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(backup: Backup): String = json.encodeToString(Backup.serializer(), backup)

    /** Returns null when [text] is not a backup this version of the app can read. */
    fun decode(text: String): Backup? =
        runCatching { json.decodeFromString(Backup.serializer(), text) }
            .getOrNull()
            ?.takeIf { it.formatVersion in 1..FORMAT_VERSION }
}

/**
 * The backup with photo file names replaced through [photoNames] (photos missing from it are
 * dropped) and with categories that are not in [knownCategoryIds] cleared from the recipes, so the
 * result can be stored as it is.
 */
fun Backup.prepareForRestore(photoNames: Map<String, String>, knownCategoryIds: Set<String>): Backup {
    val categoryIds = knownCategoryIds + categories.map { it.id }
    val recipeIds = recipes.map { it.id }.toSet()
    return copy(
        recipes = recipes.map { recipe ->
            recipe.copy(
                categoryId = recipe.categoryId?.takeIf { it in categoryIds },
                photos = recipe.photos.mapNotNull(photoNames::get),
                steps = recipe.steps.map { it.copy(photos = it.photos.mapNotNull(photoNames::get)) },
            )
        },
        schedules = schedules.filter { it.recipeId in recipeIds },
    )
}
