package com.dinner.crimeapp.data

import retrofit2.http.GET
import retrofit2.http.Query

interface PoliceApiService {

    @GET("crimes-street/all-crime")
    suspend fun getCrimesNear(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("date") month: String? = null // "yyyy-MM", null = latest
    ): List<Crime>

    @GET("crime-categories")
    suspend fun getCategories(
        @Query("date") month: String? = null
    ): List<CrimeCategory>
}
