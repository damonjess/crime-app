package com.dinner.crimeapp.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Soft additive "heatmap" rendered as radial-gradient blobs per incident.
 * No dependencies: drawn directly on the map canvas each frame.
 */
class HeatmapOverlay(
    private val points: List<Pair<Double, Double>>
) : Overlay() {

    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Additive-ish feel: lighter colour where blobs overlap
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.ADD)
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || points.isEmpty()) return
        val projection = mapView.projection
        val radiusPx = 34f * mapView.resources.displayMetrics.density

        val screen = android.graphics.Point()
        for ((lat, lng) in points) {
            projection.toPixels(GeoPoint(lat, lng), screen)
            // Cull blobs far outside the viewport
            if (screen.x < -radiusPx || screen.x > canvas.width + radiusPx ||
                screen.y < -radiusPx || screen.y > canvas.height + radiusPx
            ) continue

            blobPaint.shader = RadialGradient(
                screen.x.toFloat(), screen.y.toFloat(), radiusPx,
                intArrayOf(
                    Color.argb(150, 255, 96, 64),
                    Color.argb(70, 255, 160, 64),
                    Color.argb(0, 255, 180, 64)
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(screen.x.toFloat(), screen.y.toFloat(), radiusPx, blobPaint)
        }
    }
}

/**
 * Grid-based clustering: aggregates nearby points into numbered bubbles that
 * grow with count and zoom in when tapped. Far cheaper than per-point markers
 * on dense datasets.
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
        val zoom = mapView.zoomLevelDouble
        // Grid cell shrinks as you zoom in for tighter clusters
        val cellPx = (70f * mapView.resources.displayMetrics.density).toDouble()

        val clusters = HashMap<Long, Cluster>()
        val scratch = android.graphics.Point()

        for ((lat, lng) in points) {
            projection.toPixels(GeoPoint(lat, lng), scratch)
            if (scratch.x < -100 || scratch.y < -100 ||
                scratch.x > canvas.width + 100 || scratch.y > canvas.height + 100
            ) continue

            val gx = floor(scratch.x / cellPx).toLong()
            val gy = floor(scratch.y / cellPx).toLong()
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
            val radius = (16 + sqrt(count.toDouble()) * 5).toFloat() * density

            // Colour shifts from teal (few) to coral (many)
            val heat = (count / 25f).coerceIn(0f, 1f)
            circlePaint.color = Color.rgb(
                (60 + heat * 195).toInt(),
                (190 - heat * 120).toInt(),
                (170 - heat * 100).toInt()
            )

            canvas.drawCircle(scratch.x.toFloat(), scratch.y.toFloat(), radius, shadowPaint)
            canvas.drawCircle(scratch.x.toFloat(), scratch.y.toFloat(), radius, circlePaint)
            canvas.drawCircle(scratch.x.toFloat(), scratch.y.toFloat(), radius, ringPaint)

            textPaint.textSize = radius * (if (count < 100) 0.85f else 0.7f)
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
        // Find nearest cluster whose bubble contains the tap
        var best: Cluster? = null
        var bestDist = Float.MAX_VALUE
        val scratch = android.graphics.Point()
        for (cluster in tapCache) {
            projection.toPixels(GeoPoint(cluster.lat, cluster.lng), scratch)
            val radius = (16 + sqrt(cluster.count.toDouble()) * 5).toFloat() * density
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
