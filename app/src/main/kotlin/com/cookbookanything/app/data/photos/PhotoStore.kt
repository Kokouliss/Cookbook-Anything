package com.cookbookanything.app.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import com.cookbookanything.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

fun photoDirectory(context: Context): File = File(context.filesDir, "photos")

/** Keeps recipe photos as JPEG files in app-private storage. Photos are referred to by file name. */
@Singleton
class PhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val directory: File
        get() = photoDirectory(context).apply { mkdirs() }

    fun file(name: String): File = File(directory, name)

    /** Copies the picked image in, downscaled so photos stay light to store, share and sync. */
    suspend fun import(uri: Uri): String? = withContext(io) {
        runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_SIDE_PX) {
                    val scale = MAX_SIDE_PX.toFloat() / longest
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val name = newName()
            file(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            bitmap.recycle()
            name
        }.getOrNull()
    }

    /** Stores a photo read from a shared recipe file. Returns null when it exceeds [maxBytes]. */
    suspend fun importStream(input: InputStream, maxBytes: Long): String? = withContext(io) {
        val name = newName()
        val target = file(name)
        var total = 0L
        target.outputStream().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) break
                output.write(buffer, 0, read)
            }
        }
        if (total > maxBytes) {
            target.delete()
            null
        } else {
            name
        }
    }

    /** A temporary file the camera app can write a new picture into. */
    fun newCameraTarget(): Uri {
        val file = File(File(context.cacheDir, "camera").apply { mkdirs() }, "${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Deletes stored photos that no recipe refers to any more. Files newer than [olderThan] are kept,
     * because they may belong to a recipe that is being edited and not saved yet.
     */
    suspend fun sweepOrphans(referenced: Set<String>, olderThan: Long) = withContext(io) {
        directory.listFiles()
            ?.filter { it.name !in referenced && it.lastModified() < olderThan }
            ?.forEach { it.delete() }
        File(context.cacheDir, "camera").listFiles()
            ?.filter { it.lastModified() < olderThan }
            ?.forEach { it.delete() }
        Unit
    }

    private fun newName() = "${UUID.randomUUID()}.jpg"

    private companion object {
        const val MAX_SIDE_PX = 1600
        const val JPEG_QUALITY = 85
    }
}
