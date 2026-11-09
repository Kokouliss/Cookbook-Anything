package com.cookbookanything.core.domain.share

import com.cookbookanything.core.domain.model.Ingredient
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.model.Step
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What travels inside a shared recipe file. Personal data (check marks, comments, category, calendar)
 * stays on the sender's phone.
 */
@Serializable
data class SharedRecipe(
    val formatVersion: Int = RecipeShareCodec.FORMAT_VERSION,
    val title: String,
    val photos: List<String> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val steps: List<SharedStep> = emptyList(),
)

@Serializable
data class SharedStep(
    val text: String,
    val photos: List<String> = emptyList(),
)

object RecipeShareCodec {
    const val FORMAT_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(recipe: Recipe): String =
        json.encodeToString(SharedRecipe.serializer(), recipe.toShared())

    /** Returns null when [text] is not a recipe this version of the app can read. */
    fun decode(text: String): SharedRecipe? =
        runCatching { json.decodeFromString(SharedRecipe.serializer(), text) }
            .getOrNull()
            ?.takeIf { it.formatVersion in 1..FORMAT_VERSION && it.title.isNotBlank() }
}

fun Recipe.toShared(): SharedRecipe = SharedRecipe(
    title = title,
    photos = photos,
    ingredients = ingredients.map { it.text },
    steps = steps.map { SharedStep(text = it.text, photos = it.photos) },
)

/**
 * Builds a brand new recipe from a shared one. [photoNames] maps the file names used in the shared
 * file to the names the photos were stored under on this phone; photos missing from it are dropped.
 */
fun SharedRecipe.toRecipe(
    newId: () -> String,
    photoNames: Map<String, String>,
    now: Long,
): Recipe = Recipe(
    id = newId(),
    title = title.trim(),
    photos = photos.mapNotNull(photoNames::get),
    ingredients = ingredients.map { Ingredient(id = newId(), text = it) },
    steps = steps.map { Step(id = newId(), text = it.text, photos = it.photos.mapNotNull(photoNames::get)) },
    createdAt = now,
    updatedAt = now,
)
