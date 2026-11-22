package com.cookbookanything.app.data.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the recipe off photos of a page, a handwritten card or a screenshot with Gemini (see
 * [GeminiRecipeReader]), which reads Greek and handwriting; ML Kit on the phone has no Greek model.
 * Several photos of one long recipe go in one request so the model joins the pages.
 */
@Singleton
class PhotoTextReader @Inject constructor(@ApplicationContext private val context: Context, private val gemini: GeminiRecipeReader) {

    /** False in a build without the Firebase config file. */
    val available: Boolean get() = gemini.available

    /** The recipe on the photos as text, or null when they hold none. Throws [IOException] when the call fails. */
    suspend fun read(uris: List<Uri>): String? = withContext(Dispatchers.IO) {
        val photos = uris.map { (encode(it) ?: throw IOException("Could not open the image")) to "image/jpeg" }
        gemini.read(photos, GeminiRecipeReader.PHOTOS)
    }

    /** The photo as JPEG, shrunk so a page is about 300 dpi; a bigger upload only costs time. */
    private fun encode(uri: Uri): ByteArray? {
        val bitmap = decode(uri) ?: return null
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }.toByteArray()
        bitmap.recycle()
        return bytes
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
        const val MAX_SIDE = 2000
        const val JPEG_QUALITY = 85
    }
}
