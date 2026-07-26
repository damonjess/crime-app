package com.dinner.crimeapp.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices

@Composable
fun CrimeHomeScreen() {
    val viewModel: CrimeMapViewModel = viewModel()
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    val state by viewModel.state.collectAsState()

    var currentLat by remember { mutableDoubleStateOf(51.5074) }
    var currentLng by remember { mutableDoubleStateOf(-0.1278) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                    viewModel.loadCrimes(currentLat, currentLng)
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
                    viewModel.loadCrimes(currentLat, currentLng)
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
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Map") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("List") })
        }

        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                CategoryFilterDropdown(
                    categories = state.categories,
                    selected = state.selectedCategory,
                    onSelect = { viewModel.selectCategory(it) }
                )
            }
            IconButton(onClick = { requestMyLocation() }) {
                Icon(Icons.Filled.LocationOn, contentDescription = "Use my location")
            }
        }

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> CrimeMapScreen(viewModel = viewModel, startLat = currentLat, startLng = currentLng)
                1 -> CrimeListScreen(viewModel = viewModel)
            }
        }
    }
}
