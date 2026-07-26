package com.dinner.crimeapp.ui

import android.util.Log
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.dinner.crimeapp.data.Crime

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun CrimeMapScreen(
    viewModel: CrimeMapViewModel = viewModel(),
    startLat: Double = 51.5074,
    startLng: Double = -0.1278
) {
    Log.e("CrimeMapScreen", "Composing CrimeMapScreen: startLat=$startLat, startLng=$startLng")
    val state by viewModel.state.collectAsState()
    val mapCenter = remember(startLat, startLng) { GeoPoint(startLat, startLng) }

    // Tracks where crimes were last fetched for
    var loadedCenter by remember { mutableStateOf(mapCenter) }
    // Tracks where the user has currently panned to
    var pendingCenter by remember { mutableStateOf<GeoPoint?>(null) }
    
    // Tracks the last loaded center we actually moved the map to
    var lastMovedCenter by remember { mutableStateOf(loadedCenter) }

    var selectedCrime by remember { mutableStateOf<Crime?>(null) }

    val visibleCrimes = remember(state.crimesByMonth, state.selectedMonth, state.selectedCategory) {
        viewModel.visibleCrimes()
    }

    LaunchedEffect(startLat, startLng) {
        viewModel.loadCrimes(startLat, startLng)
        loadedCenter = mapCenter
        pendingCenter = null
    }

    Column(Modifier.fillMaxSize()) {
        // Force the month picker to always draw ABOVE the map, regardless of
        // the native MapView's own compositing layer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(2f),
            tonalElevation = 2.dp
        ) {
            val months = state.crimesByMonth.keys.sortedDescending()
            LazyRowMonthPicker(
                months = months,
                selected = state.selectedMonth,
                onSelect = { viewModel.selectMonth(it) }
            )
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds() // hard-clip the map to its own box
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx: Context ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(mapCenter)

                        // Stop the edge-glow overscroll effect from
                        // drawing outside the view's own bounds while dragging
                        overScrollMode = android.view.View.OVER_SCROLL_NEVER

                        // Fix: prevent Compose from intercepting touch events mid-drag,
                        // which otherwise causes the map to jump instead of pan smoothly
                        setOnTouchListener { view, event ->
                            when (event.action) {
                                android.view.MotionEvent.ACTION_DOWN -> {
                                    view.parent.requestDisallowInterceptTouchEvent(true)
                                }
                                android.view.MotionEvent.ACTION_UP,
                                android.view.MotionEvent.ACTION_CANCEL -> {
                                    view.parent.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                            false // let the MapView still process the touch normally
                        }

                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                pendingCenter = this@apply.mapCenter as GeoPoint
                                return true
                            }
                            override fun onZoom(event: ZoomEvent?): Boolean = false
                        })
                    }
                },
                update = { mapView ->
                    // Move map only when loadedCenter changes (e.g. from "Use my location" 
                    // or "Search this area" after fetching new data)
                    if (loadedCenter != lastMovedCenter) {
                        mapView.controller.animateTo(loadedCenter)
                        lastMovedCenter = loadedCenter
                    }

                    // Only rebuild markers if the crime data has actually changed
                    if (mapView.tag != visibleCrimes) {
                        mapView.overlays.removeAll { it is Marker }
                        visibleCrimes.forEach { crime ->
                            val lat = crime.location.latitude.toDoubleOrNull() ?: return@forEach
                            val lng = crime.location.longitude.toDoubleOrNull() ?: return@forEach
                            val marker = Marker(mapView).apply {
                                position = GeoPoint(lat, lng)
                                icon = MarkerIconFactory.dotFor(mapView.context, crime.category)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER) // center dot, not pin-style anchor
                                title = CrimeCategoryColors.displayName(crime.category)
                                snippet = "Month: ${crime.month} · Status: ${crime.outcomeStatus?.category ?: "Under investigation"}"
                                setOnMarkerClickListener { _, _ ->
                                    selectedCrime = crime
                                    true // consume the click, don't show the default InfoWindow bubble
                                }
                            }
                            mapView.overlays.add(marker)
                        }
                        mapView.invalidate()
                        mapView.tag = visibleCrimes
                    }
                }
            )

            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            // "Search this area" button — only show once panned far enough
            pendingCenter?.let { center ->
                val movedFar = distanceMeters(
                    loadedCenter.latitude, loadedCenter.longitude,
                    center.latitude, center.longitude
                ) > 400 // meters threshold

                if (movedFar) {
                    Button(
                        onClick = {
                            viewModel.loadCrimes(center.latitude, center.longitude)
                            loadedCenter = center
                            pendingCenter = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        Text("Search this area")
                    }
                }
            }
        }

        CrimeSummaryCard(
            summary = viewModel.summary(),
            modifier = Modifier.navigationBarsPadding()
        )

        if (state.error != null) {
            Text(
                "Error loading crimes: ${state.error}",
                modifier = Modifier.padding(8.dp).navigationBarsPadding(),
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    selectedCrime?.let { crime ->
        LaunchedEffect(crime.persistentId) {
            viewModel.loadOutcomeHistory(crime.persistentId)
        }
        ModalBottomSheet(
            onDismissRequest = {
                selectedCrime = null
                viewModel.clearOutcomeHistory()
            }
        ) {
            CrimeDetailContent(crime, viewModel)
        }
    }
}

/** Simple haversine distance in meters between two lat/lng points. */
private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadius = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadius * c
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
