package tech.relaycorp.gateway.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import tech.relaycorp.gateway.data.model.MessageAddress
import tech.relaycorp.gateway.data.model.MessageId
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.data.model.StorageSize
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

    @Query(
        """
        SELECT * FROM Parcel
        WHERE recipientAddress IN (:recipientAddresses) AND recipientLocation = :recipientLocation 
        ORDER BY creationTimeUtc ASC
        """
    )
    fun listForRecipients(
        recipientAddresses: List<MessageAddress>,
        recipientLocation: RecipientLocation
    ): Flow<List<StoredParcel>>

    @Query("SELECT SUM(Parcel.size) FROM Parcel WHERE recipientLocation = :recipientLocation")
    fun countSizeForRecipientLocation(recipientLocation: RecipientLocation): Flow<StorageSize>

    @Query(
        """
        SELECT * FROM Parcel
        WHERE recipientAddress = :recipientAddress 
            AND senderAddress = :senderAddress 
            AND messageId = :messageId
        LIMIT 1
        """
    )
    suspend fun get(
        recipientAddress: MessageAddress,
        senderAddress: MessageAddress,
        messageId: MessageId
    ): StoredParcel?
}
