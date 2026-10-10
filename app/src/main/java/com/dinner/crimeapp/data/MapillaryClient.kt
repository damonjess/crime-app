package com.dinner.crimeapp.data

import android.util.Log
import com.dinner.crimeapp.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

// --- API Data Models ---
data class MapillarySearchResponse(val data: List<MapillaryImage>)
data class MapillaryImage(val id: String)
data class MapillaryDetailResponse(val thumb_1024_url: String)

// --- Retrofit Interface ---
interface MapillaryApi {
    @GET("images")
    suspend fun searchImages(
        @Header("Authorization") auth: String,
        @Query("fields") fields: String = "id",
        @Query("bbox") bbox: String,
        @Query("limit") limit: Int = 1
    ): MapillarySearchResponse

    @GET("{image_id}")
    suspend fun getImageDetails(
        @Path("image_id") imageId: String,
        @Header("Authorization") auth: String,
        @Query("fields") fields: String = "thumb_1024_url"
    ): MapillaryDetailResponse
}

// --- Singleton Client ---
object MapillaryClient {
    private const val BASE_URL = "https://graph.mapillary.com/"
    private val TOKEN = BuildConfig.MAPILLARY_TOKEN
    private val AUTH_HEADER get() = "OAuth $TOKEN"

    private val api: MapillaryApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MapillaryApi::class.java)
    }

    suspend fun getNearestImageUrl(lat: Double, lng: Double): String? {
        return try {
            // Widen the search radius to ~300 meters
            val minLng = lng - 0.003
            val maxLng = lng + 0.003
            val minLat = lat - 0.003
            val maxLat = lat + 0.003
            val bbox = "$minLng,$minLat,$maxLng,$maxLat"

            // 1. Find the nearest image ID
            val searchRes = api.searchImages(auth = AUTH_HEADER, bbox = bbox)
            val firstImageId = searchRes.data.firstOrNull()?.id ?: return null

            // 2. Fetch the actual thumbnail URL
            val detailRes = api.getImageDetails(imageId = firstImageId, auth = AUTH_HEADER)
            detailRes.thumb_1024_url
        } catch (e: Exception) {
            Log.e("Mapillary", "Network or data error fetching image", e)
            null
        }
    }
}
