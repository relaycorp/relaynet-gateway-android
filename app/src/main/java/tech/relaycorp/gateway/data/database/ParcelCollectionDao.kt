package tech.relaycorp.gateway.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.ParcelCollection
import tech.relaycorp.gateway.data.model.PrivateMessageAddress

@Dao
interface ParcelCollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ParcelCollection)

    @Delete
    suspend fun delete(message: ParcelCollection)

    @Query("SELECT * FROM ParcelCollection")
    suspend fun getAll(): List<ParcelCollection>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM ParcelCollection
            WHERE recipientAddress = :recipientAddress 
                AND senderAddress = :senderAddress 
                AND messageId = :messageId
            LIMIT 1
        )
        """
    )
    suspend fun exists(
        recipientAddress: MessageAddress,
        senderAddress: PrivateMessageAddress,
        messageId: MessageId
    ): Boolean
}
