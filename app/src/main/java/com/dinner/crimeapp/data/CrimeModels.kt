package com.dinner.crimeapp.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class Crime(
    @Json(name = "id") val id: Long,
    @Json(name = "persistent_id") val persistentId: String,
    @Json(name = "category") val category: String,
    @Json(name = "location_type") val locationType: String?,
    @Json(name = "location") val location: CrimeLocation,
    @Json(name = "month") val month: String,
    @Json(name = "outcome_status") val outcomeStatus: OutcomeStatus?
)

@JsonClass(generateAdapter = false)
data class CrimeLocation(
    @Json(name = "latitude") val latitude: String,
    @Json(name = "longitude") val longitude: String,
    @Json(name = "street") val street: Street?
)

@JsonClass(generateAdapter = false)
data class Street(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = false)
data class OutcomeStatus(
    @Json(name = "category") val category: String,
    @Json(name = "date") val date: String
)

@JsonClass(generateAdapter = false)
data class CrimeCategory(
    @Json(name = "url") val url: String,
    @Json(name = "name") val name: String
)
