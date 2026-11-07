package dev.zakyymuh.timebox.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {

    @Query("SELECT * FROM plant ORDER BY name")
    fun plantsFlow(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plant ORDER BY name")
    suspend fun plants(): List<PlantEntity>

    @Query("SELECT * FROM plant WHERE id = :id")
    suspend fun plant(id: Long): PlantEntity?

    @Query("SELECT * FROM watering")
    fun wateringsFlow(): Flow<List<WateringEntity>>

    @Query("SELECT * FROM watering")
    suspend fun waterings(): List<WateringEntity>

    /** Returns the new row id on insert, -1 on update. */
    @Upsert
    suspend fun upsertPlant(plant: PlantEntity): Long

    @Query("DELETE FROM plant WHERE id = :id")
    suspend fun deletePlant(id: Long)

    @Insert
    suspend fun insertWatering(watering: WateringEntity): Long

    @Query("DELETE FROM watering WHERE id = :id")
    suspend fun deleteWatering(id: Long)
}
