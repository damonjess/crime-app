package com.dinner.crimeapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.toArgb

object MarkerIconFactory {
    private val cache = mutableMapOf<String, BitmapDrawable>()

    fun dotFor(context: Context, category: String, diameterDp: Int = 16): BitmapDrawable {
        return cache.getOrPut(category) {
            val color = CrimeCategoryColors.colorFor(category).toArgb()
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

            BitmapDrawable(context.resources, bitmap)
        }
    }
}
