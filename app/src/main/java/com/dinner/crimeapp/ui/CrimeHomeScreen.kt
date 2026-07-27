package com.dinner.crimeapp.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrimeHomeScreen() {
    val viewModel: CrimeMapViewModel = viewModel()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(0) }
    val state by viewModel.state.collectAsState()

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

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        PlaceSearchBar(
            viewModel = viewModel,
            onPlaceSelected = { lat, lng, label ->
                currentLat = lat
                currentLng = lng
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Map") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("List") })
        }

        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                SegmentedButton(
                    selected = state.viewMode == ViewMode.CRIMES,
                    onClick = { viewModel.setViewMode(ViewMode.CRIMES) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Crimes")
                }
                SegmentedButton(
                    selected = state.viewMode == ViewMode.STOP_SEARCH,
                    onClick = { viewModel.setViewMode(ViewMode.STOP_SEARCH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Stops")
                }
            }

            IconButton(onClick = { requestMyLocation() }) {
                Icon(Icons.Filled.LocationOn, contentDescription = "Use my location")
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.viewMode == ViewMode.CRIMES) {
                CategoryFilterDropdown(
                    categories = state.categories,
                    selected = state.selectedCategory,
                    onSelect = { viewModel.selectCategory(it) }
                )
            }
        }

        Box(Modifier.weight(1f)) {
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
            }
        }
    }
}
