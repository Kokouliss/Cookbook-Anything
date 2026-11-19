package com.cookbookanything.app.data.text

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.cookbookanything.app.BuildConfig
import com.cookbookanything.app.data.photos.PhotoStore
import com.cookbookanything.core.domain.text.ParsedRecipe
import com.cookbookanything.core.domain.text.instagramEmbedUrl
import com.cookbookanything.core.domain.text.parseRecipeHtml
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches the page behind a link and reads the recipe on it; recipe sites give an exact record,
 * Instagram (through its embed page), TikTok and Facebook give the caption and the picture or video thumbnail. Pages that
 * only show a login wall come back as "nothing found". The picture is stored as the recipe photo.
 */
@Singleton
class LinkRecipeReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoStore: PhotoStore,
    private val browser: BrowserPageLoader,
) {
    // In the folder the file provider already shares from.
    private val details get() = File(context.cacheDir, "shared/link-details.txt")

    /** The recipe and the name of its downloaded picture, or null when the page has none. Throws [IOException] when it cannot load. */
    suspend fun read(url: String): Pair<ParsedRecipe, String?>? = withContext(Dispatchers.IO) {
        // A plain request first, it is quick; sites that refuse it (403) or draw the recipe with
        // JavaScript get a second try through the phone's browser engine.
        val address = instagramEmbedUrl(url) ?: url
        val plain = runCatching { fetch(address) }
        var recipe = plain.getOrNull()?.let(::parseRecipeHtml)
        if (recipe == null) {
            val rendered = browser.load(address) { parseRecipeHtml(it) != null }
            recipe = rendered?.let(::parseRecipeHtml)
            if (recipe == null) {
                writeDetails("$url, read from $address", plain, rendered)
                plain.exceptionOrNull()?.let { throw it }
                return@withContext null
            }
        }
        val photo = recipe.imageUrl?.let { image ->
            runCatching { open(image).let { c -> try { c.inputStream.use { photoStore.importStream(it, MAX_IMAGE_BYTES) } } finally { c.disconnect() } } }.getOrNull()
        }
        recipe to photo
    }

    /** What the site answered to the last link that could not be read, as a file to send to the developer. Null when there is none. */
    fun detailsFile(): Uri? = details.takeIf { it.exists() }?.let { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) }

    private fun writeDetails(url: String, plain: Result<String>, rendered: String?) {
        // Best effort: a report that cannot be written must not change what the read answers.
        runCatching {
            details.parentFile?.mkdirs()
            details.writeText(
                buildString {
                    appendLine("Cookbook Anything ${BuildConfig.VERSION_NAME} on Android ${Build.VERSION.RELEASE}")
                    appendLine("Link: $url")
                    appendLine("Plain request: ${plain.fold({ "${it.length} characters" }, { "failed, $it" })}")
                    appendLine("Browser: ${rendered?.let { "${it.length} characters" } ?: "nothing"}")
                    appendLine("Each page below starts with a comment that holds the address it ended at.")
                    appendLine("\n===== plain request =====\n${plain.getOrNull().orEmpty()}")
                    appendLine("\n===== browser =====\n${rendered.orEmpty()}")
                },
            )
        }
    }

    private fun fetch(url: String): String {
        val connection = open(url)
        try {
            // shortcut: the charset comes from the header or is UTF-8, read the <meta charset> if an old Greek site comes out garbled
            val charset = connection.contentType?.substringAfter("charset=", "")?.substringBefore(';')?.trim()?.ifEmpty { null } ?: "UTF-8"
            // The address after redirects goes in front as a comment, for the details file.
            return "<!-- ${connection.url} -->\n" + String(connection.inputStream.use { it.readUpTo(MAX_PAGE_BYTES) }, charset(charset))
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
