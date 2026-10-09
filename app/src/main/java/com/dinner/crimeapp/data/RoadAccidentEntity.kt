package com.dinner.crimeapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "road_accidents")
data class RoadAccidentEntity(
    @PrimaryKey val accidentReference: String,
    val latitude: Double,
    val longitude: Double,
    val severity: Int, // 1: Fatal, 2: Serious, 3: Slight
    val date: String,
    val numberOfVehicles: Int,
    val numberOfCasualties: Int,
    val speedLimit: Int,
    val weatherConditions: String
)
