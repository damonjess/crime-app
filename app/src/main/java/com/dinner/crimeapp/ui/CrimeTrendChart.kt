package com.dinner.crimeapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinner.crimeapp.data.Crime

@Composable
fun CrimeTrendChart(
    crimesByMonth: Map<String, List<Crime>>,
    modifier: Modifier = Modifier
) {
    // Sort chronologically (oldest to newest, left to right)
    val monthCounts = crimesByMonth
        .toSortedMap()
        .mapValues { it.value.size }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Crime Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (monthCounts.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "No trend data available yet",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "Last ${monthCounts.size} months",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                TrendChartContent(monthCounts)
            }
        }
    }
}

@Composable
private fun TrendChartContent(monthCounts: Map<String, Int>) {
    val maxCount = monthCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.primary
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
    val textMeasurer = rememberTextMeasurer()

    // Animate the ratios for each bar
    val animatedRatios = monthCounts.values.map { count ->
        animateFloatAsState(
            targetValue = count.toFloat() / maxCount,
            animationSpec = tween(durationMillis = 1000),
            label = "BarHeightRatio"
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .semantics {
                val summary = monthCounts.entries.joinToString { "${it.key}: ${it.value}" }
                contentDescription = "Crime trend chart showing: $summary"
            }
    ) {
        val barCount = monthCounts.size
        val spacing = 16.dp.toPx()
        val barWidth = (size.width - spacing * (barCount + 1)) / barCount
        val xAxisHeight = 24.dp.toPx()
        val chartHeight = size.height - xAxisHeight

        monthCounts.entries.forEachIndexed { index, (month, count) ->
            val ratio = animatedRatios.getOrNull(index)?.value ?: 0f
            val left = spacing + index * (barWidth + spacing)
            val barHeight = ratio * chartHeight
            
            // Draw Bar
            drawRect(
                color = barColor,
                topLeft = Offset(left, chartHeight - barHeight),
                size = Size(barWidth, barHeight)
            )

            // Draw count above bar
            val countText = count.toString()
            val countLayout = textMeasurer.measure(countText, labelStyle)
            drawText(
                textLayoutResult = countLayout,
                topLeft = Offset(
                    left + (barWidth - countLayout.size.width) / 2f,
                    chartHeight - barHeight - countLayout.size.height - 4f
                )
            )

            // Draw month label (e.g. "05")
            val shortLabel = month.takeLast(2)
            val monthLayout = textMeasurer.measure(shortLabel, labelStyle)
            drawText(
                textLayoutResult = monthLayout,
                topLeft = Offset(
                    left + (barWidth - monthLayout.size.width) / 2f,
                    chartHeight + 4f
                )
            )
        }
    }
}
