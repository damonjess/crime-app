package com.dinner.crimeapp

import com.dinner.crimeapp.ui.CrimeCategoryIcons
import com.dinner.crimeapp.ui.CrimeReport
import com.dinner.crimeapp.ui.CrimeStats
import com.dinner.crimeapp.ui.OutcomeBucket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrimeStatsTest {

    private fun crime(
        category: String,
        month: String,
        outcome: String? = null,
        street: String = "On or near Test Street"
    ) = com.dinner.crimeapp.data.Crime(
        id = 1L,
        persistentId = "p-$category-$month-$outcome",
        category = category,
        locationType = "Force",
        location = com.dinner.crimeapp.data.CrimeLocation(
            latitude = "51.5",
            longitude = "-0.1",
            street = com.dinner.crimeapp.data.Street(1L, street)
        ),
        month = month,
        outcomeStatus = outcome?.let { com.dinner.crimeapp.data.OutcomeStatus(it, "2026-08") }
    )

    @Test
    fun `empty crime list gives full safety score`() {
        assertEquals(100, CrimeStats.safetyScore(emptyList()))
    }

    @Test
    fun `more crime lowers safety score`() {
        val quiet = (1..10).map { crime("other-crime", "2026-07") }
        val busy = (1..150).map { crime("other-crime", "2026-07") }
        assertTrue(CrimeStats.safetyScore(busy) < CrimeStats.safetyScore(quiet))
    }

    @Test
    fun `outcome bucketing is sensible`() {
        assertEquals(OutcomeBucket.UNDER_INVESTIGATION, CrimeStats.bucketFor(null))
        assertEquals(OutcomeBucket.UNDER_INVESTIGATION, CrimeStats.bucketFor("under-investigation"))
        assertEquals(OutcomeBucket.COURT, CrimeStats.bucketFor("offender-sent-to-prison"))
        assertEquals(OutcomeBucket.COURT, CrimeStats.bucketFor("suspect-charged"))
        assertEquals(OutcomeBucket.NO_FURTHER_ACTION, CrimeStats.bucketFor("investigation-complete-no-suspect-identified"))
        assertEquals(OutcomeBucket.RESOLVED, CrimeStats.bucketFor("local-resolution"))
        assertEquals(OutcomeBucket.OTHER, CrimeStats.bucketFor("weird-unknown-outcome"))
    }

    @Test
    fun `stats computes categories months and resolution rate`() {
        val crimes = listOf(
            crime("violent-crime", "2026-07", outcome = "suspect-charged"),
            crime("violent-crime", "2026-07"),
            crime("burglary", "2026-06", outcome = "local-resolution")
        )
        val stats = CrimeStats.compute(crimes)
        assertEquals(3, stats.total)
        assertEquals("violent-crime", stats.byCategory.first().category)
        assertEquals(2, stats.byMonth.size)
        assertEquals(2f / 3f, stats.resolutionRate, 0.001f)
    }

    @Test
    fun `hotspots exclude unknown streets and sort by count`() {
        val crimes = listOf(
            crime("other-crime", "2026-07", street = "On or near High Street"),
            crime("other-crime", "2026-07", street = "On or near High Street"),
            crime("other-crime", "2026-07", street = "On or near Church Lane"),
            crime("other-crime", "2026-07", street = "Unknown")
        )
        val stats = CrimeStats.compute(crimes)
        assertEquals(2, stats.hotspots.size)
        assertEquals("On or near High Street", stats.hotspots[0].streetName)
        assertEquals(2, stats.hotspots[0].count)
    }

    @Test
    fun `category icons and names map correctly`() {
        assertEquals("⚔️", CrimeCategoryIcons.emojiFor("violent-crime"))
        assertEquals("🚗", CrimeCategoryIcons.emojiFor("vehicle-crime"))
        assertEquals("Violent crime", CrimeCategoryIcons.shortName("violent-crime"))
        assertEquals("Anti-social behaviour", CrimeCategoryIcons.shortName("anti-social-behaviour"))
    }

    @Test
    fun `report builder produces shareable text`() {
        val crimes = listOf(
            crime("violent-crime", "2026-07", outcome = "suspect-charged"),
            crime("burglary", "2026-06", outcome = "local-resolution")
        )
        val report = CrimeReport.build("Testville", crimes, 6)
        assertTrue(report.contains("Testville"))
        assertTrue(report.contains("Total crimes: 2"))
        assertTrue(report.contains("Generated with Crime Map UK"))
    }
}
