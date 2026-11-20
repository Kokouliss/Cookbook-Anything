package com.cookbookanything.app.data.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Reads the text off a photo of a recipe with Google Cloud Vision, which reads Greek, printed and
 * handwritten. ML Kit on the phone has no Greek model. The Cloud Vision API has to be switched on in
 * the Firebase project (see [GoogleApi]). The first 1,000 photos a month are free, then Google charges per photo.
 */
@Singleton
class PhotoTextReader @Inject constructor(@ApplicationContext private val context: Context, private val api: GoogleApi) {

    /** False in a build without the Firebase config file. */
    val available: Boolean get() = api.available

    /** The text on the photo, or null when it holds none. Throws [IOException] when the call fails. */
    suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        val image = encode(uri) ?: throw IOException("Could not read the image")
        val request = buildJsonObject {
            putJsonArray("requests") {
                add(
                    buildJsonObject {
                        putJsonObject("image") { put("content", image) }
                        putJsonArray("features") { add(buildJsonObject { put("type", "DOCUMENT_TEXT_DETECTION") }) }
                        putJsonObject("imageContext") { putJsonArray("languageHints") { add(JsonPrimitive("el")); add(JsonPrimitive("en")) } }
                    },
                )
            }
        }
        val response = api.post(ENDPOINT, request, "Cloud Vision")
        response["responses"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("fullTextAnnotation")?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            ?.takeIf { it.isNotBlank() }
    }

    /** The photo as base64 JPEG, shrunk so a page is about 300 dpi; a bigger upload only costs time. */
    private fun encode(uri: Uri): String? {
        val bitmap = decode(uri) ?: return null
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }.toByteArray()
        bitmap.recycle()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun decode(uri: Uri): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = max(info.size.width, info.size.height)
            if (longest > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longest
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
        }
    }.getOrNull()

    private companion object {
        const val ENDPOINT = "https://vision.googleapis.com/v1/images:annotate"
        const val MAX_SIDE = 2000
        const val JPEG_QUALITY = 85
    }
}
