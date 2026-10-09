package com.dinner.crimeapp.ui

import android.content.Intent
import android.net.Uri
import android.text.Html
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dinner.crimeapp.data.AreaInfo
import com.dinner.crimeapp.data.NeighbourhoodEvent
import com.dinner.crimeapp.data.NeighbourhoodOfficer
import com.dinner.crimeapp.data.NeighbourhoodPriority

@Composable
fun NeighbourhoodScreen(viewModel: CrimeMapViewModel) {
    val isAreaLoading by viewModel.isAreaLoading.collectAsState()
    val areaError by viewModel.areaError.collectAsState()
    val areaInfo by viewModel.areaInfo.collectAsState()
    val team by viewModel.team.collectAsState()
    val events by viewModel.events.collectAsState()
    val priorities by viewModel.priorities.collectAsState()
    val locationName by viewModel.areaLocationName.collectAsState()
    val context = LocalContext.current

    val areaName = locationName ?: viewModel.areaLabel()

    if (isAreaLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Finding your local policing team…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    if (areaInfo == null) {
        Box(
            Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No policing team details found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        areaError ?: "Local policing team information is available for England, Wales, and Northern Ireland.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.reloadAreaContext() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // 1. Area Header Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        areaName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    areaInfo?.neighbourhoodName?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                    areaInfo?.population?.let { pop ->
                        Text(
                            "Population ~${pop.toLongOrNull()?.let { n -> "%,d".format(n) } ?: pop}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // 2. About this area (Description)
        areaInfo?.neighbourhoodDescription?.let { rawDesc ->
            val cleanDesc = Html.fromHtml(rawDesc, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            if (cleanDesc.isNotBlank()) {
                item {
                    SectionCard(title = "About this area") {
                        Text(
                            cleanDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 3. Next Community Meeting Banner
        if (events.isNotEmpty()) {
            item {
                EventBanner(events.first())
            }
        }

        // 4. Team Priorities
        if (priorities.isNotEmpty()) {
            item {
                SectionCard(title = "Team Priorities") {
                    Text(
                        "Issues the team is actively targeting this month:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    priorities.take(3).forEachIndexed { index, priority ->
                        val cleanIssue = Html.fromHtml(priority.issue, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("${index + 1}.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(20.dp))
                            Text(cleanIssue, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // 5. Individual Team Member Details
        item { SectionHeader("Your policing team") }
        if (team.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    team.forEach { officer ->
                        ExpandableOfficerCard(officer, context)
                    }
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No individual officer details published by ${areaInfo?.forceName ?: "the local force"} for this neighbourhood.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // 6. Additional Events
        if (events.size > 1) {
            item { SectionHeader("More upcoming events") }
            items(events.size - 1) { i ->
                EventCard(events[i + 1])
            }
        }

        // 7. Station Address & General Enquiries Footer
        item { StationFooter(areaInfo, context) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ExpandableOfficerCard(officer: NeighbourhoodOfficer, context: android.content.Context) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("👮", style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(officer.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    
                    officer.rank?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Filled.Badge,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Badge ID: #${officer.collarNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = "Expand details",
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(12.dp))

                    // Collar Number / Badge ID detail
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Badge,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Collar Number / Badge ID: ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "#${officer.collarNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Bio
                    val parsedBio = officer.bio?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() }
                    val cleanBio = if (!parsedBio.isNullOrBlank()) {
                        parsedBio
                    } else {
                        getRoleDescription(officer.rank)
                    }
                    Text("About", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(cleanBio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    // Contact Options
                    if (!officer.contactDetails.isNullOrEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            officer.contactDetails.forEach { (type, value) ->
                                val icon = when (type.lowercase()) {
                                    "email" -> Icons.Filled.Email
                                    "telephone", "phone" -> Icons.Filled.Phone
                                    "twitter", "x" -> Icons.Filled.Chat
                                    else -> Icons.Filled.Language
                                }
                                IconButton(
                                    onClick = {
                                        val uri = when (type.lowercase()) {
                                            "email" -> "mailto:$value"
                                            "telephone", "phone" -> "tel:$value"
                                            else -> value
                                        }
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }
                                    },
                                    modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                ) {
                                    Icon(icon, contentDescription = type, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventBanner(event: NeighbourhoodEvent) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Event, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Next Community Meeting", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(formatEventDate(event.startDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    event.address?.let {
                        Text(" • ${it.trim()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun StationFooter(areaInfo: AreaInfo?, context: android.content.Context) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Station & General Enquiries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            // Extract the station address from locations if available
            val station = areaInfo?.locations?.firstOrNull { it.type == "station" } ?: areaInfo?.locations?.firstOrNull()
            if (station != null) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Domain, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(station.name ?: "Police Station", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        station.address?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        station.postcode?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { areaInfo?.forceTelephone?.let { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))) } },
                    enabled = areaInfo?.forceTelephone != null,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "101",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                areaInfo?.neighbourhoodUrl?.let { url ->
                    FilledTonalButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Website",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.police.uk/pu/contact-the-police/"))) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.AlternateEmail, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Report",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                "In an emergency always call 999",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun EventCard(event: NeighbourhoodEvent) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    formatEventDate(event.startDate),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                event.address?.let {
                    Text(it.trim(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatEventDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "TBC"
    return runCatching {
        val d = java.time.LocalDateTime.parse(iso.take(16))
        d.format(java.time.format.DateTimeFormatter.ofPattern("d MMM h:mm a"))
    }.getOrDefault(iso.take(10))
}

private fun getRoleDescription(rank: String?): String {
    val r = rank?.lowercase().orEmpty()
    return when {
        r.contains("inspector") -> "Responsible for overall leadership, strategy, and deployment of the neighbourhood policing team."
        r.contains("sergeant") -> "Supervises local officers, co-ordinating crime prevention strategies and community policing."
        r.contains("pcso") || r.contains("support") -> "Provides a visible presence in the community, tackling anti-social behaviour and supporting residents."
        r.contains("constable") || r.contains("pc") -> "Patrols local neighbourhoods, responds to incidents, and works directly with residents to solve local issues."
        else -> "No detailed biography has been published by the police force for this officer."
    }
}