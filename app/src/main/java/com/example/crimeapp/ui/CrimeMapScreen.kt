package com.example.crimeapp.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.abs

@SuppressLint("MissingPermission")
@Composable
fun CrimeMapScreen(
    viewModel: CrimeMapViewModel = viewModel(),
    startLat: Double = 51.5074, // London default
    startLng: Double = -0.1278,
    onMapMoved: (Double, Double) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsState()

    // Key map movement to startLat/startLng changes
    val mapCenter = remember(startLat, startLng) { GeoPoint(startLat, startLng) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Centering only when startLat/startLng props change significantly (e.g. from parent)
    LaunchedEffect(mapCenter) {
        val map = mapViewRef ?: return@LaunchedEffect
        val currentCenter = map.mapCenter
        val latDiff = abs(currentCenter.latitude - mapCenter.latitude)
        val lngDiff = abs(currentCenter.longitude - mapCenter.longitude)
        
        // Only animate if the difference is larger than a small threshold
        // this prevents feedback loops during user scrolling
        if (latDiff > 0.001 || lngDiff > 0.001) {
            map.controller.animateTo(mapCenter)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadCrimes(startLat, startLng)
    }

    Column(Modifier.fillMaxSize()) {
        // Filter Area
        Column(Modifier.fillMaxWidth()) {
            // Month filter chips
            val months = state.crimesByMonth.keys.sortedDescending()
            LazyRowMonthPicker(
                months = months,
                selected = state.selectedMonth,
                onSelect = { viewModel.selectMonth(it) }
            )
        }

        Box(Modifier.weight(1f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx: Context ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(mapCenter)
                        
                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                onMapMoved(this@apply.mapCenter.latitude, this@apply.mapCenter.longitude)
                                return true
                            }
                            override fun onZoom(event: ZoomEvent?): Boolean = false
                        })
                        mapViewRef = this
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()
                    viewModel.visibleCrimes().forEach { crime ->
                        val lat = crime.location.latitude.toDoubleOrNull() ?: return@forEach
                        val lng = crime.location.longitude.toDoubleOrNull() ?: return@forEach
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(lat, lng)
                            title = crime.category.replace("-", " ")
                            snippet = "Month: ${crime.month} · Status: ${crime.outcomeStatus?.category ?: "Under investigation"}"
                        }
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                }
            )

            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }

        // Crime summary card
        CrimeSummaryCard(summary = viewModel.summary())

        if (state.error != null) {
            Text(
                "Error loading crimes: ${state.error}",
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun LazyRowMonthPicker(
    months: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All Months") }
            )
        }
        items(months) { month ->
            FilterChip(
                selected = selected == month,
                onClick = { onSelect(month) },
                label = { Text(month) }
            )
        }
    }
}
