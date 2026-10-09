package com.dinner.crimeapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoadAccidentDao {
    @Query("SELECT COUNT(*) FROM road_accidents")
    suspend fun getCount(): Int

    // Only fetch accidents within the map's current visible boundaries
    @Query("""
        SELECT * FROM road_accidents 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLng AND :maxLng
    """)
    suspend fun getAccidentsInArea(
        minLat: Double, 
        maxLat: Double, 
        minLng: Double, 
        maxLng: Double
    ): List<RoadAccidentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccidents(accidents: List<RoadAccidentEntity>)
}
