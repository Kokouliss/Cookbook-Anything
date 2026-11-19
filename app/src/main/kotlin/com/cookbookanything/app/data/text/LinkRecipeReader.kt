package com.cookbookanything.app.data.text

import com.cookbookanything.app.data.photos.PhotoStore
import com.cookbookanything.core.domain.text.ParsedRecipe
import com.cookbookanything.core.domain.text.parseRecipeHtml
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches the page behind a link and reads the recipe on it; recipe sites give an exact record,
 * Instagram, TikTok and Facebook give the caption and the picture or video thumbnail. Pages that
 * only show a login wall come back as "nothing found". The picture is stored as the recipe photo.
 */
@Singleton
class LinkRecipeReader @Inject constructor(
    private val photoStore: PhotoStore,
    private val browser: BrowserPageLoader,
) {

    /** The recipe and the name of its downloaded picture, or null when the page has none. Throws [IOException] when it cannot load. */
    suspend fun read(url: String): Pair<ParsedRecipe, String?>? = withContext(Dispatchers.IO) {
        // A plain request first, it is quick; sites that refuse it (403) or draw the recipe with
        // JavaScript get a second try through the phone's browser engine.
        val plain = runCatching { fetch(url) }
        val recipe = plain.getOrNull()?.let(::parseRecipeHtml)
            ?: browser.load(url) { parseRecipeHtml(it) != null }?.let(::parseRecipeHtml)
            ?: plain.exceptionOrNull()?.let { throw it }
            ?: return@withContext null
        val photo = recipe.imageUrl?.let { image ->
            runCatching { open(image).let { c -> try { c.inputStream.use { photoStore.importStream(it, MAX_IMAGE_BYTES) } } finally { c.disconnect() } } }.getOrNull()
        }
        recipe to photo
    }

    private fun fetch(url: String): String {
        val connection = open(url)
        try {
            // shortcut: the charset comes from the header or is UTF-8, read the <meta charset> if an old Greek site comes out garbled
            val charset = connection.contentType?.substringAfter("charset=", "")?.substringBefore(';')?.trim()?.ifEmpty { null } ?: "UTF-8"
            return String(connection.inputStream.use { it.readUpTo(MAX_PAGE_BYTES) }, charset(charset))
        } finally {
            connection.disconnect()
        }
    }

    // Redirects are followed by hand, because Java stops at a change of scheme and short links (vm.tiktok.com) change it.
    private fun open(url: String): HttpURLConnection {
        var current = url
        repeat(MAX_REDIRECTS) {
            val connection = URL(current).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            // Social sites hand bare clients a login page, a phone browser gets the post.
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept-Language", "el,en;q=0.8")
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location") ?: throw IOException("Redirect without a target")
                connection.disconnect()
                current = URL(URL(current), location).toString()
                return@repeat
            }
            if (code >= 400) {
                connection.disconnect()
                throw IOException("The page answered $code")
            }
            return connection
        }
        throw IOException("Too many redirects")
    }

    private fun java.io.InputStream.readUpTo(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (output.size() < limit) {
            val read = read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private companion object {
        const val TIMEOUT_MS = 20_000
        const val MAX_REDIRECTS = 6
        const val MAX_PAGE_BYTES = 4 * 1024 * 1024
        const val MAX_IMAGE_BYTES = 6L * 1024 * 1024
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    }
}
