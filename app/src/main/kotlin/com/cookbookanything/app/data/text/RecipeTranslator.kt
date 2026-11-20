package com.cookbookanything.app.data.text

import com.cookbookanything.core.domain.text.ParsedRecipe
import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Translates an imported recipe into the phone's language on the phone itself (ML Kit). The language
 * pack, about 30 MB per language, is downloaded the first time and kept.
 */
@Singleton
class RecipeTranslator @Inject constructor(private val preferences: ImportPreferences) {

    /**
     * The recipe in the phone's language. The same recipe comes back when the switch is off or the text is
     * already in that language; null when a translation was wanted and failed (offline, unknown language).
     */
    suspend fun translate(recipe: ParsedRecipe): ParsedRecipe? {
        if (!preferences.translate.value) return recipe
        return runCatching {
            val target = TranslateLanguage.fromLanguageTag(Locale.getDefault().language) ?: return recipe
            val sample = (listOf(recipe.title, recipe.description) + recipe.ingredients + recipe.steps)
                .filterNotNull().joinToString("\n").take(SAMPLE_CHARS)
            val detected = LanguageIdentification.getClient().use { it.identifyLanguage(sample).await() }
            val source = TranslateLanguage.fromLanguageTag(detected) ?: return recipe
            if (source == target) return recipe
            val options = TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build()
            Translation.getClient(options).use { translator ->
                translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                suspend fun line(text: String): String = translator.translate(text).await()
                recipe.copy(
                    title = recipe.title?.let { line(it) },
                    description = recipe.description?.let { line(it) },
                    ingredients = recipe.ingredients.map { line(it) },
                    steps = recipe.steps.map { line(it) },
                )
            }
        }.getOrNull()
    }

    private companion object {
        /** Enough text to tell the language; the whole recipe is not needed. */
        const val SAMPLE_CHARS = 1000
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
