package com.cookbookanything.app.data.share

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.cookbookanything.app.data.photos.PhotoStore
import com.cookbookanything.app.di.IoDispatcher
import com.cookbookanything.core.domain.model.Recipe
import com.cookbookanything.core.domain.share.RecipeShareCodec
import com.cookbookanything.core.domain.share.SharedRecipe
import com.cookbookanything.core.domain.share.toRecipe
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Turns a recipe into something that can leave the phone: a PDF for anyone, or a recipe file
 * (a zip holding recipe.json plus the photos) that another copy of the app can import.
 */
@Singleton
class RecipeSharer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoStore: PhotoStore,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun exportRecipeFile(recipe: Recipe): Uri = withContext(io) {
        val file = newSharedFile(recipe.title, RECIPE_FILE_EXTENSION)
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(RECIPE_ENTRY))
            zip.write(RecipeShareCodec.encode(recipe).toByteArray())
            zip.closeEntry()
            (recipe.photos + recipe.steps.flatMap { it.photos }).distinct().forEach { name ->
                val photo = photoStore.file(name)
                if (photo.exists()) {
                    zip.putNextEntry(ZipEntry(PHOTO_PREFIX + name))
                    photo.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        uriFor(file)
    }

    suspend fun exportPdf(recipe: Recipe, labels: PdfLabels): Uri = withContext(io) {
        val file = newSharedFile(recipe.title, "pdf")
        file.outputStream().use { RecipePdfWriter(photoStore::file).write(recipe, labels, it) }
        uriFor(file)
    }

    /**
     * Reads a recipe file and stores its photos. The result is a new, unsaved recipe, or null when
     * the file is not a recipe. The file comes from outside the app, so sizes are capped and entry
     * names are never used as paths.
     */
    suspend fun importRecipeFile(uri: Uri): Recipe? = withContext(io) {
        runCatching {
            var shared: SharedRecipe? = null
            val photoNames = mutableMapOf<String, String>()
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    var entries = 0
                    while (entries++ < MAX_ENTRIES) {
                        val entry = zip.nextEntry ?: break
                        when {
                            entry.name == RECIPE_ENTRY ->
                                shared = RecipeShareCodec.decode(zip.readText(MAX_JSON_BYTES))
                            entry.name.startsWith(PHOTO_PREFIX) ->
                                photoStore.importStream(zip, MAX_PHOTO_BYTES)?.let { stored ->
                                    photoNames[entry.name.removePrefix(PHOTO_PREFIX)] = stored
                                }
                        }
                    }
                }
            }
            shared?.toRecipe(
                newId = { UUID.randomUUID().toString() },
                photoNames = photoNames,
                now = System.currentTimeMillis(),
            )
        }.getOrNull()
    }

    private fun newSharedFile(title: String, extension: String): File {
        val directory = File(context.cacheDir, "shared")
        directory.deleteRecursively()
        directory.mkdirs()
        val safeTitle = title.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().take(60).ifBlank { "recipe" }
        return File(directory, "$safeTitle.$extension")
    }

    private fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun InputStream.readText(maxBytes: Int): String {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (bytes.size() <= maxBytes) {
            val read = read(buffer)
            if (read < 0) break
            bytes.write(buffer, 0, read)
        }
        // An oversized entry ends up truncated, which the decoder then rejects as invalid.
        return bytes.toString(Charsets.UTF_8.name())
    }

    companion object {
        const val RECIPE_FILE_EXTENSION = "cookbook"
        const val RECIPE_FILE_MIME_TYPE = "application/zip"
        const val PDF_MIME_TYPE = "application/pdf"

        private const val RECIPE_ENTRY = "recipe.json"
        private const val PHOTO_PREFIX = "photos/"
        private const val MAX_ENTRIES = 300
        private const val MAX_JSON_BYTES = 2 * 1024 * 1024
        private const val MAX_PHOTO_BYTES = 15L * 1024 * 1024
    }
}
