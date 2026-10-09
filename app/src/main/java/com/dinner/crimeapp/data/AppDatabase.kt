package com.dinner.crimeapp.data

import android.content.Context
import androidx.room.*

@Entity(tableName = "area_profiles")
data class AreaProfileEntity(
    @PrimaryKey val coordinateKey: String,
    val postcode: String,
    val adminWard: String?,
    val adminDistrict: String?,
    val lsoa: String?,
    val imdRank: Int?,
    val timestamp: Long
)

@Dao
interface AreaProfileDao {
    @Query("SELECT * FROM area_profiles WHERE coordinateKey = :key")
    suspend fun getProfile(key: String): AreaProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AreaProfileEntity)
}

@Database(
    entities = [AreaProfileEntity::class, RoadAccidentEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun areaProfileDao(): AreaProfileDao
    abstract fun roadAccidentDao(): RoadAccidentDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "crime_app_database"
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }
}
