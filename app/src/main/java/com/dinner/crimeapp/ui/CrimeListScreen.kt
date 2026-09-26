package com.dinner.crimeapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dinner.crimeapp.data.Crime
import com.dinner.crimeapp.data.StopSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrimeListScreen(
    viewModel: CrimeMapViewModel,
    lat: Double,
    lng: Double
) {
    LaunchedEffect(lat, lng) {
        viewModel.loadCrimes(lat, lng)
    }

    val state by viewModel.state.collectAsState()
    var selectedCrime by remember { mutableStateOf<Crime?>(null) }
    var selectedStop by remember { mutableStateOf<StopSearch?>(null) }

    val crimes = viewModel.visibleCrimes()
        .sortedByDescending { it.month }

    val stops = viewModel.visibleStopSearches()
        .sortedByDescending { it.datetime }

    val isEmpty = if (state.viewMode == ViewMode.CRIMES) crimes.isEmpty() else stops.isEmpty()
    val isRateLimited = if (state.viewMode == ViewMode.CRIMES) state.crimesRateLimited else state.stopsRateLimited

    if (isEmpty && !state.isLoading) {
        EmptyState(isRateLimited, state.viewMode)
        return
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        if (state.viewMode == ViewMode.CRIMES) {
            items(crimes, key = { it.persistentId.ifBlank { it.id.toString() } }) { crime ->
                CrimeListItem(crime = crime, onClick = { selectedCrime = crime })
            }
        } else {
            items(stops, key = { it.hashCode() }) { stop ->
                StopSearchListItem(stop = stop, onClick = { selectedStop = stop })
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    selectedCrime?.let { crime ->
        LaunchedEffect(crime.persistentId) { viewModel.loadOutcomeHistory(crime.persistentId) }
        ModalBottomSheet(
            onDismissRequest = { selectedCrime = null; viewModel.clearOutcomeHistory() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CrimeDetailContent(crime, viewModel)
        }
    }

    selectedStop?.let { stop ->
        ModalBottomSheet(
            onDismissRequest = { selectedStop = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            StopSearchDetailContent(stop)
        }
    }
}

@Composable
private fun EmptyState(isRateLimited: Boolean, viewMode: ViewMode) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                if (isRateLimited) Icons.Filled.CloudOff else Icons.Filled.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    isRateLimited -> "Rate limited — try again in a moment"
                    viewMode == ViewMode.CRIMES -> "No crimes found for this area and filter"
                    else -> "No stop and search records here"
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StopSearchListItem(stop: StopSearch, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    "🛑",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stop.objectOfSearch ?: "Stop and search",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stop.location?.street?.name ?: "Unknown location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${stop.ageRange ?: "Age unknown"} · ${stop.gender ?: "n/a"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = outcomeTint(stop.outcome)
            ) {
                Text(
                    text = stop.outcome?.let { shortenOutcome(it) } ?: "Ongoing",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun outcomeTint(outcome: String?): androidx.compose.ui.graphics.Color {
    return when {
        outcome == null -> MaterialTheme.colorScheme.tertiaryContainer
        outcome.contains("no-further-action", true) -> MaterialTheme.colorScheme.surfaceVariant
        outcome.contains("charged", true) || outcome.contains("court", true) ->
            androidx.compose.ui.graphics.Color(0xFF2FBF8F).copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
fun CrimeListItem(crime: Crime, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CrimeCategoryColors.colorFor(crime.category).copy(alpha = 0.18f)
            ) {
                Text(
                    CrimeCategoryIcons.emojiFor(crime.category),
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    CrimeCategoryIcons.shortName(crime.category),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    crime.location.street?.name ?: "Unknown location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    formatMonthLabel(crime.month),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    crime.outcomeStatus == null -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Text(
                    text = crime.outcomeStatus?.let { shortenOutcome(it.category) } ?: "Ongoing",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CrimeDetailContent(crime: Crime, viewModel: CrimeMapViewModel) {
    val history by viewModel.outcomeHistory.collectAsState()
    val loading by viewModel.outcomeLoading.collectAsState()

    Column(Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CrimeCategoryColors.colorFor(crime.category).copy(alpha = 0.18f)
            ) {
                Text(
                    CrimeCategoryIcons.emojiFor(crime.category),
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    CrimeCategoryIcons.shortName(crime.category),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    crime.outcomeStatus?.let { shortenOutcome(it.category) } ?: "Under investigation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        DetailRow("Location", crime.location.street?.name ?: "Unknown")
        DetailRow("Location type", crime.locationType ?: "Not specified")
        DetailRow("Month reported", formatMonthLabel(crime.month))
        DetailRow("Coordinates", "%.4f, %.4f".format(crime.location.latitude.toDoubleOrNull() ?: 0.0, crime.location.longitude.toDoubleOrNull() ?: 0.0))
        DetailRow("Case ID", crime.persistentId.ifBlank { crime.id.toString() })

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Text(
            "Case history",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        when {
            loading -> CircularProgressIndicator(Modifier.padding(8.dp))
            history.isEmpty() -> Text(
                "No further outcome stages recorded yet.",
                style = MaterialTheme.typography.bodyMedium
            )
            else -> history.forEachIndexed { index, entry ->
                Row(Modifier.padding(vertical = 6.dp)) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(28.dp)
                    )
                    Column {
                        Text(
                            CrimeCategoryIcons.shortName(entry.category),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            entry.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StopSearchDetailContent(stop: StopSearch) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            stop.objectOfSearch ?: "Stop and search",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        DetailRow("Date", stop.datetime?.take(10) ?: "Unknown")
        DetailRow("Time", stop.datetime?.substringAfter("T")?.take(5) ?: "Unknown")
        DetailRow("Type", stop.type ?: "N/A")
        DetailRow("Gender", stop.gender ?: "N/A")
        DetailRow("Age range", stop.ageRange ?: "N/A")
        DetailRow("Ethnicity", stop.selfDefinedEthnicity ?: "N/A")
        DetailRow("Outcome", stop.outcome ?: "N/A")

        if (stop.location?.street != null) {
            DetailRow("Location", stop.location.street.name)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun shortenOutcome(outcome: String): String = when {
    outcome.contains("no-further-action", ignoreCase = true) -> "No further action"
    outcome.contains("court", ignoreCase = true) -> "Court"
    outcome.contains("charged", ignoreCase = true) -> "Charged"
    outcome.contains("investigation", ignoreCase = true) -> "Investigating"
    else -> outcome.replace("-", " ").take(22)
}
