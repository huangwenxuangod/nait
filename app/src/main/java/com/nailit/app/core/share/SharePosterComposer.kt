package com.nailit.app.core.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream

object SharePosterComposer {
    fun createPoster(
        context: Context,
        sourceBitmap: Bitmap,
        tryOnBitmap: Bitmap,
        finalBitmap: Bitmap,
        styleTitle: String,
        note: String = "先试戴，再决定要不要做",
    ): File {
        val width = 1080
        val height = 1920
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF8F3EE.toInt()
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF171311.toInt()
            textSize = 58f
            isFakeBoldText = true
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF756A62.toInt()
            textSize = 32f
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8C7F76.toInt()
            textSize = 26f
            isFakeBoldText = true
        }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2B1D1A.toInt()
        }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFCF8.toInt()
        }

        canvas.drawRoundRect(RectF(48f, 48f, width - 48f, height - 48f), 42f, 42f, cardPaint)

        drawTextBlock(canvas, "今天真的把这个款做出来了", 88f, 124f, width - 176, titlePaint)
        drawTextBlock(canvas, styleTitle, 88f, 210f, width - 176, bodyPaint)

        val finalRect = RectF(88f, 300f, width - 88f, 1120f)
        drawBitmapCover(canvas, finalBitmap, finalRect, 34f)
        drawBadge(canvas, "最终成品", finalRect.left + 28f, finalRect.top + 28f, badgePaint)

        val smallTop = 1180f
        val gap = 28f
        val smallWidth = (width - 176f - gap) / 2f
        val sourceRect = RectF(88f, smallTop, 88f + smallWidth, smallTop + 420f)
        val tryOnRect = RectF(sourceRect.right + gap, smallTop, width - 88f, smallTop + 420f)
        drawBitmapCover(canvas, sourceBitmap, sourceRect, 28f)
        drawBitmapCover(canvas, tryOnBitmap, tryOnRect, 28f)
        drawImageLabel(canvas, "原图", sourceRect, labelPaint)
        drawImageLabel(canvas, "试戴图", tryOnRect, labelPaint)

        drawTextBlock(canvas, note, 88f, 1660f, width - 176, titlePaint.apply { textSize = 42f })
        drawTextBlock(canvas, "Nail-It AI 试戴", 88f, 1748f, width - 176, bodyPaint)

        val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(shareDir, "nailit-share-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    private fun drawBitmapCover(canvas: Canvas, bitmap: Bitmap, rect: RectF, radius: Float) {
        val save = canvas.save()
        val path = android.graphics.Path().apply {
            addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(path)

        val scale = maxOf(rect.width() / bitmap.width, rect.height() / bitmap.height)
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        val left = rect.left + (rect.width() - scaledWidth) / 2f
        val top = rect.top + (rect.height() - scaledHeight) / 2f
        val dest = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(bitmap, null, dest, null)
        canvas.restoreToCount(save)
    }

    private fun drawBadge(canvas: Canvas, text: String, left: Float, top: Float, paint: Paint) {
        val rect = RectF(left, top, left + 190f, top + 72f)
        canvas.drawRoundRect(rect, 999f, 999f, paint)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 28f
            isFakeBoldText = true
        }
        canvas.drawText(text, left + 28f, top + 46f, textPaint)
    }

    private fun drawImageLabel(canvas: Canvas, text: String, rect: RectF, paint: TextPaint) {
        canvas.drawText(text, rect.left, rect.bottom + 42f, paint)
    }

    private fun drawTextBlock(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        width: Int,
        paint: TextPaint,
    ) {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
    }
}
