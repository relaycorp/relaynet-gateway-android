package tech.relaycorp.gateway.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import tech.relaycorp.gateway.data.model.LocalEndpoint

@Dao
interface LocalEndpointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(endpoint: LocalEndpoint)

    @Delete
    suspend fun delete(endpoint: LocalEndpoint)
}
