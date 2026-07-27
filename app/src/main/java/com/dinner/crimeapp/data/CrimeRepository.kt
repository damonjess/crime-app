package com.dinner.crimeapp.data

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CrimeRepository(
    private val api: PoliceApiService = PoliceApi.service
) {
    // Cap how many requests are in flight at once, shared across crimes + stops
    private val requestSemaphore = Semaphore(permits = 4)

    private suspend fun <T> throttled(block: suspend () -> T): T =
        requestSemaphore.withPermit {
            var attempt = 0
            while (true) {
                try {
                    return@withPermit block()
                } catch (e: HttpException) {
                    if (e.code() == 429 && attempt < 2) {
                        attempt++
                        delay(500L * attempt)
                    } else {
                        throw e
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            block()
        }

    /** Search for a place or postcode. */
    suspend fun searchPlace(query: String): List<GeocodeResult> =
        runCatching { NominatimApi.service.search(query) }.getOrDefault(emptyList())

    suspend fun getStopSearches(lat: Double, lng: Double, month: String? = null): List<StopSearch> =
        api.getStopSearches(lat, lng, month)

    /** Fetch crimes for a single month near a point. month=null → latest available. */
    suspend fun getCrimes(lat: Double, lng: Double, month: String? = null): List<Crime> {
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
    ): Triple<Map<String, List<Crime>>, Boolean, String?> = coroutineScope {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        val latestAvailable = YearMonth.now().minusMonths(2)

        val months = (0 until monthsBack).map { i ->
            latestAvailable.minusMonths(i.toLong()).format(formatter)
        }

        // Fire all requests at once instead of one after another
        val results = months.map { month ->
            async {
                var rateLimited = false
                var errorMsg: String? = null
                val data = runCatching {
                    throttled { api.getCrimesNear(lat, lng, month) }
                }.onFailure { e ->
                    if (e is HttpException && e.code() == 429) rateLimited = true
                    errorMsg = e.message
                    Log.e("CrimeRepository", "Error fetching crimes for $month", e)
                }.getOrDefault(emptyList())

                Triple(month, data, rateLimited to errorMsg)
            }
        }.awaitAll()

        val map = results.associate { it.first to it.second }
        val anyRateLimited = results.any { it.third.first }
        val firstError = results.mapNotNull { it.third.second }.firstOrNull()

        Triple(map, anyRateLimited, firstError)
    }

    suspend fun getStopSearchesForRange(
        lat: Double,
        lng: Double,
        monthsBack: Int = 6
    ): Triple<Map<String, List<StopSearch>>, Boolean, String?> = coroutineScope {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        // Stop searches often have a slightly longer delay or different availability force-by-force.
        // We'll try to get the most recent 6 months, starting from 2 months ago.
        val latestAvailable = YearMonth.now().minusMonths(2)

        val months = (0 until monthsBack).map { i ->
            latestAvailable.minusMonths(i.toLong()).format(formatter)
        }

        Log.d("CrimeRepository", "Fetching stops for $lat, $lng over months: $months")

        val results = months.map { month ->
            async {
                var rateLimited = false
                var errorMsg: String? = null
                val data = runCatching {
                    throttled { getStopSearches(lat, lng, month) }
                }.onFailure { e ->
                    if (e is HttpException && e.code() == 429) rateLimited = true
                    errorMsg = e.message
                    Log.e("CrimeRepository", "Error fetching stops for $month at $lat, $lng", e)
                }.getOrDefault(emptyList())

                Triple(month, data, rateLimited to errorMsg)
            }
        }.awaitAll()

        val map = results.associate { it.first to it.second }
        val anyRateLimited = results.any { it.third.first }
        val firstError = results.mapNotNull { it.third.second }.firstOrNull()

        Triple(map, anyRateLimited, firstError)
    }

    suspend fun getOutcomeHistory(persistentId: String): List<OutcomeEntry> =
        runCatching { api.getOutcomeHistory(persistentId).outcomes }
            .getOrDefault(emptyList())
}
