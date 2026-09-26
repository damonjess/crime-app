package com.dinner.crimeapp.ui

import com.dinner.crimeapp.data.Crime
import com.dinner.crimeapp.data.StopSearch
import kotlin.math.min

/** Outcome buckets used across the app. */
enum class OutcomeBucket(val label: String) {
    UNDER_INVESTIGATION("Under investigation"),
    RESOLVED("Resolved"),
    COURT("Court / charged"),
    NO_FURTHER_ACTION("No further action"),
    OTHER("Other")
}

data class MonthCount(val month: String, val count: Int)

data class Hotspot(val streetName: String, val count: Int)

data class Stats(
    val total: Int,
    val byCategory: List<CategoryCount>,
    val byMonth: List<MonthCount>,
    val byOutcome: Map<OutcomeBucket, Int>,
    val resolutionRate: Float,
    val busiestStreet: Hotspot?,
    val hotspots: List<Hotspot>,
    val monthOverMonthChange: Float?, // fractional change latest vs previous month
    val crimesPerDay: Float
)

object CrimeStats {

    fun compute(crimes: List<Crime>): Stats {
        val byCategory = crimes.groupingBy { it.category }.eachCount()
            .entries.sortedByDescending { it.value }
            .map { CategoryCount(it.key, it.value) }

        val byMonth = crimes.groupingBy { it.month }.eachCount()
            .entries.sortedBy { it.key }
            .map { MonthCount(it.key, it.value) }

        val byOutcome = groupOutcomes(crimes)

        val resolved = crimes.count { it.outcomeStatus != null }

        val byStreet = crimes.groupingBy { it.location.street?.name ?: "Unknown" }.eachCount()
        val hotspots = byStreet.entries
            .filter { it.key != "Unknown" }
            .sortedByDescending { it.value }
            .take(5)
            .map { Hotspot(it.key, it.value) }
        val busiest = hotspots.firstOrNull()

        val months = byMonth.map { it.month }.sorted()
        val latest = months.lastOrNull()
        val previous = months.getOrNull(months.size - 2)
        val latestCount = byMonth.lastOrNull { it.month == latest }?.count
        val prevCount = byMonth.lastOrNull { it.month == previous }?.count
        val change = if (latestCount != null && prevCount != null && prevCount > 0) {
            (latestCount - prevCount).toFloat() / prevCount
        } else null

        // "Crimes per day" only makes sense per month window; average across months.
        val daysInWindow = (byMonth.size * 30).coerceAtLeast(1)
        val perDay = crimes.size.toFloat() / daysInWindow

        return Stats(
            total = crimes.size,
            byCategory = byCategory,
            byMonth = byMonth,
            byOutcome = byOutcome,
            resolutionRate = if (crimes.isEmpty()) 0f else resolved.toFloat() / crimes.size,
            busiestStreet = busiest,
            hotspots = hotspots,
            monthOverMonthChange = change,
            crimesPerDay = perDay
        )
    }

    fun groupOutcomes(crimes: List<Crime>): Map<OutcomeBucket, Int> {
        val counts = mutableMapOf<OutcomeBucket, Int>()
        for (crime in crimes) {
            val bucket = bucketFor(crime.outcomeStatus?.category)
            counts[bucket] = (counts[bucket] ?: 0) + 1
        }
        return counts
    }

    fun bucketFor(outcome: String?): OutcomeBucket = when {
        outcome == null -> OutcomeBucket.UNDER_INVESTIGATION
        // Check specific "closed" codes before the generic "investigation" catch
        outcome.contains("no-further-action", ignoreCase = true) ||
            outcome.contains("no-suspect-identified", ignoreCase = true) ||
            outcome.contains("unable-to-prosecute", ignoreCase = true) ||
            outcome.contains("unable-to-proceed", ignoreCase = true) -> OutcomeBucket.NO_FURTHER_ACTION
        outcome.contains("investigation", ignoreCase = true) -> OutcomeBucket.UNDER_INVESTIGATION
        outcome.contains("court", ignoreCase = true) ||
            outcome.contains("charged", ignoreCase = true) ||
            outcome.contains("sentenc", ignoreCase = true) ||
            outcome.contains("imprison", ignoreCase = true) ||
            outcome.contains("prison", ignoreCase = true) ||
            outcome.contains("caution", ignoreCase = true) ||
            outcome.contains("penalty", ignoreCase = true) ||
            outcome.contains("fined", ignoreCase = true) -> OutcomeBucket.COURT
        outcome.contains("local-resolution", ignoreCase = true) ||
            outcome.contains("community", ignoreCase = true) ||
            outcome.contains("compensation", ignoreCase = true) -> OutcomeBucket.RESOLVED
        else -> OutcomeBucket.OTHER
    }

    /**
     * Safety score 0–100: starts at 100 and loses points weighted by crime
     * counts relative to a "very busy" baseline (200 crimes in the window).
     */
    fun safetyScore(crimes: List<Crime>): Int {
        if (crimes.isEmpty()) return 100
        val total = crimes.size
        val violent = crimes.count { it.category.contains("violent", ignoreCase = true) }
        // Weight violent crime 2x, cap deductions so score stays meaningful.
        val weighted = total + violent
        val baseline = 200
        val deduction = min(80, (weighted * 100 / baseline).coerceAtMost(90))
        return (100 - deduction).coerceIn(1, 100)
    }

    fun compare(a: List<Crime>, b: List<Crime>): Int {
        // returns diff of counts, b - a
        return b.size - a.size
    }

    fun computeStopStats(stops: List<StopSearch>): StopStats {
        val byObject = stops.groupingBy { it.objectOfSearch ?: "Not recorded" }.eachCount()
            .entries.sortedByDescending { it.value }
            .map { CategoryCount(it.key, it.value) }
        val byOutcome = stops.groupingBy { it.outcome ?: "No outcome recorded" }.eachCount()
            .entries.sortedByDescending { it.value }
            .map { CategoryCount(it.key, it.value) }
        val byAge = stops.groupingBy { it.ageRange ?: "Unknown" }.eachCount()
            .entries.sortedByDescending { it.value }
            .map { CategoryCount(it.key, it.value) }

        return StopStats(
            total = stops.size,
            byObject = byObject,
            byOutcome = byOutcome,
            byAge = byAge
        )
    }
}

data class StopStats(
    val total: Int,
    val byObject: List<CategoryCount>,
    val byOutcome: List<CategoryCount>,
    val byAge: List<CategoryCount>
)
