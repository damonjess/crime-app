package com.dinner.crimeapp.data

import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApiService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("countrycodes") countryCodes: String = "gb",
        @Query("limit") limit: Int = 8
    ): List<GeocodeResult>

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lng: Double,
        @Query("format") format: String = "json",
        @Query("zoom") zoom: Int = 16
    ): ReverseGeocodeResponse
}
