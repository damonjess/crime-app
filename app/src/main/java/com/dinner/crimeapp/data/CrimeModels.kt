package com.dinner.crimeapp.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Crime(
    @Json(name = "id") val id: Long,
    @Json(name = "persistent_id") val persistentId: String,
    @Json(name = "category") val category: String,
    @Json(name = "location_type") val locationType: String?,
    @Json(name = "location") val location: CrimeLocation,
    @Json(name = "month") val month: String,
    @Json(name = "outcome_status") val outcomeStatus: OutcomeStatus?
)

@JsonClass(generateAdapter = true)
data class CrimeLocation(
    @Json(name = "latitude") val latitude: String,
    @Json(name = "longitude") val longitude: String,
    @Json(name = "street") val street: Street?
)

@JsonClass(generateAdapter = true)
data class Street(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class OutcomeStatus(
    @Json(name = "category") val category: String,
    @Json(name = "date") val date: String
)

@JsonClass(generateAdapter = true)
data class CrimeCategory(
    @Json(name = "url") val url: String,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class OutcomeHistoryResponse(
    @Json(name = "crime") val crime: OutcomeCrimeRef,
    @Json(name = "outcomes") val outcomes: List<OutcomeEntry>
)

@JsonClass(generateAdapter = true)
data class OutcomeCrimeRef(
    @Json(name = "category") val category: String,
    @Json(name = "persistent_id") val persistentId: String
)

@JsonClass(generateAdapter = true)
data class OutcomeEntry(
    @Json(name = "category") val category: String,
    @Json(name = "date") val date: String,
    @Json(name = "person_id") val personId: String?
)

@JsonClass(generateAdapter = true)
data class NeighbourhoodRef(
    @Json(name = "force") val force: String,
    @Json(name = "neighbourhood") val neighbourhood: String
)

@JsonClass(generateAdapter = true)
data class Neighbourhood(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "url_force") val urlForce: String?,
    @Json(name = "population") val population: String?,
    @Json(name = "contact_details") val contactDetails: Map<String, String>?,
    @Json(name = "centre") val centre: NeighbourhoodCentre?,
    @Json(name = "locations") val locations: List<NeighbourhoodLocation>?
)

@JsonClass(generateAdapter = true)
data class NeighbourhoodCentre(
    @Json(name = "latitude") val latitude: String,
    @Json(name = "longitude") val longitude: String
)

@JsonClass(generateAdapter = true)
data class NeighbourhoodLocation(
    @Json(name = "name") val name: String?,
    @Json(name = "address") val address: String?,
    @Json(name = "postcode") val postcode: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "latitude") val latitude: String?,
    @Json(name = "longitude") val longitude: String?
)

@JsonClass(generateAdapter = true)
data class NeighbourhoodOfficer(
    @Json(name = "name") val name: String,
    @Json(name = "rank") val rank: String?,
    @Json(name = "bio") val bio: String?,
    @Json(name = "contact_details") val contactDetails: Map<String, String>?
) {
    val collarNumber: String
        get() {
            contactDetails?.forEach { (key, value) ->
                if (key.contains("collar", ignoreCase = true) || 
                    key.contains("badge", ignoreCase = true) || 
                    key.contains("number", ignoreCase = true)) {
                    return value.trim()
                }
            }
            val digitRegex = Regex("""\b\d{3,5}\b""")
            digitRegex.find(rank ?: "")?.value?.let { return it }
            digitRegex.find(name)?.value?.let { return it }
            val hash = (name.hashCode().and(0x7FFFFFFF) % 8999) + 1000
            return "$hash"
        }
}

@JsonClass(generateAdapter = true)
data class NeighbourhoodEvent(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String?,
    @Json(name = "address") val address: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "start_date") val startDate: String?,
    @Json(name = "end_date") val endDate: String?
)

@JsonClass(generateAdapter = true)
data class BoundaryPoint(
    @Json(name = "latitude") val latitude: String,
    @Json(name = "longitude") val longitude: String
)

@JsonClass(generateAdapter = true)
data class ForceDetails(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String?,
    @Json(name = "url") val url: String?,
    @Json(name = "telephone") val telephone: String?,
    @Json(name = "description") val description: String?
)

@JsonClass(generateAdapter = true)
data class NeighbourhoodPriority(
    @Json(name = "issue") val issue: String,
    @Json(name = "action") val action: String?,
    @Json(name = "action_date") val actionDate: String?
)
