package tech.relaycorp.gateway.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import tech.relaycorp.gateway.data.model.ParcelCollection

@Dao
interface ParcelCollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ParcelCollection)

    @Delete
    suspend fun delete(message: ParcelCollection)

    @Query("SELECT * FROM ParcelCollection")
    fun getAll(): List<ParcelCollection>
}
