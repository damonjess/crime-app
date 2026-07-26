package com.dinner.crimeapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dinner.crimeapp.data.Crime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrimeListScreen(viewModel: CrimeMapViewModel) {
    val state by viewModel.state.collectAsState()
    var selectedCrime by remember { mutableStateOf<Crime?>(null) }

    val crimes = viewModel.visibleCrimes()
        .sortedByDescending { it.month } // most recent first

    Column(Modifier.fillMaxSize()) {
        CrimeSummaryCard(summary = viewModel.summary())
        CrimeCategoryBreakdownCard(breakdown = viewModel.categoryBreakdown())
        CrimeTrendChart(crimesByMonth = state.crimesByMonth)

        if (crimes.isEmpty() && !state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Text("No crimes found for this area/month.")
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                items(crimes, key = { it.persistentId.ifBlank { it.id.toString() } }) { crime ->
                    CrimeListItem(crime = crime, onClick = { selectedCrime = crime })
                    HorizontalDivider()
                }
            }
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

@Composable
fun CrimeListItem(crime: Crime, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = getIconForCategory(crime.category),
                contentDescription = null,
                tint = CrimeCategoryColors.colorFor(crime.category)
            )
        },
        headlineContent = {
            Text(
                CrimeCategoryColors.displayName(crime.category),
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = {
            Column {
                Text(crime.location.street?.name ?: "Unknown location")
                Text("Month: ${crime.month}")
            }
        },
        trailingContent = {
            val status = crime.outcomeStatus?.category
            Text(
                text = status?.let { shortenOutcome(it) } ?: "Ongoing",
                style = MaterialTheme.typography.labelSmall
            )
        }
    )
}

@Composable
fun CrimeDetailContent(crime: Crime, viewModel: CrimeMapViewModel) {
    val history by viewModel.outcomeHistory.collectAsState()
    val loading by viewModel.outcomeLoading.collectAsState()

    Column(Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 32.dp)) {
        Text(
            CrimeCategoryColors.displayName(crime.category),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        DetailRow("Location", crime.location.street?.name ?: "Unknown")
        DetailRow("Location type", crime.locationType ?: "Not specified")
        DetailRow("Month reported", crime.month)
        DetailRow("Coordinates", "${crime.location.latitude}, ${crime.location.longitude}")
        DetailRow("Current status", crime.outcomeStatus?.category ?: "No outcome yet / ongoing")
        DetailRow("Case ID", crime.persistentId.ifBlank { crime.id.toString() })

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Text(
            "Case History",
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
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(
                        "${index + 1}. ${CrimeCategoryColors.displayName(entry.category)}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        entry.date,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(140.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun shortenOutcome(outcome: String): String = when {
    outcome.contains("no-further-action", ignoreCase = true) -> "No further action"
    outcome.contains("court", ignoreCase = true) -> "Court"
    outcome.contains("charged", ignoreCase = true) -> "Charged"
    outcome.contains("investigation", ignoreCase = true) -> "Investigating"
    else -> outcome.replace("-", " ").take(20)
}

@Composable
private fun getIconForCategory(category: String): ImageVector = when {
    category.contains("theft", ignoreCase = true) -> Icons.Default.ShoppingBag
    category.contains("burglary", ignoreCase = true) -> Icons.Default.Home
    category.contains("violence", ignoreCase = true) -> Icons.Default.Warning
    category.contains("drugs", ignoreCase = true) -> Icons.Default.MedicalServices
    category.contains("vehicle", ignoreCase = true) -> Icons.Default.DirectionsCar
    category.contains("public-order", ignoreCase = true) -> Icons.Default.People
    else -> Icons.Default.LocationOn
}
