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
import com.cookbookanything.core.domain.text.parseRecipeText
import com.cookbookanything.core.domain.video.isVideoPost
import com.cookbookanything.core.domain.video.videoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
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
    private val gemini: GeminiRecipeReader,
) {
    // In the folder the file provider already shares from.
    private val details get() = File(context.cacheDir, "shared/link-details.txt")

    init {
        // TikTok hands out its video file only with the cookies the page set; the cookie jar carries them over.
        if (CookieHandler.getDefault() == null) CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))
    }

    /** The recipe and the name of its downloaded picture, or null when the page has none. Throws [IOException] when it cannot load. */
    suspend fun read(url: String): Pair<ParsedRecipe, String?>? = withContext(Dispatchers.IO) {
        val page = load(url) ?: return@withContext null
        // A caption that only points at a blog ("full recipe at …") is read from the blog itself.
        val blog = page.link?.takeIf { page.ingredients.isEmpty() }?.let { runCatching { load(it) }.getOrNull() }?.takeIf { it.ingredients.isNotEmpty() }
        val written = blog?.let { it.copy(title = it.title ?: page.title, description = it.description ?: page.description, imageUrl = it.imageUrl ?: page.imageUrl) } ?: page
        // A reel whose caption gives no ingredients is listened to; the model writes the recipe down.
        val spoken = written.videoUrl?.takeIf { written.ingredients.isEmpty() && gemini.available }?.let { video -> listen(url, video) }
        val text = spoken?.copy(title = written.title ?: spoken.title, description = written.description ?: spoken.description, imageUrl = written.imageUrl) ?: written
        if (text.isEmpty) return@withContext null
        // The video to watch: the post itself when it is one, else the player the page embeds, else an Instagram post that carried a video.
        // shortcut: a short link (vm.tiktok.com) only counts when its page names the full address in og:url, pass the address after redirects if they turn up without a video.
        val recipe = text.copy(videoLink = url.takeIf(::isVideoPost) ?: text.videoLink ?: url.takeIf { text.videoUrl != null && videoPlayer(it) != null })
        val photo = recipe.imageUrl?.let { image ->
            runCatching { open(image).let { c -> try { c.inputStream.use { photoStore.importStream(it, MAX_IMAGE_BYTES) } } finally { c.disconnect() } } }.getOrNull()
        }
        recipe to photo
    }

    /** The recipe said in the video at [video], or null when it holds none or is too big to send. */
    private suspend fun listen(url: String, video: String): ParsedRecipe? {
        val connection = runCatching { open(video) }.getOrNull() ?: return null
        val (bytes, mimeType) = try {
            connection.inputStream.use { it.readUpTo(MAX_VIDEO_BYTES + 1) } to (connection.contentType?.substringBefore(';')?.trim() ?: "video/mp4")
        } finally {
            connection.disconnect()
        }
        if (bytes.size > MAX_VIDEO_BYTES) return null
        val text = try {
            gemini.read(listOf(bytes to mimeType), GeminiRecipeReader.VIDEO)
        } catch (e: IOException) {
            writeDetails("$url, video $video", Result.failure(e), null)
            throw e
        }
        return text?.let(::parseRecipeText)?.takeIf { it.ingredients.isNotEmpty() || it.steps.isNotEmpty() }
    }

    /** The recipe on the page, or null when it holds none. */
    private suspend fun load(url: String): ParsedRecipe? {
        // A plain request first, it is quick; sites that refuse it (403) or draw the recipe with
        // JavaScript get a second try through the phone's browser engine.
        val address = instagramEmbedUrl(url) ?: url
        val plain = runCatching { fetch(address) }
        val recipe = plain.getOrNull()?.let(::parseRecipeHtml)
        if (recipe != null) return recipe
        val rendered = browser.load(address) { parseRecipeHtml(it) != null }
        return rendered?.let(::parseRecipeHtml) ?: run {
            writeDetails("$url, read from $address", plain, rendered)
            plain.exceptionOrNull()?.let { throw it }
            null
        }
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
        /** The model takes a request of 20 MB; a reel is a few. */
        const val MAX_VIDEO_BYTES = 15 * 1024 * 1024
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    }
}
