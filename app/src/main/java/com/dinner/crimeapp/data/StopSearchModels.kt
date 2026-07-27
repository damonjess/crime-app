package com.dinner.crimeapp.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StopSearch(
    @Json(name = "type") val type: String?,
    @Json(name = "datetime") val datetime: String?,
    @Json(name = "gender") val gender: String?,
    @Json(name = "age_range") val ageRange: String?,
    @Json(name = "self_defined_ethnicity") val selfDefinedEthnicity: String?,
    @Json(name = "object_of_search") val objectOfSearch: String?,
    @Json(name = "outcome") val outcome: String?,
    @Json(name = "location") val location: StopSearchLocation?
)

@JsonClass(generateAdapter = true)
data class StopSearchLocation(
    @Json(name = "latitude") val latitude: String?,
    @Json(name = "longitude") val longitude: String?,
    @Json(name = "street") val street: Street?
)
