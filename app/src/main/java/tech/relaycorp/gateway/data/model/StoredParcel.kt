package tech.relaycorp.gateway.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.time.ZonedDateTime

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
    val creationTimeUtc: ZonedDateTime,
    @ColumnInfo(index = true)
    val expirationTimeUtc: ZonedDateTime,
    val storagePath: String,
    val size: StorageSize,
    val inCourierTransit: Boolean = false
) {
    companion object {
        const val STORAGE_FOLDER = "parcels"
        const val STORAGE_PREFIX = "parcel_"
    }
}
