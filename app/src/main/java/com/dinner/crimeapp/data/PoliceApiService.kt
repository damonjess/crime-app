package com.dinner.crimeapp.data

import retrofit2.http.GET
import retrofit2.http.Path
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

    @GET("outcomes-for-crime/{persistentId}")
    suspend fun getOutcomeHistory(
        @Path("persistentId") persistentId: String
    ): OutcomeHistoryResponse

    @GET("stops-street")
    suspend fun getStopSearches(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("date") month: String? = null
    ): List<StopSearch>

    @GET("locate-neighbourhood")
    suspend fun locateNeighbourhood(
        @Query("q") latLng: String
    ): NeighbourhoodRef

    @GET("{force}/{neighbourhood}")
    suspend fun getNeighbourhood(
        @Path("force") force: String,
        @Path("neighbourhood") neighbourhood: String
    ): Neighbourhood

    @GET("{force}/{neighbourhood}/people")
    suspend fun getNeighbourhoodTeam(
        @Path("force") force: String,
        @Path("neighbourhood") neighbourhood: String
    ): List<NeighbourhoodOfficer>

    @GET("{force}/{neighbourhood}/events")
    suspend fun getNeighbourhoodEvents(
        @Path("force") force: String,
        @Path("neighbourhood") neighbourhood: String
    ): List<NeighbourhoodEvent>

    @GET("{force}/{neighbourhood}/boundary")
    suspend fun getNeighbourhoodBoundary(
        @Path("force") force: String,
        @Path("neighbourhood") neighbourhood: String
    ): List<BoundaryPoint>

    @GET("forces/{force}")
    suspend fun getForceDetails(
        @Path("force") force: String
    ): ForceDetails
}

