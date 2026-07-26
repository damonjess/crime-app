package com.dinner.crimeapp.data

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CrimeRepository(private val api: PoliceApiService = PoliceApi.service) {

    /** Fetch crimes for a single month near a point. month=null → latest available. */
    suspend fun getCrimes(lat: Double, lng: Double, month: String? = null): List<Crime> {
        Log.e("CrimeRepository", "getCrimes: lat=$lat, lng=$lng, month=$month")
        return api.getCrimesNear(lat, lng, month)
    }

    /** Fetch all crime categories. */
    suspend fun getCategories(): List<CrimeCategory> =
        runCatching { api.getCategories() }.getOrDefault(emptyList())

    /**
     * Fetch crimes across a range of past months (e.g. last 6 months)
     * for "recent and historical" views. Fires all requests in parallel.
     */
    suspend fun getCrimesForRange(
        lat: Double,
        lng: Double,
        monthsBack: Int = 6
    ): Map<String, List<Crime>> = coroutineScope {
        Log.e("CrimeRepository", "getCrimesForRange: lat=$lat, lng=$lng, monthsBack=$monthsBack")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        val latestAvailable = YearMonth.now().minusMonths(2)

        val months = (0 until monthsBack).map { i ->
            latestAvailable.minusMonths(i.toLong()).format(formatter)
        }

        // Fire all requests at once instead of one after another
        val results = months.map { month ->
            async {
                month to runCatching { api.getCrimesNear(lat, lng, month) }
                    .onFailure { e -> Log.e("CrimeRepository", "Error fetching crimes for $month", e) }
                    .getOrDefault(emptyList())
            }
        }.awaitAll()

        results.toMap()
    }

    suspend fun getOutcomeHistory(persistentId: String): List<OutcomeEntry> =
        runCatching { api.getOutcomeHistory(persistentId).outcomes }
            .getOrDefault(emptyList())
}
