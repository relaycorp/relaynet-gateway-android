package tech.relaycorp.gateway.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import tech.relaycorp.gateway.data.model.LocalEndpoint
import tech.relaycorp.gateway.data.model.MessageAddress

@Dao
interface LocalEndpointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(endpoint: LocalEndpoint)

    @Delete
    suspend fun delete(endpoint: LocalEndpoint)

    @Query("DELETE FROM Endpoint")
    suspend fun deleteAll()

    @Query("SELECT COUNT(DISTINCT Endpoint.applicationId) FROM Endpoint")
    fun countApplicationIds(): Flow<Int>

    @Query("SELECT * FROM Endpoint WHERE address = :address LIMIT 1")
    suspend fun get(address: MessageAddress): LocalEndpoint?

    @Query("SELECT * FROM Endpoint WHERE address IN (:addresses)")
    suspend fun list(addresses: List<MessageAddress>): List<LocalEndpoint>
}
