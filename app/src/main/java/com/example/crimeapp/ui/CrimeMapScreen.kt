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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@SuppressLint("MissingPermission")
@Composable
fun CrimeMapScreen(
    viewModel: CrimeMapViewModel = viewModel(),
    startLat: Double = 51.5074, // London default
    startLng: Double = -0.1278
) {
    val state by viewModel.state.collectAsState()

    // Key map movement to startLat/startLng changes
    val mapCenter = remember(startLat, startLng) { GeoPoint(startLat, startLng) }

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
                    }
                },
                update = { mapView ->
                    // Handle map centering when startLat/startLng changes externally
                    if (mapView.mapCenter.latitude != mapCenter.latitude || 
                        mapView.mapCenter.longitude != mapCenter.longitude) {
                        mapView.controller.animateTo(mapCenter)
                    }

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
