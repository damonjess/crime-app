package com.dinner.crimeapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dinner.crimeapp.data.Crime

@Composable
fun CrimeInsightsScreen(
    viewModel: CrimeMapViewModel,
    onShareReport: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val crimes = remember(state.crimesByMonth, state.selectedMonth, state.selectedCategory) {
        viewModel.visibleCrimes()
    }
    val stats = remember(crimes) { CrimeStats.compute(crimes) }
    val score = remember(crimes) { CrimeStats.safetyScore(crimes) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Total crimes",
                    value = stats.total.toString(),
                    icon = Icons.Filled.LocationCity,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Safety score",
                    value = "$score",
                    icon = Icons.Filled.Shield,
                    color = scoreColor(score),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Outcome known",
                    value = "${(stats.resolutionRate * 100).toInt()}%",
                    icon = Icons.Filled.Shield,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                val change = stats.monthOverMonthChange
                StatTile(
                    label = "vs last month",
                    value = if (change == null) "–" else "${if (change >= 0) "+" else ""}${(change * 100).toInt()}%",
                    icon = when {
                        change == null -> Icons.Filled.TrendingFlat
                        change > 0.02f -> Icons.Filled.TrendingUp
                        change < -0.02f -> Icons.Filled.TrendingDown
                        else -> Icons.Filled.TrendingFlat
                    },
                    color = when {
                        change == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        change > 0.02f -> MaterialTheme.colorScheme.error
                        change < -0.02f -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item { SafetyGaugeCard(score) }

        if (stats.byMonth.isNotEmpty()) {
            item { TrendCard(stats) }
        }

        if (stats.byCategory.isNotEmpty()) {
            item { CategoryDonutCard(stats) }
        }

        if (stats.hotspots.isNotEmpty()) {
            item {
                HotspotsCard(
                    hotspots = stats.hotspots,
                    busiestLabel = stats.busiestStreet?.streetName
                )
            }
        }

        item {
            OutcomeBreakdownCard(stats)
        }

        item {
            Button(
                onClick = onShareReport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share community report")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 75 -> Color(0xFF2FBF8F)
    score >= 50 -> Color(0xFFF2A93B)
    else -> Color(0xFFE05656)
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    var launched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { launched = true }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SafetyGaugeCard(score: Int) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val animated by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1200),
        label = "gauge"
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    val inset = stroke.width / 2 + 2.dp.toPx()
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    drawArc(
                        color = surfaceVariant,
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize, style = stroke
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(scoreColor((animated * 100).toInt()), scoreColor((animated * 100).toInt()).copy(alpha = 0.6f))
                        ),
                        startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize, style = stroke
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${(animated * 100).toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("/100", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text("Safety score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        score >= 75 -> "Quiet area — lower than typical crime levels."
                        score >= 50 -> "Moderate activity — fairly typical for a town area."
                        else -> "Busy area — higher crime levels than average."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrendCard(stats: Stats) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Monthly trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Crimes reported per month",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            AnimatedBarChart(counts = stats.byMonth)
        }
    }
}

@Composable
private fun AnimatedBarChart(counts: List<MonthCount>) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(counts) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(900),
        label = "chart"
    )
    val maxCount = counts.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        counts.forEach { mc ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatMonthLabel(mc.month),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    modifier = Modifier.width(52.dp)
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(trackColor)
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(mc.count.toFloat() / maxCount * progress)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(listOf(barColor, barColor.copy(alpha = 0.75f)))
                            )
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    mc.count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun CategoryDonutCard(stats: Stats) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("What's happening here", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Share of crimes by type",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryDonut(
                    segments = stats.byCategory.take(6).map {
                        DonutSegment(
                            fraction = it.count.toFloat() / stats.total.coerceAtLeast(1),
                            color = CrimeCategoryColors.colorFor(it.category)
                        )
                    },
                    modifier = Modifier.size(120.dp)
                )
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    stats.byCategory.take(4).forEach { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(CrimeCategoryColors.colorFor(entry.category))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                CrimeCategoryIcons.shortName(entry.category),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(entry.count * 100f / stats.total.coerceAtLeast(1)).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

data class DonutSegment(val fraction: Float, val color: Color)

@Composable
private fun CategoryDonut(segments: List<DonutSegment>, modifier: Modifier = Modifier) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(segments) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(900),
        label = "donut"
    )
    Canvas(modifier) {
        val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Butt)
        val inset = stroke.width / 2 + 1.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        var startAngle = -90f
        segments.forEach { seg ->
            val sweep = seg.fraction * 360f * progress
            drawArc(
                color = seg.color,
                startAngle = startAngle,
                sweepAngle = sweep - 2f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun HotspotsCard(hotspots: List<Hotspot>, busiestLabel: String?) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text("Crime hotspots", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (busiestLabel != null) {
                Text(
                    "Most incidents near ${busiestLabel.removePrefix("On or near ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            val maxCount = hotspots.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
            hotspots.forEach { hs ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            hs.streetName.removePrefix("On or near "),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${hs.count}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(hs.count.toFloat() / maxCount)
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}

@Composable
private fun OutcomeBreakdownCard(stats: Stats) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("What happened next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            val total = stats.total.coerceAtLeast(1)
            val ordered = listOf(
                OutcomeBucket.COURT,
                OutcomeBucket.RESOLVED,
                OutcomeBucket.UNDER_INVESTIGATION,
                OutcomeBucket.NO_FURTHER_ACTION,
                OutcomeBucket.OTHER
            )
            ordered.forEach { bucket ->
                val count = stats.byOutcome[bucket] ?: 0
                if (count > 0 || bucket == OutcomeBucket.UNDER_INVESTIGATION) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 5.dp)
                    ) {
                        Text(bucket.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Box(
                            Modifier
                                .weight(1.4f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(count.toFloat() / total)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (bucket) {
                                            OutcomeBucket.COURT -> Color(0xFF2FBF8F)
                                            OutcomeBucket.RESOLVED -> Color(0xFF5AB0F2)
                                            OutcomeBucket.UNDER_INVESTIGATION -> Color(0xFFF2A93B)
                                            OutcomeBucket.NO_FURTHER_ACTION -> Color(0xFF9AA3B8)
                                            OutcomeBucket.OTHER -> Color(0xFFB08BD9)
                                        }
                                    )
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$count",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
