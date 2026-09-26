package com.dinner.crimeapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
    val savedPlaces by viewModel.savedPlaces.collectAsState()

    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search a place or postcode (e.g. Scunthorpe, DN15)") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searching) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; viewModel.clearSearchResults() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { viewModel.searchPlace(query) }
            )
        )

        if (results.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) {
                Column {
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(results) { result: GeocodeResult ->
                            ListItem(
                                headlineContent = {
                                    Text(result.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Filled.LocationCity,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.clickable {
                                    val lat = result.lat.toDoubleOrNull()
                                    val lng = result.lon.toDoubleOrNull()
                                    if (lat != null && lng != null) {
                                        onPlaceSelected(lat, lng, result.displayName)
                                        query = result.displayName.substringBefore(",")
                                        viewModel.clearSearchResults()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
