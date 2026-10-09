package com.dinner.crimeapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object AccidentDataInitializer {

    suspend fun initializeAccidentData(context: Context, dao: RoadAccidentDao) {
        withContext(Dispatchers.IO) {
            // 1. Check if data is already loaded to prevent duplicate work
            if (dao.getCount() > 0) return@withContext

            val accidents = mutableListOf<RoadAccidentEntity>()
            
            // 2. Open the CSV file from the assets folder
            runCatching {
                context.assets.open("accidents.csv").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        
                        // Read and skip the header row
                        reader.readLine()
                        var line = reader.readLine()
                        
                        // 3. Parse each row
                        while (line != null) {
                            // Split by comma (ignoring commas inside quotes if needed, but DfT data is usually clean)
                            val tokens = line.split(",")
                            
                            if (tokens.size >= 9) {
                                try {
                                    val entity = RoadAccidentEntity(
                                        accidentReference = tokens[0].trim(),
                                        latitude = tokens[1].trim().toDouble(),
                                        longitude = tokens[2].trim().toDouble(),
                                        severity = tokens[3].trim().toInt(),
                                        date = tokens[4].trim(),
                                        numberOfVehicles = tokens[5].trim().toInt(),
                                        numberOfCasualties = tokens[6].trim().toInt(),
                                        speedLimit = tokens[7].trim().toInt(),
                                        weatherConditions = tokens[8].trim()
                                    )
                                    accidents.add(entity)
                                } catch (_: Exception) {
                                    // Silently skip any malformed rows or empty coordinates
                                }
                            }
                            
                            // 4. Batch insert every 1000 rows to keep memory usage extremely low
                            if (accidents.size >= 1000) {
                                dao.insertAccidents(accidents)
                                accidents.clear()
                            }
                            line = reader.readLine()
                        }
                        
                        // Insert any remaining records
                        if (accidents.isNotEmpty()) {
                            dao.insertAccidents(accidents)
                        }
                    }
                }
            }
        }
    }
}
