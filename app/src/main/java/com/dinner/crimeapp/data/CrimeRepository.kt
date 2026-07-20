package com.dinner.crimeapp.data

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object PoliceApi {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val service: PoliceApiService = Retrofit.Builder()
        .baseUrl("https://data.police.uk/api/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(PoliceApiService::class.java)
}

class CrimeRepository(private val api: PoliceApiService = PoliceApi.service) {

    /** Fetch crimes for a single month near a point. month=null → latest available. */
    suspend fun getCrimes(lat: Double, lng: Double, month: String? = null): List<Crime> =
        api.getCrimesNear(lat, lng, month)

    /** Fetch all crime categories. */
    suspend fun getCategories(): List<CrimeCategory> =
        runCatching { api.getCategories() }.getOrDefault(emptyList())

    /**
     * Fetch crimes across a range of past months (e.g. last 6 months)
     * for "recent and historical" views. Runs requests sequentially
     * to stay polite to the free API.
     */
    suspend fun getCrimesForRange(
        lat: Double,
        lng: Double,
        monthsBack: Int = 6
    ): Map<String, List<Crime>> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        // Police data usually lags ~2 months behind real time
        val latestAvailable = YearMonth.now().minusMonths(2)

        val result = mutableMapOf<String, List<Crime>>()
        for (i in 0 until monthsBack) {
            val month = latestAvailable.minusMonths(i.toLong()).format(formatter)
            result[month] = runCatching { api.getCrimesNear(lat, lng, month) }
                .getOrDefault(emptyList())
        }
        return result
    }
}
