package com.dinner.crimeapp.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Soft additive "heatmap" rendered as radial-gradient blobs per incident.
 * Pre-groups points per coordinate to avoid additive white blowout on duplicated points.
 */
class HeatmapOverlay(
    private val points: List<Pair<Double, Double>>
) : Overlay() {

    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.ADD)
    }

    // Pre-group the points. If a street has 150 crimes, we draw ONE blob scaled by 150, not 150 blobs.
    private val groupedPoints = points.groupingBy { it }.eachCount()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || groupedPoints.isEmpty()) return
        val projection = mapView.projection
        val radiusPx = 28f * mapView.resources.displayMetrics.density

        val screen = android.graphics.Point()
        for ((point, count) in groupedPoints) {
            projection.toPixels(GeoPoint(point.first, point.second), screen)
            
            if (screen.x < -radiusPx || screen.x > canvas.width + radiusPx ||
                screen.y < -radiusPx || screen.y > canvas.height + radiusPx
            ) continue

            // Scale the core opacity based on how many crimes are here (max out at 200 so it never goes pure white)
            val coreAlpha = (20 + (count * 8)).coerceAtMost(200)
            val edgeAlpha = (coreAlpha / 3).coerceAtMost(70)

            blobPaint.shader = RadialGradient(
                screen.x.toFloat(), screen.y.toFloat(), radiusPx,
                intArrayOf(
                    Color.argb(coreAlpha, 255, 60, 30),
                    Color.argb(edgeAlpha, 255, 140, 40),
                    Color.argb(0, 255, 200, 60)
                ),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(screen.x.toFloat(), screen.y.toFloat(), radiusPx, blobPaint)
        }
    }
}

/**
 * Grid-based clustering: aggregates nearby points into numbered bubbles that
 * grow with count and zoom in when tapped.
 */
class HotspotClusterOverlay(
    private val points: List<Pair<Double, Double>>,
    private val onClusterTap: (lat: Double, lng: Double, count: Int) -> Boolean
) : Overlay() {

    private val tapCache = mutableListOf<Cluster>()
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        setShadowLayer(10f, 0f, 3f, Color.argb(110, 0, 0, 0))
        color = Color.TRANSPARENT
    }

    private data class Cluster(val lat: Double, val lng: Double, val count: Int)

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection
        
        // Increased from 70f to 95f to group a wider area and prevent bubble overlap
        val cellPx = (95f * mapView.resources.displayMetrics.density).toDouble()

        val clusters = HashMap<Long, Cluster>()
        val scratch = android.graphics.Point()

        for ((lat, lng) in points) {
            projection.toPixels(GeoPoint(lat, lng), scratch)
            if (scratch.x < -100 || scratch.y < -100 ||
                scratch.x > canvas.width + 100 || scratch.y > canvas.height + 100
            ) continue

            val gx = kotlin.math.floor(scratch.x / cellPx).toLong()
            val gy = kotlin.math.floor(scratch.y / cellPx).toLong()
            val key = gx * 100003L + gy
            val existing = clusters[key]
            if (existing == null) {
                clusters[key] = Cluster(lat, lng, 1)
            } else {
                clusters[key] = Cluster(existing.lat, existing.lng, existing.count + 1)
            }
        }

        tapCache.clear()
        val density = mapView.resources.displayMetrics.density
        for (cluster in clusters.values) {
            projection.toPixels(GeoPoint(cluster.lat, cluster.lng), scratch)
            val count = cluster.count
            
            // Reduced base size and strict cap at 30dp
            val radiusDp = (12f + (Math.log10(count.toDouble()) * 8f).toFloat()).coerceAtMost(30f)
            val radius = radiusDp * density

            val heat = (count / 25f).coerceIn(0f, 1f)
            circlePaint.color = Color.rgb(
                (60 + heat * 195).toInt(),
                (190 - heat * 120).toInt(),
                (170 - heat * 100).toInt()
            )

            canvas.drawCircle(scratch.x.toFloat(), scratch.y.toFloat(), radius, shadowPaint)
            canvas.drawCircle(scratch.x.toFloat(), scratch.y.toFloat(), radius, circlePaint)
            canvas.drawCircle(scratch.x.toFloat(), scratch.y.toFloat(), radius, ringPaint)

            textPaint.textSize = radius * (if (count < 100) 0.85f else 0.60f)
            val y = scratch.y - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(
                if (count > 999) "1k+" else count.toString(),
                scratch.x.toFloat(), y, textPaint
            )

            tapCache.add(cluster)
        }
    }

    override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
        val projection = mapView.projection
        val density = mapView.resources.displayMetrics.density
        val px = e.x
        val py = e.y
        var best: Cluster? = null
        var bestDist = Float.MAX_VALUE
        val scratch = android.graphics.Point()
        
        for (cluster in tapCache) {
            projection.toPixels(GeoPoint(cluster.lat, cluster.lng), scratch)
            val radiusDp = (12f + (Math.log10(cluster.count.toDouble()) * 8f).toFloat()).coerceAtMost(30f)
            val radius = radiusDp * density
            
            val dx = scratch.x - px
            val dy = scratch.y - py
            val d = dx * dx + dy * dy
            if (d <= radius * radius && d < bestDist) {
                bestDist = d
                best = cluster
            }
        }
        return best?.let { onClusterTap(it.lat, it.lng, it.count) } ?: false
    }
}
