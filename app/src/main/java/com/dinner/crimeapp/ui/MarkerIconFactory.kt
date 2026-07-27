package com.dinner.crimeapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.toArgb

object MarkerIconFactory {
    private val categoryCache = mutableMapOf<String, BitmapDrawable>()
    private val colorCache = mutableMapOf<Int, BitmapDrawable>()

    fun dotFor(context: Context, category: String, diameterDp: Int = 16): BitmapDrawable {
        return categoryCache.getOrPut(category) {
            createDot(context, CrimeCategoryColors.colorFor(category).toArgb(), diameterDp)
        }
    }

    fun createCustomDot(context: Context, color: Int, diameterDp: Int = 16): BitmapDrawable {
        return colorCache.getOrPut(color) {
            createDot(context, color, diameterDp)
        }
    }

    private fun createDot(context: Context, color: Int, diameterDp: Int): BitmapDrawable {
        val density = context.resources.displayMetrics.density
        val size = (diameterDp * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = size / 2f

        val fill = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            this.color = color
        }
        canvas.drawCircle(radius, radius, radius - 2f, fill)

        val border = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            this.color = android.graphics.Color.WHITE
            strokeWidth = 2f
        }
        canvas.drawCircle(radius, radius, radius - 2f, border)

        return BitmapDrawable(context.resources, bitmap)
    }
}
