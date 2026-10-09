package com.dinner.crimeapp.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- Models ---
@JsonClass(generateAdapter = true)
data class PostcodesResponse(
    @Json(name = "status") val status: Int,
    @Json(name = "result") val result: List<PostcodeResult>?
)

@JsonClass(generateAdapter = true)
data class PostcodeResult(
    @Json(name = "postcode") val postcode: String,
    @Json(name = "admin_ward") val adminWard: String?,
    @Json(name = "admin_district") val adminDistrict: String?,
    @Json(name = "lsoa") val lsoa: String?,
    @Json(name = "index_of_multiple_deprivation") val imdRank: Int?
)

// --- Service ---
interface PostcodesApiService {
    @GET("postcodes")
    suspend fun getAreaStats(
        @Query("lon") lon: Double,
        @Query("lat") lat: Double
    ): PostcodesResponse
}

// --- API Client ---
object PostcodesApi {
    private val moshi = Moshi.Builder().build()
    
    val service: PostcodesApiService = Retrofit.Builder()
        .baseUrl("https://api.postcodes.io/")
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PostcodesApiService::class.java)
}
