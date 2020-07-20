package tech.relaycorp.gateway.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.util.Date

@Entity(
    tableName = "ParcelCollection",
    primaryKeys = ["recipientAddress", "senderAddress", "messageId"]
)
data class ParcelCollection(
    val recipientAddress: MessageAddress,
    val senderAddress: MessageAddress,
    val messageId: MessageId,
    @ColumnInfo(index = true)
    val creationTimeUtc: Date,
    @ColumnInfo(index = true)
    val expirationTimeUtc: Date
)
