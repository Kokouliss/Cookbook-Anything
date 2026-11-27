package com.cookbookanything.app.data.text

import android.util.Base64
import com.cookbookanything.core.domain.text.firstAvailableModel
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
 * Writes down the recipe in photos or a cooking video through a Google AI model (Gemini), called
 * with the project's key (see [GoogleApi]); the Generative Language API has to be switched on in the
 * project and allowed for that key. It reads Greek and English, printed and handwritten, and hears
 * what is said in a video, so no text recognition or audio extraction runs on the phone.
 */
@Singleton
class GeminiRecipeReader @Inject constructor(private val api: GoogleApi) {

    val available: Boolean get() = api.available

    // The place in MODELS of the one to ask first: the one that last answered, so a model that is retired or
    // overloaded is not sent a whole video again.
    // shortcut: it only moves on, so a busy hour leaves the app on a backup model until it restarts
    @Volatile private var model = 0

    /**
     * The recipe in [media] (bytes and MIME type, in order) as text laid out for
     * [com.cookbookanything.core.domain.text.parseRecipeText], or null when there is none.
     * [subject] says what the media are, [PHOTOS] or [VIDEO]. Throws [IOException] when the call fails
     * or the model takes longer than [timeoutMs].
     */
    suspend fun read(media: List<Pair<ByteArray, String>>, subject: String, timeoutMs: Int = TIMEOUT_MS): String? = withContext(Dispatchers.IO) {
        val request = buildJsonObject {
            putJsonArray("contents") {
                add(
                    buildJsonObject {
                        putJsonArray("parts") {
                            media.forEach { (bytes, mimeType) ->
                                add(buildJsonObject { putJsonObject("inline_data") { put("mime_type", mimeType); put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)) } })
                            }
                            add(buildJsonObject { put("text", PROMPT.format(subject)) })
                        }
                    },
                )
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.2)
                // Copying out a recipe needs no pondering; the default (medium) only makes the reader wait.
                putJsonObject("thinkingConfig") { put("thinkingLevel", "LOW") }
            }
        }
        var asked = model
        val (working, answer) = try {
            firstAvailableModel(MODELS, model, unavailable = { (it as? GoogleApi.Refused)?.code in MOVE_ON }) {
                asked = MODELS.indexOf(it)
                api.post("$ENDPOINT/$it:generateContent", request, "Gemini", timeoutMs = timeoutMs)
            }
        } catch (e: GoogleApi.TimedOut) {
            // Trying the next model now would double the wait; it is asked first the next time instead.
            model = (asked + 1) % MODELS.size
            throw e
        }
        model = working
        val text = answer["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }
            ?.joinToString("\n")?.trim().orEmpty()
        text.takeIf { it.isNotEmpty() && !it.startsWith(NONE) }
    }

    companion object {
        const val PHOTOS = "one or more photos of the same recipe, in order (a cookbook page, a handwritten card or a screenshot)"
        const val VIDEO = "a cooking video"

        // Google retires models with a 404 (gemini-2.5-flash, November 2026) and turns requests away when one is
        // overloaded (503, seen on gemini-3.8-flash the same month) or out of free quota (429). Each of these
        // moves on to the next: the older flash models, then the alias for its newest one, which can be a preview.
        // shortcut: the names are fixed here, move them to a remote setting if the alias goes too
        private val MODELS = listOf("gemini-3.8-flash", "gemini-3.6-flash", "gemini-3.5-flash-lite", "gemini-flash-latest")
        private val MOVE_ON = setOf(404, 429, 503)
        private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val TIMEOUT_MS = 120_000
        private const val NONE = "NONE"
        private const val PROMPT = "This is %s. Write down the recipe that is written, spoken or shown in it, in its own language, " +
            "as plain text in exactly this shape: the dish's name on the first line; then a line that says only \"Ingredients:\" followed by " +
            "one ingredient per line with its amount; then a line that says only \"Steps:\" followed by the steps in order, one per line, " +
            "with the times and temperatures that are mentioned. Keep the two labels \"Ingredients:\" and \"Steps:\" in English whatever the " +
            "recipe's language. No other text. If there is no recipe, answer with the single word NONE."
    }
}
