package com.cookbookanything.app.data.text

import android.util.Base64
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Listens to a cooking video and writes down the recipe said or shown in it, through a Google AI
 * model (Gemini) called with the project's key like Cloud Vision; the Generative Language API has to
 * be switched on in the project. The video goes whole, so no audio has to be pulled out on the phone.
 */
@Singleton
class SpokenRecipeReader @Inject constructor(private val api: GoogleApi) {

    val available: Boolean get() = api.available

    /** The recipe as text, laid out for [com.cookbookanything.core.domain.text.parseRecipeText], or null when the video gives none. Throws [IOException] when the call fails. */
    suspend fun read(video: ByteArray, mimeType: String): String? = withContext(Dispatchers.IO) {
        val request = buildJsonObject {
            putJsonArray("contents") {
                add(
                    buildJsonObject {
                        putJsonArray("parts") {
                            add(buildJsonObject { putJsonObject("inline_data") { put("mime_type", mimeType); put("data", Base64.encodeToString(video, Base64.NO_WRAP)) } })
                            add(buildJsonObject { put("text", PROMPT) })
                        }
                    },
                )
            }
            putJsonObject("generationConfig") { put("temperature", 0.2) }
        }
        val answer = api.post(ENDPOINT, request, "Gemini", timeoutMs = TIMEOUT_MS)
        val text = answer["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }
            ?.joinToString("\n")?.trim().orEmpty()
        text.takeIf { it.isNotEmpty() && !it.startsWith(NONE) }
    }

    private companion object {
        // shortcut: the model name is fixed here, move it to a remote setting if Google retires it
        const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        const val TIMEOUT_MS = 120_000
        const val NONE = "NONE"
        const val PROMPT = "This is a cooking video. Write down the recipe that is spoken or shown in it, in the language it is spoken in, " +
            "as plain text in exactly this shape: the dish's name on the first line; then a line that says only \"Ingredients:\" followed by " +
            "one ingredient per line with its amount; then a line that says only \"Steps:\" followed by the steps in order, one per line, " +
            "with the times and temperatures that are mentioned. Keep the two labels \"Ingredients:\" and \"Steps:\" in English whatever the " +
            "spoken language. No other text. If the video has no recipe, answer with the single word NONE."
    }
}
