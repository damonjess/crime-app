package com.dinner.crimeapp.ui

import com.dinner.crimeapp.data.Crime
import com.dinner.crimeapp.ui.CrimeCategoryColors.displayName
import com.dinner.crimeapp.ui.CrimeStats.bucketFor
import com.dinner.crimeapp.ui.CrimeStats.safetyScore

/**
 * Builds and shares a plain-text report of the current view's crime data.
 * Used by the "Share report" action so users can send a summary to
 * neighbours, community groups etc.
 */
object CrimeReport {

    fun build(
        areaName: String?,
        crimes: List<Crime>,
        monthsCovered: Int
    ): String {
        val stats = CrimeStats.compute(crimes)
        val sb = StringBuilder()

        sb.appendLine("Crime report — ${areaName ?: "selected area"}")
        sb.appendLine("Data: data.police.uk, last $monthsCovered month(s)")
        sb.appendLine()

        sb.appendLine("SUMMARY")
        sb.appendLine("• Total crimes: ${stats.total}")
        stats.byCategory.firstOrNull()?.let {
            sb.appendLine("• Most common: ${displayName(it.category)} (${it.count})")
        }
        sb.appendLine("• Safety score: ${safetyScore(crimes)}/100")

        if (stats.byMonth.size >= 2) {
            val change = stats.monthOverMonthChange
            if (change != null) {
                val pct = (change * 100).toInt()
                val dir = if (pct >= 0) "up" else "down"
                sb.appendLine("• Trend: $dir ${kotlin.math.abs(pct)}% vs previous month")
            }
        }
        sb.appendLine()

        if (stats.byCategory.isNotEmpty()) {
            sb.appendLine("TOP CATEGORIES")
            stats.byCategory.take(5).forEach {
                sb.appendLine("• ${displayName(it.category)}: ${it.count}")
            }
            sb.appendLine()
        }

        if (stats.hotspots.isNotEmpty()) {
            sb.appendLine("CRIME HOTSPOTS")
            stats.hotspots.take(3).forEach {
                sb.appendLine("• ${it.streetName.removePrefix("On or near ")}: ${it.count} crimes")
            }
            sb.appendLine()
        }

        sb.appendLine("OUTCOMES")
        val under = stats.byOutcome[bucketFor(null)] ?: 0
        sb.appendLine("• Under investigation: $under")
        val resolved = stats.byOutcome.entries
            .filter { it.key != bucketFor(null) }
            .sumOf { it.value }
        sb.appendLine("• Outcome known: $resolved")

        sb.appendLine()
        sb.appendLine("Generated with Crime Map UK")

        return sb.toString()
    }
}
