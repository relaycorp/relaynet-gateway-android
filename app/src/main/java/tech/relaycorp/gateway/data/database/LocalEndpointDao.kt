package tech.relaycorp.gateway.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import tech.relaycorp.gateway.data.model.LocalEndpoint
import tech.relaycorp.gateway.data.model.MessageAddress

@Dao
interface LocalEndpointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(endpoint: LocalEndpoint)

    @Delete
    suspend fun delete(endpoint: LocalEndpoint)

    @Query("SELECT * FROM Endpoint WHERE address = :address LIMIT 1")
    suspend fun get(address: MessageAddress): LocalEndpoint?
}
