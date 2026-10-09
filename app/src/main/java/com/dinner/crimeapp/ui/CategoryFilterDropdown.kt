package com.dinner.crimeapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dinner.crimeapp.data.CrimeCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterDropdown(
    categories: List<CrimeCategory>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val selectedName = categories.find { it.url == selected }?.name

    Row(
        modifier = Modifier, // Removed fillMaxWidth and padding to fit in the combined row
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = sheetOpen || selected != null,
            onClick = { sheetOpen = true },
            label = {
                Text(selectedName?.let { CrimeCategoryIcons.shortName(it) } ?: "All categories")
            },
            leadingIcon = {
                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        )
        if (selected != null) {
            FilterChip(
                selected = false,
                onClick = { onSelect(null) },
                label = { Text("Clear") },
                leadingIcon = {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Filter by crime type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    modifier = Modifier.height(380.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        CategoryCard(
                            label = "All categories",
                            emoji = "🌐",
                            selected = selected == null,
                            onClick = { onSelect(null); sheetOpen = false }
                        )
                    }
                    items(categories.size) { i ->
                        val cat = categories[i]
                        CategoryCard(
                            label = CrimeCategoryIcons.shortName(cat.url),
                            emoji = CrimeCategoryIcons.emojiFor(cat.url),
                            selected = selected == cat.url,
                            onClick = { onSelect(cat.url); sheetOpen = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(label: String, emoji: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
            }
        }
    }
}
