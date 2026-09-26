package com.dinner.crimeapp.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores user's saved (favourite) locations in SharedPreferences as JSON.
 * No external dependencies — uses Android's built-in org.json.
 */
data class SavedPlace(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double
)

object SavedPlacesStore {

    private const val PREFS = "crimeapp_saved_places"
    private const val KEY = "places"

    fun load(context: Context): List<SavedPlace> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SavedPlace(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    lat = o.getDouble("lat"),
                    lng = o.getDouble("lng")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, places: List<SavedPlace>) {
        val arr = JSONArray()
        places.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("lat", p.lat)
                put("lng", p.lng)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun add(context: Context, name: String, lat: Double, lng: Double): List<SavedPlace> {
        val places = load(context).toMutableList()
        // Avoid duplicates at the same spot
        val exists = places.any { it.lat == lat && it.lng == lng }
        if (!exists) {
            places.add(
                SavedPlace(
                    id = System.currentTimeMillis().toString(),
                    name = name,
                    lat = lat,
                    lng = lng
                )
            )
            save(context, places)
        }
        return places
    }

    fun remove(context: Context, id: String): List<SavedPlace> {
        val places = load(context).filter { it.id != id }
        save(context, places)
        return places
    }
}
