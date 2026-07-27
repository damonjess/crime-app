package com.dinner.crimeapp.data

import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApiService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("countrycodes") countryCodes: String = "gb",
        @Query("limit") limit: Int = 5
    ): List<GeocodeResult>
}
