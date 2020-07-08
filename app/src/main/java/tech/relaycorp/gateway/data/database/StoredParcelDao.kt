package tech.relaycorp.gateway.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StoredParcel

@Dao
interface StoredParcelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: StoredParcel)

    @Delete
    suspend fun delete(message: StoredParcel)

    @Query("SELECT * FROM Parcel")
    fun observeAll(): Flow<List<StoredParcel>>

    @Query(
        """
        SELECT * FROM Parcel
        WHERE recipientLocation = :recipientLocation 
        ORDER BY creationTimeUtc ASC
        """
    )
    suspend fun listForRecipientLocation(recipientLocation: RecipientLocation): List<StoredParcel>
}
