package tech.relaycorp.gateway.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.util.Date

@Entity(
    tableName = "Parcel",
    primaryKeys = ["recipientAddress", "senderAddress", "messageId"]
)
data class StoredParcel(
    val recipientAddress: MessageAddress,
    val senderAddress: MessageAddress,
    val messageId: MessageId,
    @ColumnInfo(index = true)
    val recipientLocation: RecipientLocation,
    @ColumnInfo(index = true)
    val creationTimeUtc: Date, // in UTC
    @ColumnInfo(index = true)
    val expirationTimeUtc: Date, // in UTC
    val storagePath: String,
    val size: StorageSize
) {
    companion object {
        const val STORAGE_FOLDER = "parcels"
        const val STORAGE_PREFIX = "parcel_"
    }
}
