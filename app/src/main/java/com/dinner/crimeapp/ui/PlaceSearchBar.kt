package com.dinner.crimeapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dinner.crimeapp.data.GeocodeResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSearchBar(
    viewModel: CrimeMapViewModel,
    onPlaceSelected: (lat: Double, lng: Double, label: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.searchResults.collectAsState()
    val searching by viewModel.searching.collectAsState()

    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search a place or postcode (e.g. Scunthorpe, DN15)") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searching) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { viewModel.searchPlace(query) }
            )
        )

        if (results.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(results) { result: GeocodeResult ->
                        ListItem(
                            headlineContent = { Text(result.displayName, maxLines = 2) },
                            modifier = Modifier.clickable {
                                val lat = result.lat.toDoubleOrNull()
                                val lng = result.lon.toDoubleOrNull()
                                if (lat != null && lng != null) {
                                    onPlaceSelected(lat, lng, result.displayName)
                                    query = result.displayName
                                    viewModel.clearSearchResults()
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
