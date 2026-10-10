package com.dinner.crimeapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dinner.crimeapp.data.Crime
import com.dinner.crimeapp.data.MapillaryClient
import com.dinner.crimeapp.data.RoadAccidentEntity
import com.dinner.crimeapp.data.StopSearch
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrimeMapScreen(
    viewModel: CrimeMapViewModel = viewModel(),
    startLat: Double = 51.5074,
    startLng: Double = -0.1278,
    locationNonce: Long = 0L,
    onLocationChanged: (Double, Double) -> Unit = { _, _ -> },
    onRequestMyLocation: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var animateTarget by remember { mutableStateOf<GeoPoint?>(GeoPoint(startLat, startLng)) }
    var loadedCenter by remember { mutableStateOf(GeoPoint(startLat, startLng)) }
    var pendingCenter by remember { mutableStateOf<GeoPoint?>(null) }

    var selectedCrime by remember { mutableStateOf<Crime?>(null) }
    var selectedStop by remember { mutableStateOf<StopSearch?>(null) }
    var selectedAccident by remember { mutableStateOf<RoadAccidentEntity?>(null) }
    var streetImageUrl by remember { mutableStateOf<String?>(null) }
    var isImageLoading by remember { mutableStateOf(false) }

    var clusterSheetOpen by remember { mutableStateOf<List<Crime>?>(null) }

    // Layer mode: dots, heat, or clusters
    var layerMode by rememberSaveable { mutableStateOf(MapLayerMode.DOTS) }
    var layersSheetOpen by remember { mutableStateOf(false) }
    var legendSheetOpen by remember { mutableStateOf(false) }

    val visibleCrimes = remember(state.crimesByMonth, state.selectedMonth, state.selectedCategory, state.viewMode) {
        viewModel.visibleCrimes()
    }
    val visibleStops = remember(state.stopSearchesByMonth, state.selectedMonth, state.viewMode) {
        viewModel.visibleStopSearches()
    }

    val boundary by viewModel.boundary.collectAsState()

    // Capture theme colours outside AndroidView (update {} is not composable)
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryArgb = MaterialTheme.colorScheme.secondary.toArgb()
    val boundaryFillArgb = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f).toArgb()

    // Search selection updates the shared location through the parent
    var searchLat by remember { mutableStateOf(startLat) }
    var searchLng by remember { mutableStateOf(startLng) }

    LaunchedEffect(startLat, startLng, locationNonce) {
        searchLat = startLat
        searchLng = startLng
        viewModel.loadCrimes(startLat, startLng)
        val target = GeoPoint(startLat, startLng)
        loadedCenter = target
        animateTarget = target
        pendingCenter = null
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(state.viewMode) {
        if (state.viewMode == ViewMode.ACCIDENTS) {
            mapViewRef?.let { viewModel.loadAccidentsForVisibleArea(it) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Search + filters
        Surface(tonalElevation = 1.dp) {
            Column {
                PlaceSearchBar(
                    viewModel = viewModel,
                    onPlaceSelected = { lat, lng, _ ->
                        searchLat = lat
                        searchLng = lng
                        onLocationChanged(lat, lng)
                        val target = GeoPoint(lat, lng)
                        loadedCenter = target
                        animateTarget = target
                        pendingCenter = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp, bottom = 2.dp) // Tightened padding
                )

                // Merged Toggle and Category row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(Modifier.width(180.dp)) {
                        SegmentedButton(
                            selected = state.viewMode == ViewMode.CRIMES,
                            onClick = { viewModel.setViewMode(ViewMode.CRIMES) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("Crimes") }
                        SegmentedButton(
                            selected = state.viewMode == ViewMode.STOP_SEARCH,
                            onClick = { viewModel.setViewMode(ViewMode.STOP_SEARCH) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("Stops") }
                    }

                    FilterChip(
                        selected = state.viewMode == ViewMode.ACCIDENTS,
                        onClick = { viewModel.setViewMode(ViewMode.ACCIDENTS) },
                        label = { Text("Road Safety") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )

                    if (state.viewMode == ViewMode.CRIMES) {
                        CategoryFilterDropdown(
                            categories = state.categories,
                            selected = state.selectedCategory,
                            onSelect = { viewModel.selectCategory(it) }
                        )
                    }
                }

                LazyRowMonthPicker(
                    months = state.crimesByMonth.keys.sortedDescending(),
                    selected = state.selectedMonth,
                    onSelect = { viewModel.selectMonth(it) }
                )
            }
        }

    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .clipToBounds()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx: Context ->
                MapView(ctx).apply {
                    mapViewRef = this
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(mapCenter)
                    overScrollMode = android.view.View.OVER_SCROLL_NEVER

                    // Smooth panning inside Compose
                    setOnTouchListener { view, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN ->
                                view.parent.requestDisallowInterceptTouchEvent(true)
                            android.view.MotionEvent.ACTION_UP,
                            android.view.MotionEvent.ACTION_CANCEL ->
                                view.parent.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            val c = this@apply.mapCenter
                            pendingCenter = GeoPoint(c.latitude, c.longitude)
                            return true
                        }
                        override fun onZoom(event: ZoomEvent?): Boolean = false
                    })
                }
            },
            update = { mapView ->
                animateTarget?.let { target ->
                    mapView.controller.animateTo(target)
                    animateTarget = null
                }

                val dataHash = buildString {
                    append(state.viewMode).append('|')
                    append(state.selectedMonth).append('|')
                    append(state.selectedCategory).append('|')
                    append(layerMode).append('|')
                    append(visibleCrimes.size).append('|')
                    append(visibleStops.size).append('|')
                    append(state.visibleAccidents.size).append('|')
                    append(boundary.size)
                }.hashCode()

                if (mapView.tag != dataHash) {
                    mapView.tag = dataHash
                    mapView.overlays.removeAll {
                        it is Marker || it is Polygon || it is HotspotClusterOverlay || it is HeatmapOverlay
                    }

                    // Neighbourhood boundary outline
                    if (boundary.size > 2) {
                        val polygon = Polygon(mapView).apply {
                            points = boundary.map { GeoPoint(it.first, it.second) }
                            outlinePaint.strokeWidth = 5f
                            outlinePaint.color = primaryArgb
                            outlinePaint.isAntiAlias = true
                            outlinePaint.style = Paint.Style.STROKE
                            outlinePaint.pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
                            outlinePaint.alpha = 180
                            fillPaint.style = Paint.Style.FILL
                            fillPaint.color = boundaryFillArgb
                        }
                        mapView.overlays.add(polygon)
                    }

                    when {
                        // 1. Heatmap
                        state.viewMode == ViewMode.CRIMES && layerMode == MapLayerMode.HEAT -> {
                            mapView.overlays.add(
                                HeatmapOverlay(
                                    visibleCrimes.mapNotNull { c ->
                                        val lat = c.location.latitude.toDoubleOrNull() ?: return@mapNotNull null
                                        val lng = c.location.longitude.toDoubleOrNull() ?: return@mapNotNull null
                                        lat to lng
                                    }
                                )
                            )
                        }
                        
                        // 2. Clusters (Zooms in, then opens a list)
                        state.viewMode == ViewMode.CRIMES && layerMode == MapLayerMode.CLUSTERS -> {
                            mapView.overlays.add(
                                HotspotClusterOverlay(
                                    visibleCrimes.mapNotNull { c ->
                                        val lat = c.location.latitude.toDoubleOrNull() ?: return@mapNotNull null
                                        val lng = c.location.longitude.toDoubleOrNull() ?: return@mapNotNull null
                                        lat to lng
                                    },
                                    onClusterTap = { lat, lng, _ ->
                                        val currentZoom = mapView.zoomLevelDouble
                                        if (currentZoom < 17.5) {
                                            mapView.controller.animateTo(GeoPoint(lat, lng))
                                            mapView.controller.setZoom(currentZoom + 1.5)
                                        } else {
                                            val clusterCrimes = visibleCrimes.filter { c ->
                                                val cLat = c.location.latitude.toDoubleOrNull() ?: 0.0
                                                val cLng = c.location.longitude.toDoubleOrNull() ?: 0.0
                                                distanceMeters(lat, lng, cLat, cLng) < 80.0
                                            }
                                            if (clusterCrimes.isNotEmpty()) {
                                                clusterSheetOpen = clusterCrimes
                                            }
                                        }
                                        true
                                    }
                                )
                            )
                        }
                        
                        // 3. Dots (Simple, single tap markers)
                        state.viewMode == ViewMode.CRIMES -> {
                            visibleCrimes.forEach { crime ->
                                val lat = crime.location.latitude.toDoubleOrNull() ?: return@forEach
                                val lng = crime.location.longitude.toDoubleOrNull() ?: return@forEach
                                val marker = Marker(mapView).apply {
                                    position = GeoPoint(lat, lng)
                                    icon = MarkerIconFactory.markerFor(mapView.context, crime.category)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    title = CrimeCategoryIcons.shortName(crime.category)
                                    snippet = "${crime.month} · ${crime.location.street?.name ?: ""}"
                                    setOnMarkerClickListener { _, _ ->
                                        selectedCrime = crime
                                        true
                                    }
                                }
                                mapView.overlays.add(marker)
                            }
                        }
                        
                        // 4. Stop and Searches
                        state.viewMode == ViewMode.STOP_SEARCH -> {
                            visibleStops.forEach { stop ->
                                val lat = stop.location?.latitude?.toDoubleOrNull() ?: return@forEach
                                val lng = stop.location?.longitude?.toDoubleOrNull() ?: return@forEach
                                val marker = Marker(mapView).apply {
                                    position = GeoPoint(lat, lng)
                                    icon = MarkerIconFactory.plainDot(mapView.context, secondaryArgb, 14)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    title = stop.objectOfSearch ?: "Stop and search"
                                    snippet = "Outcome: ${stop.outcome ?: "None recorded"}"
                                    setOnMarkerClickListener { _, _ ->
                                        selectedStop = stop
                                        true
                                    }
                                }
                                mapView.overlays.add(marker)
                            }
                        }

                        // 5. Road Safety Accidents
                        state.viewMode == ViewMode.ACCIDENTS -> {
                            state.visibleAccidents.forEach { accident ->
                                val marker = Marker(mapView).apply {
                                    position = GeoPoint(accident.latitude, accident.longitude)

                                    val dotColor = when (accident.severity) {
                                        1 -> Color.RED              // Fatal
                                        2 -> Color.rgb(255, 140, 0) // Serious (Orange)
                                        else -> Color.YELLOW        // Slight
                                    }

                                    icon = MarkerIconFactory.plainDot(mapView.context, dotColor, 14)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                                    val severityText = when (accident.severity) {
                                        1 -> "Fatal Collision"
                                        2 -> "Serious Collision"
                                        else -> "Slight Collision"
                                    }

                                    title = severityText
                                    snippet = "${accident.date} · ${accident.weatherConditions} · ${accident.numberOfVehicles} vehicles involved"

                                    setOnMarkerClickListener { _, _ ->
                                        selectedAccident = accident
                                        true // Return true to consume the click and prevent default tooltips
                                    }
                                }
                                mapView.overlays.add(marker)
                            }
                        }
                    }
                    mapView.invalidate()
                }
            }
        )

        // ---- Floating controls ----
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .zIndex(3f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MapFab(icon = { Icon(Icons.Outlined.Layers, "Map layers") }, onClick = { layersSheetOpen = true })

            // ADD THIS NEW BUTTON:
            if (state.viewMode == ViewMode.CRIMES && layerMode != MapLayerMode.CLUSTERS && visibleCrimes.isNotEmpty()) {
                MapFab(icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, "Legend") }, onClick = { legendSheetOpen = true })
            }

            if (onRequestMyLocation != null) {
                MapFab(icon = { Icon(Icons.Filled.MyLocation, "My location") }, onClick = { onRequestMyLocation() })
            }
            MapFab(icon = { Icon(Icons.Filled.CenterFocusStrong, "Re-centre search") }, onClick = {
                val target = GeoPoint(searchLat, searchLng)
                loadedCenter = target
                animateTarget = target
                pendingCenter = null
            })
            MapFab(icon = { Icon(Icons.Filled.BookmarkAdd, "Save this place") }, onClick = {
                viewModel.saveCurrentPlace(
                    name = viewModel.areaLabel(),
                    lat = searchLat,
                    lng = searchLng
                )
                Toast.makeText(context, "Place saved", Toast.LENGTH_SHORT).show()
            })
        }

        // ---- Search this area ----
        pendingCenter?.let { center ->
            val movedFar = distanceMeters(
                loadedCenter.latitude, loadedCenter.longitude,
                center.latitude, center.longitude
            ) > 400
            if (movedFar) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (state.viewMode == ViewMode.ACCIDENTS) {
                            mapViewRef?.let { viewModel.loadAccidentsForVisibleArea(it) }
                        } else {
                            viewModel.loadCrimes(center.latitude, center.longitude)
                        }
                        onLocationChanged(center.latitude, center.longitude)
                        loadedCenter = center
                        animateTarget = center
                        pendingCenter = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .zIndex(3f),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (state.viewMode) {
                            ViewMode.CRIMES -> "Search this area"
                            ViewMode.STOP_SEARCH -> "Search stops here"
                            ViewMode.ACCIDENTS -> "Search accidents here"
                        }
                    )
                }
            }
        }

        if (state.isLoading) {
            Surface(
                modifier = Modifier.align(Alignment.Center).zIndex(4f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(10.dp))
                    Text("Loading…", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    }

    // ---- Detail sheets ----
    selectedCrime?.let { crime ->
        LaunchedEffect(crime) {
            streetImageUrl = null
            isImageLoading = true
            val lat = crime.location.latitude.toDoubleOrNull()
            val lng = crime.location.longitude.toDoubleOrNull()
            if (lat != null && lng != null) {
                streetImageUrl = MapillaryClient.getNearestImageUrl(lat, lng)
            }
            isImageLoading = false
        }
        LaunchedEffect(crime.persistentId) { viewModel.loadOutcomeHistory(crime.persistentId) }

        ModalBottomSheet(
            onDismissRequest = { 
                selectedCrime = null
                viewModel.clearOutcomeHistory()
                viewModel.clearAreaProfile()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                MapillaryImageHeader(imageUrl = streetImageUrl, isLoading = isImageLoading)
                Spacer(modifier = Modifier.height(16.dp))
                CrimeDetailContent(crime, viewModel)
            }
        }
    }

    selectedStop?.let { stop ->
        LaunchedEffect(stop) {
            streetImageUrl = null
            isImageLoading = true
            val lat = stop.location?.latitude?.toDoubleOrNull()
            val lng = stop.location?.longitude?.toDoubleOrNull()
            if (lat != null && lng != null) {
                streetImageUrl = MapillaryClient.getNearestImageUrl(lat, lng)
            }
            isImageLoading = false
        }

        ModalBottomSheet(
            onDismissRequest = { 
                selectedStop = null
                viewModel.clearAreaProfile()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                MapillaryImageHeader(imageUrl = streetImageUrl, isLoading = isImageLoading)
                Spacer(modifier = Modifier.height(16.dp))
                StopSearchDetailContent(stop, viewModel)
            }
        }
    }

    if (layersSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { layersSheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            LayerPickerContent(
                selected = layerMode,
                onSelect = { layerMode = it }
            )
        }
    }

    if (legendSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { legendSheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            val entries = visibleCrimes.groupingBy { it.category }.eachCount()
                .entries.sortedByDescending { it.value }
                .map {
                    LegendEntry(
                        CrimeCategoryIcons.shortName(it.key),
                        CrimeCategoryColors.colorFor(it.key),
                        it.value
                    )
                }

            Column(Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
                Text("Map Legend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                LazyColumn {
                    items(entries) { e ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(e.color)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(e.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(e.count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (clusterSheetOpen != null) {
        val clusterCrimes = clusterSheetOpen!!
        ModalBottomSheet(
            onDismissRequest = { clusterSheetOpen = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(
                    "${clusterCrimes.size} crimes in this area",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clusterCrimes) { crime ->
                        CrimeListItem(crime = crime, onClick = {
                            clusterSheetOpen = null
                            selectedCrime = crime
                        })
                    }
                }
            }
        }
    }

    if (selectedAccident != null) {
        val accident = selectedAccident!!

        LaunchedEffect(accident) {
            streetImageUrl = null
            isImageLoading = true
            streetImageUrl = MapillaryClient.getNearestImageUrl(accident.latitude, accident.longitude)
            isImageLoading = false
        }

        ModalBottomSheet(
            onDismissRequest = { selectedAccident = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                MapillaryImageHeader(imageUrl = streetImageUrl, isLoading = isImageLoading)
                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic header color and text based on severity
                val (severityText, severityColor) = when (accident.severity) {
                    1 -> "Fatal Collision" to androidx.compose.ui.graphics.Color(0xFFD32F2F) // Red
                    2 -> "Serious Collision" to androidx.compose.ui.graphics.Color(0xFFF57C00) // Orange
                    else -> "Slight Collision" to androidx.compose.ui.graphics.Color(0xFFFBC02D) // Yellow
                }

                Text(
                    text = severityText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = severityColor,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )

                // Detail Grid
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(accident.date, style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Weather", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(accident.weatherConditions, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vehicles Involved", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(accident.numberOfVehicles.toString(), style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Casualties", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(accident.numberOfCasualties.toString(), style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Speed Limit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${accident.speedLimit} mph", style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reference", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(accident.accidentReference, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

enum class MapLayerMode { DOTS, HEAT, CLUSTERS }

@Composable
private fun MapFab(icon: @Composable () -> Unit, onClick: () -> Unit) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        icon()
    }
}

data class LegendEntry(val label: String, val color: androidx.compose.ui.graphics.Color, val count: Int)

@Composable
fun LayerPickerContent(selected: MapLayerMode, onSelect: (MapLayerMode) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
        Text("Map layers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        listOf(
            Triple(MapLayerMode.DOTS, "Dots", "One marker per incident, colour-coded by category"),
            Triple(MapLayerMode.HEAT, "Heatmap", "Soft glow showing where incidents pile up"),
            Triple(MapLayerMode.CLUSTERS, "Clusters", "Numbered bubbles that zoom in when tapped")
        ).forEach { (mode, title, desc) ->
            Surface(
                onClick = { onSelect(mode) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected == mode) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (mode) {
                            MapLayerMode.DOTS -> Icons.Filled.Place
                            MapLayerMode.HEAT -> Icons.Filled.Whatshot
                            MapLayerMode.CLUSTERS -> Icons.Filled.Workspaces
                        },
                        contentDescription = null,
                        tint = if (selected == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Simple haversine distance in meters between two lat/lng points. */
fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
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
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 6.dp), // Compacting vertical padding
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All months") }
            )
        }
        items(months) { month ->
            FilterChip(
                selected = selected == month,
                onClick = { onSelect(month) },
                label = { Text(formatMonthLabel(month)) }
            )
        }
    }
}

/** "2026-07" -> "Jul 26" */
fun formatMonthLabel(month: String): String {
    return runCatching {
        val parts = month.split("-")
        val year = parts[0].takeLast(2)
        val name = java.time.Month.of(parts[1].toInt()).name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        "$name $year"
    }.getOrDefault(month)
}

@Composable
fun MapillaryImageHeader(imageUrl: String?, isLoading: Boolean) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Street level view",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No street view available",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
