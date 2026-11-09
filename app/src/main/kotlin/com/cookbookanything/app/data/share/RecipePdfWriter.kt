package com.cookbookanything.app.data.share

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import com.cookbookanything.core.domain.model.Recipe
import java.io.File
import java.io.OutputStream

/** Section headings, passed in so the PDF follows the language the user picked in the app. */
data class PdfLabels(val ingredients: String, val steps: String)

/** Lays a recipe out on A4 pages: title, cover photo, ingredients, then numbered steps with photos. */
class RecipePdfWriter(private val photoFile: (String) -> File) {

    fun write(recipe: Recipe, labels: PdfLabels, output: OutputStream) {
        val document = PdfDocument()
        try {
            val pages = PageCursor(document)
            pages.text(recipe.title, TITLE, spacingAfter = 12f)
            recipe.photos.firstOrNull()?.let { pages.photo(photoFile(it), maxHeight = 260f) }

            if (recipe.ingredients.isNotEmpty()) {
                pages.text(labels.ingredients, HEADING, spacingAfter = 6f)
                recipe.ingredients.forEach { pages.text("•  ${it.text}", BODY, spacingAfter = 4f) }
                pages.gap(12f)
            }
            if (recipe.steps.isNotEmpty()) {
                pages.text(labels.steps, HEADING, spacingAfter = 6f)
                recipe.steps.forEachIndexed { index, step ->
                    pages.text("${index + 1}.  ${step.text}", BODY, spacingAfter = 6f)
                    step.photos.forEach { pages.photo(photoFile(it), maxHeight = 170f) }
                }
            }
            pages.finish()
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private class PageCursor(private val document: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y = MARGIN

        /** The canvas to draw a block of [height] on, starting a new page when it does not fit. */
        private fun canvasFor(height: Float): Canvas {
            val current = page
            val fits = y + height <= PAGE_HEIGHT - MARGIN
            // shortcut: a block taller than a whole page is drawn anyway and gets cut off, split long text if that ever shows up
            if (current != null && (fits || y == MARGIN)) return current.canvas
            current?.let(document::finishPage)
            pageNumber += 1
            val next = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
            )
            page = next
            y = MARGIN
            return next.canvas
        }

        fun text(text: String, paint: TextPaint, spacingAfter: Float) {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, CONTENT_WIDTH).build()
            val canvas = canvasFor(layout.height.toFloat())
            canvas.save()
            canvas.translate(MARGIN, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + spacingAfter
        }

        fun photo(file: File, maxHeight: Float) {
            // Half resolution keeps the PDF small; PdfDocument stores bitmaps uncompressed.
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return
            val scale = minOf(CONTENT_WIDTH.toFloat() / bitmap.width, maxHeight / bitmap.height)
            val width = bitmap.width * scale
            val height = bitmap.height * scale
            val canvas = canvasFor(height)
            canvas.drawBitmap(bitmap, null, RectF(MARGIN, y, MARGIN + width, y + height), BITMAP)
            bitmap.recycle()
            y += height + 10f
        }

        fun gap(height: Float) {
            y += height
        }

        fun finish() {
            page?.let(document::finishPage)
            page = null
        }
    }

    private companion object {
        // A4 in PostScript points.
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
        const val CONTENT_WIDTH = 515

        val TITLE = textPaint(size = 24f, bold = true)
        val HEADING = textPaint(size = 15f, bold = true)
        val BODY = textPaint(size = 12f, bold = false)
        val BITMAP = Paint(Paint.FILTER_BITMAP_FLAG)

        fun textPaint(size: Float, bold: Boolean) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            color = Color.BLACK
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }
}
