package com.dinner.crimeapp.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dinner.crimeapp.ui.theme.ThemeMode
import com.dinner.crimeapp.ui.theme.ThemePrefs
import com.google.android.gms.location.LocationServices

data class BottomTab(val label: String, val iconSelected: androidx.compose.ui.graphics.vector.ImageVector, val iconOutline: androidx.compose.ui.graphics.vector.ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrimeHomeScreen() {
    val viewModel: CrimeMapViewModel = viewModel()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(0) }

    var currentLat by rememberSaveable { mutableStateOf(51.5074) }
    var currentLng by rememberSaveable { mutableStateOf(-0.1278) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                }
            }
        }
    }

    fun requestMyLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                }
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(Unit) { viewModel.loadCategories() }

    val areaName by viewModel.areaLocationName.collectAsState()

    // Share report via a proper activity launcher (no state mutation during composition)
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    fun shareReport() {
        val report = CrimeReport.build(
            areaName = areaName ?: viewModel.areaLabel(),
            crimes = viewModel.visibleCrimes(),
            monthsCovered = viewModel.monthsCovered().size
        )
        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, report)
        }
        shareLauncher.launch(android.content.Intent.createChooser(sendIntent, "Share crime report"))
    }

    val savedPlaces by viewModel.savedPlaces.collectAsState()
    var placesSheetOpen by remember { mutableStateOf(false) }
    var themeMenuOpen by remember { mutableStateOf(false) }
    val themeMode by ThemePrefs.mode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Crime Map UK",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                areaName ?: viewModel.areaLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { requestMyLocation() }) {
                        Icon(Icons.Filled.LocationOn, contentDescription = "Use my location")
                    }
                    IconButton(onClick = { placesSheetOpen = true }) {
                        Icon(Icons.Filled.Bookmark, contentDescription = "Saved places")
                    }
                    // Theme toggle
                    Box {
                        IconButton(onClick = { themeMenuOpen = true }) {
                            Icon(
                                when (themeMode) {
                                    ThemeMode.SYSTEM -> Icons.Filled.SettingsBrightness
                                    ThemeMode.LIGHT -> Icons.Filled.LightMode
                                    ThemeMode.DARK -> Icons.Filled.DarkMode
                                },
                                contentDescription = "Theme"
                            )
                        }
                        DropdownMenu(
                            expanded = themeMenuOpen,
                            onDismissRequest = { themeMenuOpen = false }
                        ) {
                            listOf(
                                ThemeMode.SYSTEM to "System default",
                                ThemeMode.LIGHT to "Light",
                                ThemeMode.DARK to "Dark"
                            ).forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        ThemePrefs.setMode(context, mode)
                                        themeMenuOpen = false
                                    },
                                    trailingIcon = {
                                        if (themeMode == mode) Icon(Icons.Filled.Check, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val tabs = listOf(
                    BottomTab("Map", Icons.Filled.Map, Icons.Outlined.Map),
                    BottomTab("List", Icons.Filled.FormatListBulleted, Icons.Outlined.ListAlt),
                    BottomTab("Insights", Icons.Filled.Insights, Icons.Outlined.Insights),
                    BottomTab("Area", Icons.Filled.Home, Icons.Outlined.Home)
                )
                tabs.forEachIndexed { index, tabDef ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = {
                            Icon(
                                if (tab == index) tabDef.iconSelected else tabDef.iconOutline,
                                contentDescription = tabDef.label
                            )
                        },
                        label = { Text(tabDef.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (tab) {
                0 -> CrimeMapScreen(
                    viewModel = viewModel,
                    startLat = currentLat,
                    startLng = currentLng,
                    onLocationChanged = { lat, lng ->
                        currentLat = lat
                        currentLng = lng
                    }
                )
                1 -> CrimeListScreen(
                    viewModel = viewModel,
                    lat = currentLat,
                    lng = currentLng
                )
                2 -> CrimeInsightsScreen(
                    viewModel = viewModel,
                    onShareReport = { shareReport() }
                )
                3 -> NeighbourhoodScreen(viewModel)
            }
        }
    }

    // Saved places bottom sheet
    if (placesSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { placesSheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SavedPlacesContent(
                places = savedPlaces,
                onSelect = { place ->
                    currentLat = place.lat
                    currentLng = place.lng
                    placesSheetOpen = false
                },
                onRemove = { viewModel.removeSavedPlace(it.id) }
            )
        }
    }
}

@Composable
fun SavedPlacesContent(
    places: List<SavedPlace>,
    onSelect: (SavedPlace) -> Unit,
    onRemove: (SavedPlace) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
        Text("Saved places", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Bookmark areas you check often — home, work, family.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        if (places.isEmpty()) {
            Text(
                "No saved places yet.\nOpen an area and tap Save to add it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            places.forEach { place ->
                ListItem(
                    headlineContent = { Text(place.name) },
                    supportingContent = { Text("%.3f, %.3f".format(place.lat, place.lng)) },
                    leadingContent = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = { onRemove(place) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove ${place.name}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(place) }
                )
            }
        }
    }
}
