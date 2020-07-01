package tech.relaycorp.gateway.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.util.Date

@Entity(
    tableName = "Parcel",
    primaryKeys = ["senderAddress", "messageId"]
)
data class StoredParcel(
    @ColumnInfo(index = true)
    val recipientAddress: MessageAddress,
    val senderAddress: MessageAddress,
    val messageId: MessageId,
    val creationTimeUtc: Date, // in UTC
    @ColumnInfo(index = true)
    val expirationTimeUtc: Date, // in UTC
    val storagePath: String,
    val size: StorageSize
)
