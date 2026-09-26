package com.dinner.crimeapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.toArgb

/**
 * Builds marker icons for the map:
 *  - [markerFor]      — emoji-in-circle category marker (normal / selected)
 *  - [clusterFor]     — count bubble used for hotspot clusters
 *  - [heatBlobFor]    — soft radial blob for heatmap mode
 * All results are cached by key so panning/zooming stays smooth.
 */
object MarkerIconFactory {

    private val markerCache = mutableMapOf<String, BitmapDrawable>()
    private val clusterCache = mutableMapOf<String, BitmapDrawable>()
    private val heatCache = mutableMapOf<String, BitmapDrawable>()

    fun markerFor(
        context: Context,
        category: String,
        selected: Boolean = false,
        diameterDp: Int = 22
    ): BitmapDrawable {
        val key = "$category-$selected-$diameterDp"
        return markerCache.getOrPut(key) {
            val argb = CrimeCategoryColors.colorFor(category).toArgb()
            buildMarker(context, argb, emoji = CrimeCategoryIcons.emojiFor(category), selected, diameterDp)
        }
    }

    fun plainDot(context: Context, colorArgb: Int, diameterDp: Int = 16): BitmapDrawable {
        val key = "dot-$colorArgb-$diameterDp"
        return markerCache.getOrPut(key) { buildMarker(context, colorArgb, null, false, diameterDp) }
    }

    private fun buildMarker(
        context: Context,
        colorArgb: Int,
        emoji: String?,
        selected: Boolean,
        diameterDp: Int
    ): BitmapDrawable {
        val density = context.resources.displayMetrics.density
        val size = (diameterDp * density).toInt() + if (selected) (6 * density).toInt() else 0
        val bitmap = Bitmap.createBitmap(size.coerceAtLeast(8), size.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f
        val ring = if (selected) 3f * density else 0f
        val radius = size / 2f - ring

        // Drop shadow for depth
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setShadowLayer(4f * density, 0f, 2f * density, Color.argb(90, 0, 0, 0))
            color = Color.TRANSPARENT
        }
        canvas.drawCircle(cx, cy, radius, shadow)

        // Fill
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorArgb }
        canvas.drawCircle(cx, cy, radius, fill)

        // White ring (or accent ring when selected)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (2f + ring) * density
            color = if (selected) Color.WHITE else Color.argb(235, 255, 255, 255)
        }
        canvas.drawCircle(cx, cy, radius - border.strokeWidth / 2f, border)

        // Emoji glyph centred in the bubble
        if (emoji != null && radius > 10f * density) {
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = radius * 1.05f
                typeface = Typeface.DEFAULT
            }
            val y = cy - (text.descent() + text.ascent()) / 2f
            canvas.drawText(emoji, cx, y, text)
        }

        return BitmapDrawable(context.resources, bitmap)
    }

    /** Count bubble for cluster/hotspot pins. */
    fun clusterFor(context: Context, count: Int, colorArgb: Int, diameterDp: Int = 30): BitmapDrawable {
        val key = "$count-$colorArgb-$diameterDp"
        return clusterCache.getOrPut(key) {
            val density = context.resources.displayMetrics.density
            val size = (diameterDp * density).toInt()
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val cx = size / 2f
            val radius = size / 2f - 1.5f * density

            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                setShadowLayer(5f * density, 0f, 2.5f * density, Color.argb(110, 0, 0, 0))
                color = Color.TRANSPARENT
            }
            canvas.drawCircle(cx, cx, radius, shadow)

            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorArgb }
            canvas.drawCircle(cx, cx, radius, fill)

            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f * density
                color = Color.WHITE
            }
            canvas.drawCircle(cx, cx, radius - border.strokeWidth / 2f, border)

            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = if (count < 100) radius * 0.95f else radius * 0.75f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val label = if (count > 999) "1k+" else count.toString()
            val y = cx - (text.descent() + text.ascent()) / 2f
            canvas.drawText(label, cx, y, text)

            BitmapDrawable(context.resources, bitmap)
        }
    }

    /** Soft radial gradient blob for heatmap mode. */
    fun heatBlobFor(context: Context, colorArgb: Int, diameterDp: Int = 44): BitmapDrawable {
        val key = "$colorArgb-$diameterDp"
        return heatCache.getOrPut(key) {
            val density = context.resources.displayMetrics.density
            val size = (diameterDp * density).toInt()
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    size / 2f, size / 2f, size / 2f,
                    intArrayOf(colorArgb, colorArgb and 0x00FFFFFF or (60 shl 24), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)
            BitmapDrawable(context.resources, bitmap)
        }
    }
}
